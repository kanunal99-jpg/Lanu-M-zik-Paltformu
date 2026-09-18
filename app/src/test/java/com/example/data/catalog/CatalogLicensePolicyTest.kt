package com.example.data.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogLicensePolicyTest {
    @Test
    fun allowsCc0() {
        assertTrue(CatalogLicensePolicy.isPermittedRemoteLicense("CC0 1.0 Universal"))
    }

    @Test
    fun allowsCcByUrl() {
        assertTrue(CatalogLicensePolicy.isPermittedRemoteLicense("https://creativecommons.org/licenses/by/4.0/"))
    }

    @Test
    fun allowsCcByText() {
        assertTrue(CatalogLicensePolicy.isPermittedRemoteLicense("CC BY 4.0 International"))
    }

    @Test
    fun allowsCommercialCreativeCommonsVariants() {
        assertTrue(CatalogLicensePolicy.isPermittedRemoteLicense("CC BY-SA 4.0"))
        assertTrue(CatalogLicensePolicy.isPermittedRemoteLicense("CC BY-ND 4.0"))
        assertTrue(CatalogLicensePolicy.isPermittedRemoteLicense("https://creativecommons.org/licenses/by-sa/4.0/"))
    }

    @Test
    fun allowsAudiusOpenMusicLicense() {
        assertTrue(CatalogLicensePolicy.isPermittedRemoteLicense("Audius Open Music License"))
        assertTrue(CatalogLicensePolicy.isPermittedRemoteLicense("https://audius.org/open-music-license"))
    }

    @Test
    fun rejectsNonCommercialCreativeCommons() {
        assertFalse(CatalogLicensePolicy.isPermittedRemoteLicense("CC BY-NC 4.0"))
        assertFalse(CatalogLicensePolicy.isPermittedRemoteLicense("CC BY-NC-ND 3.0"))
    }

    @Test
    fun rejectsUnknownOrMissingLicense() {
        assertFalse(CatalogLicensePolicy.isPermittedRemoteLicense(""))
        assertFalse(CatalogLicensePolicy.isPermittedRemoteLicense("All Rights Reserved"))
        assertFalse(CatalogLicensePolicy.isPermittedRemoteLicense("custom license"))
    }
}
