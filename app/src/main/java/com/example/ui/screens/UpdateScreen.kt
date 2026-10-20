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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    val state by updates.screen.collectAsState()
    LaunchedEffect(Unit) { updates.checkForUpdates() }

    Column(
        modifier = Modifier.fillMaxSize().background(LanuDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("update_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Güncelleme Kontrol", color = LanuTextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
        Card(
            colors = CardDefaults.cardColors(containerColor = LanuDarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = LanuGreen)
                Text("LANU Müzik", color = LanuTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Yüklü sürüm: " + updates.installedVersion + " (" + updates.installedCode + ")",
                    color = LanuTextMuted, fontSize = 13.sp, modifier = Modifier.testTag("installed_version"))
                Text("Güncellemeler resmî LANU GitHub Releases kanalından denetlenir.",
                    color = LanuTextMuted, fontSize = 12.sp)
            }
        }
        when (val current = state) {
            UpdateScreenState.Idle, UpdateScreenState.Checking -> {
                CircularProgressIndicator(color = LanuGreen)
                Text("Yeni sürüm denetleniyor…", color = LanuTextPrimary, modifier = Modifier.testTag("update_checking"))
            }
            UpdateScreenState.UpToDate -> {
                Text("Yüklü sürüm için daha yeni yayın bulunamadı.", color = LanuTextPrimary,
                    modifier = Modifier.testTag("update_up_to_date"))
            }
            UpdateScreenState.WaitingForSignedApk -> {
                Text("Yeni sürüm kaydı var ancak güvenli, imzalı güncelleme APK'sı henüz yayımlanmadı.",
                    color = LanuTextPrimary, modifier = Modifier.testTag("update_no_signed_apk"))
                Text("Önizleme/debug APK dosyalarını otomatik güncelleme olarak kurmuyoruz. " +
                    "Kişisel verilerin korunması için aynı imzayla üretilmiş yayın gerekir.",
                    color = LanuTextMuted, fontSize = 13.sp)
            }
            is UpdateScreenState.Available -> {
                Text("Yeni sürüm bulundu: Build " + current.release.versionCode,
                    color = LanuGreen, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("update_available"))
                Text("Dosya boyutu: " + (current.release.sizeBytes / (1024 * 1024)) + " MB",
                    color = LanuTextMuted)
                Button(
                    onClick = updates::downloadUpdate,
                    modifier = Modifier.fillMaxWidth().testTag("download_update")
                ) { Text("Güncellemeyi indir ve doğrula") }
            }
            is UpdateScreenState.Downloading -> {
                Text("Güncelleme indiriliyor: %" + current.progress, color = LanuTextPrimary)
                LinearProgressIndicator(
                    progress = { current.progress / 100f },
                    modifier = Modifier.fillMaxWidth().testTag("update_progress"),
                    color = LanuGreen
                )
                Text("APK indirildikten sonra SHA-256, paket kimliği, sürüm ve imza denetlenecek.",
                    color = LanuTextMuted, fontSize = 12.sp)
            }
            UpdateScreenState.VerifyingInstaller -> {
                CircularProgressIndicator(color = LanuGreen)
                Text("APK imzası ve kurulum güvenliği yeniden doğrulanıyor…",
                    color = LanuTextPrimary, modifier = Modifier.testTag("ota_verifying_install"))
            }
            is UpdateScreenState.ReadyToInstall -> {
                Text("Güncelleme indirildi ve doğrulandı.", color = LanuGreen,
                    fontWeight = FontWeight.Bold, modifier = Modifier.testTag("update_verified"))
                Text(
                    if (current.permissionsNeeded)
                        "Android'in 'Bu kaynaktan yüklemeye izin ver' ayarını açıp buraya dönün."
                    else "Kurulum için Android'in onay ekranı açılacak. Mevcut uygulamayı kaldırmayın.",
                    color = LanuTextPrimary
                )
                Button(
                    onClick = { updates.requestInstall(context) },
                    modifier = Modifier.fillMaxWidth().testTag("install_verified_update")
                ) { Text("Güncellemeyi yükle") }
            }
            is UpdateScreenState.Failed -> {
                Text(current.reason, color = LanuTextPrimary, modifier = Modifier.testTag("update_error"))
            }
        }
        OutlinedButton(
            onClick = updates::checkForUpdates,
            enabled = state !is UpdateScreenState.Checking && state !is UpdateScreenState.Downloading && state !is UpdateScreenState.VerifyingInstaller,
            modifier = Modifier.fillMaxWidth().testTag("check_updates")
        ) { Text("Güncellemeleri denetle") }

        Spacer(Modifier.height(4.dp))
        Icon(Icons.Default.Security, contentDescription = null, tint = LanuGreen)
        Text(
            "LANU hiçbir zaman uygulamayı sessizce kaldırmaz. Android'in kurulum onayı ve " +
                "aynı imza zorunludur. Debug/önizleme sürümleri farklı imzalıysa veri kaybını önlemek için güncelleme engellenir.",
            color = LanuTextMuted, fontSize = 12.sp
        )
        Text(
            "Yayın kanalı: " + UpdateReleasePolicy.releasesPage,
            color = LanuTextMuted, fontSize = 11.sp
        )
    }
}
