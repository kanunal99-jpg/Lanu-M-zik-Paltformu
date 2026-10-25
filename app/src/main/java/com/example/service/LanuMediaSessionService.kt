package com.example.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.data.MusicRepository
import com.example.model.Song
import com.google.common.util.concurrent.Futures

/**
 * Single playback + browsable media-library authority for LANU.
 * The library is built only from the repository's real current catalog.
 */
class LanuMediaSessionService : MediaLibraryService() {
    private var mediaLibrarySession: MediaLibraryService.MediaLibrarySession? = null
    private var player: ExoPlayer? = null
    private lateinit var repository: MusicRepository

    override fun onCreate() {
        super.onCreate()
        repository = MusicRepository(applicationContext)

        val createdPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = createdPlayer

        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivity = launchIntent?.let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val callback = object : MediaLibraryService.MediaLibrarySession.Callback {
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<MediaItem>
            ) = Futures.immediateFuture(
                if (controller.packageName == packageName) {
                    // Our own app has already applied PlaybackUriPolicy and may
                    // have a playable queue before this separate service has
                    // finished its asynchronous database/catalog initialization.
                    // Preserve trusted same-package playback and startup taps.
                    mediaItems
                } else {
                    // Android Auto/other controllers must not supply arbitrary
                    // mediaUri or metadata. Canonicalize by verified catalog ID.
                    // One invalid item fails closed for the whole queue.
                    MediaSessionQueuePolicy.resolve(
                        mediaItems.map { it.mediaId }, repository.songs.value
                    ).map(::toMediaItem)
                }
            )

            override fun onGetLibraryRoot(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: MediaLibraryService.LibraryParams?
            ) = Futures.immediateFuture(LibraryResult.ofItem(rootItem(), params))

            override fun onGetChildren(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: MediaLibraryService.LibraryParams?
            ) = Futures.immediateFuture(
                LibraryResult.ofItemList(childrenFor(parentId, page, pageSize), params)
            )

            override fun onGetItem(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String
            ) = Futures.immediateFuture(LibraryResult.ofItem(itemFor(mediaId), null))

            override fun onGetSearchResult(
                session: MediaLibraryService.MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: MediaLibraryService.LibraryParams?
            ) = Futures.immediateFuture(
                LibraryResult.ofItemList(
                    MediaLibrarySearchInputPolicy.accept(query)?.let { safeQuery ->
                        paginate(
                            MediaLibrarySongPolicy.searchableSongs(
                                repository.searchSongs(safeQuery)
                            ).map(::toMediaItem),
                            page,
                            pageSize
                        )
                    }.orEmpty(),
                    params
                )
            )
        }

        val builder = MediaLibraryService.MediaLibrarySession.Builder(this, createdPlayer, callback)
        if (sessionActivity != null) builder.setSessionActivity(sessionActivity)
        mediaLibrarySession = builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibraryService.MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        player?.release()
        player = null
        repository.close()
        super.onDestroy()
    }

    private fun rootItem(): MediaItem = MediaItem.Builder()
        .setMediaId(ROOT_ID)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("LANU Müzik")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        )
        .build()

    private fun childrenFor(parentId: String, page: Int, pageSize: Int): List<MediaItem> = when (parentId) {
        ROOT_ID -> paginate(listOf(
            browsableItem(LOCAL_ID, "Cihazımdaki Müzikler"),
            browsableItem(CATALOG_ID, "LANU Kataloğu")
        ), page, pageSize)
        LOCAL_ID -> paginate(MediaLibrarySongPolicy.localSongs(repository.songs.value).map(::toMediaItem), page, pageSize)
        CATALOG_ID -> paginate(MediaLibrarySongPolicy.catalogSongs(repository.songs.value).map(::toMediaItem), page, pageSize)
        else -> emptyList()
    }

    private fun itemFor(mediaId: String): MediaItem {
        if (mediaId == ROOT_ID) return rootItem()
        if (mediaId == LOCAL_ID) return browsableItem(LOCAL_ID, "Cihazımdaki Müzikler")
        if (mediaId == CATALOG_ID) return browsableItem(CATALOG_ID, "LANU Kataloğu")
        return repository.songs.value.firstOrNull { it.id == mediaId }?.let(::toMediaItem)
            ?: MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("İçerik bulunamadı")
                        .setIsBrowsable(false)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
    }

    private fun toMediaItem(song: Song): MediaItem {
        val playableUri = MediaLibrarySongPolicy.playableUri(song)
        val builder = MediaItem.Builder()
            .setMediaId(song.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.coverUrl.takeIf { it.isNotBlank() }?.let(android.net.Uri::parse))
                    .setIsBrowsable(false)
                    .setIsPlayable(playableUri != null)
                    .build()
            )
        // Never expose a URI from unverified, unlicensed or wrongly grouped content.
        playableUri?.let(builder::setUri)
        return builder.build()
    }

    private fun browsableItem(id: String, title: String): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        )
        .build()

    private fun <T> paginate(items: List<T>, page: Int, pageSize: Int): List<T> =
        MediaLibraryPaginationPolicy.page(items, page, pageSize)

    private companion object {
        const val ROOT_ID = "root"
        const val LOCAL_ID = "local"
        const val CATALOG_ID = "catalog"
    }
}
