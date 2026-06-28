package com.streambert.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streambert.app.data.api.imgUrl
import com.streambert.app.data.model.MediaItem
import com.streambert.app.data.repository.MediaRepository
import com.streambert.app.ui.navigation.Routes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TvSearchScreen(navController: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<MediaItem>()) }
    var loading by remember { mutableStateOf(false) }
    var searchJob: Job? by remember { mutableStateOf(null) }
    val queryFocus = remember { FocusRequester() }

    LaunchedEffect(queryFocus) {
        kotlinx.coroutines.delay(300)
        queryFocus.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(24.dp),
    ) {
        Text("Search", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { newQuery ->
                query = newQuery
                searchJob?.cancel()
                if (newQuery.isBlank()) { results = emptyList(); return@OutlinedTextField }
                searchJob = scope.launch {
                    delay(380)
                    loading = true
                    try { results = MediaRepository.search(newQuery) } catch (_: Exception) {}
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth().focusRequester(queryFocus).focusable(),
            placeholder = { Text("Search movies and series...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE50914),
                unfocusedBorderColor = Color(0xFF333333),
                cursorColor = Color(0xFFE50914),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        } else if (results.isEmpty() && query.isNotBlank()) {
            Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("No results for \"$query\"", color = Color(0xFF888888))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(results, key = { "${it.mediaType}_${it.id}" }) { item ->
                    val focusReq = remember { FocusRequester() }
                    var focused by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusReq)
                            .focusable()
                            .onFocusChanged { focused = it.hasFocus }
                            .background(if (focused) Color(0xFF1A1A1A) else Color.Transparent)
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = imgUrl(item.posterPath, "w92"),
                            contentDescription = null,
                            modifier = Modifier.size(50.dp, 72.dp),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.displayTitle, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.displayYear} · ${item.voteAverage}", color = Color(0xFF888888), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}