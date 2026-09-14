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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LanuDarkBorder
import com.example.ui.theme.LanuDarkSurface
import com.example.ui.theme.LanuDarkSurfaceElevated
import com.example.ui.theme.LanuGreen
import com.example.ui.theme.LanuTextMuted
import com.example.ui.theme.LanuTextPrimary
import com.example.ui.theme.LanuTextSecondary

/**
 * Filter scope for finding songs, artists, or albums in the music library.
 */
enum class SearchFilterScope(val label: String, val icon: ImageVector?) {
    ALL("Tümü", null),
    SONGS("Şarkılar", Icons.Default.MusicNote),
    ARTISTS("Sanatçılar", Icons.Default.Person),
    ALBUMS("Albümler", Icons.Default.Album)
}

/**
 * Dedicated SearchBar component with leading icon, clear button, and scope chips
 * for searching songs, artists, or albums within the music library.
 */
@Composable
fun MusicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Şarkı, sanatçı veya albüm ara...",
    selectedScope: SearchFilterScope = SearchFilterScope.ALL,
    onScopeChange: ((SearchFilterScope) -> Unit)? = null,
    onSearchAction: (() -> Unit)? = null,
    showScopeFilters: Boolean = true
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("music_search_bar")
    ) {
        // Search Input Field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
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
                    tint = if (query.isNotEmpty()) LanuGreen else LanuTextSecondary,
                    modifier = Modifier
                        .size(22.dp)
                        .testTag("search_icon")
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("search_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Aramayı Temizle",
                            tint = LanuTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LanuDarkSurface,
                unfocusedContainerColor = LanuDarkSurface,
                focusedBorderColor = LanuGreen,
                unfocusedBorderColor = LanuDarkBorder,
                focusedTextColor = LanuTextPrimary,
                unfocusedTextColor = LanuTextPrimary,
                cursorColor = LanuGreen
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    onSearchAction?.invoke()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input_field")
        )

        // Filter Scope Chips: Tümü, Şarkılar, Sanatçılar, Albümler
        if (showScopeFilters && onScopeChange != null) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_filter_row")
            ) {
                items(SearchFilterScope.values()) { scope ->
                    val isSelected = scope == selectedScope
                    Surface(
                        color = if (isSelected) LanuGreen else LanuDarkSurfaceElevated,
                        contentColor = if (isSelected) Color.Black else LanuTextSecondary,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) LanuGreen else LanuDarkBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onScopeChange(scope) }
                            .testTag("search_filter_chip_${scope.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (scope.icon != null) {
                                Icon(
                                    imageVector = scope.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else LanuTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = scope.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
