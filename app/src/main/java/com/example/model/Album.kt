package com.example.model

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val coverUrl: String,
    val releaseYear: Int,
    val genre: String,
    val songs: List<Song> = emptyList()
)
