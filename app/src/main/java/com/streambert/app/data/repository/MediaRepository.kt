package com.streambert.app.data.repository

import android.content.Context
import com.streambert.app.data.api.*
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.model.*
import kotlinx.coroutines.*

object MediaRepository {

    // ── Home page data ──
    suspend fun getTrendingMovies(): List<MediaItem> {
        return TmdbApi.getTrendingMovies().map { it.copy(mediaType = "movie") }
    }

    suspend fun getTrendingTv(): List<MediaItem> {
        return TmdbApi.getTrendingTv().map { it.copy(mediaType = "tv") }
    }

    suspend fun getTopRated(): List<MediaItem> {
        val movies = TmdbApi.getTopRatedMovies().map { it.copy(mediaType = "movie") }
        val tv = TmdbApi.getTopRatedTv().map { it.copy(mediaType = "tv") }
        return interleave(movies, tv)
    }

    suspend fun getRecommended(ctx: Context): List<MediaItem> {
        val history = Prefs.getHistory(ctx)
        if (history.isEmpty()) return emptyList()

        val sources = history.take(5)
        val watchedIds = history.map { "${it.mediaType}_${it.id}" }.toSet()

        val allResults = sources.mapNotNull { entry ->
            val type = if (entry.mediaType == "tv") "tv" else "movie"
            try {
                val recs = if (type == "tv") TmdbApi.getTvRecommendations(entry.id)
                else TmdbApi.getMovieRecommendations(entry.id)
                if (recs.isNotEmpty()) recs.map { it.copy(mediaType = type) }
                else {
                    val similar = if (type == "tv") TmdbApi.getTvSimilar(entry.id)
                    else TmdbApi.getMovieSimilar(entry.id)
                    similar.map { it.copy(mediaType = type) }
                }
            } catch (_: Exception) { emptyList() }
        }

        // Interleave for variety, dedup
        val merged = mutableListOf<MediaItem>()
        val maxLen = allResults.maxOfOrNull { it.size } ?: 0
        for (i in 0 until maxLen) {
            for (arr in allResults) {
                if (i < arr.size) merged.add(arr[i])
            }
        }

        val seen = mutableSetOf<String>()
        return merged.filter { item ->
            val key = "${item.mediaType}_${item.id}"
            if (seen.contains(key) || watchedIds.contains(key)) false
            else { seen.add(key); true }
        }.take(20)
    }

    suspend fun getContinueWatching(ctx: Context): List<MediaItem> {
        val progress = Prefs.getProgress(ctx)
        val history = Prefs.getHistory(ctx)
        val watched = Prefs.getWatched(ctx)

        return history.filter { entry ->
            val key = if (entry.mediaType == "tv") "tv_${entry.id}_s${entry.season ?: 1}e${entry.episode ?: 1}"
            else "movie_${entry.id}"
            val pct = progress[key] ?: 0f
            pct > 2f && pct < 98f && watched[key] != true
        }.map { entry ->
            MediaItem(
                id = entry.id,
                title = entry.title,
                posterPath = entry.posterPath,
                mediaType = entry.mediaType,
                season = entry.season,
                episode = entry.episode,
                episodeName = entry.episodeName,
                watchedAt = entry.watchedAt,
            )
        }
    }

    // ── Search ──
    suspend fun search(query: String): List<MediaItem> {
        return TmdbApi.search(query)
    }

    // ── Detail ──
    suspend fun getMovieDetail(id: Int): MovieDetail = TmdbApi.getMovieDetail(id)

    suspend fun getTvDetail(id: Int): TvDetail = TmdbApi.getTvDetail(id)

    suspend fun getTvSeasonDetail(showId: Int, season: Int): TvSeason = TmdbApi.getTvSeason(showId, season)

    // ── Trailers ──
    suspend fun getMovieTrailers(id: Int): List<TmdbVideo> {
        val results = TmdbApi.getMovieVideos(id).results
        return results.filter { it.site == "YouTube" && it.type == "Trailer" }
    }

    suspend fun getTvTrailers(id: Int): List<TmdbVideo> {
        val results = TmdbApi.getTvVideos(id).results
        return results.filter { it.site == "YouTube" && it.type == "Trailer" }
    }

    // ── Age ratings ──
    suspend fun getMovieRating(id: Int, countryCode: String): AgeRating {
        try {
            val data = TmdbApi.getMovieReleaseDates(id)
            val codesToTry = if (countryCode != "US") listOf(countryCode, "US") else listOf("US")
            for (code in codesToTry) {
                val entry = data.results.find { it.iso_3166_1 == code } ?: continue
                val sorted = entry.release_dates.sortedByDescending { it.type }
                val certEntry = sorted.find { it.certification.isNotBlank() } ?: continue
                val cert = certEntry.certification.trim()
                return AgeRating(cert, certToMinAge(cert, code))
            }
        } catch (_: Exception) {}
        return AgeRating()
    }

    suspend fun getTvRating(id: Int, countryCode: String): AgeRating {
        try {
            val data = TmdbApi.getTvContentRatings(id)
            val codesToTry = if (countryCode != "US") listOf(countryCode, "US") else listOf("US")
            for (code in codesToTry) {
                val entry = data.results.find { it.iso_3166_1 == code } ?: continue
                if (entry.rating.isNotBlank()) {
                    val cert = entry.rating.trim()
                    return AgeRating(cert, certToMinAge(cert, code))
                }
            }
        } catch (_: Exception) {}
        return AgeRating()
    }

    // ── AniList ──
    suspend fun fetchAnilistData(title: String, type: String = "ANIME", tmdbId: Int? = null): AniListMedia? {
        return AniListApi.fetchAnilistData(title, type, tmdbId)
    }

    fun buildAnilistSeasons(data: AniListMedia?): List<AniListSeason>? {
        return AniListApi.buildAnilistSeasons(data)
    }

    // ── Streaming URL ──
    fun buildSourceUrl(
        sourceId: String, type: String, tmdbId: Int,
        season: Int? = null, episode: Int? = null,
        accentColor: String? = null, subtitleLang: String? = null,
    ): String {
        return getSourceUrl(sourceId, type, tmdbId, season, episode, accentColor, subtitleLang)
    }

    suspend fun scrapeVidSrcStreamUrl(tmdbId: Int, type: String, season: Int?, episode: Int?): String? {
        return scrapeVidSrcStreamUrl(tmdbId, type, season, episode)
    }

    // ── Progress & History ──
    suspend fun saveProgress(ctx: Context, key: String, percentage: Float) {
        Prefs.updateProgress(ctx, key, percentage)
    }

    suspend fun getProgress(ctx: Context, key: String): Float {
        return Prefs.getProgress(ctx)[key] ?: 0f
    }

    suspend fun markWatched(ctx: Context, key: String) {
        Prefs.markWatched(ctx, key)
    }

    suspend fun markUnwatched(ctx: Context, key: String) {
        Prefs.markUnwatched(ctx, key)
    }

    suspend fun addHistoryEntry(ctx: Context, entry: HistoryEntry) {
        if (Prefs.isHistoryEnabled(ctx)) {
            Prefs.addHistoryEntry(ctx, entry)
        }
    }

    // ── Saved / Watchlist ──
    suspend fun toggleSaved(ctx: Context, item: SavedItem) {
        Prefs.toggleSaved(ctx, item)
    }

    suspend fun getSaved(ctx: Context): Map<String, SavedItem> = Prefs.getSaved(ctx)

    suspend fun isSaved(ctx: Context, id: Int, mediaType: String): Boolean = Prefs.isSaved(ctx, id, mediaType)

    // ── Configure API ──
    fun configureApi(apiKey: String, lang: String) {
        TmdbApi.configure(apiKey, lang)
    }

    fun clearCache() {
        TmdbApi.clearCache()
    }

    // ── Utility ──
    private fun <T> interleave(a: List<T>, b: List<T>): List<T> {
        val result = mutableListOf<T>()
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            if (i < a.size) result.add(a[i])
            if (i < b.size) result.add(b[i])
        }
        return result
    }
}