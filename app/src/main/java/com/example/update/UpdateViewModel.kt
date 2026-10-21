package com.example.update

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.update.GithubUpdateClient
import com.example.data.update.UpdateAvailability
import com.example.data.update.VerifiedRelease
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.io.File

sealed interface UpdateScreenState {
    data object Idle : UpdateScreenState
    data object Checking : UpdateScreenState
    data object UpToDate : UpdateScreenState
    data object WaitingForSignedApk : UpdateScreenState
    data class Available(val release: VerifiedRelease) : UpdateScreenState
    data class Downloading(val release: VerifiedRelease, val progress: Int) : UpdateScreenState
    data class Cancelling(val release: VerifiedRelease) : UpdateScreenState
    data class ReadyToInstall(val release: VerifiedRelease, val file: File, val expectedSha256: String, val permissionsNeeded: Boolean = false) : UpdateScreenState
    data object VerifyingInstaller : UpdateScreenState
    data class Failed(val reason: String) : UpdateScreenState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val updater = GithubUpdateClient(application)
    private val sessions = OtaSessionStore(application)
    private var activeDownload: Job? = null
    private val _screen = MutableStateFlow<UpdateScreenState>(UpdateScreenState.Idle)
    val screen: StateFlow<UpdateScreenState> = _screen

    val installedVersion: String get() = BuildConfig.VERSION_NAME
    val installedCode: Int get() = BuildConfig.VERSION_CODE

    /** Crash/reboot recovery performs real cryptographic re-verification, never trusts SharedPreferences alone. */
    fun checkOnAppLaunch() {
        if (_screen.value !is UpdateScreenState.Idle) return
        val stored = sessions.restore(installedCode)
        if (stored != null) {
            _screen.value = UpdateScreenState.VerifyingInstaller
            viewModelScope.launch {
                try {
                    updater.verifyForInstall(stored.apk, stored.release.versionCode, stored.expectedSha256)
                    _screen.value = UpdateScreenState.ReadyToInstall(
                        stored.release, stored.apk, stored.expectedSha256
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    sessions.clear()
                    _screen.value = UpdateScreenState.Idle
                    checkForUpdates()
                }
            }
            return
        }
        val prefs = getApplication<Application>()
            .getSharedPreferences("lanu_ota_check", Context.MODE_PRIVATE)
        if (OtaCheckCooldownPolicy.canSkipCheck(prefs, System.currentTimeMillis())) return
        checkForUpdates()
    }

    fun checkForUpdates() {
        if (_screen.value is UpdateScreenState.Checking || _screen.value is UpdateScreenState.Downloading || _screen.value is UpdateScreenState.Cancelling || _screen.value is UpdateScreenState.VerifyingInstaller) return
        _screen.value = UpdateScreenState.Checking
        viewModelScope.launch {
            try {
                _screen.value = when (val result = updater.check(installedCode)) {
                    is UpdateAvailability.Available -> UpdateScreenState.Available(result.release)
                    UpdateAvailability.UpToDate -> UpdateScreenState.UpToDate
                    UpdateAvailability.AwaitingSignedRelease -> UpdateScreenState.WaitingForSignedApk
                }
                getApplication<Application>()
                    .getSharedPreferences("lanu_ota_check", Context.MODE_PRIVATE)
                    .edit().putLong("last_successful_check", System.currentTimeMillis()).apply()
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
        activeDownload = viewModelScope.launch {
            try {
                val downloaded = updater.downloadAndVerify(available.release) { percent ->
                    if (_screen.value is UpdateScreenState.Downloading) {
                        _screen.value = UpdateScreenState.Downloading(available.release, percent)
                    }
                }
                if (_screen.value is UpdateScreenState.Downloading) {
                    sessions.save(available.release, downloaded.sha256)
                    _screen.value = UpdateScreenState.ReadyToInstall(available.release, downloaded.file, downloaded.sha256)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _screen.value = UpdateScreenState.Failed(
                    "Güncelleme kuruluma hazırlanamadı: " + (e.message ?: "Bilinmeyen hata.")
                )
            } finally {
                activeDownload = null
            }
        }
    }

    /** Abort this transfer without losing the already downloaded partial bytes. */
    fun cancelDownload() {
        val inProgress = _screen.value as? UpdateScreenState.Downloading ?: return
        _screen.value = UpdateScreenState.Cancelling(inProgress.release)
        val interrupted = activeDownload
        interrupted?.cancel()
        viewModelScope.launch {
            interrupted?.join()
            if (_screen.value is UpdateScreenState.Cancelling) {
                _screen.value = UpdateScreenState.Available(inProgress.release)
            }
        }
    }

    /**
     * Revalidate on IO before opening the system installer. Android decides
     * whether to install; LANU never removes an installed package or its data.
     */
    fun requestInstall(context: Context) {
        val ready = _screen.value as? UpdateScreenState.ReadyToInstall ?: return
        _screen.value = UpdateScreenState.VerifyingInstaller
        viewModelScope.launch {
            var cryptographicVerificationPassed = false
            try {
                if (!ready.file.isFile)
                    throw java.io.IOException("İndirilen APK bulunamadı. Yeniden denetleyin.")
                withContext(Dispatchers.IO) {
                    updater.verifyForInstall(ready.file, ready.release.versionCode, ready.expectedSha256)
                }
                cryptographicVerificationPassed = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()
                ) {
                    _screen.value = ready.copy(permissionsNeeded = true)
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.packageName)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(settingsIntent)
                    return@launch
                }
                _screen.value = ready.copy(permissionsNeeded = false)
                val uri = FileProvider.getUriForFile(
                    context, context.packageName + ".ota.fileprovider", ready.file
                )
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // A missing installer app / permission settings activity is NOT
                // cryptographic corruption. Keep the verified resume hint so
                // an interrupted OEM-specific flow can safely retry later.
                if (InstallerFailurePolicy.shouldDiscardCachedVerification(cryptographicVerificationPassed)) {
                    sessions.clear()
                }
                _screen.value = UpdateScreenState.Failed(
                    "Android kurulumu başlatılamadı: " + (e.message ?: "Bilinmeyen hata.")
                )
            }
        }
    }
}
