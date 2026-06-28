package com.streambert.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.repository.MediaRepository
import com.streambert.app.ui.navigation.PhoneNavHost
import com.streambert.app.ui.navigation.TvNavHost
import com.streambert.app.ui.theme.StreambertTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var tvMode by remember { mutableStateOf(false) }
            var themeId by remember { mutableStateOf("dark") }
            var accentHex by remember { mutableStateOf("#e50914") }
            var isSetup by remember { mutableStateOf(true) }
            var showSetup by remember { mutableStateOf(false) }

            val scope = rememberCoroutineScope()
            val ctx = LocalContext.current

            LaunchedEffect(Unit) {
                tvMode = Prefs.isTvMode(ctx)
                themeId = Prefs.getTheme(ctx)
                accentHex = Prefs.getAccentColor(ctx)
                isSetup = Prefs.isSetupDone(ctx)
                showSetup = !isSetup

                // Configure API
                val key = Prefs.getTmdbKey(ctx)
                if (!key.isNullOrBlank()) {
                    val lang = Prefs.getTmdbLang(ctx)
                    MediaRepository.configureApi(key, lang)
                }
            }

            StreambertTheme(themeId = themeId, accentHex = accentHex) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (showSetup) {
                        // Setup screen will handle its own navigation
                        val navController = androidx.navigation.compose.rememberNavController()
                        androidx.navigation.compose.NavHost(
                            navController = navController,
                            startDestination = "setup"
                        ) {
                            androidx.navigation.compose.composable("setup") {
                                com.streambert.app.ui.phone.SetupScreen(
                                    navController = navController,
                                    onSetupComplete = {
                                        showSetup = false
                                        scope.launch {
                                            Prefs.setSetupDone(ctx, true)
                                        }
                                    }
                                )
                            }
                        }
                    } else if (tvMode) {
                        val tvNavController = rememberNavController()
                        TvNavHost(navController = tvNavController)
                    } else {
                        val phoneNavController = rememberNavController()
                        // Phone bottom bar
                        Scaffold(
                            bottomBar = {
                                PhoneBottomBar(
                                    navController = phoneNavController,
                                    onSettingsChanged = {
                                        scope.launch {
                                            tvMode = Prefs.isTvMode(ctx)
                                            themeId = Prefs.getTheme(ctx)
                                            accentHex = Prefs.getAccentColor(ctx)
                                        }
                                    }
                                )
                            },
                            containerColor = MaterialTheme.colorScheme.background,
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                PhoneNavHost(navController = phoneNavController)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneBottomBar(
    navController: androidx.navigation.NavController,
    onSettingsChanged: () -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isPlayer = currentRoute == "player"

    if (isPlayer) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = {
                if (currentRoute != "home") navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
            ),
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") },
            selected = currentRoute == "search",
            onClick = {
                if (currentRoute != "search") navController.navigate("search")
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
            ),
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Library") },
            label = { Text("Library") },
            selected = currentRoute == "library",
            onClick = {
                if (currentRoute != "library") navController.navigate("library")
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
            ),
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = {
                if (currentRoute != "settings") navController.navigate("settings")
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}