package com.example.service

/**
 * Media3 controller connection is asynchronous. Keep only the most recent
 * user request until it becomes ready. No fake playback or early state writes.
 *
 * This object is owned by a Main-thread controller; it never holds Context,
 * files, streams, or a separate long-lived CoroutineScope.
 */
internal class StartupPlaybackQueue<T> {
    private var pending: T? = null
    private var closed = false

    fun submit(value: T) {
        if (!closed) pending = value
    }

    fun hasPending(): Boolean = !closed && pending != null

    fun drain(): T? {
        if (closed) return null
        val result = pending
        pending = null
        return result
    }

    fun close() {
        pending = null
        closed = true
    }
}
