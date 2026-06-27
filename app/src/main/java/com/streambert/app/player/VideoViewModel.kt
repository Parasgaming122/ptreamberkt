package com.streambert.app.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class PlayerState { IDLE, BUFFERING, READY, ENDED }

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    // Use APPLICATION context, never Activity context
    val player: ExoPlayer = ExoPlayer.Builder(getApplication()).build().apply {
        playWhenReady = true
    }

    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    val playerState: StateFlow<PlayerState> = _playerState

    private val _progressPercent = MutableStateFlow(0f)
    val progressPercent: StateFlow<Float> = _progressPercent

    private val _showControls = MutableStateFlow(true)
    val showControls: StateFlow<Boolean> = _showControls

    private var progressJob: Job? = null
    private var allJobs = mutableListOf<Job>()

    fun loadStream(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _playerState.value = when (state) {
                    Player.STATE_BUFFERING -> PlayerState.BUFFERING
                    Player.STATE_READY -> PlayerState.READY
                    Player.STATE_ENDED -> PlayerState.ENDED
                    else -> PlayerState.IDLE
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }
        })
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekForward() {
        player.seekTo(player.currentPosition + 10_000)
    }

    fun seekBackward() {
        player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
    }

    fun toggleControls() {
        _showControls.value = !_showControls.value
    }

    fun getProgressInfo(): Pair<Long, Long> {
        return player.currentPosition to player.duration.coerceAtLeast(1)
    }

    fun getProgressPercent(): Float {
        val dur = player.duration
        return if (dur > 0) (player.currentPosition.toFloat() / dur) * 100f else 0f
    }

    fun getCurrentPosition(): Long = player.currentPosition
    fun getDuration(): Long = player.duration.coerceAtLeast(1)

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = viewModelScope.launch {
            while (true) {
                val dur = player.duration
                _progressPercent.value = if (dur > 0) (player.currentPosition.toFloat() / dur) * 100f else 0f
                delay(1000)
            }
        }?.also { allJobs.add(it) }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        // Memory safety: release player, cancel all coroutines
        stopProgressTracking()
        player.release()
        allJobs.forEach { it.cancel() }
        allJobs.clear()
    }
}