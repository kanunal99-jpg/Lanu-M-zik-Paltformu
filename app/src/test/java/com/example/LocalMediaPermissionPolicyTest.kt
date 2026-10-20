package com.example

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaPermissionPolicyTest {
    @Test fun mediaStoragePermissionChangesAtAndroid13AcrossOEMs() {
        listOf(24, 26, 29, 30, 32).forEach {
            assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE,
                LocalMediaPermissionPolicy.mediaPermission(it))
            assertNull(LocalMediaPermissionPolicy.notificationPermission(it))
        }
        listOf(33, 34, 35, 36, 37).forEach {
            assertEquals(Manifest.permission.READ_MEDIA_AUDIO,
                LocalMediaPermissionPolicy.mediaPermission(it))
            assertEquals(Manifest.permission.POST_NOTIFICATIONS,
                LocalMediaPermissionPolicy.notificationPermission(it))
        }
    }

    @Test fun firstLaunchNeverPromptsForNotificationsOnAnyAndroid() {
        for (api in listOf(24, 28, 29, 32, 33, 34, 35, 36, 37)) {
            val requested = LocalMediaPermissionPolicy.startupPermissions(api, false)
            assertEquals(listOf(LocalMediaPermissionPolicy.mediaPermission(api)), requested)
            assertTrue(Manifest.permission.POST_NOTIFICATIONS !in requested)
            assertTrue(LocalMediaPermissionPolicy.startupPermissions(api, true).isEmpty())
        }
    }
}
