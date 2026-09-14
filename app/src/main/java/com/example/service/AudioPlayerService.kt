package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.MainActivity
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

/**
 * Playback state representation for AudioPlayerService.
 */
enum class AudioPlayerState {
    IDLE,
    BUFFERING,
    READY,
    ENDED
}

/**
 * Production-ready Android Service utilizing the ExoPlayer (Media3) library to handle
 * background and foreground music playback, including play, pause, skip, seek, and
 * playlist queue management.
 */
class AudioPlayerService : Service() {

    private val binder = AudioPlayerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    // ExoPlayer instance
    private lateinit var exoPlayer: ExoPlayer

    // Reactive playback states
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(AudioPlayerState.IDLE)
    val playbackState: StateFlow<AudioPlayerState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    inner class AudioPlayerBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initExoPlayer()
    }

    private fun initExoPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
                updateNotification()
            }

            override fun onPlaybackStateChanged(state: Int) {
                val mappedState = when (state) {
                    Player.STATE_IDLE -> AudioPlayerState.IDLE
                    Player.STATE_BUFFERING -> AudioPlayerState.BUFFERING
                    Player.STATE_READY -> {
                        _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                        AudioPlayerState.READY
                    }
                    Player.STATE_ENDED -> {
                        AudioPlayerState.ENDED
                    }
                    else -> AudioPlayerState.IDLE
                }
                _playbackState.value = mappedState

                // Auto-advance to next song on song end
                if (state == Player.STATE_ENDED) {
                    skipToNext()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                val foundSong = _playlist.value.find { it.id == mediaId }
                if (foundSong != null) {
                    _currentSong.value = foundSong
                    _currentIndex.value = _playlist.value.indexOf(foundSong).coerceAtLeast(0)
                    _durationMs.value = foundSong.durationMs
                }
                _currentPositionMs.value = 0L
                updateNotification()
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_SKIP_NEXT -> skipToNext()
            ACTION_SKIP_PREV -> skipToPrevious()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    // =========================================================================
    // PLAYBACK CONTROLS (Play, Pause, Skip, Seek)
    // =========================================================================

    /**
     * Set a new playlist queue and optionally start playback from the given index.
     */
    fun setPlaylist(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return

        _playlist.value = songs
        val validIndex = startIndex.coerceIn(0, songs.size - 1)
        _currentIndex.value = validIndex
        val targetSong = songs[validIndex]
        _currentSong.value = targetSong
        _durationMs.value = targetSong.durationMs
        _currentPositionMs.value = 0L

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(Uri.parse(song.audioUrl))
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

        exoPlayer.setMediaItems(mediaItems, validIndex, 0L)
        exoPlayer.prepare()
        if (autoPlay) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
        updateNotification()
    }

    /**
     * Play a single song directly, updating the queue.
     */
    fun playSong(song: Song, autoPlay: Boolean = true) {
        val existingIndex = _playlist.value.indexOfFirst { it.id == song.id }
        if (existingIndex != -1) {
            _currentIndex.value = existingIndex
            _currentSong.value = song
            exoPlayer.seekTo(existingIndex, 0L)
            if (autoPlay) {
                exoPlayer.play()
            }
        } else {
            setPlaylist(listOf(song), startIndex = 0, autoPlay = autoPlay)
        }
    }

    /**
     * Resume or start audio playback.
     */
    fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0, 0L)
        }
        exoPlayer.play()
        _isPlaying.value = true
        startProgressTracker()
        updateNotification()
    }

    /**
     * Pause audio playback.
     */
    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
        stopProgressTracker()
        updateNotification()
    }

    /**
     * Toggle play and pause state.
     */
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Skip to the next song in the playlist queue.
     */
    fun skipToNext() {
        val songs = _playlist.value
        if (songs.isEmpty()) return

        val nextIndex = (_currentIndex.value + 1) % songs.size
        _currentIndex.value = nextIndex
        val nextSong = songs[nextIndex]
        _currentSong.value = nextSong
        _durationMs.value = nextSong.durationMs
        _currentPositionMs.value = 0L

        if (exoPlayer.mediaItemCount > nextIndex) {
            exoPlayer.seekTo(nextIndex, 0L)
            exoPlayer.play()
        } else {
            setPlaylist(songs, nextIndex, autoPlay = true)
        }
        updateNotification()
    }

    /**
     * Skip to the previous song in the playlist queue or restart the current track.
     */
    fun skipToPrevious() {
        val songs = _playlist.value
        if (songs.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0L)
            _currentPositionMs.value = 0L
            return
        }

        val prevIndex = if (_currentIndex.value > 0) _currentIndex.value - 1 else songs.size - 1
        _currentIndex.value = prevIndex
        val prevSong = songs[prevIndex]
        _currentSong.value = prevSong
        _durationMs.value = prevSong.durationMs
        _currentPositionMs.value = 0L

        if (exoPlayer.mediaItemCount > prevIndex) {
            exoPlayer.seekTo(prevIndex, 0L)
            exoPlayer.play()
        } else {
            setPlaylist(songs, prevIndex, autoPlay = true)
        }
        updateNotification()
    }

    /**
     * Seek to a specific playback position in milliseconds.
     */
    fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L)
        exoPlayer.seekTo(safePosition)
        _currentPositionMs.value = safePosition
    }

    /**
     * Stop playback and release foreground notification.
     */
    fun stopPlayback() {
        exoPlayer.stop()
        _isPlaying.value = false
        stopProgressTracker()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // =========================================================================
    // PROGRESS TRACKER
    // =========================================================================

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = serviceScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPositionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
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

    // =========================================================================
    // NOTIFICATION & FOREGROUND SERVICE
    // =========================================================================

    private fun updateNotification() {
        val song = _currentSong.value ?: return
        val isCurrentlyPlaying = _isPlaying.value

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AudioPlayerService::class.java).apply { action = ACTION_SKIP_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AudioPlayerService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AudioPlayerService::class.java).apply { action = ACTION_SKIP_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isCurrentlyPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseActionTitle = if (isCurrentlyPlaying) "Duraklat" else "Oynat"

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText("${song.artist} • ${song.album}")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_previous, "Önceki", prevPendingIntent)
            .addAction(playPauseIcon, playPauseActionTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Sonraki", nextPendingIntent)
            .setOngoing(isCurrentlyPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LANU ExoPlayer Müzik Çalar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "LANU ExoPlayer müzik oynatma ve kilit ekranı kontrolleri"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopProgressTracker()
        exoPlayer.release()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "lanu_exoplayer_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_PLAY = "com.example.service.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_PLAY_PAUSE = "com.example.service.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.example.service.ACTION_SKIP_NEXT"
        const val ACTION_SKIP_PREV = "com.example.service.ACTION_SKIP_PREV"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        fun playSongIntent(context: Context, song: Song): Intent {
            return Intent(context, AudioPlayerService::class.java).apply {
                action = ACTION_PLAY
            }
        }

        fun playPauseIntent(context: Context): Intent {
            return Intent(context, AudioPlayerService::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
        }

        fun nextIntent(context: Context): Intent {
            return Intent(context, AudioPlayerService::class.java).apply {
                action = ACTION_SKIP_NEXT
            }
        }

        fun prevIntent(context: Context): Intent {
            return Intent(context, AudioPlayerService::class.java).apply {
                action = ACTION_SKIP_PREV
            }
        }
    }
}
