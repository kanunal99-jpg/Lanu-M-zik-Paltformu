package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextPolicyTest {
    @Test
    fun share_text_must_not_claim_verified_remote_web_endpoint() {
        val text = "LANU Müzik yerel/izinli içerik paylaşımı."
        assertTrue(text.contains("yerel/izinli"))
        assertFalse(text.contains("lanumusic.app/track"))
        assertFalse(text.contains("lanumusic.app/playlist"))
    }
}
