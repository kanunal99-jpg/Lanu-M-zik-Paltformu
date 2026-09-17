package com.example.service

import com.example.data.MusicRepository

/**
 * Compatibility lifecycle hook for the media-session service.
 * MusicRepository currently owns application-scoped resources and does not expose a close operation.
 */
fun MusicRepository.close() = Unit
