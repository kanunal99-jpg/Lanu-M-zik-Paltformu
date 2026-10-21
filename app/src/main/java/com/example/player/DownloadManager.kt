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
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Stable offline filename policy: reject path separators and suspicious IDs,
 * never interpolate untrusted catalog identifiers into arbitrary filesystem paths.
 */
internal object OfflineFileNamePolicy {
    private val safe = Regex("[A-Za-z0-9][A-Za-z0-9_.:-]{0,159}")
    fun accepts(songId: String): Boolean = safe.matches(songId) && !songId.contains("..")
}

/**
 * One owner per song ID within the process. Operations are atomic so parallel
 * Compose/ViewModel callbacks cannot schedule competing writes to the same .part file.
 */
internal class InFlightDownloadRegistry {
    companion object {
        // Different ViewModel and MediaSession instances may share offline_audio.
        private val processJobs = ConcurrentHashMap<String, Job>()
        private val deletionInProgress = mutableSetOf<String>()
        private val processLock = Any()
    }
    private val ownedJobs = ConcurrentHashMap<String, Job>()

    fun register(songId: String, job: Job): Boolean = synchronized(processLock) {
        if (songId in deletionInProgress || processJobs.putIfAbsent(songId, job) != null)
            return@synchronized false
        ownedJobs[songId] = job
        true
    }

    fun release(songId: String, job: Job): Boolean = synchronized(processLock) {
        ownedJobs.remove(songId, job)
        processJobs.remove(songId, job)
    }

    /** Exclude all new writers, await the current owner, then delete its media. */
    suspend fun deleteExclusively(songId: String, action: suspend () -> Boolean): Boolean {
        val ongoing = synchronized(processLock) {
            if (!deletionInProgress.add(songId)) return false
            processJobs[songId]
        }
        try {
            ongoing?.cancelAndJoin()
            return action()
        } finally {
            synchronized(processLock) { deletionInProgress.remove(songId) }
        }
    }

    fun cancel(songId: String) { ownedJobs[songId]?.cancel() }
    fun cancelAll() {
        // Closing one manager must NOT cancel another manager's active transfer.
        ownedJobs.values.forEach { it.cancel() }
        ownedJobs.clear()
    }
}

/** Hard limit protects Android storage against a corrupt ContentResolver source. */
internal object OfflineTransferSizePolicy {
    const val MAX_BYTES: Long = 512L * 1024L * 1024L

    fun requireExpectedSize(contentLength: Long) {
        if (contentLength > MAX_BYTES) throw IllegalStateException("OFFLINE_AUDIO_TOO_LARGE")
    }

    fun requireNextChunk(totalWritten: Long, chunkBytes: Int) {
        if (chunkBytes < 0 || totalWritten < 0 || chunkBytes.toLong() > MAX_BYTES - totalWritten)
            throw IllegalStateException("OFFLINE_AUDIO_TOO_LARGE")
    }
}

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
    private val activeJobs = InFlightDownloadRegistry()
    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    fun getDownloadProgress(songId: String): DownloadProgress? = _downloadStates.value[songId]

    fun startDownload(song: Song, quality: AudioQuality, onFinished: (DownloadProgress) -> Unit = {}) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            var target: File? = null
            var temp: File? = null
            try {
                updateState(song.id, DownloadStatus.QUEUED, 0f, 0L, 0L)
                if (!OfflineFileNamePolicy.accepts(song.id))
                    throw IllegalArgumentException("INVALID_OFFLINE_TRACK_ID")
                val uri = Uri.parse(song.audioUrl)
                if (!OfflineSourcePolicy.isAuthorized(uri)) throw IllegalStateException("REMOTE_DOWNLOAD_NOT_AUTHORIZED")
                val offlineDir = File(context.filesDir, "offline_audio")
                check(offlineDir.exists() || offlineDir.mkdirs()) { "OFFLINE_STORAGE_UNAVAILABLE" }
                temp = File(offlineDir, "${song.id}.part")
                target = File(offlineDir, "${song.id}.bin")
                temp.delete()

                val source = openSource(uri)
                source.stream.use { stream ->
                    val total = source.contentLength
                    OfflineTransferSizePolicy.requireExpectedSize(total)
                    updateState(song.id, DownloadStatus.DOWNLOADING, 0f, 0L, total)
                    var copied = 0L
                    temp!!.outputStream().use { out ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            ensureActive()
                            val read = stream.read(buffer)
                            if (read < 0) break
                            OfflineTransferSizePolicy.requireNextChunk(copied, read)
                            out.write(buffer, 0, read)
                            copied += read
                            val progress = if (total > 0) (copied.toFloat() / total).coerceIn(0f, 1f) else 0f
                            updateState(song.id, DownloadStatus.DOWNLOADING, progress, copied, total)
                        }
                    }
                    if (copied == 0L) throw IllegalStateException("EMPTY_AUDIO_SOURCE")
                    // Verify the newly downloaded temp file BEFORE replacing existing offline media.
                    // POSIX rename replaces the destination atomically; if it fails, keep the old file.
                    val checksum = calculateSHA256(temp!!)
                    check(temp!!.renameTo(target!!)) { "ATOMIC_MOVE_FAILED" }
                    val completed = DownloadProgress(song.id, DownloadStatus.COMPLETED, 1f, copied, copied, target!!.absolutePath, checksum, null)
                    updateState(completed)
                    onFinished(completed)
                    temp = null
                }
            } catch (e: CancellationException) {
                temp?.delete()
                // Never delete an earlier successfully cached track when this attempt fails.
                val state = DownloadProgress(song.id, DownloadStatus.CANCELLED, 0f, 0L, 0L, error = "Cancelled")
                updateState(state)
                onFinished(state)
            } catch (e: Exception) {
                temp?.delete()
                // Never delete an earlier successfully cached track when this attempt fails.
                val state = DownloadProgress(song.id, DownloadStatus.FAILED, 0f, 0L, 0L, error = e.message ?: "DOWNLOAD_FAILED")
                Log.e("DownloadManager", "Download failed for ${song.id}", e)
                updateState(state)
                onFinished(state)
            } finally {
                coroutineContext[Job]?.let { activeJobs.release(song.id, it) }
            }
        }
        if (activeJobs.register(song.id, job)) {
            if (!job.start()) activeJobs.release(song.id, job)
        } else {
            // Another caller owns the transfer. Cancel the unstarted coroutine.
            job.cancel()
        }
    }

    fun cancelDownload(songId: String) { activeJobs.cancel(songId) }

    fun close() {
        activeJobs.cancelAll()
        scope.cancel()
        _downloadStates.value = emptyMap()
    }

    suspend fun deleteDownloadedFile(songId: String): Boolean {
        if (!OfflineFileNamePolicy.accepts(songId)) return false
        return activeJobs.deleteExclusively(songId) {
            val dir = File(context.filesDir, "offline_audio")
            val files = listOf(File(dir, "$songId.bin"), File(dir, "$songId.part"))
            var deleted = false
            for (file in files) {
                if (file.exists()) {
                    if (!file.delete()) return@deleteExclusively false
                    deleted = true
                }
            }
            _downloadStates.value = _downloadStates.value.toMutableMap().also { it.remove(songId) }
            deleted
        }
    }

    private data class OpenedSource(val stream: java.io.InputStream, val contentLength: Long)

    private fun openSource(uri: Uri): OpenedSource {
        return when (uri.scheme?.lowercase()) {
            "content" -> OpenedSource(
                context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("SOURCE_NOT_READABLE"),
                resolveSourceLength(context.contentResolver, uri)
            )
            "file" -> {
                val file = File(uri.path ?: throw IllegalArgumentException("INVALID_FILE_URI"))
                OpenedSource(FileInputStream(file), file.length())
            }
            "http", "https" -> openHttpSource(uri)
            else -> throw IllegalStateException("UNSUPPORTED_SOURCE_SCHEME")
        }
    }

    private fun openHttpSource(uri: Uri): OpenedSource {
        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("Accept", "audio/*")
            setRequestProperty("User-Agent", "LANU-Music/1.0")
        }
        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            throw IllegalStateException("HTTP_AUDIO_SOURCE_$code")
        }
        val stream = connection.inputStream
        val length = connection.contentLengthLong
        return OpenedSource(stream, length)
    }

    private fun resolveSourceLength(resolver: ContentResolver, uri: Uri): Long = runCatching {
        resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor -> descriptor.length } ?: -1L
    }.getOrDefault(-1L)

    private fun updateState(state: DownloadProgress) { _downloadStates.value = _downloadStates.value.toMutableMap().also { it[state.songId] = state } }
    private fun updateState(songId: String, status: DownloadStatus, progress: Float, bytesDownloaded: Long, totalBytes: Long, localFilePath: String? = null, checksum: String? = null, error: String? = null) = updateState(DownloadProgress(songId, status, progress, bytesDownloaded, totalBytes, localFilePath, checksum, error))

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val HTTP_TIMEOUT_MS = 20_000
    }
}
