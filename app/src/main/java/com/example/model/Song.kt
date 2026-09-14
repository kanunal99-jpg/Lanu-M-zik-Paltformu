package com.example.model

import androidx.compose.ui.graphics.Color

enum class MusicCategory(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val emoji: String = "🎵"
) {
    TURKCE_POP("tr_pop", "Türkçe Pop", "Turkish Pop", Color(0xFFFF416C), Color(0xFFFF4B2B), "🎤"),
    TURKCE_RAP("tr_rap", "Türkçe Rap", "Turkish Rap & Hip-Hop", Color(0xFF8A2387), Color(0xFFE94057), "🎧"),
    TURKCE_ROCK("tr_rock", "Türkçe Rock", "Turkish Rock", Color(0xFF11998E), Color(0xFF38EF7D), "🎸"),
    ANADOLU_ROCK("anadolu_rock", "Anadolu Rock", "Anatolian Rock", Color(0xFFF7971E), Color(0xFFFFD200), "🥁"),
    TURK_SANAT("tsm", "Türk Sanat Müziği", "Classical Turkish", Color(0xFF4776E6), Color(0xFF8E54E9), "🎻"),
    GLOBAL_POP("global_pop", "Global Pop", "Global Pop Hits", Color(0xFF00B4DB), Color(0xFF0083B0), "✨"),
    HIP_HOP("hip_hop", "Hip-Hop & R&B", "Hip-Hop & R&B", Color(0xFFF857A6), Color(0xFFFF5858), "🔥"),
    ROCK_CLASSICS("rock_classics", "Rock Efsaneleri", "Rock Classics", Color(0xFF232526), Color(0xFF414345), "⚡"),
    EDM_DANCE("edm_dance", "Elektronik & Dans", "EDM & Dance", Color(0xFF00F2FE), Color(0xFF4FACFE), "🔊"),
    CHILL_LOFI("chill_lofi", "Akustik & Chill", "Acoustic & Chill", Color(0xFF56AB2F), Color(0xFFA8E063), "☕");

    val displayNameTr: String get() = titleTr
    val displayNameEn: String get() = titleEn
}

data class TimedLyric(
    val timeMs: Long,
    val text: String
)

enum class AudioQuality(val title: String, val bitRate: String, val description: String) {
    STANDARD("Standart", "128 kbps", "Düşük veri kullanımı"),
    HIGH("Yüksek", "320 kbps", "Zengin ve kristal ses"),
    HIFI("Hi-Fi Kayıpsız", "FLAC (24-bit 96kHz)", "Stüdyo kalitesinde saf ses");

    val bitrate: String get() = bitRate
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val durationMs: Long,
    val category: MusicCategory,
    val language: String = "tr", // "tr" or "en"
    val coverUrl: String,
    val audioUrl: String,
    val releaseYear: Int,
    val lyrics: List<TimedLyric> = emptyList(),
    val isNewRelease: Boolean = false,
    val playCount: Long = 100000L
)
