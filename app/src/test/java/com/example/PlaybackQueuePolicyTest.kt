package com.example

import com.example.service.PlaybackQueuePolicy
import com.example.service.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueuePolicyTest {
    @Test
    fun `next moves forward inside queue`() {
        assertEquals(
            PlaybackQueuePolicy.NextAction.MoveTo(2),
            PlaybackQueuePolicy.nextAction(currentIndex = 1, queueSize = 4, repeatMode = RepeatMode.OFF)
        )
    }

    @Test
    fun `next stops at end when repeat is off`() {
        assertEquals(
            PlaybackQueuePolicy.NextAction.Stop,
            PlaybackQueuePolicy.nextAction(currentIndex = 3, queueSize = 4, repeatMode = RepeatMode.OFF)
        )
    }

    @Test
    fun `next wraps at end for repeat all`() {
        assertEquals(
            PlaybackQueuePolicy.NextAction.MoveTo(0),
            PlaybackQueuePolicy.nextAction(currentIndex = 3, queueSize = 4, repeatMode = RepeatMode.ALL)
        )
    }

    @Test
    fun `previous restarts current track after three seconds`() {
        assertEquals(
            PlaybackQueuePolicy.PreviousAction.RestartCurrent,
            PlaybackQueuePolicy.previousAction(currentIndex = 0, currentPositionMs = 3_001L, queueSize = 4, repeatMode = RepeatMode.OFF)
        )
    }

    @Test
    fun `previous moves backward before first track`() {
        assertEquals(
            PlaybackQueuePolicy.PreviousAction.MoveTo(1),
            PlaybackQueuePolicy.previousAction(currentIndex = 2, currentPositionMs = 500L, queueSize = 4, repeatMode = RepeatMode.OFF)
        )
    }

    @Test
    fun `previous wraps to last for repeat all`() {
        assertEquals(
            PlaybackQueuePolicy.PreviousAction.MoveTo(3),
            PlaybackQueuePolicy.previousAction(currentIndex = 0, currentPositionMs = 500L, queueSize = 4, repeatMode = RepeatMode.ALL)
        )
    }
}
