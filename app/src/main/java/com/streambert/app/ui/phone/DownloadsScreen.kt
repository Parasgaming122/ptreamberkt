package com.streambert.app.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.streambert.app.data.api.imgUrl
import com.streambert.app.data.model.DownloadEntry
import com.streambert.app.ui.navigation.Routes

@Composable
fun DownloadsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Text("Downloads", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Text("Downloaded videos will appear here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        // Placeholder: Download functionality requires ffmpeg on Android
        // which is complex. This shows an empty state.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Download, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No downloads yet", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Downloads will appear here. Tap the download button\non any movie or episode detail page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}