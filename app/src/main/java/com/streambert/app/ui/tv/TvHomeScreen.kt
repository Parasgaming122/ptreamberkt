package com.streambert.app.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
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
import com.streambert.app.data.api.imgUrl
import com.streambert.app.data.api.isAnimeContent
import com.streambert.app.data.model.MediaItem
import com.streambert.app.ui.navigation.Routes

@Composable
fun TvHomeScreen(navController: NavController) {
    val ctx = LocalContext.current
    val vm: com.streambert.app.ui.phone.HomeViewModel = viewModel()
    val trendingMovies by vm.trendingMovies.collectAsState()
    val trendingTv by vm.trendingTv.collectAsState()
    val loading by vm.loading.collectAsState()

    LaunchedEffect(Unit) { vm.loadHome(ctx) }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        // ── Left Sidebar (140dp) ──
        TvSidebar(navController = navController)

        // ── Main Content Area ──
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp, top = 16.dp)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE50914))
                }
            } else {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())) {
                    // Hero
                    val hero = trendingMovies.firstOrNull()
                    if (hero != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { navController.navigate(Routes.detail(hero.id, hero.mediaType)) },
                        ) {
                            AsyncImage(
                                model = imgUrl(hero.backdropPath, "original"),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Box(modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xFF0A0A0A)), endY = 500f)
                            ))
                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                                Text(hero.displayTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${hero.voteAverage} · ${hero.displayYear}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                            }
                        }
                    }

                    // Content rows
                    if (trendingMovies.isNotEmpty()) {
                        TvContentSection(title = "Trending Movies", items = trendingMovies, navController = navController)
                    }
                    if (trendingTv.isNotEmpty()) {
                        TvContentSection(title = "Trending Series", items = trendingTv, navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun TvSidebar(navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val sidebarItems = listOf("Home", "Search", "Library", "Settings")

    Column(
        modifier = Modifier
            .width(140.dp)
            .fillMaxHeight()
            .background(Color(0xFF111111))
            .padding(vertical = 16.dp),
    ) {
        // Logo
        Text(
            "STREAMBERT",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = Color(0xFFE50914),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
        )
        HorizontalDivider(color = Color(0xFF2A2A2A))
        Spacer(modifier = Modifier.height(8.dp))

        sidebarItems.forEachIndexed { index, label ->
            val focusRequester = remember { FocusRequester() }
            val isFocused = remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onFocusChanged { isFocused.value = it.hasFocus },
                shape = MaterialTheme.shapes.small,
                color = if (isFocused.value) Color(0xFFE50914).copy(alpha = 0.15f) else Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedItem = index
                            val route = when (label) {
                                "Home" -> Routes.HOME
                                "Search" -> Routes.SEARCH
                                "Library" -> Routes.LIBRARY
                                "Settings" -> Routes.SETTINGS
                                else -> Routes.HOME
                            }
                            navController.navigate(route)
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (label) {
                            "Home" -> Icons.Default.Home
                            "Search" -> Icons.Default.Search
                            "Library" -> Icons.Default.VideoLibrary
                            "Settings" -> Icons.Default.Settings
                            else -> Icons.Default.Home
                        },
                        contentDescription = label,
                        tint = if (isFocused.value) Color(0xFFE50914) else Color(0xFFAAAAAA),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        label,
                        color = if (isFocused.value) Color(0xFFE50914) else Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun TvContentSection(title: String, items: List<MediaItem>, navController: NavController) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { "${it.mediaType}_${it.id}" }) { item ->
                TvMediaCard(item = item, onClick = {
                    navController.navigate(Routes.detail(item.id, item.mediaType))
                })
            }
        }
    }
}

@Composable
fun TvMediaCard(item: MediaItem, onClick: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(200.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { isFocused.value = it.hasFocus }
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(MaterialTheme.shapes.medium)
                .then(
                    if (isFocused.value) Modifier.background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xFFE50914).copy(alpha = 0.3f)))
                    ) else Modifier
                ),
        ) {
            AsyncImage(
                model = imgUrl(item.posterPath, "w342"),
                contentDescription = item.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Focus highlight border
            if (isFocused.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(2.dp, Color(0xFFE50914), MaterialTheme.shapes.medium)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            item.displayTitle,
            color = if (isFocused.value) Color.White else Color(0xFFCCCCCC),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}