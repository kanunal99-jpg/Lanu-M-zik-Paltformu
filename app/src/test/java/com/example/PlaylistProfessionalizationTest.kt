package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistProfessionalizationTest {
    @Test
    fun reorder_moves_song_to_requested_position() {
        val ids = listOf("a", "b", "c", "d")
        val songId = "d"
        val targetIndex = 1
        val mutable = ids.toMutableList()
        mutable.removeAt(mutable.indexOf(songId))
        mutable.add(targetIndex.coerceIn(0, mutable.size), songId)
        assertEquals(listOf("a", "d", "b", "c"), mutable)
    }

    @Test
    fun reorder_clamps_out_of_range_target() {
        val ids = listOf("a", "b", "c")
        val mutable = ids.toMutableList()
        val songId = "a"
        mutable.removeAt(0)
        mutable.add(99.coerceIn(0, mutable.size), songId)
        assertEquals(listOf("b", "c", "a"), mutable)
    }

    @Test
    fun reorder_preserves_unique_song_ids() {
        val ids = listOf("a", "b", "c")
        val mutable = ids.toMutableList()
        val songId = "b"
        mutable.removeAt(mutable.indexOf(songId))
        mutable.add(0, songId)
        assertEquals(ids.toSet().size, mutable.toSet().size)
        assertTrue(mutable.containsAll(ids))
    }
}
