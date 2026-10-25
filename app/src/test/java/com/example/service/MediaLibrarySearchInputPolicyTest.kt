package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaLibrarySearchInputPolicyTest {
    @Test fun turkishValidQueriesAreNotModified() {
        assertEquals("Şarkı Çağrı", MediaLibrarySearchInputPolicy.accept("  Şarkı Çağrı  "))
    }

    @Test fun blankQueryCannotTriggerFullCatalogScan() {
        assertNull(MediaLibrarySearchInputPolicy.accept(""))
        assertNull(MediaLibrarySearchInputPolicy.accept("   "))
    }

    @Test fun oversizedQueryFailsClosed() {
        assertEquals("a".repeat(200), MediaLibrarySearchInputPolicy.accept("a".repeat(200)))
        assertNull(MediaLibrarySearchInputPolicy.accept("a".repeat(201)))
        assertNull(MediaLibrarySearchInputPolicy.accept("a".repeat(100_000)))
    }

    @Test fun controlCharactersCannotEnterExternalMediaSearch() {
        assertNull(MediaLibrarySearchInputPolicy.accept("song\u0000private"))
        assertNull(MediaLibrarySearchInputPolicy.accept("song\nquery"))
        assertNull(MediaLibrarySearchInputPolicy.accept("song\tquery"))
    }
}
