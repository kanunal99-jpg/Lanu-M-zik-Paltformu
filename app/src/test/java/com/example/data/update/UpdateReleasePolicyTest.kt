package com.example.data.update

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UpdateReleasePolicyTest {
    private val sha = "a".repeat(40)
    private val base = "https://github.com/kanunal99-jpg/Lanu-M-zik-Paltformu/releases"

    private fun entry(version: Int, signed: Boolean = true, malicious: Boolean = false): JSONObject {
        val tag = "lanu-music-" + version + "-" + sha
        val prefix = if (malicious) "https://evil.example/download/" else base + "/download/" + tag + "/"
        fun asset(name: String, bytes: Long) = JSONObject()
            .put("name", name)
            .put("browser_download_url", prefix + name)
            .put("size", bytes)
        val assets = JSONArray()
        if (signed) {
            assets.put(asset("app-release.apk", 25000000))
            assets.put(asset("app-release.apk.sha256", 100))
        } else {
            assets.put(asset("app-debug.apk", 25000000))
        }
        return JSONObject()
            .put("tag_name", tag)
            .put("html_url", base + "/tag/" + tag)
            .put("draft", false)
            .put("prerelease", false)
            .put("assets", assets)
    }

    private fun list(vararg releases: JSONObject) = JSONArray().apply {
        releases.forEach { put(it) }
    }.toString()

    @Test fun newerSignedReleaseIsAvailable() {
        val result = UpdateReleasePolicy.choose(list(entry(340)), 339)
        assertTrue(result is UpdateAvailability.Available)
        assertEquals(340, (result as UpdateAvailability.Available).release.versionCode)
    }

    @Test fun neverInstallsDebugOrPreviewAsOta() {
        assertEquals(UpdateAvailability.AwaitingSignedRelease,
            UpdateReleasePolicy.choose(list(entry(345, signed = false)), 339))
    }

    @Test fun olderReleaseDoesNotTriggerUpdate() {
        assertEquals(UpdateAvailability.UpToDate, UpdateReleasePolicy.choose(list(entry(339)), 339))
        assertEquals(UpdateAvailability.UpToDate, UpdateReleasePolicy.choose(list(entry(338)), 339))
    }

    @Test fun maliciousUrlIsRejectedEvenWithRealTag() {
        assertEquals(UpdateAvailability.AwaitingSignedRelease,
            UpdateReleasePolicy.choose(list(entry(350, malicious = true)), 339))
    }

    @Test fun choosesNewestAvailableSignedReleaseRatherThanDebugBuild() {
        val releases = list(entry(345, signed = false), entry(344), entry(343))
        val result = UpdateReleasePolicy.choose(releases, 340) as UpdateAvailability.Available
        assertEquals(344, result.release.versionCode)
    }

    @Test fun malformedTagsAreNeverConsidered() {
        val invalid = JSONArray().put(JSONObject().put("tag_name", "v999").put("assets", JSONArray())).toString()
        assertEquals(UpdateAvailability.UpToDate, UpdateReleasePolicy.choose(invalid, 340))
    }

    @Test fun parsesStandardShaFileAndRejectsMalformedHash() {
        val digest = "ABCDEF0123456789".repeat(4)
        assertEquals(digest.lowercase(), UpdateReleasePolicy.parseExpectedSha256(digest + "  app-release.apk"))
        assertTrue(runCatching { UpdateReleasePolicy.parseExpectedSha256("invalid") }.exceptionOrNull() is IOException)
    }

    @Test fun newestStableReleaseIsFoundDespiteCrowdedOlderDrafts() {
        val oldDrafts = (1..40).map { entry(180 + it).put("draft", true) }
        val newest = entry(340)
        val releaseResponse = list(*(oldDrafts + newest).toTypedArray())
        val result = UpdateReleasePolicy.choose(releaseResponse, 333)
        assertEquals(340, (result as UpdateAvailability.Available).release.versionCode)
    }

    @Test fun doesNotSelectAnUnpublishedDraftOrPreRelease() {
        assertEquals(UpdateAvailability.UpToDate,
            UpdateReleasePolicy.choose(list(
                entry(400).put("draft", true),
                entry(399).put("prerelease", true)
            ), 350)
        )
    }

    @Test fun refusesIncompleteSignatureOrApkAssets() {
        val broken = entry(350)
        val assets = broken.getJSONArray("assets")
        assets.getJSONObject(0).put("browser_download_url", "https://github.com/evil/app-release.apk")
        assertEquals(UpdateAvailability.AwaitingSignedRelease,
            UpdateReleasePolicy.choose(list(broken), 340))
        assets.getJSONObject(0).put("browser_download_url",
            base + "/download/lanu-music-350-" + sha + "/app-release.apk")
        assets.getJSONObject(0).put("size", 300L * 1024L * 1024L)
        assertEquals(UpdateAvailability.AwaitingSignedRelease,
            UpdateReleasePolicy.choose(list(broken), 340))
    }

    @Test fun choosesOlderSignedIfLatestDebugOnlyInSameBatch() {
        val releases = list(entry(360, signed = false), entry(359, signed = true))
        val result = UpdateReleasePolicy.choose(releases, 350) as UpdateAvailability.Available
        assertEquals(359, result.release.versionCode)
    }

    @Test fun emptyReleasesDoNotCreatePhantomUpdate() {
        assertEquals(UpdateAvailability.UpToDate, UpdateReleasePolicy.choose("[]", 341))
    }
}
