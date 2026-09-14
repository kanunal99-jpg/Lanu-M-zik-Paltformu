package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Song
import com.example.ui.theme.LanuDarkBg
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

/**
 * Filter mode for searching cached songs:
 * - ALL: Searches both song title and artist
 * - TITLE_ONLY: Matches only song title
 * - ARTIST_ONLY: Matches only artist name
 */
enum class SearchFilterMode(val label: String) {
    ALL("Tümü (Başlık & Sanatçı)"),
    TITLE_ONLY("Yalnızca Başlık"),
    ARTIST_ONLY("Yalnızca Sanatçı")
}

/**
 * A SearchBar component with a list view below it to allow users to find songs
 * by title or artist, using Room for local caching.
 *
 * @param cachedSongs List of songs retrieved or cached from the local Room database.
 * @param onSongClick Invoked when a song item is selected or played.
 * @param modifier Composable modifier.
 * @param initialQuery Optional initial search string.
 * @param placeholder Hint text displayed in the search input.
 * @param currentlyPlayingSongId Optional ID of the currently playing song to show active playback state.
 */
@Composable
fun SongSearchBarWithListView(
    cachedSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    placeholder: String = "Şarkı veya sanatçı ara...",
    currentlyPlayingSongId: String? = null,
    onQueryChange: ((String) -> Unit)? = null
) {
    var searchQuery by remember(initialQuery) { mutableStateOf(initialQuery) }
    var filterMode by remember { mutableStateOf(SearchFilterMode.ALL) }
    val focusManager = LocalFocusManager.current

    val trimmedQuery = searchQuery.trim().lowercase()

    // Filter songs from the Room cache based on title or artist
    val filteredSongs = remember(cachedSongs, trimmedQuery, filterMode) {
        if (trimmedQuery.isEmpty()) {
            cachedSongs
        } else {
            cachedSongs.filter { song ->
                val titleMatches = song.title.lowercase().contains(trimmedQuery)
                val artistMatches = song.artist.lowercase().contains(trimmedQuery)

                when (filterMode) {
                    SearchFilterMode.ALL -> titleMatches || artistMatches
                    SearchFilterMode.TITLE_ONLY -> titleMatches
                    SearchFilterMode.ARTIST_ONLY -> artistMatches
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LanuDarkBg)
            .testTag("song_search_bar_with_list")
    ) {
        // --- SearchBar Component Header ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Room Cache Indicator Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LanuDarkSurface)
                        .border(1.dp, LanuDarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("room_cache_badge")
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Room Veritabanı",
                        tint = LanuGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Room Önbellek (${cachedSongs.size} Şarkı)",
                        color = LanuGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${filteredSongs.size} Sonuç",
                    color = LanuTextMuted,
                    fontSize = 12.sp
                )
            }

            // Material 3 SearchBar TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onQueryChange?.invoke(it)
                },
                placeholder = {
                    Text(
                        text = placeholder,
                        color = LanuTextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Ara",
                        tint = if (searchQuery.isNotEmpty()) LanuGreen else LanuTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                onQueryChange?.invoke("")
                            },
                            modifier = Modifier.testTag("song_search_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Temizle",
                                tint = LanuTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LanuDarkSurfaceElevated,
                    unfocusedContainerColor = LanuDarkSurface,
                    disabledContainerColor = LanuDarkSurface,
                    focusedBorderColor = LanuGreen,
                    unfocusedBorderColor = LanuDarkBorder,
                    cursorColor = LanuGreen,
                    focusedTextColor = LanuTextPrimary,
                    unfocusedTextColor = LanuTextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("song_search_bar_input")
            )

            // Filter Chips to filter strictly by title or artist
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                SearchFilterMode.values().forEach { mode ->
                    val isSelected = filterMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { filterMode = mode },
                        label = {
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = LanuGreen
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = LanuDarkSurface,
                            labelColor = LanuTextSecondary,
                            selectedContainerColor = LanuGreen.copy(alpha = 0.2f),
                            selectedLabelColor = LanuGreen
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) LanuGreen else LanuDarkBorder,
                            borderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_${mode.name.lowercase()}")
                    )
                }
            }
        }

        // --- List View Below SearchBar ---
        if (filteredSongs.isEmpty()) {
            // Empty State
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag("song_search_empty_state")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = LanuTextMuted.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sonuç Bulunamadı",
                        color = LanuTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"$searchQuery\" için Room önbelleğinde şarkı başlığı veya sanatçı bulunamadı.",
                        color = LanuTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            // Scrollable List View of matching songs
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("song_search_list_view")
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    val isPlaying = song.id == currentlyPlayingSongId
                    val isTitleMatched = trimmedQuery.isNotEmpty() && song.title.lowercase().contains(trimmedQuery)
                    val isArtistMatched = trimmedQuery.isNotEmpty() && song.artist.lowercase().contains(trimmedQuery)

                    CachedSongListItem(
                        song = song,
                        isPlaying = isPlaying,
                        isTitleMatched = isTitleMatched,
                        isArtistMatched = isArtistMatched,
                        onClick = { onSongClick(song, filteredSongs) }
                    )
                }
            }
        }
    }
}

/**
 * List Item card representing a song retrieved from Room cache.
 */
@Composable
private fun CachedSongListItem(
    song: Song,
    isPlaying: Boolean,
    isTitleMatched: Boolean,
    isArtistMatched: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) LanuGreen.copy(alpha = 0.12f) else LanuDarkSurface)
            .border(
                1.dp,
                if (isPlaying) LanuGreen.copy(alpha = 0.5f) else LanuDarkBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("song_search_item_${song.id}")
    ) {
        // Cover Art Thumbnail
        AsyncImage(
            model = song.coverUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LanuDarkSurfaceElevated)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist Details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    color = if (isPlaying) LanuGreen else LanuTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isTitleMatched) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Başlık Eşleşti",
                        color = LanuGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LanuGreen.copy(alpha = 0.18f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isArtistMatched) LanuGreen else LanuTextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = song.artist,
                    color = if (isArtistMatched) LanuGreen else LanuTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isArtistMatched) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )

                if (isArtistMatched) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sanatçı Eşleşti",
                        color = LanuGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LanuGreen.copy(alpha = 0.18f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${song.album} • ${formatTrackDuration(song.durationMs)}",
                color = LanuTextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Playback trigger button
        Surface(
            shape = CircleShape,
            color = if (isPlaying) LanuGreen else LanuGreen.copy(alpha = 0.2f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Oynatılıyor" else "Çal",
                    tint = if (isPlaying) Color.Black else LanuGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatTrackDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Standard alias for the SearchBar component with list view below it.
 */
@Composable
fun SearchBarWithListView(
    cachedSongs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    placeholder: String = "Şarkı veya sanatçı ara...",
    currentlyPlayingSongId: String? = null,
    onQueryChange: ((String) -> Unit)? = null
) {
    SongSearchBarWithListView(
        cachedSongs = cachedSongs,
        onSongClick = onSongClick,
        modifier = modifier,
        initialQuery = initialQuery,
        placeholder = placeholder,
        currentlyPlayingSongId = currentlyPlayingSongId,
        onQueryChange = onQueryChange
    )
}
