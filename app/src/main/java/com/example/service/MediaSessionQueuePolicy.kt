package com.example.service

import com.example.model.Song

/**
 * Media3's default onAddMediaItems may accept caller-supplied playable URIs.
 * A media session must resolve all incoming media IDs against LANU's own
 * current, provenance-validated catalog instead.
 */
internal object MediaSessionQueuePolicy {
    const val MAX_QUEUE_ITEMS = 500
    const val MAX_MEDIA_ID_LENGTH = 512

    fun resolve(requestedIds: List<String>, catalog: List<Song>): List<Song> {
        if (requestedIds.isEmpty() || requestedIds.size > MAX_QUEUE_ITEMS) return emptyList()
        if (requestedIds.any { it.isBlank() || it.length > MAX_MEDIA_ID_LENGTH }) return emptyList()
        val trusted = catalog.asSequence()
            .filter { MediaLibrarySongPolicy.playableUri(it) != null }
            .associateBy { it.id }
        // Never partially apply an attacker-controlled multi-item queue:
        // a missing or unlicensed item rejects the whole request.
        val result = ArrayList<Song>(requestedIds.size)
        for (id in requestedIds) result.add(trusted[id] ?: return emptyList())
        return result
    }
}
