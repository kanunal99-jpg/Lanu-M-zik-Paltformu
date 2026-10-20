package com.example.update

/** Never destroy a previously hash/signature-verified APK hint on OEM installer UI failure. */
internal object InstallerFailurePolicy {
    fun shouldDiscardCachedVerification(cryptographicVerificationPassed: Boolean): Boolean =
        !cryptographicVerificationPassed
}
