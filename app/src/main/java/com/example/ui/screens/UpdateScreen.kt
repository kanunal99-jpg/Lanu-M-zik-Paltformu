package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.remember
import com.example.update.InstalledSigningIdentity
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.update.UpdateReleasePolicy
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.update.UpdateScreenState
import com.example.update.UpdateViewModel

@Composable
fun UpdateScreen(updates: UpdateViewModel = viewModel()) {
    val context = LocalContext.current
    val state by updates.screen.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (updates.screen.value is UpdateScreenState.Idle) updates.checkForUpdates()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(LanuDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("update_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.ota_title), color = LanuTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = LanuGreen)
                Text(stringResource(R.string.app_name), color = LanuTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(stringResource(R.string.ota_installed_version, updates.installedVersion, updates.installedCode),
                    color = LanuTextMuted, fontSize = 13.sp, modifier = Modifier.testTag("installed_version"))
                Text(stringResource(R.string.ota_release_source),
                    color = LanuTextMuted, fontSize = 12.sp)
                val certificates = remember(context) { InstalledSigningIdentity.read(context) }
                val clipboard = remember(context) {
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                }
                Text(stringResource(R.string.ota_installed_certificate_title),
                    color = LanuTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (certificates.isEmpty()) {
                    Text(stringResource(R.string.ota_installed_certificate_unavailable),
                        color = LanuTextMuted, fontSize = 12.sp)
                } else {
                    certificates.forEach { certificate ->
                        Text(stringResource(R.string.ota_installed_certificate_sha256, certificate.sha256),
                            color = LanuTextMuted, fontSize = 11.sp,
                            modifier = Modifier.testTag("ota_certificate_sha256"))
                        OutlinedButton(
                            onClick = {
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("LANU installed certificate SHA-256", certificate.sha256)
                                )
                            },
                            enabled = clipboard != null,
                            modifier = Modifier.testTag("copy_ota_certificate_sha256")
                        ) { Text(stringResource(R.string.ota_copy_certificate_sha256)) }
                        Text(stringResource(R.string.ota_installed_certificate_sha1, certificate.sha1),
                            color = LanuTextMuted, fontSize = 11.sp,
                            modifier = Modifier.testTag("ota_certificate_sha1"))
                        OutlinedButton(
                            onClick = {
                                clipboard?.setPrimaryClip(
                                    ClipData.newPlainText("LANU installed certificate SHA-1", certificate.sha1)
                                )
                            },
                            enabled = clipboard != null,
                            modifier = Modifier.testTag("copy_ota_certificate_sha1")
                        ) { Text(stringResource(R.string.ota_copy_certificate_sha1)) }
                    }
                    Text(stringResource(R.string.ota_certificate_notice),
                        color = LanuTextMuted, fontSize = 11.sp)
                }
            }
        }
        when (val current = state) {
            UpdateScreenState.Idle, UpdateScreenState.Checking -> {
                CircularProgressIndicator(color = LanuGreen)
                Text(stringResource(R.string.ota_checking), color = LanuTextPrimary, modifier = Modifier.testTag("update_checking"))
            }
            UpdateScreenState.UpToDate -> {
                Text(stringResource(R.string.ota_up_to_date), color = LanuTextPrimary,
                    modifier = Modifier.testTag("update_up_to_date").semantics { liveRegion = LiveRegionMode.Polite })
            }
            UpdateScreenState.WaitingForSignedApk -> {
                Text(stringResource(R.string.ota_waiting_signed),
                    color = LanuTextPrimary, modifier = Modifier.testTag("update_no_signed_apk").semantics { liveRegion = LiveRegionMode.Polite })
                Text(stringResource(R.string.ota_waiting_signed_detail),
                    color = LanuTextMuted, fontSize = 13.sp)
            }
            is UpdateScreenState.Available -> {
                Text(stringResource(R.string.ota_update_available, current.release.versionCode),
                    color = LanuGreen, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("update_available").semantics { liveRegion = LiveRegionMode.Polite })
                Text(stringResource(R.string.ota_size_mb, current.release.sizeBytes / (1024 * 1024)),
                    color = LanuTextMuted)
                if (current.release.notes.isNotBlank()) {
                    Text(stringResource(R.string.ota_release_notes), color = LanuTextPrimary,
                        fontWeight = FontWeight.SemiBold)
                    Text(current.release.notes, color = LanuTextMuted, fontSize = 12.sp)
                }
                Button(
                    onClick = updates::downloadUpdate,
                    modifier = Modifier.fillMaxWidth().testTag("download_update")
                ) { Text(stringResource(R.string.ota_download_verify)) }
            }
            is UpdateScreenState.Downloading -> {
                Text(stringResource(R.string.ota_downloading, current.progress), color = LanuTextPrimary)
                LinearProgressIndicator(
                    progress = { current.progress / 100f },
                    modifier = Modifier.fillMaxWidth().testTag("update_progress"),
                    color = LanuGreen
                )
                Text(stringResource(R.string.ota_verify_description),
                    color = LanuTextMuted, fontSize = 12.sp)
                Text(stringResource(R.string.ota_resume_hint),
                    color = LanuTextMuted, fontSize = 12.sp)
                OutlinedButton(
                    onClick = updates::cancelDownload,
                    modifier = Modifier.fillMaxWidth().testTag("cancel_ota_download")
                ) { Text(stringResource(R.string.ota_cancel)) }
            }
            UpdateScreenState.VerifyingInstaller -> {
                CircularProgressIndicator(color = LanuGreen)
                Text(stringResource(R.string.ota_verifying_install),
                    color = LanuTextPrimary, modifier = Modifier.testTag("ota_verifying_install").semantics { liveRegion = LiveRegionMode.Polite })
            }
            is UpdateScreenState.Cancelling -> {
                CircularProgressIndicator(color = LanuGreen)
                Text(stringResource(R.string.ota_cancelling), color = LanuTextPrimary,
                    modifier = Modifier.testTag("update_cancelling").semantics { liveRegion = LiveRegionMode.Polite })
            }
            is UpdateScreenState.ReadyToInstall -> {
                Text(stringResource(R.string.ota_verified), color = LanuGreen,
                    fontWeight = FontWeight.Bold, modifier = Modifier.testTag("update_verified").semantics { liveRegion = LiveRegionMode.Polite })
                Text(
                    if (current.permissionsNeeded)
                        stringResource(R.string.ota_permission_needed)
                    else stringResource(R.string.ota_install_confirm),
                    color = LanuTextPrimary
                )
                Button(
                    onClick = { updates.requestInstall(context) },
                    modifier = Modifier.fillMaxWidth().testTag("install_verified_update")
                ) { Text(stringResource(R.string.ota_install)) }
            }
            UpdateScreenState.WaitingForAndroidApproval -> {
                Text(stringResource(R.string.ota_awaiting_android_approval),
                    color = LanuTextPrimary,
                    modifier = Modifier.testTag("ota_wait_android_approval")
                        .semantics { liveRegion = LiveRegionMode.Polite })
                Text(stringResource(R.string.ota_no_uninstall_warning),
                    color = LanuTextMuted, fontSize = 12.sp)
            }
            is UpdateScreenState.Failed -> {
                Text(current.reason, color = LanuTextPrimary, modifier = Modifier.testTag("update_error").semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        OutlinedButton(
            onClick = updates::checkForUpdates,
            enabled = state !is UpdateScreenState.Checking && state !is UpdateScreenState.Downloading && state !is UpdateScreenState.Cancelling && state !is UpdateScreenState.VerifyingInstaller,
            modifier = Modifier.fillMaxWidth().testTag("check_updates")
        ) { Text(stringResource(R.string.ota_check_button)) }

        Spacer(Modifier.height(4.dp))
        Icon(Icons.Default.Security, contentDescription = null, tint = LanuGreen)
        Text(
            stringResource(R.string.ota_legal_description),
            color = LanuTextMuted, fontSize = 12.sp
        )
        Text(
            stringResource(R.string.ota_channel, UpdateReleasePolicy.releasesPage),
            color = LanuTextMuted, fontSize = 11.sp
        )
    }
}
