package com.streambert.app.ui.phone

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.streambert.app.data.local.Prefs
import com.streambert.app.player.PlayerState
import com.streambert.app.player.VideoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun PlayerScreen(
    navController: NavController,
    streamUrl: String,
    mediaType: String,
    tmdbId: Int,
    season: Int?,
    episode: Int?,
    title: String,
) {
    val ctx = LocalContext.current
    val viewModel: VideoViewModel = viewModel()
    val playerState by viewModel.playerState.collectAsState()
    val showControls by viewModel.showControls.collectAsState()
    val scope = rememberCoroutineScope()

    // Load stream URL in a WebView-based player since sources are embed URLs
    // For direct m3u8 URLs, use ExoPlayer
    val isDirectStream = streamUrl.contains(".m3u8")

    DisposableEffect(streamUrl) {
        if (isDirectStream) {
            viewModel.loadStream(streamUrl)
        }
        onDispose {
            viewModel.player.stop()
            viewModel.player.clearMediaItems()
        }
    }

    // Save progress periodically
    LaunchedEffect(playerState) {
        if (playerState == PlayerState.READY) {
            while (true) {
                delay(5000)
                val (pos, dur) = viewModel.getProgressInfo()
                val pct = (pos.toFloat() / dur) * 100f
                val key = if (mediaType == "tv") "tv_${tmdbId}_s${season ?: 1}e${episode ?: 1}" else "movie_${tmdbId}"
                com.streambert.app.data.repository.MediaRepository.saveProgress(ctx, key, pct)
                // Auto-watched check
                val remainingMs = dur - pos
                val threshold = 20_000L
                if (remainingMs <= threshold) {
                    com.streambert.app.data.repository.MediaRepository.markWatched(ctx, key)
                    // Add to history
                    com.streambert.app.data.repository.MediaRepository.addHistoryEntry(ctx,
                        com.streambert.app.data.model.HistoryEntry(
                            id = tmdbId, title = title, mediaType = mediaType,
                            season = season, episode = episode,
                        )
                    )
                }
            }
        }
    }

    BackHandler { navController.popBackStack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isDirectStream) {
            // ExoPlayer for direct m3u8
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = viewModel.player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // WebView for embed sources (Videasy, VidSrc, Vidking)
            AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = android.webkit.WebViewClient()
                        webChromeClient = android.webkit.WebChromeClient()
                        loadUrl(streamUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Controls overlay
        if (isDirectStream && showControls) {
            // Gradient overlays
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { viewModel.toggleControls() },
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2f) viewModel.seekBackward()
                                else viewModel.seekForward()
                            },
                        )
                    },
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(title, color = Color.White, fontSize = 16.sp, maxLines = 1,
                        modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    // Progress bar
                    if (viewModel.getDuration() > 0) {
                        Slider(
                            value = viewModel.getCurrentPosition().toFloat(),
                            onValueChange = { viewModel.player.seekTo(it.toLong()) },
                            valueRange = 0f..viewModel.getDuration().toFloat(),
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                formatTime(viewModel.getCurrentPosition()),
                                color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp,
                            )
                            Text(
                                formatTime(viewModel.getDuration()),
                                color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp,
                            )
                        }
                    }
                    // Play/Pause + Seek buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { viewModel.seekBackward() }) {
                            Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        IconButton(onClick = { viewModel.togglePlayPause() }) {
                            Icon(
                                if (viewModel.player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        IconButton(onClick = { viewModel.seekForward() }) {
                            Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Auto-hide controls
            LaunchedEffect(showControls) {
                if (showControls) {
                    delay(3000)
                    viewModel.toggleControls()
                }
            }
        }

        // Buffering indicator
        if (playerState == PlayerState.BUFFERING) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}