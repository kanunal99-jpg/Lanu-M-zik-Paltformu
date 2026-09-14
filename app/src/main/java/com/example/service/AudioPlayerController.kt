package com.example.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import com.example.model.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerController(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()
    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()
    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, LanuMediaSessionService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupControllerListener()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressTracker() else stopProgressTracker()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _durationMs.value = mediaController?.duration?.coerceAtLeast(1) ?: 1L
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val foundSong = _queue.value.find { it.id == mediaItem?.mediaId }
                if (foundSong != null) {
                    _currentSong.value = foundSong
                    _queueIndex.value = _queue.value.indexOf(foundSong)
                    _durationMs.value = foundSong.durationMs
                }
                _currentPositionMs.value = 0L
            }
        })
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        val playableSongs = songs.filter { it.audioUrl.isNotBlank() }
        if (playableSongs.isEmpty()) {
            showUnavailablePlayback()
            return
        }

        val requestedSong = songs.getOrNull(startIndex)
        val validSongs = if (requestedSong != null && requestedSong.audioUrl.isNotBlank()) {
            playableSongs
        } else {
            showUnavailablePlayback()
            playableSongs
        }
        val validIndex = validSongs.indexOfFirst { it.id == requestedSong?.id }.takeIf { it >= 0 } ?: 0

        _queue.value = validSongs
        _queueIndex.value = validIndex
        val mediaItems = validSongs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(Uri.parse(song.audioUrl))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                        .build()
                )
                .build()
        }
        mediaController?.setMediaItems(mediaItems, validIndex, 0L)
        mediaController?.prepare()
        if (autoPlay) mediaController?.play()
    }

    private fun showUnavailablePlayback() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Bu parçanın doğrulanmış bir yayın kaynağı bulunmuyor.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        val existingIndex = _queue.value.indexOfFirst { it.id == song.id }
        if (existingIndex >= 0) {
            _queueIndex.value = existingIndex
            mediaController?.seekTo(existingIndex, 0L)
            if (autoPlay) mediaController?.play()
        } else {
            setQueue(listOf(song), 0, autoPlay)
        }
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        mediaController?.let { controller ->
            if (controller.hasNextMediaItem()) controller.seekToNextMediaItem()
            else if (_queue.value.isNotEmpty()) controller.seekTo(0, 0L)
        }
    }

    fun previous() {
        mediaController?.let { controller ->
            when {
                controller.currentPosition > 3000 -> controller.seekTo(0L)
                controller.hasPreviousMediaItem() -> controller.seekToPreviousMediaItem()
                _queue.value.isNotEmpty() -> controller.seekTo(_queue.value.size - 1, 0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.let { controller ->
            val target = positionMs.coerceIn(0, controller.duration.coerceAtLeast(1))
            controller.seekTo(target)
            _currentPositionMs.value = target
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        mediaController?.shuffleModeEnabled = _isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaController?.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    // These state controls are intentionally UI-only until a real audio-session DSP implementation is wired.
    fun toggleEqualizerEnabled() { _equalizerState.value = _equalizerState.value.copy(isEnabled = !_equalizerState.value.isEnabled) }
    fun setEqualizerPreset(preset: EqualizerPreset) { _equalizerState.value = _equalizerState.value.copy(activePreset = preset) }
    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        val bands = _equalizerState.value.bands.toMutableList()
        if (bandIndex in bands.indices) bands[bandIndex] = bands[bandIndex].copy(levelDb = levelDb)
        _equalizerState.value = _equalizerState.value.copy(bands = bands, activePreset = EqualizerPreset.CUSTOM)
    }
    fun setBassBoost(percent: Float) { _equalizerState.value = _equalizerState.value.copy(bassBoostPercent = percent.coerceIn(0f, 1f)) }
    fun setVirtualizer(percent: Float) { _equalizerState.value = _equalizerState.value.copy(virtualizerPercent = percent.coerceIn(0f, 1f)) }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (mediaController?.isPlaying == true) {
                    _currentPositionMs.value = mediaController?.currentPosition ?: 0L
                    mediaController?.duration?.takeIf { it > 0 }?.let { _durationMs.value = it }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() { progressJob?.cancel(); progressJob = null }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}

enum class RepeatMode { OFF, ALL, ONE }
