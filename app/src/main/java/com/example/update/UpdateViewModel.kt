package com.example.update

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.update.GithubUpdateClient
import com.example.data.update.UpdateAvailability
import com.example.data.update.VerifiedRelease
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateScreenState {
    data object Idle : UpdateScreenState
    data object Checking : UpdateScreenState
    data object UpToDate : UpdateScreenState
    data object WaitingForSignedApk : UpdateScreenState
    data class Available(val release: VerifiedRelease) : UpdateScreenState
    data class Downloading(val release: VerifiedRelease, val progress: Int) : UpdateScreenState
    data class ReadyToInstall(val release: VerifiedRelease, val file: File, val permissionsNeeded: Boolean = false) : UpdateScreenState
    data class Failed(val reason: String) : UpdateScreenState
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val updater = GithubUpdateClient(application)
    private val _screen = MutableStateFlow<UpdateScreenState>(UpdateScreenState.Idle)
    val screen: StateFlow<UpdateScreenState> = _screen

    val installedVersion: String get() = BuildConfig.VERSION_NAME
    val installedCode: Int get() = BuildConfig.VERSION_CODE

    fun checkForUpdates() {
        if (_screen.value is UpdateScreenState.Checking || _screen.value is UpdateScreenState.Downloading) return
        viewModelScope.launch {
            _screen.value = UpdateScreenState.Checking
            try {
                _screen.value = when (val result = updater.check(installedCode)) {
                    is UpdateAvailability.Available -> UpdateScreenState.Available(result.release)
                    UpdateAvailability.UpToDate -> UpdateScreenState.UpToDate
                    UpdateAvailability.AwaitingSignedRelease -> UpdateScreenState.WaitingForSignedApk
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _screen.value = UpdateScreenState.Failed(
                    "Sürüm bilgisi alınamadı: " + (e.message ?: "İnternet bağlantısını kontrol edin.")
                )
            }
        }
    }

    fun downloadUpdate() {
        val available = _screen.value as? UpdateScreenState.Available ?: return
        _screen.value = UpdateScreenState.Downloading(available.release, 0)
        viewModelScope.launch {
            try {
                val downloaded = updater.downloadAndVerify(available.release) { percent ->
                    _screen.value = UpdateScreenState.Downloading(available.release, percent)
                }
                _screen.value = UpdateScreenState.ReadyToInstall(available.release, downloaded)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _screen.value = UpdateScreenState.Failed(
                    "Güncelleme kuruluma hazırlanamadı: " + (e.message ?: "Bilinmeyen hata.")
                )
            }
        }
    }

    /** This opens Android's package installer; silent installation is deliberately not attempted. */
    fun requestInstall(context: Context) {
        val ready = _screen.value as? UpdateScreenState.ReadyToInstall ?: return
        try {
            if (!ready.file.isFile) {
                _screen.value = UpdateScreenState.Failed("İndirilen APK bulunamadı. Tekrar denetleyin.")
                return
            }
            updater.verifyApk(ready.file, ready.release.versionCode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                _screen.value = ready.copy(permissionsNeeded = true)
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
            _screen.value = ready.copy(permissionsNeeded = false)
            val uri = FileProvider.getUriForFile(
                context, context.packageName + ".ota.fileprovider", ready.file
            )
            val installIntent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _screen.value = UpdateScreenState.Failed(
                "Android kurulum ekranı açılamadı: " + (e.message ?: "Bilinmeyen hata.")
            )
        }
    }
}
