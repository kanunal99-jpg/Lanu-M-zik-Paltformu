package com.example

import com.example.data.MusicRepository

/**
 * Compatibility lifecycle hook for MainViewModel.
 * MusicRepository currently owns application-scoped resources and does not expose a close operation.
 */
fun MusicRepository.close() = Unit
