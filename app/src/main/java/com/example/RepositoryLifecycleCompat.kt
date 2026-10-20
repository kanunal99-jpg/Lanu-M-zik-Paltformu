package com.example

import com.example.data.MusicRepository

/**
 * Compatibility lifecycle hook for MainViewModel.
 * Closing cancels the owned repository scope and does not close the shared Room database.
 */
fun MusicRepository.close() = shutdown()
