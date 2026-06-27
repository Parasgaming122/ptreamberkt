package com.streambert.app.data.api

import com.streambert.app.data.model.PlayerSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

// ── Player source definitions (matching Streambert exactly) ──
object PlayerSources {
    val all: List<PlayerSource> = listOf(
        PlayerSource(
            id = "videasy",
            label = "Videasy",
            supportsProgress = true,
            colorParam = "color",
            extraParams = mapOf("overlay" to "true"),
        ),
        PlayerSource(
            id = "vidsrc",
            label = "VidSrc",
            supportsProgress = true,
            langParam = "ds_lang",
        ),
        PlayerSource(
            id = "vidking",
            label = "Vidking",
            supportsProgress = true,
            colorParam = "color",
            extraParams = mapOf("autoPlay" to "true"),
        ),
        PlayerSource(
            id = "allmanga",
            label = "AllManga",
            supportsProgress = true,
            isAsync = true,
        ),
    )

    val nonAsyncSources get() = all.filter { !it.isAsync }

    const val ANIME_DEFAULT = "allmanga"
    const val NON_ANIME_DEFAULT = "vidking"
}

// ── Build streaming URL ──
fun getSourceUrl(
    sourceId: String,
    type: String,
    tmdbId: Int,
    season: Int? = null,
    episode: Int? = null,
    accentColor: String? = null,
    subtitleLang: String? = null,
): String {
    val source = PlayerSources.all.find { it.id == sourceId } ?: PlayerSources.all.first()

    val baseUrl = when {
        type == "movie" -> when (sourceId) {
            "videasy" -> "https://player.videasy.to/movie/$tmdbId"
            "vidsrc" -> "https://vsembed.su/embed/movie/$tmdbId"
            "vidking" -> "https://www.vidking.net/embed/movie/$tmdbId"
            else -> "https://www.vidking.net/embed/movie/$tmdbId"
        }
        season != null && episode != null -> when (sourceId) {
            "videasy" -> "https://player.videasy.to/tv/$tmdbId/$season/$episode"
            "vidsrc" -> "https://vsembed.su/embed/tv/$tmdbId/$season/$episode"
            "vidking" -> "https://www.vidking.net/embed/tv/$tmdbId/$season/$episode"
            else -> "https://www.vidking.net/embed/tv/$tmdbId/$season/$episode"
        }
        else -> "https://www.vidking.net/embed/$type/$tmdbId"
    }

    val url = StringBuilder(baseUrl)

    // Add source default params
    source.extraParams.forEach { (k, v) ->
        url.append("&$k=${URLEncoder.encode(v, "UTF-8")}")
    }

    // Accent color
    if (!accentColor.isNullOrBlank() && source.colorParam != null) {
        val hex = accentColor.removePrefix("#")
        url.append("&${source.colorParam}=$hex")
    }

    // Subtitle lang
    if (!subtitleLang.isNullOrBlank() && source.langParam != null) {
        url.append("&${source.langParam}=$subtitleLang")
    }

    return url.toString().replaceFirst("&", "?")
}

// ── Anime detection (genre 16 + Japanese origin) ──
fun isAnimeContent(genreIds: List<Int>, originalLanguage: String, originCountry: List<String>): Boolean {
    val hasAnimation = genreIds.contains(16)
    return hasAnimation && (originalLanguage == "ja" || originCountry.contains("JP"))
}

// ── VidSrc scraper: extract m3u8 from embed page ──
private val scraperClient = OkHttpClient.Builder().followRedirects(true).build()

suspend fun scrapeVidSrcStreamUrl(tmdbId: Int, type: String, season: Int?, episode: Int?): String? =
    withContext(Dispatchers.IO) {
        try {
            val path = if (type == "movie") "movie/$tmdbId"
            else "tv/$tmdbId/${season ?: 1}/${episode ?: 1}"
            val url = "https://vsembed.su/embed/$path"

            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
                .build()

            val response = scraperClient.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext null

            // Extract first m3u8 URL from the page
            val m3u8Regex = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
            m3u8Regex.find(html)?.value
        } catch (_: Exception) {
            null
        }
    }