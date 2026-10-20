package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineStorageReconciliationIntegrationTest {
    @Test fun startupAuditDoesNotDeleteAnUnindexedRealOfflineFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val folder = File(context.filesDir, "offline_audio")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val existing = File(folder, "reconciliation-preserve-user-media.bin")
        val bytes = "important media content that must not be deleted".toByteArray()
        existing.writeBytes(bytes)
        val repo = MusicRepository(context)
        try {
            repo.syncOfflineStorage()
            assertTrue("Startup must retain media not present in Room", existing.isFile)
            assertEquals(bytes.toList(), existing.readBytes().toList())
        } finally {
            repo.shutdown()
            existing.delete()
        }
    }
}
