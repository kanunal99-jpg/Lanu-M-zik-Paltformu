package com.example.service

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.EqualizerPreset
import com.example.model.EqualizerState
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF, ALL, ONE
}

class AudioPlayerController(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var hardwareEqualizer: Equalizer? = null
    private var hardwareBassBoost: BassBoost? = null
    private var progressJob: Job? = null
    private val exoPlayer: ExoPlayer

    // Playback state
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    // Equalizer state
    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    init {
        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            
        exoPlayer.addListener(object : Player.Listener {
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
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(1)
                    attachEqualizer(exoPlayer.audioSessionId)
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

        applyEqualizerToHardware(_equalizerState.value)
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        _queue.value = songs
        val validIndex = startIndex.coerceIn(0, songs.size - 1)
        _queueIndex.value = validIndex
        
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(Uri.parse(song.audioUrl))
                .build()
        }
        
        exoPlayer.setMediaItems(mediaItems, validIndex, 0L)
        exoPlayer.prepare()
        if (autoPlay) {
            exoPlayer.play()
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        val existingIndex = _queue.value.indexOfFirst { it.id == song.id }
        if (existingIndex != -1) {
            _queueIndex.value = existingIndex
            exoPlayer.seekTo(existingIndex, 0L)
            if (autoPlay) {
                exoPlayer.play()
            }
        } else {
            setQueue(listOf(song), 0, autoPlay)
        }
    }

    private fun attachEqualizer(audioSessionId: Int) {
        try {
            hardwareEqualizer?.release()
            hardwareEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            hardwareBassBoost?.release()
            hardwareBassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
                if (strengthSupported) {
                    setStrength((_equalizerState.value.bassBoostPercent * 1000).toInt().toShort())
                }
            }
            applyEqualizerToHardware(_equalizerState.value)
        } catch (e: Exception) {
            Log.w("AudioPlayerController", "Equalizer hardware attach warning: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else if (_queue.value.isNotEmpty()) {
            exoPlayer.seekTo(0, 0L)
        }
    }

    fun previous() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else if (_queue.value.isNotEmpty()) {
            exoPlayer.seekTo(_queue.value.size - 1, 0L)
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0, exoPlayer.duration.coerceAtLeast(1))
        exoPlayer.seekTo(target)
        _currentPositionMs.value = target
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        exoPlayer.shuffleModeEnabled = _isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        exoPlayer.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    // --- Equalizer Features ---
    fun toggleEqualizerEnabled() {
        val newState = _equalizerState.value.copy(isEnabled = !_equalizerState.value.isEnabled)
        _equalizerState.value = newState
        applyEqualizerToHardware(newState)
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        val newState = _equalizerState.value.copy(activePreset = preset)
        _equalizerState.value = newState
        applyEqualizerToHardware(newState)
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        val currentBands = _equalizerState.value.bands.toMutableList()
        if (bandIndex in currentBands.indices) {
            val band = currentBands[bandIndex]
            currentBands[bandIndex] = band.copy(levelDb = levelDb)
        }
        val newState = _equalizerState.value.copy(
            bands = currentBands,
            activePreset = EqualizerPreset.CUSTOM
        )
        _equalizerState.value = newState
        applyEqualizerToHardware(newState)
    }

    fun setBassBoost(percent: Float) {
        val newState = _equalizerState.value.copy(bassBoostPercent = percent.coerceIn(0f, 1f))
        _equalizerState.value = newState
        applyEqualizerToHardware(newState)
    }

    fun setVirtualizer(percent: Float) {
        val newState = _equalizerState.value.copy(virtualizerPercent = percent.coerceIn(0f, 1f))
        _equalizerState.value = newState
    }

    private fun applyEqualizerToHardware(state: EqualizerState) {
        hardwareEqualizer?.enabled = state.isEnabled
        hardwareBassBoost?.enabled = state.isEnabled

        if (state.isEnabled) {
            val preset = state.activePreset
            val bands = if (preset == EqualizerPreset.CUSTOM) {
                state.bands
            } else {
                EqualizerState.getPresetBands(preset)
            }

            try {
                hardwareEqualizer?.let { eq ->
                    for (i in 0 until eq.numberOfBands) {
                        if (i < bands.size) {
                            val levelMilliDb = (bands[i].levelDb * 100).toInt().toShort()
                            eq.setBandLevel(i.toShort(), levelMilliDb)
                        }
                    }
                }
                hardwareBassBoost?.let { bb ->
                    if (bb.strengthSupported) {
                        bb.setStrength((state.bassBoostPercent * 1000).toInt().toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w("AudioPlayerController", "Error applying EQ to hardware", e)
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPositionMs.value = exoPlayer.currentPosition
                    val dur = exoPlayer.duration
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
        exoPlayer.release()
        hardwareEqualizer?.release()
        hardwareBassBoost?.release()
    }
}
