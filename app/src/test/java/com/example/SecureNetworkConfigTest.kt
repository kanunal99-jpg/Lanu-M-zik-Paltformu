package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 28, 35])
class SecureNetworkConfigTest {
    @Test fun baseNetworkPolicyDeniesUnencryptedHttpOnOldAndNewAndroid() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val xml = context.resources.getXml(R.xml.lanu_network_security_config)
        var found = false
        try {
            while (xml.eventType != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType == XmlPullParser.START_TAG && xml.name == "base-config") {
                    assertEquals("false", xml.getAttributeValue(null, "cleartextTrafficPermitted"))
                    found = true
                }
                xml.next()
            }
        } finally { xml.close() }
        assertTrue("A strict HTTPS-only base-config is required", found)
    }
}
