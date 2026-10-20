package com.example.update

import android.content.Context
import com.example.data.update.VerifiedRelease
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OtaSessionStoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val sessions: OtaSessionStore get() = OtaSessionStore(context)
    private val base = "https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases"

    private fun release(code: Int): VerifiedRelease {
        val tag = "lanu-music-" + code + "-" + "a".repeat(40)
        return VerifiedRelease(code, tag, base + "/tag/" + tag,
            base + "/download/" + tag + "/app-release.apk",
            base + "/download/" + tag + "/app-release.apk.sha256",
            30000000L, "b".repeat(64))
    }

    @After fun cleanUp() { sessions.clear() }

    @Test fun saveRestoreReconstructsOfficialLinksWithoutPersistingArbitraryUrl() {
        val release = release(345)
        sessions.save(release, "c".repeat(64))
        val restored = sessions.restore(344)
        assertNotNull(restored)
        assertEquals(release, restored?.release)
        assertEquals("c".repeat(64), restored?.expectedSha256)
        assertEquals("lanu-345.apk", restored?.apk?.name)
    }

    @Test fun installedOrDowngradeVersionInvalidatesPendingSession() {
        sessions.save(release(345), "c".repeat(64))
        assertNull(sessions.restore(345))
        assertNull(sessions.restore(344))
    }

    @Test fun installerRejectsCacheTamperingAndOutsidePaths() {
        val code = 355
        val directory = java.io.File(context.cacheDir, "lanu_updates")
        directory.mkdirs()
        val file = java.io.File(directory, "lanu-" + code + ".apk")
        file.writeText("APK changed on disk")
        try {
            val updater = com.example.data.update.GithubUpdateClient(context)
            val fakeSha = "0".repeat(64)
            val tampered = runCatching {
                kotlinx.coroutines.runBlocking {
                    updater.verifyForInstall(file, code, fakeSha)
                }
            }.exceptionOrNull()
            org.junit.Assert.assertTrue(tampered is java.io.IOException)
            val other = java.io.File(context.cacheDir, "another.apk")
            other.writeText("wrong-path")
            val wrongPath = runCatching {
                kotlinx.coroutines.runBlocking {
                    updater.verifyForInstall(other, code, fakeSha)
                }
            }.exceptionOrNull()
            org.junit.Assert.assertTrue(wrongPath is java.io.IOException)
            other.delete()
        } finally {
            file.delete()
        }
    }

    @Test fun forgedTagAndInvalidHashNeverBecomeReady() {
        sessions.save(release(345), "c".repeat(64))
        val prefs = context.getSharedPreferences("lanu_ota_verified_session", Context.MODE_PRIVATE)
        prefs.edit().putString("tag", "https://evil.test").commit()
        assertNull(sessions.restore(344))
        sessions.save(release(346), "c".repeat(64))
        prefs.edit().putString("sha256", "not-a-sha256").commit()
        assertNull(sessions.restore(344))
    }

    @Test fun rejectsMismatchedTagVersionWithoutOverwritingLastVerifiedSession() {
        val valid = release(345)
        sessions.save(valid, "c".repeat(64))
        val mismatched = valid.copy(versionCode = 346)
        org.junit.Assert.assertTrue(
            runCatching { sessions.save(mismatched, "d".repeat(64)) }.isFailure
        )
        assertEquals(345, sessions.restore(344)?.release?.versionCode)
        assertEquals("c".repeat(64), sessions.restore(344)?.expectedSha256)
    }

    @Test fun refusesUntrustedUrlsOversizeAndInvalidGitHubDigestBeforeWriting() {
        val valid = release(345)
        sessions.save(valid, "c".repeat(64))
        val tampered = listOf(
            valid.copy(apkUrl = "https://evil.example/app-release.apk"),
            valid.copy(sha256Url = "https://evil.example/app-release.apk.sha256"),
            valid.copy(pageUrl = "https://evil.example/releases"),
            valid.copy(sizeBytes = 0L),
            valid.copy(sizeBytes = 201L * 1024L * 1024L),
            valid.copy(githubDigest = "invalid")
        )
        for (candidate in tampered) {
            org.junit.Assert.assertTrue(candidate.toString(),
                runCatching { sessions.save(candidate, "d".repeat(64)) }.isFailure)
        }
        val restored = sessions.restore(344)
        assertEquals(valid, restored?.release)
        assertEquals("c".repeat(64), restored?.expectedSha256)
    }

    @Test fun invalidExpectedShaDoesNotClobberPreviousResumeHint() {
        sessions.save(release(345), "c".repeat(64))
        org.junit.Assert.assertTrue(
            runCatching { sessions.save(release(346), "not-a-hash") }.isFailure
        )
        assertEquals(345, sessions.restore(344)?.release?.versionCode)
    }
}
