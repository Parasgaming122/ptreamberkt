package com.streambert.app.data.api

import com.streambert.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// ── TMDB Base ──
private const val TMDB_BASE = "https://api.themoviedb.org/3"
private const val IMG_BASE = "https://image.tmdb.org/t/p"

fun imgUrl(path: String?, size: String = "w500"): String? {
    return if (path.isNullOrEmpty()) null else "$IMG_BASE/$size$path"
}

// ── Retrofit Interface ──
interface TmdbApiService {
    @GET("trending/movie/week")
    suspend fun trendingMovies(@Query("language") lang: String): TmdbResponse<MediaItem>

    @GET("trending/tv/week")
    suspend fun trendingTv(@Query("language") lang: String): TmdbResponse<MediaItem>

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") lang: String,
        @Query("page") page: Int = 1,
    ): TmdbResponse<MediaItem>

    @GET("movie/{id}")
    suspend fun movieDetails(
        @Path("id") id: Int,
        @Query("language") lang: String,
    ): MovieDetail

    @GET("tv/{id}")
    suspend fun tvDetails(
        @Path("id") id: Int,
        @Query("language") lang: String,
    ): TvDetail

    @GET("tv/{id}/season/{season}")
    suspend fun tvSeasonDetails(
        @Path("id") id: Int,
        @Path("season") season: Int,
        @Query("language") lang: String,
    ): TvSeason

    @GET("movie/{id}/videos")
    suspend fun movieVideos(@Path("id") id: Int, @Query("language") lang: String): VideoResults

    @GET("tv/{id}/videos")
    suspend fun tvVideos(@Path("id") id: Int, @Query("language") lang: String): VideoResults

    @GET("movie/{id}/release_dates")
    suspend fun movieReleaseDates(@Path("id") id: Int): MovieReleaseDatesResponse

    @GET("tv/{id}/content_ratings")
    suspend fun tvContentRatings(@Path("id") id: Int): TvContentRatingsResponse

    @GET("movie/{id}/recommendations")
    suspend fun movieRecommendations(
        @Path("id") id: Int,
        @Query("language") lang: String,
    ): TmdbResponse<MediaItem>

    @GET("tv/{id}/recommendations")
    suspend fun tvRecommendations(
        @Path("id") id: Int,
        @Query("language") lang: String,
    ): TmdbResponse<MediaItem>

    @GET("movie/{id}/similar")
    suspend fun movieSimilar(
        @Path("id") id: Int,
        @Query("language") lang: String,
    ): TmdbResponse<MediaItem>

    @GET("tv/{id}/similar")
    suspend fun tvSimilar(
        @Path("id") id: Int,
        @Query("language") lang: String,
    ): TmdbResponse<MediaItem>

    @GET("movie/top_rated")
    suspend fun topRatedMovies(@Query("language") lang: String, @Query("page") page: Int = 1): TmdbResponse<MediaItem>

    @GET("tv/top_rated")
    suspend fun topRatedTv(@Query("language") lang: String, @Query("page") page: Int = 1): TmdbResponse<MediaItem>

    @GET("tv/episode_group/{groupId}")
    suspend fun episodeGroup(@Path("groupId") groupId: String): EpisodeGroupResponse

    @GET("configuration")
    suspend fun configuration(): Any
}

// Extra response types for ratings
data class MovieReleaseDatesResponse(val results: List<ReleaseDateResult> = emptyList())
data class ReleaseDateResult(val iso_3166_1: String = "", val release_dates: List<ReleaseDate> = emptyList())
data class ReleaseDate(val certification: String = "", val type: Int = 0)
data class TvContentRatingsResponse(val results: List<ContentRating> = emptyList())
data class ContentRating(val iso_3166_1: String = "", val rating: String = "")
data class EpisodeGroupResponse(val id: String = "", val name: String = "", val groups: List<EpisodeGroupDef> = emptyList(), val episodes: List<EpisodeGroupEpisode> = emptyList())
data class EpisodeGroupDef(val id: String = "", val name: String = "", val order: Int = 0, val locked: Boolean = false, val type: Int = 0)

// ── In-memory cache with TTL ──
private data class CacheEntry<T>(val data: T, val expiresAt: Long)
private val tmdbCache = ConcurrentHashMap<String, CacheEntry<Any>>()
private const val CACHE_TTL = 5 * 60 * 1000L // 5 minutes
private const val MAX_CACHE_SIZE = 80

// Concurrency limiter (max 4 in-flight TMDB requests)
private val semaphore = Semaphore(4)

// ── TmdbApi singleton ──
object TmdbApi {
    private var currentApiKey: String = ""
    private var currentLang: String = "en-US"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TMDB_BASE)
            .client(okHttpClient.newBuilder().addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $currentApiKey")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val service: TmdbApiService by lazy { retrofit.create(TmdbApiService::class.java) }

    fun configure(apiKey: String, lang: String) {
        currentApiKey = apiKey
        currentLang = lang
    }

    private fun cacheKey(path: String): String = "$currentApiKey|$currentLang|$path"

    private inline fun <reified T> getCached(key: String): T? {
        val entry = tmdbCache[key] ?: return null
        if (System.currentTimeMillis() < entry.expiresAt) {
            @Suppress("UNCHECKED_CAST")
            return entry.data as? T
        }
        tmdbCache.remove(key)
        return null
    }

    private fun evictStale() {
        if (tmdbCache.size > MAX_CACHE_SIZE) {
            val now = System.currentTimeMillis()
            val toRemove = tmdbCache.entries.filter { now >= it.value.expiresAt }.map { it.key }
            toRemove.forEach { tmdbCache.remove(it) }
        }
    }

    private fun <T> putCache(key: String, data: T) {
        tmdbCache[key] = CacheEntry(data as Any, System.currentTimeMillis() + CACHE_TTL)
        evictStale()
    }

    fun clearCache() {
        tmdbCache.clear()
    }

    private suspend inline fun <T> withThrottle(crossinline block: suspend () -> T): T {
        return semaphore.withPermit { block() }
    }

    suspend fun getTrendingMovies(): List<MediaItem> = withThrottle {
        val key = cacheKey("trending/movie/week")
        getCached<List<MediaItem>>(key) ?: run {
            val data = service.trendingMovies(currentLang).results
            putCache(key, data)
            data
        }
    }

    suspend fun getTrendingTv(): List<MediaItem> = withThrottle {
        val key = cacheKey("trending/tv/week")
        getCached<List<MediaItem>>(key) ?: run {
            val data = service.trendingTv(currentLang).results
            putCache(key, data)
            data
        }
    }

    suspend fun search(query: String): List<MediaItem> = withThrottle {
        service.searchMulti(query, currentLang).results
            .filter { it.mediaType != "person" }
            .take(12)
    }

    suspend fun getMovieDetail(id: Int): MovieDetail = withThrottle {
        val key = cacheKey("movie/$id")
        getCached<MovieDetail>(key) ?: run {
            val data = service.movieDetails(id, currentLang)
            putCache(key, data)
            data
        }
    }

    suspend fun getTvDetail(id: Int): TvDetail = withThrottle {
        val key = cacheKey("tv/$id")
        getCached<TvDetail>(key) ?: run {
            val data = service.tvDetails(id, currentLang)
            putCache(key, data)
            data
        }
    }

    suspend fun getTvSeason(id: Int, season: Int): TvSeason = withThrottle {
        val key = cacheKey("tv/$id/season/$season")
        getCached<TvSeason>(key) ?: run {
            val data = service.tvSeasonDetails(id, season, currentLang)
            putCache(key, data)
            data
        }
    }

    suspend fun getMovieVideos(id: Int): VideoResults = withThrottle {
        service.movieVideos(id, currentLang)
    }

    suspend fun getTvVideos(id: Int): VideoResults = withThrottle {
        service.tvVideos(id, currentLang)
    }

    suspend fun getMovieReleaseDates(id: Int): MovieReleaseDatesResponse = withThrottle {
        service.movieReleaseDates(id)
    }

    suspend fun getTvContentRatings(id: Int): TvContentRatingsResponse = withThrottle {
        service.tvContentRatings(id)
    }

    suspend fun getMovieRecommendations(id: Int): List<MediaItem> = withThrottle {
        service.movieRecommendations(id, currentLang).results
    }

    suspend fun getTvRecommendations(id: Int): List<MediaItem> = withThrottle {
        service.tvRecommendations(id, currentLang).results
    }

    suspend fun getMovieSimilar(id: Int): List<MediaItem> = withThrottle {
        service.movieSimilar(id, currentLang).results
    }

    suspend fun getTvSimilar(id: Int): List<MediaItem> = withThrottle {
        service.tvSimilar(id, currentLang).results
    }

    suspend fun getTopRatedMovies(): List<MediaItem> = withThrottle {
        val key = cacheKey("movie/top_rated")
        getCached<List<MediaItem>>(key) ?: run {
            val data = service.topRatedMovies(currentLang).results.take(8)
            putCache(key, data)
            data
        }
    }

    suspend fun getTopRatedTv(): List<MediaItem> = withThrottle {
        val key = cacheKey("tv/top_rated")
        getCached<List<MediaItem>>(key) ?: run {
            val data = service.topRatedTv(currentLang).results.take(8)
            putCache(key, data)
            data
        }
    }

    suspend fun getEpisodeGroup(groupId: String): EpisodeGroupResponse = withThrottle {
        service.episodeGroup(groupId)
    }
}