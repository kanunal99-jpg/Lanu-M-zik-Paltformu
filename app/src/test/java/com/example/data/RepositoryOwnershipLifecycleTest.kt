package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryOwnershipLifecycleTest {
    @Test fun closingActiveRepositoryRemovesStaleGlobalRegistryPointer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicRepository(context)
        try {
            assertSame(repo, RepositoryRegistry.repository)
        } finally {
            repo.shutdown()
        }
        assertNull(RepositoryRegistry.repository)
    }

    @Test fun closingOlderRepositoryNeverClearsNewOwnersRegistry() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val older = MusicRepository(context)
        try {
            val newer = MusicRepository(context)
            try {
                older.shutdown()
                assertSame(newer, RepositoryRegistry.repository)
            } finally {
                newer.shutdown()
            }
        } finally {
            older.shutdown()
        }
        assertNull(RepositoryRegistry.repository)
    }
}
