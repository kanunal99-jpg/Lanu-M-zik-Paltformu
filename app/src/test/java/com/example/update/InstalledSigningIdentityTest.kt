package com.example.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class InstalledSigningIdentityTest {
    @Test fun certificateDigestMatchesStandardShaValues() {
        val input = "abc".toByteArray(Charsets.UTF_8)
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d",
            InstalledSigningIdentity.digestHex(input, "SHA-1"))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            InstalledSigningIdentity.digestHex(input, "SHA-256"))
    }

    @Test fun digestChangesWithCertificateBytes() {
        val first = InstalledSigningIdentity.digestHex(byteArrayOf(1, 2, 3), "SHA-256")
        val second = InstalledSigningIdentity.digestHex(byteArrayOf(1, 2, 4), "SHA-256")
        assertEquals(64, first.length)
        assertNotEquals(first, second)
    }
}
