package com.example.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Read-only certificate identity of the CURRENTLY INSTALLED app.
 *
 * Fingerprints are public metadata: no private signing keys, keystore files,
 * passwords or API keys are requested or stored. A matching fingerprint is
 * necessary, not sufficient, for a safe OTA upgrade.
 */
internal data class InstalledCertificateFingerprints(
    val sha1: String,
    val sha256: String
)

internal object InstalledSigningIdentity {
    internal fun digestHex(certificate: ByteArray, algorithm: String): String =
        MessageDigest.getInstance(algorithm).digest(certificate)
            .joinToString("") { byte -> "%02x".format(byte) }

    fun read(context: Context): List<InstalledCertificateFingerprints> {
        return runCatching {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            @Suppress("DEPRECATION")
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                packageInfo.signatures
            }
            signatures.orEmpty().map { certificate ->
                val bytes = certificate.toByteArray()
                InstalledCertificateFingerprints(
                    sha1 = digestHex(bytes, "SHA-1"),
                    sha256 = digestHex(bytes, "SHA-256")
                )
            }.distinctBy { it.sha256 }.sortedBy { it.sha256 }
        }.getOrDefault(emptyList())
    }
}
