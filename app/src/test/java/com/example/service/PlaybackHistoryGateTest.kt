package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackHistoryGateTest {
    @Test fun tapOrPreparationNeverRecords() {
        val gate = PlaybackHistoryGate()
        assertNull(gate.onPlaybackState("local:1", false))
        assertNull(gate.onPlaybackState("local:1", false))
        assertEquals("local:1", gate.onPlaybackState("local:1", true))
    }

    @Test fun duplicateEventsDoNotDuplicateHistory() {
        val gate = PlaybackHistoryGate()
        assertEquals("audius:1", gate.onPlaybackState("audius:1", true))
        assertNull(gate.onPlaybackState("audius:1", true))
        assertNull(gate.onPlaybackState("audius:1", true))
    }

    @Test fun nextSongPlayingIsNewHistory() {
        val gate = PlaybackHistoryGate()
        assertEquals("local:1", gate.onPlaybackState("local:1", true))
        assertEquals("local:2", gate.onPlaybackState("local:2", true))
    }

    @Test fun realResumeRecordsNewListeningStart() {
        val gate = PlaybackHistoryGate()
        assertEquals("local:1", gate.onPlaybackState("local:1", true))
        assertNull(gate.onPlaybackState("local:1", false))
        assertEquals("local:1", gate.onPlaybackState("local:1", true))
    }

    @Test fun missingTrackCannotBeHistory() {
        val gate = PlaybackHistoryGate()
        assertNull(gate.onPlaybackState(null, true))
        assertNull(gate.onPlaybackState("", true))
        assertNull(gate.onPlaybackState(" ", true))
    }

    @Test fun failedPlaybackProducesNoHistory() {
        val gate = PlaybackHistoryGate()
        assertNull(gate.onPlaybackState("deezer:1", false))
        assertNull(gate.onPlaybackState(null, false))
        assertNull(gate.onPlaybackState("deezer:1", false))
    }
}
