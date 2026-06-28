package com.streambert.app.ui.phone

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.streambert.app.data.api.imgUrl
import com.streambert.app.data.api.isAnimeContent
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.model.MediaItem
import com.streambert.app.data.repository.MediaRepository
import com.streambert.app.ui.navigation.Routes
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: HomeViewModel = viewModel()

    val trendingMovies by viewModel.trendingMovies.collectAsState()
    val trendingTv by viewModel.trendingTv.collectAsState()
    val recommended by viewModel.recommended.collectAsState()
    val topRated by viewModel.topRated.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val hero = trendingMovies.firstOrNull()

    LaunchedEffect(Unit) { viewModel.loadHome(ctx) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Hero Banner ──
        if (!loading && hero != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                AsyncImage(
                    model = imgUrl(hero.backdropPath, "original"),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background,
                                ),
                                endY = 600f,
                            )
                        )
                )
                Column(
                    modifier = Modifier.padding(start = 20.dp, bottom = 24.dp, end = 20.dp),
                ) {
                    Text(
                        text = hero.displayTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = hero.voteAverage.toString(),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = hero.displayYear,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (hero.mediaType == "tv") "Series" else "Movie",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                    }
                    if (hero.overview.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = hero.overview,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row {
                        Button(
                            onClick = {
                                navController.navigate(Routes.detail(hero.id, hero.mediaType))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Watch Now")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { navController.navigate(Routes.detail(hero.id, hero.mediaType)) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        ) {
                            Text("More Info")
                        }
                    }
                }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // ── Content Rows ──
        if (continueWatching.isNotEmpty()) {
            ContentSection(title = "Continue Watching", items = continueWatching, navController = navController, showProgress = true)
        }
        if (recommended.isNotEmpty()) {
            ContentSection(title = "Recommended for You", items = recommended, navController = navController)
        }
        if (trendingMovies.isNotEmpty()) {
            ContentSection(title = "Trending Movies", items = trendingMovies, navController = navController)
        }
        if (trendingTv.isNotEmpty()) {
            ContentSection(title = "Trending Series", items = trendingTv, navController = navController)
        }
        if (topRated.isNotEmpty()) {
            ContentSection(title = "Top Rated", items = topRated, navController = navController)
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ContentSection(
    title: String,
    items: List<MediaItem>,
    navController: NavController,
    showProgress: Boolean = false,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { "${it.mediaType}_${it.id}" }) { item ->
                MediaCard(item = item, onClick = {
                    navController.navigate(Routes.detail(item.id, item.mediaType))
                }, showProgress = showProgress)
            }
        }
    }
}

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    showProgress: Boolean = false,
    progress: Float = 0f,
) {
    val anime = isAnimeContent(item.genreIds, item.originalLanguage, item.originCountry)

    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .clip(MaterialTheme.shapes.medium),
        ) {
            AsyncImage(
                model = imgUrl(item.posterPath, "w342"),
                contentDescription = item.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Badges
            if (item.isUnreleased) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("SOON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            // Type badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                color = if (anime) Color(0xFF7C3AED) else MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    text = when {
                        item.isUnreleased -> "SOON"
                        anime -> "ANIME"
                        item.isTv -> "TV"
                        else -> "HD"
                    },
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            // Progress bar
            if (showProgress && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress / 100f)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Text(
            text = item.displayYear,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}