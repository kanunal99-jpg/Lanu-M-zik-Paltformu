package com.example.model

data class FriendActivity(
    val id: String,
    val friendName: String,
    val avatarUrl: String,
    val song: Song,
    val statusText: String, // e.g., "Şu an dinliyor" or "12 dk önce"
    val isCurrentlyPlaying: Boolean,
    val mutualNote: String? = null,
    val recommendationLikes: Int = 0
)
