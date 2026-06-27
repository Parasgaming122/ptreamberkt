package com.streambert.app.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streambert.app.data.api.imgUrl
import com.streambert.app.data.model.MediaItem
import com.streambert.app.data.repository.MediaRepository
import com.streambert.app.ui.navigation.Routes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val ctx = LocalContext.current
    val viewModel: SearchViewModel = viewModel()
    val query by viewModel.query
    val results by viewModel.results
    val searchHistory by viewModel.searchHistory
    val loading by viewModel.loading

    LaunchedEffect(Unit) { viewModel.loadHistory(ctx) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.onQueryChanged(it, ctx) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Search movies and series...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChanged("", ctx) }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
        )

        if (query.isBlank() && searchHistory.isNotEmpty()) {
            // Search history
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent searches", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { viewModel.clearHistory(ctx) }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.primary)
                }
            }
            LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
                items(searchHistory) { term ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onQueryChanged(term, ctx) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.History, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(term, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeHistoryTerm(term, ctx) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        if (!loading && query.isNotBlank()) {
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 12.dp)) {
                    items(results, key = { "${it.mediaType}_${it.id}" }) { item ->
                        SearchResultRow(item = item, onClick = {
                            navController.navigate(Routes.detail(item.id, item.mediaType))
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultRow(item: MediaItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = imgUrl(item.posterPath, "w92"),
            contentDescription = null,
            modifier = Modifier
                .width(45.dp)
                .height(65.dp)
                .clip(MaterialTheme.shapes.extraSmall),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.displayYear}${if (item.voteAverage > 0) " · ★ ${item.voteAverage}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = if (item.mediaType == "tv") "Series" else "Movie",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

class SearchViewModel : androidx.lifecycle.ViewModel() {
    private val _query = mutableStateOf("")
    val query: State<String> = _query

    private val _results = mutableStateOf<List<MediaItem>>(emptyList())
    val results: State<List<MediaItem>> = _results

    private val _searchHistory = mutableStateOf<List<String>>(emptyList())
    val searchHistory: State<List<String>> = _searchHistory

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private var searchJob: Job? = null

    fun loadHistory(ctx: android.content.Context) {
        androidx.lifecycle.viewModelScope.launch {
            _searchHistory.value = com.streambert.app.data.local.Prefs.getSearchHistory(ctx)
        }
    }

    fun onQueryChanged(newQuery: String, ctx: android.content.Context) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _results.value = emptyList()
            return
        }
        searchJob = androidx.lifecycle.viewModelScope.launch {
            delay(380) // Debounce matching Streambert
            _loading.value = true
            try {
                _results.value = MediaRepository.search(newQuery)
            } catch (_: Exception) { _results.value = emptyList() }
            _loading.value = false
        }
    }

    fun removeHistoryTerm(term: String, ctx: android.content.Context) {
        androidx.lifecycle.viewModelScope.launch {
            com.streambert.app.data.local.Prefs.removeSearchTerm(ctx, term)
            _searchHistory.value = com.streambert.app.data.local.Prefs.getSearchHistory(ctx)
        }
    }

    fun clearHistory(ctx: android.content.Context) {
        androidx.lifecycle.viewModelScope.launch {
            com.streambert.app.data.local.Prefs.clearSearchHistory(ctx)
            _searchHistory.value = emptyList()
        }
    }
}