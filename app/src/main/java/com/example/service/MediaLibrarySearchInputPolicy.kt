package com.example.service

/**
 * Android Auto, wearables and other media browsers can request a search.
 * Apply validation before running any full catalog scan or fuzzy matching.
 * Invalid input is a safe empty result; it is never interpreted as "all songs".
 */
internal object MediaLibrarySearchInputPolicy {
    const val MAX_QUERY_LENGTH = 200

    fun accept(raw: String): String? {
        if (raw.length > MAX_QUERY_LENGTH || raw.any { Character.isISOControl(it) }) return null
        return raw.trim().takeIf { it.isNotEmpty() }
    }
}
