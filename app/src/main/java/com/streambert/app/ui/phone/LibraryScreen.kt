package com.streambert.app.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streambert.app.data.api.imgUrl
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.model.HistoryEntry
import com.streambert.app.data.model.SavedItem
import com.streambert.app.data.repository.MediaRepository
import com.streambert.app.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LibraryScreen(navController: NavController) {
    val ctx = LocalContext.current
    val viewModel: LibraryViewModel = viewModel()

    LaunchedEffect(Unit) { viewModel.load(ctx) }

    val continueWatching by viewModel.continueWatching.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val history by viewModel.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp),
    ) {
        Text("My Library", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp))
        Text("Watch history, progress, and saved titles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, bottom = 12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Continue Watching
            if (continueWatching.isNotEmpty()) {
                item {
                    Text("Continue Watching", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(continueWatching, key = { "${it.mediaType}_${it.id}" }) { item ->
                    HistoryRow(item = item, onClick = {
                        navController.navigate(Routes.detail(item.id, item.mediaType))
                    }, onRemove = { viewModel.removeHistory(ctx, item.id, item.mediaType, item.season, item.episode) })
                }
            }

            // Watchlist
            if (watchlist.isNotEmpty()) {
                item {
                    Text("Watchlist (${watchlist.size})", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(watchlist, key = { "${it.mediaType}_${it.id}" }) { item ->
                    WatchlistRow(item = item, onClick = {
                        navController.navigate(Routes.detail(item.id, item.mediaType))
                    })
                }
            }

            // Watch History
            if (history.isNotEmpty()) {
                item {
                    Text("Watch History", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(history, key = { "${it.mediaType}_${it.id}_s${it.season}e${it.episode}" }) { entry ->
                    HistoryRow(item = entry, onClick = {
                        navController.navigate(Routes.detail(entry.id, entry.mediaType))
                    }, onRemove = { viewModel.removeHistory(ctx, entry.id, entry.mediaType, entry.season, entry.episode) })
                }
            }

            if (continueWatching.isEmpty() && watchlist.isEmpty() && history.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Visibility, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Nothing here yet", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text("Start watching and your history will appear here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(item: HistoryEntry, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = imgUrl(item.posterPath, "w92"),
            contentDescription = null,
            modifier = Modifier.size(42.dp, 62.dp).clip(MaterialTheme.shapes.extraSmall),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${if (item.mediaType == "tv" && item.season != null) "S${item.season}E${item.episode}" else ""}" +
                "${item.episodeName?.let { " · $it" } ?: ""} · " +
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.watchedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(if (item.mediaType == "tv") "Series" else "Movie",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun WatchlistRow(item: SavedItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = imgUrl(item.posterPath, "w92"),
            contentDescription = null,
            modifier = Modifier.size(42.dp, 62.dp).clip(MaterialTheme.shapes.extraSmall),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.year} · ★ ${item.voteAverage}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

class LibraryViewModel : ViewModel() {
    private val _continueWatching = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val continueWatching: StateFlow<List<HistoryEntry>> = _continueWatching

    private val _watchlist = MutableStateFlow<List<SavedItem>>(emptyList())
    val watchlist: StateFlow<List<SavedItem>> = _watchlist

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history

    fun load(ctx: android.content.Context) {
        viewModelScope.launch {
            val allHistory = Prefs.getHistory(ctx)
            val progress = Prefs.getProgress(ctx)
            val watched = Prefs.getWatched(ctx)

            _continueWatching.value = allHistory.filter { entry ->
                val key = if (entry.mediaType == "tv") "tv_${entry.id}_s${entry.season ?: 1}e${entry.episode ?: 1}" else "movie_${entry.id}"
                val pct = progress[key] ?: 0f
                pct > 2f && pct < 98f && watched[key] != true
            }

            val savedMap = Prefs.getSaved(ctx)
            val order = Prefs.getSavedOrder(ctx)
            val ordered = order.mapNotNull { savedMap[it] }
            val remaining = savedMap.values.filterNot { saved -> ordered.any { "${saved.mediaType}_${saved.id}" == it } }
            _watchlist.value = ordered + remaining

            _history.value = allHistory.filterNot { entry ->
                _continueWatching.value.any { it.id == entry.id && it.mediaType == entry.mediaType && it.season == entry.season && it.episode == entry.episode }
            }
        }
    }

    fun removeHistory(ctx: android.content.Context, id: Int, mediaType: String, season: Int?, episode: Int?) {
        viewModelScope.launch {
            Prefs.removeHistoryEntry(ctx, id, mediaType, season, episode)
            load(ctx)
        }
    }
}