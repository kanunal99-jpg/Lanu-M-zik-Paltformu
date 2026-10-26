package com.example.update

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast

/**
 * Receives Android PackageInstaller commit status in an actual foreground
 * Activity. A BroadcastReceiver is not allowed to start install confirmation
 * from the background on modern Android versions.
 *
 * Internal-only component; the status PendingIntent is explicit and mutable
 * only because PackageInstaller must populate EXTRA_STATUS / EXTRA_INTENT.
 * NEVER uninstalls or clears app data.
 */
class OtaInstallResultActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleInstallStatus(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInstallStatus(intent)
    }

    private fun handleInstallStatus(result: Intent?) {
        if (result?.action != OtaPackageInstaller.STATUS_ACTION) {
            finish()
            return
        }
        val status = result.getIntExtra(
            PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE
        )
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    result.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                }
                if (confirmation == null) {
                    showFailure("Android kurulum onayı oluşturulamadı. LANU güncellemeler ekranından tekrar deneyin.")
                } else {
                    try {
                        // This call originates from a foreground Activity, not a
                        // background receiver. Android still requires the user
                        // to accept the system-owned installation confirmation.
                        startActivity(confirmation)
                    } catch (failure: RuntimeException) {
                        Log.w("LanuOtaInstaller", "System install confirmation was not launched", failure)
                        showFailure("Android kurulum onayı açılamadı. LANU içindeki alternatif yükleyiciyi deneyin.")
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // Android owns the result. Never claim Room data preserved
                // until an actual post-update device smoke test verifies it.
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                showFailure("APK imzası uyuşmuyor. Veri kaybını önlemek için kurulum durduruldu.")
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                showFailure("APK kurulumu için yeterli boş alan yok.")
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                showFailure("Android paket kurulumunu engelledi. LANU uygulamasını kaldırmayın.")
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                showFailure("Güncelleme kurulumu iptal edildi.")
            else -> {
                Log.w("LanuOtaInstaller", "Android package installer failed; status=" + status)
                showFailure("Güncelleme tamamlanamadı. Mevcut uygulama korunuyor.")
            }
        }
        finish()
    }

    private fun showFailure(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
