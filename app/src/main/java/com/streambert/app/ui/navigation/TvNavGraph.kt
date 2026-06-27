package com.streambert.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.streambert.app.ui.tv.*

@Composable
fun TvNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            TvHomeScreen(navController = navController)
        }
        composable(Routes.SEARCH) {
            TvSearchScreen(navController = navController)
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(
                androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType },
                androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType },
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            val type = backStackEntry.arguments?.getString("type") ?: "movie"
            TvDetailScreen(navController = navController, mediaId = id, mediaType = type)
        }
        composable(
            Routes.PLAYER,
            arguments = listOf(
                androidx.navigation.navArgument("url") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("tmdbId") { type = androidx.navigation.NavType.IntType },
                androidx.navigation.navArgument("season") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("episode") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            TvPlayerScreen(
                navController = navController,
                streamUrl = args.getString("url") ?: "",
                mediaType = args.getString("type") ?: "movie",
                tmdbId = args.getInt("tmdbId"),
                season = args.getString("season")?.ifBlank { null }?.toIntOrNull(),
                episode = args.getString("episode")?.ifBlank { null }?.toIntOrNull(),
                title = args.getString("title") ?: "",
            )
        }
        composable(Routes.LIBRARY) {
            TvLibraryScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            TvSettingsScreen(navController = navController)
        }
    }
}