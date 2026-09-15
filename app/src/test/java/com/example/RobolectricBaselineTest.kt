package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RobolectricBaselineTest {
    @Test
    fun androidRuntimeIsAvailable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.aistudio.lanumusic.kpxzqa", context.packageName)
    }
}
