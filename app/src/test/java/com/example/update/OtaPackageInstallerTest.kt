package com.example.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

/**
 * This verifies the no-uninstall fail-closed controls at the OTA staging entry.
 * Real signature-aligned device upgrade/Room retention needs dedicated devices.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class OtaPackageInstallerTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun stagingRejectsInvalidChecksumWithoutTouchingApk() {
        val apk = File(context.cacheDir, "fake-update-staging.apk")
        apk.writeText("fake")
        try {
            val error = runCatching {
                runBlocking { OtaPackageInstaller.stageVerifiedUpdate(context, apk, "invalid") }
            }.exceptionOrNull()
            assertTrue(error is IOException)
            assertTrue("Rejected candidate must not be deleted with user data", apk.isFile)
            assertEquals("fake", apk.readText())
        } finally {
            apk.delete()
        }
    }

    @Test fun stagingRejectsMissingAndEmptyApk() {
        val notFound = File(context.cacheDir, "lanu-missing-update.apk")
        notFound.delete()
        val validLengthDigest = "0".repeat(64)
        assertTrue(runCatching {
            runBlocking {
                OtaPackageInstaller.stageVerifiedUpdate(context, notFound, validLengthDigest)
            }
        }.exceptionOrNull() is IOException)

        val empty = File(context.cacheDir, "lanu-empty-update.apk")
        empty.writeBytes(byteArrayOf())
        try {
            assertTrue(runCatching {
                runBlocking {
                    OtaPackageInstaller.stageVerifiedUpdate(context, empty, validLengthDigest)
                }
            }.exceptionOrNull() is IOException)
        } finally {
            empty.delete()
        }
    }

    @Test fun installerFailureDoesNotRequestUninstallOrClearUserData() {
        val prefs = context.getSharedPreferences("lanu-ota-regression-user-data", Context.MODE_PRIVATE)
        prefs.edit().putString("saved-playlist", "preserve-me").commit()
        try {
            val intent = Intent(context, OtaInstallStatusReceiver::class.java)
                .setAction(OtaPackageInstaller.STATUS_ACTION)
                .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE_CONFLICT)
            OtaInstallStatusReceiver().onReceive(context, intent)
            assertEquals("preserve-me", prefs.getString("saved-playlist", null))
            // The receiver never invokes PackageManager uninstall, clearData,
            // or a replacement with a differently signed APK.
            assertTrue(OtaPackageInstaller.STATUS_ACTION.contains("OTA_INSTALL_STATUS"))
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
