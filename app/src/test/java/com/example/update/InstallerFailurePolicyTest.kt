package com.example.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallerFailurePolicyTest {
    @Test fun verificationFailureDiscardsStaleOtaResumeHint() {
        assertTrue(InstallerFailurePolicy.shouldDiscardCachedVerification(false))
    }

    @Test fun settingsActivityOrInstallerLaunchFailureRetainsVerifiedOtaResumeHint() {
        assertFalse(InstallerFailurePolicy.shouldDiscardCachedVerification(true))
    }
}
