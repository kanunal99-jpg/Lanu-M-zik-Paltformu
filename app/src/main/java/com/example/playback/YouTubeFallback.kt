package com.example.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.Song

/**
 * Policy-safe YouTube fallback.
 *
 * LANU never extracts, downloads, caches, separates, or background-plays
 * YouTube audiovisual content. When a verified LANU stream is unavailable,
 * this helper hands the user to the official YouTube app/browser with a
 * search for the current track. Playback therefore remains inside YouTube.
 */
object YouTubeFallback {
    fun searchUri(song: Song): Uri {
        val query = "${song.artist} ${song.title}".trim()
        return Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
    }

    fun open(context: Context, song: Song) {
        val intent = Intent(Intent.ACTION_VIEW, searchUri(song)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
