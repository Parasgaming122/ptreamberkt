package com.streambert.app.data.api

import com.streambert.app.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap

private const val ANILIST_API = "https://graphql.anilist.co"
private const val ANILIST_CACHE_TTL = 7 * 24 * 60 * 60 * 1000L

private data class AniListCacheEntry(val data: AniListMedia?, val ts: Long)
private val anilistCache = ConcurrentHashMap<String, AniListCacheEntry>()

private val anilistClient = OkHttpClient.Builder().build()

object AniListApi {
    private val gson = Gson()

    private val QUERY = """
query (${'$'}search: String, ${'$'}type: MediaType) {
  Media(search: ${'$'}search, type: ${'$'}type, sort: SEARCH_MATCH) {
    id
    idMal
    title { romaji english native }
    description(asHtml: false)
    coverImage { extraLarge large }
    bannerImage
    genres
    averageScore
    episodes
    status
    season
    seasonYear
    studios(isMain: true) { nodes { name } }
    startDate { year month }
    relations {
      edges {
        relationType
        node {
          id
          type
          format
          title { romaji english }
          episodes
          startDate { year month }
          seasonYear
        }
      }
    }
  }
}"""

    suspend fun fetchAnilistData(title: String, type: String = "ANIME", tmdbId: Int? = null): AniListMedia? = withContext(Dispatchers.IO) {
        val cacheKey = tmdbId?.let { "${type}__tmdb_$it" } ?: "${type}__${title.lowercase().trim()}"

        // Check cache
        anilistCache[cacheKey]?.let { entry ->
            if (System.currentTimeMillis() - entry.ts <= ANILIST_CACHE_TTL) {
                val cached = entry.data
                if (cached != null) {
                    val titles = listOfNotNull(cached.title.romaji, cached.title.english, cached.title.native).map { it.lowercase() }
                    val search = title.lowercase()
                    val mismatch = titles.isNotEmpty() && !titles.any { it.contains(search) || search.contains(it) }
                    if (!mismatch) return@withContext cached
                    else anilistCache.remove(cacheKey)
                } else return@withContext null
            } else anilistCache.remove(cacheKey)
        }

        try {
            val body = gson.toJson(mapOf("query" to QUERY, "variables" to mapOf("search" to title, "type" to type)))
            val request = Request.Builder()
                .url(ANILIST_API)
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("Accept", "application/json")
                .build()

            val response = anilistClient.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext null

            val obj = JsonParser.parseString(json).asJsonObject
            val mediaObj = obj.getAsJsonObject("data")?.getAsJsonObject("Media")
            val data = gson.fromJson(mediaObj, AniListMedia::class.java)

            anilistCache[cacheKey] = AniListCacheEntry(data, System.currentTimeMillis())
            data
        } catch (_: Exception) {
            anilistCache[cacheKey]?.data
        }
    }

    fun buildAnilistSeasons(anilistData: AniListMedia?): List<AniListSeason>? {
        if (anilistData == null) return null

        val main = AniListSeason(
            seasonNum = 1,
            id = anilistData.id,
            title = anilistData.title.english ?: anilistData.title.romaji ?: anilistData.title.native ?: "",
            episodes = anilistData.episodes,
            year = anilistData.startDate?.year ?: anilistData.seasonYear ?: 9999,
            month = anilistData.startDate?.month ?: 0,
        )

        val sequels = anilistData.relations.edges
            .filter { it.relationType == "SEQUEL" && it.node.type == "ANIME" && (it.node.format == "TV" || it.node.format == "TV_SHORT") }
            .map {
                AniListSeason(
                    seasonNum = 0, // will be assigned below
                    id = it.node.id,
                    title = it.node.title.english ?: it.node.title.romaji ?: "",
                    episodes = it.node.episodes,
                    year = it.node.startDate?.year ?: it.node.seasonYear ?: 9999,
                    month = it.node.startDate?.month ?: 0,
                )
            }

        return (listOf(main) + sequels)
            .sortedWith(compareBy({ it.year }, { it.month }))
            .mapIndexed { index, season -> season.copy(seasonNum = index + 1) }
    }

    fun cleanDescription(desc: String?): String {
        if (desc.isNullOrEmpty()) return ""
        var clean = desc.split("<").mapIndexed { i, chunk ->
            if (i == 0) chunk else chunk.indexOf(">").let { idx -> if (idx >= 0) chunk.substring(idx + 1) else chunk }
        }.joinToString("").replace(">", "")
        clean = clean.replace(Regex("\\(Source:[^)]*\\)", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("\\bNote:[^\\n]*", RegexOption.IGNORE_CASE), "")
        return clean.trim()
    }
}