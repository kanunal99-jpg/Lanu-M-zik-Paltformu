package com.example

import com.example.search.TypoTolerantSearch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypoTolerantSearchTest {
    @Test
    fun matchesTurkishCharactersAndSmallTypos() {
        assertTrue(TypoTolerantSearch.matches("sezen", "Sezen Aksu"))
        assertTrue(TypoTolerantSearch.matches("sark", "Şarkı"))
        assertTrue(TypoTolerantSearch.matches("aksu", "Aksu"))
    }

    @Test
    fun rejectsUnrelatedLongQuery() {
        assertFalse(TypoTolerantSearch.matches("mozarttttt", "Sezen Aksu"))
    }

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(TypoTolerantSearch.matches("", "Herhangi Bir Şarkı"))
    }
}
