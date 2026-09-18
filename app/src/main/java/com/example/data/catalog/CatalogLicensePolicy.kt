package com.example.data.catalog

/**
 * Single source of truth for remote content that LANU may treat as continuously playable.
 * Unknown/custom licenses stay blocked; only explicit CC0 or CC BY are accepted.
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

        return value.contains("cc0") ||
            value.contains("creative commons zero") ||
            value.contains("creativecommons.org/licenses/by/") ||
            Regex("""(^|[^a-z])cc[- ]?by(?:[ -][0-9.]+)?(?:[ -]international)?$""")
                .containsMatchIn(value)
    }
}
