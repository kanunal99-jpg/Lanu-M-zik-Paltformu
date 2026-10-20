package com.example.data

/**
 * Lifecycle hook used by ViewModel/service owners.
 * Cancels repository-owned jobs without removing the persisted library or shared database.
 */
fun MusicRepository.close() = shutdown()
