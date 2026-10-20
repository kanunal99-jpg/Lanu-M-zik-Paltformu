package com.example.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UpdateReleasePolicyTest {
    private val sha = "a".repeat(40)
    private val base = "https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases"
    private fun entry(version: Int, signed: Boolean = true, malicious: Boolean = false): String {
        val tag = "lanu-music-" + version + "-" + sha
        val prefix = if (malicious) "https://evil.example/download/" else base + "/download/" + tag + "/"
        val assets = if (signed) {
            """[{"name":"app-release.apk","browser_download_url":"""" +
                prefix + """app-release.apk","size":25000000},{"name":"app-release.apk.sha256","browser_download_url":"""" +
                prefix + """app-release.apk.sha256","size":100}]"""
        } else {
            """[{"name":"app-debug.apk","browser_download_url":"""" +
                prefix + """app-debug.apk","size":25000000}]"""
        }
        return """{"tag_name":"""" + tag + """","html_url":"""" + base + "/tag/" + tag +
            """","draft":false,"prerelease":false,"assets":""" + assets + "}"
    }

    @Test fun newerSignedReleaseIsAvailable() {
        val result = UpdateReleasePolicy.choose("[" + entry(340) + "]", 339)
        assertTrue(result is UpdateAvailability.Available)
        assertEquals(340, (result as UpdateAvailability.Available).release.versionCode)
    }

    @Test fun neverInstallsDebugOrPreviewAsOta() {
        assertEquals(UpdateAvailability.AwaitingSignedRelease,
            UpdateReleasePolicy.choose("[" + entry(345, signed = false) + "]", 339))
    }

    @Test fun olderReleaseDoesNotTriggerUpdate() {
        assertEquals(UpdateAvailability.UpToDate, UpdateReleasePolicy.choose("[" + entry(339) + "]", 339))
        assertEquals(UpdateAvailability.UpToDate, UpdateReleasePolicy.choose("[" + entry(338) + "]", 339))
    }

    @Test fun maliciousUrlIsRejectedEvenWithRealTag() {
        assertEquals(UpdateAvailability.AwaitingSignedRelease,
            UpdateReleasePolicy.choose("[" + entry(350, malicious = true) + "]", 339))
    }

    @Test fun choosesNewestAvailableSignedReleaseRatherThanDebugBuild() {
        val releases = "[" + entry(345, signed = false) + "," + entry(344) + "," + entry(343) + "]"
        val result = UpdateReleasePolicy.choose(releases, 340) as UpdateAvailability.Available
        assertEquals(344, result.release.versionCode)
    }

    @Test fun malformedTagsAreNeverConsidered() {
        val invalid = """[{"tag_name":"v999","html_url":"https://example.com","assets":[]}]"""
        assertEquals(UpdateAvailability.UpToDate, UpdateReleasePolicy.choose(invalid, 340))
    }

    @Test fun parsesStandardShaFileAndRejectsMalformedHash() {
        val digest = "ABCDEF0123456789".repeat(4)
        assertEquals(digest.lowercase(), UpdateReleasePolicy.parseExpectedSha256(digest + "  app-release.apk"))
        assertTrue(runCatching { UpdateReleasePolicy.parseExpectedSha256("invalid") }.exceptionOrNull() is IOException)
    }

    @Test fun emptyReleasesDoNotCreatePhantomUpdate() {
        assertEquals(UpdateAvailability.UpToDate, UpdateReleasePolicy.choose("[]", 341))
    }
}
