package com.example.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSourcePolicyTest {
    @Test
    fun allowsVerifiedLocalAndRemoteSchemes() {
        assertTrue(OfflineSourcePolicy.isAuthorized("content://media/external/audio/1"))
        assertTrue(OfflineSourcePolicy.isAuthorized("file:///data/user/0/com.example/files/audio.bin"))
        assertTrue(OfflineSourcePolicy.isAuthorized("https://example.test/audio.mp3"))
        assertTrue(OfflineSourcePolicy.isAuthorized("http://example.test/audio.mp3"))
    }

    @Test
    fun rejectsUnsupportedSchemes() {
        assertFalse(OfflineSourcePolicy.isAuthorized("rtmp://example.test/audio"))
        assertFalse(OfflineSourcePolicy.isAuthorized("ftp://example.test/audio.mp3"))
        assertFalse(OfflineSourcePolicy.isAuthorized("javascript:alert(1)"))
        assertFalse(OfflineSourcePolicy.isAuthorized(""))
    }
}
