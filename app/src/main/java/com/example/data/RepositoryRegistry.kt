package com.example.data

/**
 * Process-local bridge for UI surfaces that are already hosted by the app's
 * single MainViewModel repository. It avoids constructing a second repository
 * instance from Compose while keeping the storage implementation provider-agnostic.
 */
object RepositoryRegistry {
    @Volatile
    var repository: MusicRepository? = null

    @Synchronized fun register(owner: MusicRepository) {
        repository = owner
    }

    @Synchronized fun clearIfOwner(owner: MusicRepository) {
        if (repository === owner) repository = null
    }
}
