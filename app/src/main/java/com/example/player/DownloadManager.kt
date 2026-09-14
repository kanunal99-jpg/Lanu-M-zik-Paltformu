package com.example.player

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.AudioQuality
import com.example.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

enum class DownloadStatus { QUEUED, DOWNLOADING, COMPLETED, FAILED, CANCELLED }

data class DownloadProgress(
    val songId: String,
    val status: DownloadStatus,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val localFilePath: String? = null,
    val checksum: String? = null,
    val error: String? = null
)

class DownloadManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    fun getDownloadProgress(songId: String): DownloadProgress? = _downloadStates.value[songId]

    fun startDownload(
        song: Song,
        quality: AudioQuality,
        onFinished: (DownloadProgress) -> Unit = {}
    ) {
        if (activeJobs.containsKey(song.id)) return

        val job = scope.launch {
            var target: File? = null
            try {
                updateState(song.id, DownloadStatus.QUEUED, 0f, 0L, 0L)

                val uri = Uri.parse(song.audioUrl)
                val scheme = uri.scheme?.lowercase()
                if (scheme != "content" && scheme != "file") {
                    throw IllegalStateException("REMOTE_DOWNLOAD_NOT_AUTHORIZED")
                }

                val offlineDir = File(context.filesDir, "offline_audio")
                check(offlineDir.exists() || offlineDir.mkdirs()) { "OFFLINE_STORAGE_UNAVAILABLE" }
                val temp = File(offlineDir, "${song.id}.part")
                target = File(offlineDir, "${song.id}.bin")
                temp.delete()

                val resolver = context.contentResolver
                val input = openInputStream(resolver, uri)
                    ?: throw IllegalStateException("SOURCE_NOT_READABLE")
                input.use { stream ->
                    val total = stream.available().toLong().takeIf { it > 0L } ?: -1L
                    var copied = 0L
                    updateState(song.id, DownloadStatus.DOWNLOADING, 0f, 0L, total)
                    temp.outputStream().use { out ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            ensureActive()
                            val read = stream.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            copied += read
                            val progress = if (total > 0) (copied.toFloat() / total).coerceIn(0f, 1f) else 0f
                            updateState(song.id, DownloadStatus.DOWNLOADING, progress, copied, total)
                        }
                    }
                    if (copied == 0L) throw IllegalStateException("EMPTY_AUDIO_SOURCE")
                    if (target.exists()) target.delete()
                    check(temp.renameTo(target)) { "ATOMIC_MOVE_FAILED" }
                    val checksum = calculateSHA256(target)
                    val completed = DownloadProgress(
                        songId = song.id,
                        status = DownloadStatus.COMPLETED,
                        progress = 1f,
                        bytesDownloaded = copied,
                        totalBytes = copied,
                        localFilePath = target.absolutePath,
                        checksum = checksum
                    )
                    updateState(completed)
                    onFinished(completed)
                }
            } catch (e: CancellationException) {
                target?.delete()
                val state = DownloadProgress(song.id, DownloadStatus.CANCELLED, 0f, 0L, 0L, error = "Cancelled")
                updateState(state)
                onFinished(state)
            } catch (e: Exception) {
                target?.delete()
                val state = DownloadProgress(song.id, DownloadStatus.FAILED, 0f, 0L, 0L, error = e.message ?: "DOWNLOAD_FAILED")
                Log.e("DownloadManager", "Download failed for ${song.id}", e)
                updateState(state)
                onFinished(state)
            } finally {
                activeJobs.remove(song.id)
            }
        }
        activeJobs[song.id] = job
    }

    fun cancelDownload(songId: String) {
        activeJobs[songId]?.cancel()
    }

    fun deleteDownloadedFile(songId: String): Boolean {
        cancelDownload(songId)
        val dir = File(context.filesDir, "offline_audio")
        val deleted = listOf(File(dir, "$songId.bin"), File(dir, "$songId.part"))
            .filter { it.exists() }
            .map { it.delete() }
            .any { it }
        _downloadStates.value = _downloadStates.value.toMutableMap().also { it.remove(songId) }
        return deleted
    }

    private fun openInputStream(resolver: ContentResolver, uri: Uri) = when (uri.scheme?.lowercase()) {
        "content" -> resolver.openInputStream(uri)
        "file" -> FileInputStream(File(uri.path ?: throw IllegalArgumentException("INVALID_FILE_URI")))
        else -> null
    }

    private fun updateState(state: DownloadProgress) {
        _downloadStates.value = _downloadStates.value.toMutableMap().also { it[state.songId] = state }
    }

    private fun updateState(
        songId: String,
        status: DownloadStatus,
        progress: Float,
        bytesDownloaded: Long,
        totalBytes: Long,
        localFilePath: String? = null,
        checksum: String? = null,
        error: String? = null
    ) = updateState(DownloadProgress(songId, status, progress, bytesDownloaded, totalBytes, localFilePath, checksum, error))

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
