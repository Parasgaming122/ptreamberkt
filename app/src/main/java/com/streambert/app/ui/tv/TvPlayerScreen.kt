package com.streambert.app.ui.tv

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvPlayerScreen(
    navController: NavController,
    streamUrl: String,
    mediaType: String,
    tmdbId: Int,
    season: Int?,
    episode: Int?,
    title: String,
) {
    var showOverlay by remember { mutableStateOf(true) }
    var overlayJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    fun hideOverlayDelayed() {
        overlayJob?.cancel()
        overlayJob = scope.launch {
            delay(3000)
            showOverlay = false
        }
    }

    // Save progress on leave
    BackHandler {
        scope.launch {
            val key = if (mediaType == "tv") "tv_${tmdbId}_s${season ?: 1}e${episode ?: 1}" else "movie_${tmdbId}"
            com.streambert.app.data.repository.MediaRepository.markWatched(ctx, key)
            com.streambert.app.data.repository.MediaRepository.addHistoryEntry(ctx,
                com.streambert.app.data.model.HistoryEntry(id = tmdbId, title = title, mediaType = mediaType, season = season, episode = episode))
        }
        navController.popBackStack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    showOverlay = true
                    hideOverlayDelayed()
                    when (event.key) {
                        Key.Escape, Key.Back -> {
                            navController.popBackStack()
                            true
                        }
                        Key.DirectionCenter -> {
                            // Toggle play/pause via JS for webview
                            showOverlay = !showOverlay
                            hideOverlayDelayed()
                            true
                        }
                        Key.DirectionRight -> {
                            // Seek +10s
                            true
                        }
                        Key.DirectionLeft -> {
                            // Seek -10s
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        // WebView player
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webViewClient = WebViewClient()
                    webChromeClient = android.webkit.WebChromeClient()
                    loadUrl(streamUrl)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Custom overlay
        if (showOverlay) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(title, color = Color.White, fontSize = 18.sp, maxLines = 1)
                }

                // Center controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.width(40.dp))
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.width(40.dp))
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(48.dp))
                }

                // Bottom hint
                Text("D-Pad to control · Back to exit", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }

        // Auto-hide on any interaction
        LaunchedEffect(showOverlay) {
            if (showOverlay) hideOverlayDelayed()
        }
    }
}