package com.streambert.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.streambert.app.ui.phone.*

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val DETAIL = "detail/{id}/{type}"
    const val PLAYER = "player"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val DOWNLOADS = "downloads"
    const val SETUP = "setup"

    fun detail(id: Int, type: String) = "detail/$id/$type"
    fun playerArgs(url: String, type: String, tmdbId: Int, season: Int?, episode: Int?, title: String) =
        "player?url=${android.net.Uri.encode(url)}&type=$type&tmdbId=$tmdbId&season=${season ?: ""}&episode=${episode ?: ""}&title=${android.net.Uri.encode(title)}"
}

@Composable
fun PhoneNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(Routes.SEARCH) {
            SearchScreen(navController = navController)
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(
                navArgument("id") { type = NavType.IntType },
                navArgument("type") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: return@composable
            val type = backStackEntry.arguments?.getString("type") ?: "movie"
            DetailScreen(navController = navController, mediaId = id, mediaType = type)
        }
        composable(
            Routes.PLAYER,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("tmdbId") { type = NavType.IntType },
                navArgument("season") { type = NavType.StringType },
                navArgument("episode") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments ?: return@composable
            PlayerScreen(
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
            LibraryScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.DOWNLOADS) {
            DownloadsScreen(navController = navController)
        }
        composable(Routes.SETUP) {
            SetupScreen(navController = navController)
        }
    }
}