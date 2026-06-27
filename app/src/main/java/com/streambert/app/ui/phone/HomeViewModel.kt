package com.streambert.app.ui.phone

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streambert.app.data.model.MediaItem
import com.streambert.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _trendingMovies = MutableStateFlow<List<MediaItem>>(emptyList())
    val trendingMovies: StateFlow<List<MediaItem>> = _trendingMovies

    private val _trendingTv = MutableStateFlow<List<MediaItem>>(emptyList())
    val trendingTv: StateFlow<List<MediaItem>> = _trendingTv

    private val _recommended = MutableStateFlow<List<MediaItem>>(emptyList())
    val recommended: StateFlow<List<MediaItem>> = _recommended

    private val _topRated = MutableStateFlow<List<MediaItem>>(emptyList())
    val topRated: StateFlow<List<MediaItem>> = _topRated

    private val _continueWatching = MutableStateFlow<List<MediaItem>>(emptyList())
    val continueWatching: StateFlow<List<MediaItem>> = _continueWatching

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    fun loadHome(ctx: android.content.Context) {
        viewModelScope.launch {
            _loading.value = true
            try {
                launch { _trendingMovies.value = MediaRepository.getTrendingMovies() }
                launch { _trendingTv.value = MediaRepository.getTrendingTv() }
                launch { _topRated.value = MediaRepository.getTopRated() }
                launch {
                    try { _recommended.value = MediaRepository.getRecommended(ctx) } catch (_: Exception) {}
                }
                launch {
                    try { _continueWatching.value = MediaRepository.getContinueWatching(ctx) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            _loading.value = false
        }
    }
}