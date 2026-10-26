package com.example.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Stream an already-verified OTA APK into Android's real in-place package installer.
 * The system requires the same package identity, compatible signing certificate,
 * higher version and explicit user consent. This class NEVER uninstalls/clears data.
 *
 * The incoming APK was verified against official GitHub release metadata and the
 * current installed signing certificate before this method was invoked. The exact
 * bytes transferred into the installation session are hashed AGAIN while writing,
 * so a file change after verification cannot be committed.
 */
internal object OtaPackageInstaller {
    const val STATUS_ACTION = "com.aistudio.lanumusic.kpxzqa.action.OTA_INSTALL_STATUS"
    private const val CHECKSUM = "SHA-256"
    private val SHA256_PATTERN = Regex("[a-fA-F0-9]{64}")

    suspend fun stageVerifiedUpdate(context: Context, apk: File, expectedSha256: String) =
        withContext(Dispatchers.IO) {
            if (!SHA256_PATTERN.matches(expectedSha256) || !apk.isFile || apk.length() == 0L) {
                throw IOException("Doğrulanmış güncelleme APK'sı bulunamadı.")
            }
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                .apply {
                    setAppPackageName(context.packageName)
                    setSize(apk.length())
                }
            val sessionId = installer.createSession(params)
            var committed = false
            try {
                installer.openSession(sessionId).use { session ->
                    val digest = MessageDigest.getInstance(CHECKSUM)
                    val expectedLength = apk.length()
                    var copied = 0L
                    apk.inputStream().buffered().use { input ->
                        session.openWrite("base.apk", 0L, expectedLength).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count == -1) break
                                digest.update(buffer, 0, count)
                                output.write(buffer, 0, count)
                                copied += count
                                if (copied > expectedLength) throw IOException("APK boyutu değişti; kurulum engellendi.")
                            }
                            session.fsync(output)
                        }
                    }
                    val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                    if (copied != expectedLength ||
                        !actualSha.equals(expectedSha256, ignoreCase = true)
                    ) throw IOException("APK kurulum sırasında değişti. Veri güvenliği için işlem durduruldu.")

                    val receiver = Intent(context, OtaInstallStatusReceiver::class.java)
                        .setAction(STATUS_ACTION)
                        .setPackage(context.packageName)
                        .putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                    val sender = PendingIntent.getBroadcast(context, sessionId, receiver, flags)
                        .intentSender
                    session.commit(sender)
                    committed = true
                }
            } finally {
                if (!committed) runCatching { installer.abandonSession(sessionId) }
            }
        }
}

/** Android reports user approval or explicit failure; no automatic uninstall or bypass. */
class OtaInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != OtaPackageInstaller.STATUS_ACTION) return
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmation: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmation == null) {
                    showStatus(context, "Android yükleyicisinin onay ekranı bulunamadı.")
                    return
                }
                try {
                    context.startActivity(confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    showStatus(context, "Android yükleyici ekranı açılamadı. LANU'yu açıp tekrar deneyin.")
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // OS owns completion and will replace this process. Never claim
                // database/playlist survival without subsequent device inspection.
            }
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                showStatus(context, "Güncelleme imzası mevcut kurulumla uyuşmuyor. Uygulamayı kaldırmayın.")
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                showStatus(context, "Android veya cihaz ilkesi güncellemeyi engelledi. Uygulamayı kaldırmayın.")
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                showStatus(context, "Güncelleme için yeterli boş alan yok.")
            else -> showStatus(context, "Android güncellemeyi tamamlayamadı. Verileriniz korunuyor.")
        }
    }

    private fun showStatus(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
