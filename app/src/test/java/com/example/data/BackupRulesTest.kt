package com.example.data

import android.content.Context
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class BackupRulesTest {
    private val expected = setOf(
        "database:lanu_music_db",
        "sharedpref:lanu_user_library.xml",
        "sharedpref:lanu_auth.xml"
    )

    private fun includedResources(resourceId: Int): Map<String, Set<String>> {
        val context: Context = RuntimeEnvironment.getApplication()
        val parser = context.resources.getXml(resourceId)
        val result = mutableMapOf<String, MutableSet<String>>()
        var section = "legacy"
        try {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "cloud-backup", "device-transfer" -> section = parser.name
                        "include" -> {
                            val domain = parser.getAttributeValue(null, "domain")
                            val path = parser.getAttributeValue(null, "path")
                            result.getOrPut(section) { mutableSetOf() }.add("$domain:$path")
                        }
                    }
                }
                parser.next()
            }
        } finally {
            parser.close()
        }
        return result
    }

    @Test fun legacyBackupContainsOnlyDurableUserState() {
        assertEquals(expected, includedResources(R.xml.backup_rules)["legacy"])
    }

    @Test fun modernBackupAndTransferExcludeCachesAndUpdaterState() {
        val records = includedResources(R.xml.data_extraction_rules)
        assertEquals(expected, records["cloud-backup"])
        assertEquals(expected, records["device-transfer"])
        assertTrue(records.values.flatten().none {
            it.contains("ota") || it.contains("cache") || it.contains("offline_audio")
        })
    }

    @Test fun cloudBackupRequiresEncryption() {
        val context: Context = RuntimeEnvironment.getApplication()
        val parser = context.resources.getXml(R.xml.data_extraction_rules)
        try {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "cloud-backup") {
                    assertEquals("true", parser.getAttributeValue(null, "disableIfNoEncryptionCapabilities"))
                    return
                }
                parser.next()
            }
            throw AssertionError("Missing cloud-backup section")
        } finally {
            parser.close()
        }
    }
}
