package com.example

import kotlinx.coroutines.launch

fun MainViewModel.renamePlaylist(playlistId: String, name: String) {
    viewModelScope.launch {
        runCatching { repository.renamePlaylist(playlistId, name) }
            .onFailure { android.util.Log.e("MainViewModel", "Playlist rename failed", it) }
    }
}

fun MainViewModel.reorderPlaylist(playlistId: String, fromIndex: Int, toIndex: Int) {
    viewModelScope.launch {
        runCatching { repository.reorderPlaylist(playlistId, fromIndex, toIndex) }
            .onFailure { android.util.Log.e("MainViewModel", "Playlist reorder failed", it) }
    }
}
