package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
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
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private var progressJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var audioEffects: AudioEffectsController? = null
    private var pendingQueue: Pair<List<Song>, Int>? = null
    var mediaController: MediaController? = null
        private set

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
    private val _isShuffle = MutableStateFlow(preferences.getBoolean(KEY_SHUFFLE, false))
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()
    private val _repeatMode = MutableStateFlow(readRepeatMode())
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, LanuMediaSessionService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = runCatching { controllerFuture?.get() }.getOrNull()
            mediaController = controller
            setupControllerListener(controller)
            applyPersistentPlaybackModes()
            ensureAudioEffects()
            pendingQueue?.let { (songs, index) ->
                pendingQueue = null
                setQueue(songs, index, true)
            }
            syncControllerState()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupControllerListener(controller: MediaController?) {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressTracker() else stopProgressTracker()
                ensureAudioEffects()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _durationMs.value = controller.duration.coerceAtLeast(1)
                    ensureAudioEffects()
                    audioEffects?.apply(_equalizerState.value)
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffects?.release(); audioEffects = null
                ensureAudioEffects(audioSessionId)
                audioEffects?.apply(_equalizerState.value)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val id = mediaItem?.mediaId ?: return
                val foundSong = _queue.value.firstOrNull { it.id == id }
                if (foundSong != null) {
                    _currentSong.value = foundSong
                    _queueIndex.value = _queue.value.indexOf(foundSong)
                    _durationMs.value = foundSong.durationMs.coerceAtLeast(1)
                } else {
                    _currentSong.value = null
                    _queueIndex.value = 0
                }
                _currentPositionMs.value = 0L
            }

            override fun onPositionDiscontinuity(reason: Int) {
                _currentPositionMs.value = controller.currentPosition.coerceAtLeast(0L)
            }
        })
    }

    private fun syncControllerState() {
        mediaController?.let { controller ->
            _isPlaying.value = controller.isPlaying
            _currentPositionMs.value = controller.currentPosition.coerceAtLeast(0L)
            _durationMs.value = controller.duration.coerceAtLeast(0L)
            _isShuffle.value = controller.shuffleModeEnabled
            _repeatMode.value = fromPlayerRepeatMode(controller.repeatMode)
            controller.currentMediaItem?.let { item ->
                _currentSong.value = _queue.value.firstOrNull { it.id == item.mediaId }
            }
            if (controller.isPlaying) startProgressTracker()
        }
    }

    private fun applyPersistentPlaybackModes() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = _isShuffle.value
        controller.repeatMode = toPlayerRepeatMode(_repeatMode.value)
    }

    private fun ensureAudioEffects(sessionIdHint: Int? = null) {
        if (audioEffects?.isSupported() == true) return
        val sessionId = sessionIdHint ?: runCatching {
            val method = mediaController?.javaClass?.methods?.firstOrNull { it.name == "getAudioSessionId" && it.parameterTypes.isEmpty() } ?: return
            (method.invoke(mediaController) as? Int) ?: 0
        }.getOrDefault(0)
        if (sessionId > 0) {
            audioEffects?.release(); audioEffects = AudioEffectsController(sessionId)
            audioEffects?.apply(_equalizerState.value)
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        val requestedSong = songs.getOrNull(startIndex)
        if (requestedSong == null || requestedSong.audioUrl.isBlank()) {
            showUnavailablePlayback(); return
        }
        val playableSongs = songs.filter { it.audioUrl.isNotBlank() }
        val validIndex = playableSongs.indexOfFirst { it.id == requestedSong.id }
        if (playableSongs.isEmpty() || validIndex < 0) {
            showUnavailablePlayback(); return
        }
        val controller = mediaController
        if (controller == null) {
            pendingQueue = playableSongs to validIndex
            return
        }
        _queue.value = playableSongs
        _queueIndex.value = validIndex
        val mediaItems = playableSongs.map { song ->
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
                ).build()
        }
        controller.setMediaItems(mediaItems, validIndex, 0L)
        controller.prepare()
        applyPersistentPlaybackModes()
        if (autoPlay) controller.play()
    }

    private fun showUnavailablePlayback() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Bu parçanın doğrulanmış bir oynatma kaynağı bulunmuyor.", Toast.LENGTH_LONG).show()
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        val controller = mediaController
        if (controller == null) {
            pendingQueue = listOf(song) to 0
            return
        }
        val existingIndex = _queue.value.indexOfFirst { it.id == song.id }
        if (existingIndex >= 0) {
            _queueIndex.value = existingIndex
            controller.seekTo(existingIndex, 0L)
            if (autoPlay) controller.play()
        } else {
            setQueue(listOf(song), 0, autoPlay)
        }
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else if (it.currentMediaItem != null) it.play() }
    }

    fun next() {
        mediaController?.let { controller ->
            when {
                controller.hasNextMediaItem() -> controller.seekToNextMediaItem()
                _repeatMode.value == RepeatMode.ALL && _queue.value.isNotEmpty() -> controller.seekTo(0, 0L)
            }
        }
    }

    fun previous() {
        mediaController?.let { controller ->
            when {
                controller.currentPosition > 3000L -> controller.seekTo(0L)
                controller.hasPreviousMediaItem() -> controller.seekToPreviousMediaItem()
                _repeatMode.value == RepeatMode.ALL && _queue.value.isNotEmpty() -> controller.seekTo(_queue.value.lastIndex, 0L)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.let { controller ->
            val target = positionMs.coerceIn(0L, controller.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
            controller.seekTo(target)
            _currentPositionMs.value = target
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        preferences.edit().putBoolean(KEY_SHUFFLE, _isShuffle.value).apply()
        mediaController?.shuffleModeEnabled = _isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        preferences.edit().putString(KEY_REPEAT, _repeatMode.value.name).apply()
        mediaController?.repeatMode = toPlayerRepeatMode(_repeatMode.value)
    }

    fun toggleEqualizerEnabled() {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = !_equalizerState.value.isEnabled)
        ensureAudioEffects(); audioEffects?.apply(_equalizerState.value)
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        ensureAudioEffects()
        _equalizerState.value = audioEffects?.applyPreset(preset, _equalizerState.value)
            ?: _equalizerState.value.copy(activePreset = preset, bands = EqualizerState.getPresetBands(preset))
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        val bands = _equalizerState.value.bands.toMutableList()
        if (bandIndex in bands.indices) bands[bandIndex] = bands[bandIndex].copy(levelDb = levelDb.coerceIn(-12f, 12f))
        _equalizerState.value = _equalizerState.value.copy(bands = bands, activePreset = EqualizerPreset.CUSTOM)
        ensureAudioEffects(); audioEffects?.apply(_equalizerState.value)
    }

    fun setBassBoost(percent: Float) {
        _equalizerState.value = _equalizerState.value.copy(bassBoostPercent = percent.coerceIn(0f, 1f))
        ensureAudioEffects(); audioEffects?.apply(_equalizerState.value)
    }

    fun setVirtualizer(percent: Float) {
        _equalizerState.value = _equalizerState.value.copy(virtualizerPercent = percent.coerceIn(0f, 1f))
        ensureAudioEffects(); audioEffects?.apply(_equalizerState.value)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _currentPositionMs.value = controller.currentPosition.coerceAtLeast(0L)
                        if (controller.duration > 0) _durationMs.value = controller.duration
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() { progressJob?.cancel(); progressJob = null }

    private fun readRepeatMode(): RepeatMode = when (preferences.getString(KEY_REPEAT, RepeatMode.OFF.name)) {
        RepeatMode.ALL.name -> RepeatMode.ALL
        RepeatMode.ONE.name -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    private fun toPlayerRepeatMode(mode: RepeatMode): Int = when (mode) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

    private fun fromPlayerRepeatMode(mode: Int): RepeatMode = when (mode) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    fun release() {
        stopProgressTracker()
        audioEffects?.release(); audioEffects = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }

    private companion object {
        const val PREFERENCES_NAME = "lanu_player_preferences"
        const val KEY_SHUFFLE = "shuffle_enabled"
        const val KEY_REPEAT = "repeat_mode"
    }
}

enum class RepeatMode { OFF, ALL, ONE }
