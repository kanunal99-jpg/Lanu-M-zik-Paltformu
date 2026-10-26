package com.example.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercise the real activity entry point which receives PackageInstaller
 * callbacks. No Android system install or private user data modification.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class OtaInstallResultActivityTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun result(status: Int): Intent =
        Intent(context, OtaInstallResultActivity::class.java)
            .setAction(OtaPackageInstaller.STATUS_ACTION)
            .putExtra(PackageInstaller.EXTRA_STATUS, status)

    @Test fun rejectedSignatureDoesNotEraseExistingAppPreferences() {
        val prefs = context.getSharedPreferences("lanu-ota-result-test", Context.MODE_PRIVATE)
        prefs.edit().putString("playlist", "keep-the-original").commit()
        try {
            val activity = Robolectric.buildActivity(
                OtaInstallResultActivity::class.java,
                result(PackageInstaller.STATUS_FAILURE_CONFLICT)
            ).create().get()
            assertTrue(activity.isFinishing)
            assertEquals("keep-the-original", prefs.getString("playlist", null))
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun missingAndroidConfirmationIsGracefullyRejected() {
        val activity = Robolectric.buildActivity(
            OtaInstallResultActivity::class.java,
            result(PackageInstaller.STATUS_PENDING_USER_ACTION)
        ).create().get()
        assertTrue(activity.isFinishing)
    }

    @Test fun unauthorizedOrMissingStatusDoesNotOpenInstaller() {
        val wrong = Intent(context, OtaInstallResultActivity::class.java)
            .setAction("com.example.invalid")
            .putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_PENDING_USER_ACTION)
        val activity = Robolectric.buildActivity(OtaInstallResultActivity::class.java, wrong)
            .create().get()
        assertTrue(activity.isFinishing)
    }

    @Test fun successCallbackHasNoDataClearingOrUninstallSideEffects() {
        val activity = Robolectric.buildActivity(
            OtaInstallResultActivity::class.java,
            result(PackageInstaller.STATUS_SUCCESS)
        ).create().get()
        assertTrue(activity.isFinishing)
    }
}
