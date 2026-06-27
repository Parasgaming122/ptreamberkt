package com.streambert.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streambert.app.data.api.*
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.model.*
import com.streambert.app.data.repository.MediaRepository
import com.streambert.app.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun TvDetailScreen(navController: NavController, mediaId: Int, mediaType: String) {
    val ctx = LocalContext.current
    val vm: com.streambert.app.ui.phone.DetailViewModel = viewModel()
    val detail by vm.detail
    val episodes by vm.episodes
    val trailers by vm.trailers
    val loading by vm.loading

    LaunchedEffect(mediaId, mediaType) { vm.load(ctx, mediaId, mediaType) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        } else {
            LazyColumn {
                // Backdrop
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        AsyncImage(
                            model = imgUrl(detail?.backdropPath, "original"),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xFF0A0A0A)), endY = 500f)
                        ))
                        // Back button
                        val focusReq = remember { FocusRequester() }
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.padding(12.dp).background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.circle)
                                .focusRequester(focusReq).focusable(),
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                }

                // Info
                item {
                    Row(modifier = Modifier.padding(horizontal = 20.dp).offset(y = (-40).dp)) {
                        AsyncImage(
                            model = imgUrl(detail?.posterPath, "w342"),
                            contentDescription = null,
                            modifier = Modifier.width(120.dp).height(180.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f).align(Alignment.Bottom)) {
                            Text(detail?.displayTitle ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${detail?.voteAverage ?: 0f} · ${detail?.displayYear ?: ""}", color = Color(0xFFAAAAAA), fontSize = 14.sp)
                        }
                    }
                }

                // Action buttons
                item {
                    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvActionButton("Play", Icons.Default.PlayArrow, true) {
                            vm.play(navController, ctx, mediaId, mediaType)
                        }
                        if (trailers.isNotEmpty()) {
                            TvActionButton("Trailer", Icons.Default.OndemandVideo, false) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/watch?v=${trailers.first().key}"))
                                ctx.startActivity(intent)
                            }
                        }
                        TvActionButton("Save", Icons.Default.BookmarkBorder, false) {
                            vm.toggleSave(ctx, mediaId, mediaType)
                        }
                    }
                }

                // Overview
                item {
                    Text(detail?.overview ?: "", color = Color(0xFFCCCCCC), fontSize = 14.sp, lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 20.dp))
                }

                // Episodes
                if (mediaType == "tv") {
                    item {
                        Text("Episodes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
                    }
                    items(episodes) { ep ->
                        TvEpisodeRow(episode = ep, onClick = {
                            vm.playEpisode(navController, ctx, mediaId, ep.seasonNumber, ep.episodeNumber, ep.name)
                        })
                    }
                }

                item { Spacer(modifier = Modifier.height(60.dp)) }
            }
        }
    }
}

@Composable
fun TvActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPrimary: Boolean, onClick: () -> Unit) {
    val focusReq = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.focusRequester(focusReq).focusable()
            .onFocusChanged { focused = it.hasFocus },
        shape = MaterialTheme.shapes.medium,
        color = when {
            isPrimary -> Color(0xFFE50914)
            focused -> Color(0xFF333333)
            else -> Color(0xFF222222)
        },
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun TvEpisodeRow(episode: TvEpisode, onClick: () -> Unit) {
    val focusReq = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusReq)
            .focusable()
            .onFocusChanged { focused = it.hasFocus }
            .background(if (focused) Color(0xFF1A1A1A) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("E${episode.episodeNumber}", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(40.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(episode.name, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(episode.airDate ?: "", color = Color(0xFF666666), fontSize = 12.sp)
    }
}