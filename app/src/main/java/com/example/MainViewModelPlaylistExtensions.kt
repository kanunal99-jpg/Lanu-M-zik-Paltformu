package com.example

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val playlistActionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

fun MainViewModel.renamePlaylist(playlistId: String, name: String) {
    playlistActionScope.launch {
        runCatching { repository.renamePlaylist(playlistId, name) }
            .onFailure { Log.e("MainViewModel", "Playlist rename failed", it) }
    }
}

fun MainViewModel.reorderPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
    playlistActionScope.launch {
        runCatching { repository.reorderPlaylist(playlistId, fromIndex, toIndex) }
            .onFailure { Log.e("MainViewModel", "Playlist reorder failed", it) }
    }
}
