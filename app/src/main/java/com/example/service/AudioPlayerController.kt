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
import com.example.model.SongSourceType
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
import java.io.File

class AudioPlayerController(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val playerStateStore = PlayerStateStore(context)
    private var progressJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var audioEffects: AudioEffectsController? = null
    private var pendingRestore: PlayerStateStore.Snapshot? = null
    private var hasAttemptedRestore = false
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
        pendingRestore = playerStateStore.read().takeIf { !it.songId.isNullOrBlank() }
        val sessionToken = SessionToken(context, ComponentName(context, LanuMediaSessionService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = runCatching { controllerFuture?.get() }.getOrNull()
            setupControllerListener()
            ensureAudioEffects()
        }, ContextCompat.getMainExecutor(context))
    }

    fun restoreStateFromCatalog(catalog: List<Song>) {
        if (hasAttemptedRestore || catalog.isEmpty()) return
        val snapshot = pendingRestore ?: run {
            hasAttemptedRestore = true
            return
        }
        val song = catalog.firstOrNull { it.id == snapshot.songId }
        if (song == null || playableUri(song) == null || mediaController == null) {
            if (song == null) return
            hasAttemptedRestore = true
            pendingRestore = null
            return
        }
        hasAttemptedRestore = true
        pendingRestore = null
        _isShuffle.value = snapshot.shuffle
        _repeatMode.value = snapshot.repeatMode
        _currentPositionMs.value = snapshot.positionMs.coerceAtLeast(0L)
        setQueue(listOf(song), 0, autoPlay = false)
        mediaController?.seekTo(snapshot.positionMs.coerceIn(0L, song.durationMs.coerceAtLeast(1L)))
        _isPlaying.value = false
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startProgressTracker() else {
                    stopProgressTracker()
                    persistState()
                }
                ensureAudioEffects()
                persistState()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    _durationMs.value = mediaController?.duration?.coerceAtLeast(1) ?: 1L
                    ensureAudioEffects()
                    audioEffects?.apply(_equalizerState.value)
                    if (pendingRestore != null) persistState()
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffects?.release()
                audioEffects = null
                ensureAudioEffects(audioSessionId)
                audioEffects?.apply(_equalizerState.value)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val foundSong = _queue.value.find { it.id == mediaItem?.mediaId }
                if (foundSong != null) {
                    _currentSong.value = foundSong
                    _queueIndex.value = _queue.value.indexOf(foundSong)
                    _durationMs.value = foundSong.durationMs
                }
                _currentPositionMs.value = 0L
                persistState()
            }
        })
    }

    private fun ensureAudioEffects(sessionIdHint: Int? = null) {
        if (audioEffects?.isSupported() == true) return
        val sessionId = sessionIdHint ?: runCatching {
            val method = mediaController?.javaClass?.methods?.firstOrNull { it.name == "getAudioSessionId" && it.parameterTypes.isEmpty() }
                ?: return
            (method.invoke(mediaController) as? Int) ?: 0
        }.getOrDefault(0)
        if (sessionId > 0) {
            audioEffects?.release()
            audioEffects = AudioEffectsController(sessionId)
            audioEffects?.apply(_equalizerState.value)
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        val requestedSong = songs.getOrNull(startIndex)
        if (requestedSong == null || playableUri(requestedSong) == null) {
            showUnavailablePlayback()
            return
        }
        val playableSongs = songs.filter { playableUri(it) != null }
        if (playableSongs.isEmpty()) {
            showUnavailablePlayback()
            return
        }
        val validIndex = playableSongs.indexOfFirst { it.id == requestedSong.id }
        if (validIndex < 0) {
            showUnavailablePlayback()
            return
        }
        _queue.value = playableSongs
        _queueIndex.value = validIndex
        val mediaItems = playableSongs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(playableUri(song)!!)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                        .build()
                ).build()
        }
        mediaController?.setMediaItems(mediaItems, validIndex, 0L)
        mediaController?.shuffleModeEnabled = _isShuffle.value
        mediaController?.repeatMode = _repeatMode.value.toMedia3RepeatMode()
        mediaController?.prepare()
        if (autoPlay) mediaController?.play()
        persistState()
    }

    /**
     * Playback gate: an offline file is always authoritative; otherwise only a
     * URI explicitly mapped by a verified remote catalog provider may reach Media3.
     * Legacy/static/uncertain catalog entries therefore cannot silently become playable.
     */
    private fun playableUri(song: Song): Uri? {
        val offline = File(context.filesDir, "offline_audio/${song.id}.bin")
        if (offline.isFile && offline.length() > 0L) return Uri.fromFile(offline)
        if (song.sourceType != SongSourceType.VERIFIED_REMOTE) return null
        return song.audioUrl.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let(Uri::parse)
    }

    private fun showUnavailablePlayback() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Bu parçanın doğrulanmış bir yayın kaynağı bulunmuyor.", Toast.LENGTH_LONG).show()
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        val existingIndex = _queue.value.indexOfFirst { it.id == song.id }
        if (existingIndex >= 0) {
            _queueIndex.value = existingIndex
            mediaController?.seekTo(existingIndex, 0L)
            if (autoPlay) mediaController?.play()
            persistState()
        } else setQueue(listOf(song), 0, autoPlay)
    }

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
        persistState()
    }

    fun next() {
        mediaController?.let { controller ->
            when (val action = PlaybackQueuePolicy.nextAction(controller.currentMediaItemIndex, _queue.value.size, _repeatMode.value)) {
                is PlaybackQueuePolicy.NextAction.MoveTo -> controller.seekTo(action.index, 0L)
                PlaybackQueuePolicy.NextAction.Stop -> controller.pause()
            }
        }
        persistState()
    }

    fun previous() {
        mediaController?.let { controller ->
            when (val action = PlaybackQueuePolicy.previousAction(controller.currentMediaItemIndex, controller.currentPosition, _queue.value.size, _repeatMode.value)) {
                PlaybackQueuePolicy.PreviousAction.RestartCurrent -> controller.seekTo(0L)
                is PlaybackQueuePolicy.PreviousAction.MoveTo -> controller.seekTo(action.index, 0L)
                PlaybackQueuePolicy.PreviousAction.Stop -> controller.pause()
            }
        }
        persistState()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.let { controller ->
            val target = positionMs.coerceIn(0, controller.duration.coerceAtLeast(1))
            controller.seekTo(target)
            _currentPositionMs.value = target
        }
        persistState()
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        mediaController?.shuffleModeEnabled = _isShuffle.value
        persistState()
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaController?.repeatMode = _repeatMode.value.toMedia3RepeatMode()
        persistState()
    }

    fun addToQueue(song: Song) {
        if (playableUri(song) == null) {
            showUnavailablePlayback()
            return
        }
        _queue.value = _queue.value + song
        mediaController?.addMediaItem(
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(playableUri(song)!!)
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setAlbumTitle(song.album).build()
                ).build()
        )
        persistState()
    }

    fun removeFromQueue(index: Int) {
        if (index !in _queue.value.indices) return
        val updated = _queue.value.toMutableList().apply { removeAt(index) }
        _queue.value = updated
        mediaController?.removeMediaItem(index)
        _queueIndex.value = _queueIndex.value.coerceAtMost((updated.size - 1).coerceAtLeast(0))
        persistState()
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        _equalizerState.value = _equalizerState.value.copy(preset = preset)
        audioEffects?.apply(_equalizerState.value)
        persistState()
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(enabled = enabled)
        audioEffects?.apply(_equalizerState.value)
        persistState()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { _currentPositionMs.value = it.currentPosition.coerceAtLeast(0L) }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun persistState() {
        playerStateStore.write(
            PlayerStateStore.Snapshot(
                songId = _currentSong.value?.id ?: _queue.value.getOrNull(_queueIndex.value)?.id,
                positionMs = _currentPositionMs.value,
                shuffle = _isShuffle.value,
                repeatMode = _repeatMode.value
            )
        )
    }
}
