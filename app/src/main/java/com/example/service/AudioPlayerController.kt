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
import com.example.player.PlaybackUriPolicy
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayerController(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val playerStateStore = PlayerStateStore(context)
    private var progressJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var audioEffects: AudioEffectsController? = null
    private var pendingRestore: PlayerStateStore.Snapshot? = null
    private var hasAttemptedRestore = false
    private var latestCatalogForRestore: List<Song> = emptyList()
    private val queuedBeforeConnection = StartupPlaybackQueue<PendingQueueRequest>()
    private var released = false
    var mediaController: MediaController? = null

    private data class PendingQueueRequest(
        val songs: List<Song>,
        val startIndex: Int,
        val autoPlay: Boolean
    )

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
            if (released) return@addListener
            mediaController = runCatching { controllerFuture?.get() }.getOrNull()
            if (mediaController == null) return@addListener
            setupControllerListener()
            ensureAudioEffects()
            // Explicit playback taps win over a persisted resume snapshot.
            val queued = queuedBeforeConnection.drain()
            if (queued != null) setQueue(queued.songs, queued.startIndex, queued.autoPlay)
            else restoreStateFromCatalog(latestCatalogForRestore)
        }, ContextCompat.getMainExecutor(context))
    }

    fun restoreStateFromCatalog(catalog: List<Song>) {
        if (released) return
        latestCatalogForRestore = catalog
        if (hasAttemptedRestore || catalog.isEmpty() || queuedBeforeConnection.hasPending()) return
        val snapshot = pendingRestore ?: run {
            hasAttemptedRestore = true
            return
        }
        val song = catalog.firstOrNull { it.id == snapshot.songId }
        if (song == null) return
        // Media3 may connect after the first Room/MediaStore emission.
        // Keep the snapshot for a retry instead of discarding it prematurely.
        if (mediaController == null) return
        if (playableUri(song) == null) {
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

            @androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
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
        if (released || songs.isEmpty()) return
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
        if (mediaController == null) {
            // User tapped play before MediaController.Builder.buildAsync completed.
            // Only the newest request is retained; never persist an empty song.
            hasAttemptedRestore = true
            pendingRestore = null
            queuedBeforeConnection.submit(PendingQueueRequest(playableSongs, validIndex, autoPlay))
            return
        }
        hasAttemptedRestore = true
        pendingRestore = null
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
     * Playback provenance gate:
     * - app-managed offline files are always local and authoritative;
     * - MediaStore content:// and file:// URIs are local sources;
     * - remote playback is allowed only when a provider explicitly marked the song verified.
     * Static/legacy catalog entries therefore cannot accidentally become playable.
     */
    private fun playableUri(song: Song): Uri? {
        // Use the same stable filename allowlist as DownloadManager. Legacy
        // valid IDs still map to their original .bin bytes; no files are moved,
        // rewritten or deleted by this security change.
        val offline = PlaybackUriPolicy.managedOfflineFile(context.filesDir, song.id)
        if (offline?.isFile == true && offline.length() > 0L) return Uri.fromFile(offline)

        // A remote catalog entry must not spoof file/content URIs or downgrade
        // encrypted HTTPS playback to plaintext HTTP.
        return PlaybackUriPolicy.verifiedSource(song)?.let(Uri::parse)
    }

    private fun showUnavailablePlayback() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Bu parçanın doğrulanmış bir yayın kaynağı bulunmuyor.", Toast.LENGTH_LONG).show()
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        val existingIndex = _queue.value.indexOfFirst { it.id == song.id }
        if (existingIndex >= 0) {
            if (mediaController == null) {
                setQueue(_queue.value, existingIndex, autoPlay)
                return
            }
            hasAttemptedRestore = true
            pendingRestore = null
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

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaController?.repeatMode = _repeatMode.value.toMedia3RepeatMode()
        persistState()
    }

    fun toggleEqualizerEnabled() {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = !_equalizerState.value.isEnabled)
        ensureAudioEffects()
        audioEffects?.apply(_equalizerState.value)
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
        ensureAudioEffects()
        audioEffects?.apply(_equalizerState.value)
    }

    fun setBassBoost(percent: Float) {
        _equalizerState.value = _equalizerState.value.copy(bassBoostPercent = percent.coerceIn(0f, 1f))
        ensureAudioEffects()
        audioEffects?.apply(_equalizerState.value)
    }

    fun setVirtualizer(percent: Float) {
        _equalizerState.value = _equalizerState.value.copy(virtualizerPercent = percent.coerceIn(0f, 1f))
        ensureAudioEffects()
        audioEffects?.apply(_equalizerState.value)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var persistTick = 0
            while (isActive) {
                if (mediaController?.isPlaying == true) {
                    _currentPositionMs.value = mediaController?.currentPosition ?: 0L
                    mediaController?.duration?.takeIf { it > 0 }?.let { _durationMs.value = it }
                    persistTick++
                    if (persistTick % 4 == 0) persistState()
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() { progressJob?.cancel(); progressJob = null }

    private fun RepeatMode.toMedia3RepeatMode(): Int = when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

    private fun persistState() {
        playerStateStore.save(
            PlayerStateStore.Snapshot(
                songId = _currentSong.value?.id,
                title = _currentSong.value?.title,
                artist = _currentSong.value?.artist,
                positionMs = _currentPositionMs.value,
                isPlaying = _isPlaying.value,
                shuffle = _isShuffle.value,
                repeatMode = _repeatMode.value
            )
        )
    }

    fun release() {
        if (released) return
        released = true
        // If connection never completed, don't erase the persisted last song.
        if (_currentSong.value != null) persistState()
        queuedBeforeConnection.close()
        latestCatalogForRestore = emptyList()
        stopProgressTracker()
        scope.cancel()
        audioEffects?.release()
        audioEffects = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }
}

enum class RepeatMode { OFF, ALL, ONE }
