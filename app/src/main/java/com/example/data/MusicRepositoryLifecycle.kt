package com.example.data

/**
 * Lifecycle hook used by ViewModel/service owners.
 * Repository resources are application-scoped; this hook is intentionally safe to call multiple times.
 */
fun MusicRepository.close() = Unit
