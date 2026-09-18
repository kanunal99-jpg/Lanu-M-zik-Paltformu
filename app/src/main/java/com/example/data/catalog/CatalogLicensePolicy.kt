package com.example.data.catalog

/**
 * Single source of truth for remote content that LANU may treat as continuously playable.
 * UNKNOWN/NC/All-Rights-Reserved licenses stay blocked.
 *
 * Audius' Open Music License explicitly grants Music Players rights to stream and
 * publicly perform Licensed Material; commercial use requires the attribution terms
 * from the license. Creative Commons BY/BY-SA/BY-ND permit commercial use with their
 * respective attribution/share-alike/no-derivatives conditions.
 */
object CatalogLicensePolicy {
    fun isPermittedRemoteLicense(rawLicense: String): Boolean {
        val value = rawLicense.trim().lowercase()
        if (value.isBlank()) return false
        if (value.contains("noncommercial") ||
            value.contains("non-commercial") ||
            value.contains("cc by-nc") ||
            value.contains("cc-by-nc")
        ) return false
        if (value.contains("all rights reserved") || value.contains("all-rights-reserved")) return false

        return value.contains("open music license") ||
            value.contains("audius open music license") ||
            value.contains("openmusiclicense") ||
            value.contains("creativecommons.org/licenses/by/") ||
            value.contains("creativecommons.org/licenses/by-sa/") ||
            value.contains("creativecommons.org/licenses/by-nd/") ||
            Regex("""(^|[^a-z])cc[- ]?by(?:[- ]?(?:sa|nd))?(?:[ -][0-9.]+)?(?:[ -]international)?$""")
                .containsMatchIn(value)
    }
}
