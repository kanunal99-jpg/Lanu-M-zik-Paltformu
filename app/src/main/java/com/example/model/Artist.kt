package com.example.model

data class Artist(
    val id: String,
    val name: String,
    val genre: String,
    val bio: String,
    val imageUrl: String,
    val monthlyListeners: String,
    val isVerified: Boolean = true
)
