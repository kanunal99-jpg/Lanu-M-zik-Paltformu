package com.example.service

/**
 * Records a listening start only when the real Media3 player reports playing.
 * A tap, buffering or prepared media item is not evidence of playback.
 */
internal class PlaybackHistoryGate {
    private var playingId: String? = null

    fun onPlaybackState(mediaId: String?, isPlaying: Boolean): String? {
        if (!isPlaying || mediaId.isNullOrBlank()) {
            playingId = null
            return null
        }
        if (playingId == mediaId) return null
        playingId = mediaId
        return mediaId
    }
}
