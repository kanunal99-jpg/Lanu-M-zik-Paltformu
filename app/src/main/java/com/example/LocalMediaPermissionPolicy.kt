package com.example

import android.Manifest
import android.os.Build

/**
 * OEM-neutral storage/notification permission selection based on platform API,
 * never on manufacturer. Notifications may be declined without blocking music.
 */
internal object LocalMediaPermissionPolicy {
    fun mediaPermission(apiLevel: Int): String =
        if (apiLevel >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    // No notification permission on initial launch across any Android OEM.
    // Request it later from an explicit notification feature entry point.
    fun startupPermissions(apiLevel: Int, audioAlreadyGranted: Boolean): List<String> =
        if (audioAlreadyGranted) emptyList() else listOf(mediaPermission(apiLevel))

    fun notificationPermission(apiLevel: Int): String? =
        if (apiLevel >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.POST_NOTIFICATIONS
        else null
}
