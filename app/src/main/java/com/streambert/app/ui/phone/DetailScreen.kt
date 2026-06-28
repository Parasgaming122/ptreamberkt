package com.streambert.app.ui.phone

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streambert.app.data.api.*
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.model.*
import com.streambert.app.data.repository.MediaRepository
import com.streambert.app.ui.navigation.Routes
import com.streambert.app.player.VideoViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── Detail Screen ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(navController: NavController, mediaId: Int, mediaType: String) {
    val ctx = LocalContext.current
    val viewModel: DetailViewModel = viewModel()
    val detail by viewModel.detail
    val episodes by viewModel.episodes
    val trailers by viewModel.trailers
    val anilistData by viewModel.anilistData
    val loading by viewModel.loading
    val isSaved by viewModel.isSaved
    val progress by viewModel.progress
    val watched by viewModel.watched

    LaunchedEffect(mediaId, mediaType) {
        viewModel.load(ctx, mediaId, mediaType)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn {
                // Backdrop
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        AsyncImage(
                            model = imgUrl(detail?.backdropPath, "original"),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent, MaterialTheme.colorScheme.background),
                                    endY = 500f,
                                )
                            )
                        )
                        // Back button
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                }

                // Poster + Title + Info
                item {
                    Row(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-40).dp)) {
                        AsyncImage(
                            model = imgUrl(detail?.posterPath, "w342"),
                            contentDescription = null,
                            modifier = Modifier
                                .width(120.dp)
                                .height(180.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f).align(Alignment.Bottom)) {
                            Text(
                                text = detail?.displayTitle ?: "",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${detail?.voteAverage ?: 0f}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(detail?.displayYear ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                val movieDetail = detail as? MovieDetail
                                if (movieDetail != null && movieDetail.runtime > 0) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("${movieDetail.runtime} min", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                }
                            }
                            // Genres
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (detail?.genres ?: emptyList()).take(3).forEach { genre ->
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                    ) {
                                        Text(genre.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Action buttons
                item {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Play button
                        Button(
                            onClick = {
                                viewModel.play(navController, ctx, mediaId, mediaType)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play")
                        }
                        // Trailer button
                        if (trailers.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/watch?v=${trailers.first().key}"))
                                    ctx.startActivity(intent)
                                },
                            ) {
                                Icon(Icons.Default.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Trailer")
                            }
                        }
                        // Save/Bookmark
                        OutlinedButton(
                            onClick = { viewModel.toggleSave(ctx, mediaId, mediaType) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                            ),
                        ) {
                            Icon(
                                if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Overview
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text("Overview", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = detail?.overview ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp,
                        )
                    }
                }

                // AniList description for anime
                if (anilistData != null && !AniListApi.cleanDescription(anilistData!!.description).isNullOrBlank()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("Anime Info (AniList)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = AniListApi.cleanDescription(anilistData!!.description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }

                // Episodes for TV
                if (mediaType == "tv") {
                    item {
                        Text(
                            "Episodes",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(episodes) { ep ->
                        EpisodeRow(
                            episode = ep,
                            onClick = {
                                viewModel.playEpisode(navController, ctx, mediaId, ep.seasonNumber, ep.episodeNumber, ep.name)
                            },
                            progress = progress,
                            watched = watched,
                        )
                    }
                }

                // Collection info for movies
                val movieForCollection = detail as? MovieDetail
                if (movieForCollection?.belongsToCollection != null) {
                    item {
                        Text(
                            "Collection",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        val collection = movieForCollection.belongsToCollection!!
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = imgUrl(collection.posterPath, "w92"),
                                contentDescription = null,
                                modifier = Modifier.size(60.dp, 90.dp).clip(MaterialTheme.shapes.extraSmall),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(collection.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun EpisodeRow(
    episode: TvEpisode,
    onClick: () -> Unit,
    progress: Map<String, Float>,
    watched: Map<String, Boolean>,
) {
    val key = "tv_${episode.showId}_s${episode.seasonNumber}e${episode.episodeNumber}"
    val pct = progress[key] ?: 0f
    val isWatched = watched[key] == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Still image
        AsyncImage(
            model = imgUrl(episode.stillPath, "w300"),
            contentDescription = null,
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "E${episode.episodeNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    episode.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isWatched) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = "Watched", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${episode.airDate ?: ""}${if (episode.runtime != null) " · ${episode.runtime} min" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pct > 0f && !isWatched) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { pct / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(MaterialTheme.shapes.extraSmall),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

// ── Detail ViewModel ──
class DetailViewModel : ViewModel() {
    private val _detail = mutableStateOf<DetailCommon?>(null)
    val detail: State<DetailCommon?> = _detail

    private val _episodes = mutableStateOf<List<TvEpisode>>(emptyList())
    val episodes: State<List<TvEpisode>> = _episodes

    private val _trailers = mutableStateOf<List<TmdbVideo>>(emptyList())
    val trailers: State<List<TmdbVideo>> = _trailers

    private val _anilistData = mutableStateOf<AniListMedia?>(null)
    val anilistData: State<AniListMedia?> = _anilistData

    private val _loading = mutableStateOf(true)
    val loading: State<Boolean> = _loading

    private val _isSaved = mutableStateOf(false)
    val isSaved: State<Boolean> = _isSaved

    private val _progress = mutableStateOf<Map<String, Float>>(emptyMap())
    val progress: State<Map<String, Float>> = _progress

    private val _watched = mutableStateOf<Map<String, Boolean>>(emptyMap())
    val watched: State<Map<String, Boolean>> = _watched

    fun load(ctx: android.content.Context, id: Int, type: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _isSaved.value = Prefs.isSaved(ctx, id, type)
                _progress.value = Prefs.getProgress(ctx)
                _watched.value = Prefs.getWatched(ctx)

                if (type == "movie") {
                    val movie = MediaRepository.getMovieDetail(id)
                    _detail.value = movie
                    _trailers.value = MediaRepository.getMovieTrailers(id)
                    // Check AniList for anime
                    val genreIds = movie.genres.map { it.id }
                    if (isAnimeContent(genreIds, movie.originalLanguage, movie.originCountry)) {
                        _anilistData.value = MediaRepository.fetchAnilistData(movie.title, tmdbId = id)
                    }
                } else {
                    val tv = MediaRepository.getTvDetail(id)
                    _detail.value = tv
                    _trailers.value = MediaRepository.getTvTrailers(id)
                    // Load first season episodes
                    if (tv.seasons.isNotEmpty()) {
                        val firstSeason = tv.seasons.minByOrNull { it.seasonNumber }
                        if (firstSeason != null && firstSeason.seasonNumber > 0) {
                            val seasonDetail = MediaRepository.getTvSeasonDetail(id, firstSeason.seasonNumber)
                            _episodes.value = seasonDetail.episodes
                        }
                    }
                    // Check AniList
                    val genreIds = tv.genres.map { it.id }
                    if (isAnimeContent(genreIds, tv.originalLanguage, tv.originCountry)) {
                        _anilistData.value = MediaRepository.fetchAnilistData(tv.name, tmdbId = id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _loading.value = false
        }
    }

    fun play(navController: NavController, ctx: android.content.Context, id: Int, type: String) {
        viewModelScope.launch {
            val sourceId = Prefs.getPlayerSource(ctx).ifBlank {
                val d = _detail.value
                val genreIds = d?.genres?.map { it.id } ?: emptyList()
                val lang = d?.originalLanguage ?: ""
                val country = d?.originCountry ?: emptyList()
                if (isAnimeContent(genreIds, lang, country)) PlayerSources.ANIME_DEFAULT else PlayerSources.NON_ANIME_DEFAULT
            }
            val accent = Prefs.getAccentColor(ctx)
            val title = _detail.value?.displayTitle ?: ""
            val url = MediaRepository.buildSourceUrl(sourceId, type, id, accentColor = accent)
            navController.navigate(Routes.playerArgs(url, type, id, null, null, title))
        }
    }

    fun playEpisode(navController: NavController, ctx: android.content.Context, showId: Int, season: Int, episode: Int, epName: String) {
        viewModelScope.launch {
            val sourceId = Prefs.getPlayerSource(ctx).ifBlank { PlayerSources.NON_ANIME_DEFAULT }
            val accent = Prefs.getAccentColor(ctx)
            val url = MediaRepository.buildSourceUrl(sourceId, "tv", showId, season, episode, accentColor = accent)
            navController.navigate(Routes.playerArgs(url, "tv", showId, season, episode, epName))
        }
    }

    fun toggleSave(ctx: android.content.Context, id: Int, type: String) {
        viewModelScope.launch {
            val d = _detail.value
            val item = SavedItem(
                id = id,
                title = d?.displayTitle ?: "",
                posterPath = d?.posterPath,
                mediaType = type,
                voteAverage = d?.voteAverage ?: 0f,
                year = d?.displayYear ?: "",
            )
            MediaRepository.toggleSaved(ctx, item)
            _isSaved.value = MediaRepository.isSaved(ctx, id, type)
        }
    }
}