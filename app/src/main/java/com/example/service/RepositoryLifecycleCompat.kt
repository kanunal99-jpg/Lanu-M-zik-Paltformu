package com.example.service

import com.example.data.MusicRepository

/**
 * Compatibility lifecycle hook for the media-session service.
 * Closing cancels the owned repository scope and does not close the shared Room database.
 */
fun MusicRepository.close() = shutdown()
