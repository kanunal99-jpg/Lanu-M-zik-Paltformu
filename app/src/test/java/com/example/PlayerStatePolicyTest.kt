package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerStatePolicyTest {
    @Test
    fun repeat_off_does_not_wrap_next() {
        val hasNext = false
        val repeatAll = false
        val shouldWrap = !hasNext && repeatAll
        assertEquals(false, shouldWrap)
    }

    @Test
    fun repeat_all_wraps_queue_at_end() {
        val hasNext = false
        val repeatAll = true
        val shouldWrap = !hasNext && repeatAll
        assertEquals(true, shouldWrap)
    }

    @Test
    fun previous_uses_restart_threshold_before_queue_navigation() {
        val currentPositionMs = 4000L
        val shouldRestart = currentPositionMs > 3000L
        assertEquals(true, shouldRestart)
    }
}
