package com.streambert.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.model.HistoryEntry
import com.streambert.app.ui.navigation.Routes

@Composable
fun TvLibraryScreen(navController: NavController) {
    val ctx = LocalContext.current
    val vm: com.streambert.app.ui.phone.LibraryViewModel = viewModel()

    LaunchedEffect(Unit) { vm.load(ctx) }

    val watchlist by vm.watchlist.collectAsState()
    val history by vm.history.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(24.dp),
    ) {
        Text("My Library", color = Color.White, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (watchlist.isEmpty() && history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nothing here yet", color = Color(0xFF888888), fontSize = 16.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (watchlist.isNotEmpty()) {
                    item { Text("Watchlist (${watchlist.size})", color = Color.White, fontSize = 18.sp) }
                    items(watchlist) { item ->
                        Text(item.title, color = Color(0xFFCCCCCC), fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 6.dp).clickable { navController.navigate(Routes.detail(item.id, item.mediaType)) })
                    }
                }
                if (history.isNotEmpty()) {
                    item { Text("Watch History", color = Color.White, fontSize = 18.sp) }
                    items(history) { entry ->
                        Text(
                            "${entry.title} ${if (entry.mediaType == "tv" && entry.season != null) "S${entry.season}E${entry.episode}" else ""}",
                            color = Color(0xFFCCCCCC), fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 6.dp).clickable { navController.navigate(Routes.detail(entry.id, entry.mediaType)) })
                    }
                }
            }
        }
    }
}

@Composable
fun TvSettingsScreen(navController: NavController) {
    val ctx = LocalContext.current
    val vm: com.streambert.app.ui.phone.SettingsViewModel = viewModel()

    LaunchedEffect(Unit) { vm.load(ctx) }
    val theme by vm.theme
    val tvMode by vm.tvMode

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(24.dp),
    ) {
        Text("Settings", color = Color.White, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // TV Mode
        Text("TV Mode: ${if (tvMode) "ON" else "OFF"}", color = Color(0xFFCCCCCC), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Theme: $theme", color = Color(0xFFCCCCCC), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))
        Text("For full settings, switch to Phone mode in Settings.", color = Color(0xFF888888), fontSize = 13.sp)
    }
}