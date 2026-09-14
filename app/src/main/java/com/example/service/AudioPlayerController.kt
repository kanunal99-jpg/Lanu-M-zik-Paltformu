package com.example.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.Song
import com.example.model.EqualizerState
import com.example.model.EqualizerPreset
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                setupControllerListener()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _durationMs.value = mediaController?.duration?.coerceAtLeast(1) ?: 1L
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val foundSong = _queue.value.find { it.id == mediaId }
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
        val targetSong = songs.getOrNull(startIndex)
        if (targetSong != null && targetSong.audioUrl.isEmpty()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "Playback Unavailable: Bu parçanın telif hakları nedeniyle yayını bulunmamaktadır.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            return
        }
        _queue.value = songs
        val validIndex = startIndex.coerceIn(0, songs.size - 1)
        _queueIndex.value = validIndex
        
        val mediaItems = songs.map { song ->
            val uriStr = if (song.audioUrl.isEmpty()) "https://lanumusic.app/empty_fallback.mp3" else song.audioUrl
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(Uri.parse(uriStr))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(Uri.parse(song.coverUrl))
                        .build()
                )
                .build()
        }
        
        mediaController?.setMediaItems(mediaItems, validIndex, 0L)
        mediaController?.prepare()
        if (autoPlay) {
            mediaController?.play()
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        val existingIndex = _queue.value.indexOfFirst { it.id == song.id }
        if (existingIndex != -1) {
            _queueIndex.value = existingIndex
            mediaController?.seekTo(existingIndex, 0L)
            if (autoPlay) {
                mediaController?.play()
            }
        } else {
            setQueue(listOf(song), 0, autoPlay)
        }
    }

    fun togglePlayPause() {
        val ctrl = mediaController ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else {
            ctrl.play()
        }
    }

    fun next() {
        val ctrl = mediaController ?: return
        if (ctrl.hasNextMediaItem()) {
            ctrl.seekToNextMediaItem()
        } else if (_queue.value.isNotEmpty()) {
            ctrl.seekTo(0, 0L)
        }
    }

    fun previous() {
        val ctrl = mediaController ?: return
        if (ctrl.currentPosition > 3000) {
            ctrl.seekTo(0L)
        } else if (ctrl.hasPreviousMediaItem()) {
            ctrl.seekToPreviousMediaItem()
        } else if (_queue.value.isNotEmpty()) {
            ctrl.seekTo(_queue.value.size - 1, 0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val ctrl = mediaController ?: return
        val target = positionMs.coerceIn(0, ctrl.duration.coerceAtLeast(1))
        ctrl.seekTo(target)
        _currentPositionMs.value = target
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

    // --- Equalizer Features (Dummy implementation for now since hardware EQ requires audioSessionId) ---
    fun toggleEqualizerEnabled() {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = !_equalizerState.value.isEnabled)
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _equalizerState.value = _equalizerState.value.copy(activePreset = preset)
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        val currentBands = _equalizerState.value.bands.toMutableList()
        if (bandIndex in currentBands.indices) {
            val band = currentBands[bandIndex]
            currentBands[bandIndex] = band.copy(levelDb = levelDb)
        }
        _equalizerState.value = _equalizerState.value.copy(
            bands = currentBands,
            activePreset = EqualizerPreset.CUSTOM
        )
    }

    fun setBassBoost(percent: Float) {
        _equalizerState.value = _equalizerState.value.copy(bassBoostPercent = percent.coerceIn(0f, 1f))
    }

    fun setVirtualizer(percent: Float) {
        _equalizerState.value = _equalizerState.value.copy(virtualizerPercent = percent.coerceIn(0f, 1f))
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (mediaController?.isPlaying == true) {
                    _currentPositionMs.value = mediaController?.currentPosition ?: 0L
                    val dur = mediaController?.duration ?: 0L
                    if (dur > 0) {
                        _durationMs.value = dur
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}

enum class RepeatMode {
    OFF, ALL, ONE
}


