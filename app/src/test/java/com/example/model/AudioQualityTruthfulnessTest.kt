package com.example.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioQualityTruthfulnessTest {
    @Test fun qualityPreferencesNeverPromiseAnUnmeasuredFixedBitrate() {
        AudioQuality.values().forEach {
            assertFalse(it.bitRate.contains("128"))
            assertFalse(it.bitRate.contains("320"))
            assertFalse(it.bitRate.contains("FLAC"))
            assertFalse(it.title.contains("Kayıpsız", ignoreCase = true) &&
                it.description.contains("garanti", ignoreCase = true))
            assertTrue(it.bitRate.contains("Kaynağa bağlı"))
        }
    }
}
