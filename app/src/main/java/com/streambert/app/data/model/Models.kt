package com.streambert.app.data.model

import com.google.gson.annotations.SerializedName

// ── Unified media item (used in lists, search, history, etc.) ──
data class MediaItem(
    val id: Int,
    val title: String = "",
    val name: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val overview: String = "",
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Float = 0f,
    val genreIds: List<Int> = emptyList(),
    val originalLanguage: String = "",
    val originCountry: List<String> = emptyList(),
    val mediaType: String = "movie",
    // Extended fields
    val year: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val episodeName: String? = null,
    val watchedAt: Long? = null,
) {
    val displayTitle: String get() = title.ifEmpty { name }
    val displayYear: String get() = (releaseDate ?: firstAirDate ?: "").take(4)
    val isTv: Boolean get() = mediaType == "tv"
    val isUnreleased: Boolean
        get() {
            val raw = releaseDate ?: firstAirDate ?: return false
            return try {
                val parts = raw.split("-")
                if (parts.size >= 3) {
                    val y = parts[0].toIntOrNull() ?: return false
                    val m = parts[1].toIntOrNull()?.minus(1) ?: return false
                    val d = parts[2].toIntOrNull() ?: return false
                    val release = java.util.Calendar.getInstance().apply {
                        set(y, m, d)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    release.timeInMillis > System.currentTimeMillis()
                } else false
            } catch (_: Exception) { false }
        }
}

// ── Common detail interface (shared between MovieDetail & TvDetail) ──
interface DetailCommon {
    val displayTitle: String
    val displayYear: String
    val backdropPath: String?
    val posterPath: String?
    val overview: String
    val voteAverage: Float
    val genres: List<Genre>
    val originalLanguage: String
    val originCountry: List<String>
}

// ── Detailed movie info ──
data class MovieDetail(
    val id: Int,
    val title: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val overview: String = "",
    val releaseDate: String? = null,
    val voteAverage: Float = 0f,
    val voteCount: Int = 0,
    val runtime: Int = 0,
    override val genres: List<Genre> = emptyList(),
    override val originalLanguage: String = "",
    override val originCountry: List<String> = emptyList(),
    val spokenLanguages: List<SpokenLanguage> = emptyList(),
    val tagline: String? = null,
    val status: String? = null,
    val budget: Long = 0,
    val revenue: Long = 0,
    val belongsToCollection: CollectionInfo? = null,
    val videos: VideoResults? = null,
) : DetailCommon {
    override val displayTitle: String get() = title
    override val displayYear: String get() = (releaseDate ?: "").take(4)
}

// ── Detailed TV show info ──
data class TvDetail(
    val id: Int,
    val name: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val overview: String = "",
    val firstAirDate: String? = null,
    val lastAirDate: String? = null,
    val voteAverage: Float = 0f,
    val voteCount: Int = 0,
    val episodeRunTime: List<Int> = emptyList(),
    override val genres: List<Genre> = emptyList(),
    override val originalLanguage: String = "",
    override val originCountry: List<String> = emptyList(),
    val numberOfSeasons: Int = 0,
    val numberOfEpisodes: Int = 0,
    val status: String? = null,
    val tagline: String? = null,
    val seasons: List<TvSeason> = emptyList(),
    val lastEpisodeToAir: TvEpisodeInfo? = null,
    val nextEpisodeToAir: TvEpisodeInfo? = null,
    val videos: VideoResults? = null,
    val createdBy: List<CreatedBy> = emptyList(),
) : DetailCommon {
    override val displayTitle: String get() = name
    override val displayYear: String get() = (firstAirDate ?: "").take(4)
}

// ── Supporting types ──
data class Genre(
    val id: Int,
    val name: String,
)

data class SpokenLanguage(
    val englishName: String = "",
    val iso6391: String = "",
    val name: String = "",
)

data class CollectionInfo(
    val id: Int,
    val name: String,
    val posterPath: String? = null,
    val backdropPath: String? = null,
)

data class CreatedBy(
    val id: Int,
    val name: String = "",
    val profilePath: String? = null,
)

data class VideoResults(
    val results: List<TmdbVideo> = emptyList(),
)

data class TmdbVideo(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = "",
    @SerializedName("official")
    val isOfficial: Boolean = false,
)

// ── TV Season ──
data class TvSeason(
    val id: Int,
    val seasonNumber: Int,
    val name: String = "",
    val overview: String = "",
    val posterPath: String? = null,
    val episodeCount: Int = 0,
    val airDate: String? = null,
    val episodes: List<TvEpisode> = emptyList(),
)

// ── TV Episode ──
data class TvEpisode(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String = "",
    val overview: String = "",
    val airDate: String? = null,
    val stillPath: String? = null,
    val voteAverage: Float = 0f,
    val voteCount: Int = 0,
    val runtime: Int? = null,
    val showId: Int? = null,
)

data class TvEpisodeInfo(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String = "",
    val airDate: String? = null,
    val overview: String = "",
    val stillPath: String? = null,
)

// ── TMDB Paginated Response ──
data class TmdbResponse<T>(
    val page: Int = 1,
    val results: List<T> = emptyList(),
    val totalPages: Int = 0,
    val totalResults: Int = 0,
)

// ── AniList data ──
data class AniListMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: AniListTitle = AniListTitle(),
    val description: String? = null,
    val coverImage: AniListCoverImage = AniListCoverImage(),
    val bannerImage: String? = null,
    val genres: List<String> = emptyList(),
    val averageScore: Int? = null,
    val episodes: Int? = null,
    val status: String? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val studios: AniListStudios = AniListStudios(),
    val startDate: AniListDate? = null,
    val relations: AniListRelations = AniListRelations(),
) {
    val bestTitle: String
        get() = title.english ?: title.romaji ?: title.native ?: ""
}

data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
)

data class AniListCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
)

data class AniListStudios(
    val nodes: List<AniListStudioNode> = emptyList(),
)

data class AniListStudioNode(
    val name: String = "",
    val isAnimationStudio: Boolean = false,
)

data class AniListDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
)

data class AniListRelations(
    val edges: List<AniListRelationEdge> = emptyList(),
)

data class AniListRelationEdge(
    val relationType: String = "",
    val node: AniListRelationNode = AniListRelationNode(),
)

data class AniListRelationNode(
    val id: Int = 0,
    val type: String = "",
    val format: String = "",
    val title: AniListTitle = AniListTitle(),
    val episodes: Int? = null,
    val startDate: AniListDate? = null,
    val seasonYear: Int? = null,
)

// ── Player sources ──
data class PlayerSource(
    val id: String,
    val label: String,
    val supportsProgress: Boolean = true,
    val isAsync: Boolean = false,
    val colorParam: String? = null,
    val langParam: String? = null,
    val extraParams: Map<String, String> = emptyMap(),
)

// ── Age rating ──
data class AgeRating(
    val cert: String? = null,
    val minAge: Int? = null,
)

// ── Watch history entry ──
data class HistoryEntry(
    val id: Int,
    val title: String = "",
    val posterPath: String? = null,
    val mediaType: String = "movie",
    val season: Int? = null,
    val episode: Int? = null,
    val episodeName: String? = null,
    val watchedAt: Long = System.currentTimeMillis(),
)

// ── Saved/watchlist entry ──
data class SavedItem(
    val id: Int,
    val title: String = "",
    val posterPath: String? = null,
    val mediaType: String = "movie",
    val voteAverage: Float = 0f,
    val year: String = "",
)

// ── AniList season chain ──
data class AniListSeason(
    val seasonNum: Int,
    val id: Int,
    val title: String = "",
    val episodes: Int? = null,
    val year: Int = 9999,
    val month: Int = 0,
)

// ── Episode group ──
data class EpisodeGroup(
    val id: String,
    val name: String = "",
    val type: Int = 0,
    val episodes: List<EpisodeGroupEpisode> = emptyList(),
)

data class EpisodeGroupEpisode(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String = "",
    val overview: String = "",
    val airDate: String? = null,
    val stillPath: String? = null,
    val order: Int = 0,
)

// ── Download entry ──
data class DownloadEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val tmdbId: Int = 0,
    val mediaType: String = "movie",
    val season: Int? = null,
    val episode: Int? = null,
    val posterPath: String? = null,
    val filePath: String = "",
    val status: String = "downloading",
    val size: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)