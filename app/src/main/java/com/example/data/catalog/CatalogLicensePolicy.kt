package com.example.data.catalog

/**
 * Single source of truth for remote content that LANU may treat as continuously playable.
 *
 * Audius applies its Open Music License (OML) to API-accessed content unless a creator
 * supplies an alternative license. Audius' current terms explicitly describe Music Player
 * rights to stream and publicly perform the licensed material, with attribution obligations
 * for commercial use. Creative Commons BY/BY-SA/BY-ND are also commercially usable under
 * their respective conditions. NC variants stay blocked.
 */
object CatalogLicensePolicy {
    private const val AUDIUS_OML = "Audius Open Music License (default API license)"

    fun isPermittedRemoteLicense(rawLicense: String): Boolean {
        val value = rawLicense.trim().lowercase()
        if (value.isBlank()) return false
        if (value.contains("noncommercial") ||
            value.contains("non-commercial") ||
            value.contains("cc by-nc") ||
            value.contains("cc-by-nc")
        ) return false
        if (value.contains("all rights reserved") || value.contains("all-rights-reserved")) return false

        return value.contains("cc0") ||
            value.contains("creative commons zero") ||
            isExplicitOpenLicense(value)
    }

    fun isPermittedAudiusLicense(rawLicense: String): Boolean {
        val value = rawLicense.trim().lowercase()
        if (value.isBlank()) return false
        if (value.contains("noncommercial") ||
            value.contains("non-commercial") ||
            value.contains("cc by-nc") ||
            value.contains("cc-by-nc")
        ) return false

        return value.contains("cc0") ||
            value.contains("creative commons zero") ||
            isExplicitOpenLicense(value) ||
            value.contains("all rights reserved") ||
            value.contains("all-rights-reserved")
    }

    fun effectiveAudiusLicense(rawLicense: String): String {
        val value = rawLicense.trim().lowercase()
        return if (value.contains("all rights reserved") || value.contains("all-rights-reserved")) {
            AUDIUS_OML
        } else {
            rawLicense.trim()
        }
    }

    fun isAudiusOmlLicense(rawLicense: String): Boolean =
        effectiveAudiusLicense(rawLicense).contains("audius open music license", ignoreCase = true)

    private fun isExplicitOpenLicense(value: String): Boolean =
        value.contains("open music license") ||
            value.contains("open-music-license") ||
            value.contains("audius open music license") ||
            value.contains("audius-open-music-license") ||
            value.contains("openmusiclicense") ||
            value.contains("creativecommons.org/licenses/by/") ||
            value.contains("creativecommons.org/licenses/by-sa/") ||
            value.contains("creativecommons.org/licenses/by-nd/") ||
            Regex("""(^|[^a-z])cc[- ]?by(?:[- ]?(?:sa|nd))?(?:[ -][0-9.]+)?(?:[ -]international)?$""")
                .containsMatchIn(value)
}
