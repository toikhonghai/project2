package app.vitune.android.ui.screens.podcast

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.models.PodcastPlaylist
import app.vitune.android.models.PodcastPlaylistPreview
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.query
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.components.themed.Menu
import app.vitune.android.ui.components.themed.MenuEntry
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.components.themed.TextFieldDialog
import app.vitune.android.ui.components.themed.VerticalDivider
import app.vitune.android.ui.items.PlaylistItem
import app.vitune.android.utils.toast
import app.vitune.compose.persist.persistList
import app.vitune.core.data.enums.PlaylistSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomePodcastPlaylists(
    onPlaylistClick: (PodcastPlaylist) -> Unit,
    onSearchClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val scope = rememberCoroutineScope()
    var isCreatingANewPlaylist by rememberSaveable { mutableStateOf(false) }
    var isRenamingPlaylist by rememberSaveable { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<PodcastPlaylistPreview?>(null) }
    var items by persistList<PodcastPlaylistPreview>("podcastScreen/playlists")
    var sortBy by rememberSaveable { mutableStateOf(PlaylistSortBy.Name) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.Ascending) }

    // Dialog for creating a new playlist
    if (isCreatingANewPlaylist) TextFieldDialog(
        hintText = stringResource(R.string.enter_podcast_playlist_name_prompt),
        onDismiss = { isCreatingANewPlaylist = false },
        onAccept = { text ->
            query {
                Database.insert(PodcastPlaylist(name = text))
            }
        }
    )

    // Dialog for renaming a playlist
    if (isRenamingPlaylist && playlistToRename != null) TextFieldDialog(
        hintText = stringResource(R.string.enter_podcast_playlist_name_prompt),
        initialTextInput = playlistToRename?.name ?: "",
        onDismiss = {
            isRenamingPlaylist = false
            playlistToRename = null
        },
        onAccept = { newName ->
            playlistToRename?.let { playlist ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        Database.update(PodcastPlaylist(id = playlist.id, name = newName, thumbnail = playlist.thumbnail))
                    }
                    Database.podcastPlaylistPreviews(sortBy, sortOrder).collect { items = it.toImmutableList() }
                }
            }
            isRenamingPlaylist = false
            playlistToRename = null
        }
    )

    LaunchedEffect(sortBy, sortOrder) {
        Database
            .podcastPlaylistPreviews(sortBy, sortOrder)
            .collect { items = it.toImmutableList() }
    }

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = ""
    )

    val lazyGridState = rememberLazyGridState()

    Box {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = if (UIStatePreferences.playlistsAsGrid)
                GridCells.Adaptive(Dimensions.thumbnails.playlist + Dimensions.items.alternativePadding * 2)
            else GridCells.Fixed(1),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.items.alternativePadding),
            verticalArrangement = if (UIStatePreferences.playlistsAsGrid) Arrangement.spacedBy(Dimensions.items.alternativePadding)
            else Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .background(colorPalette.background0)
        ) {
            item(key = "header", contentType = 0, span = { GridItemSpan(maxLineSpan) }) {
                Header(title = stringResource(R.string.podcast_playlists)) {
                    SecondaryTextButton(
                        text = stringResource(R.string.new_podcast_playlist),
                        onClick = { isCreatingANewPlaylist = true }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    HeaderIconButton(
                        icon = if (UIStatePreferences.playlistsAsGrid) R.drawable.grid else R.drawable.list,
                        onClick = { UIStatePreferences.playlistsAsGrid = !UIStatePreferences.playlistsAsGrid }
                    )
                    VerticalDivider(modifier = Modifier.height(8.dp))
                    HeaderIconButton(
                        icon = R.drawable.medical,
                        enabled = sortBy == PlaylistSortBy.SongCount,
                        onClick = { sortBy = PlaylistSortBy.SongCount }
                    )
                    HeaderIconButton(
                        icon = R.drawable.text,
                        enabled = sortBy == PlaylistSortBy.Name,
                        onClick = { sortBy = PlaylistSortBy.Name }
                    )
                    HeaderIconButton(
                        icon = R.drawable.time,
                        enabled = sortBy == PlaylistSortBy.DateAdded,
                        onClick = { sortBy = PlaylistSortBy.DateAdded }
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    HeaderIconButton(
                        icon = R.drawable.arrow_up,
                        color = colorPalette.text,
                        onClick = { sortOrder = !sortOrder },
                        modifier = Modifier.graphicsLayer { rotationZ = sortOrderIconRotation }
                    )
                }
            }

            items(
                items = items,
                key = { it.id }
            ) { playlistPreview ->
                PlaylistItem(
                    playlist = playlistPreview,
                    thumbnailSize = Dimensions.thumbnails.playlist,
                    alternative = UIStatePreferences.playlistsAsGrid,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    onPlaylistClick(PodcastPlaylist(id = playlistPreview.id, name = playlistPreview.name, thumbnail = playlistPreview.thumbnail))
                                },
                                onLongPress = {
                                    menuState.display {
                                        Menu {
                                            MenuEntry(
                                                icon = R.drawable.pencil,
                                                text = stringResource(R.string.rename),
                                                onClick = {
                                                    menuState.hide()
                                                    isRenamingPlaylist = true
                                                    playlistToRename = playlistPreview
                                                }
                                            )
                                            MenuEntry(
                                                icon = R.drawable.trash,
                                                text = stringResource(R.string.delete),
                                                onClick = {
                                                    menuState.hide()
                                                    query {
                                                        Database.delete(PodcastPlaylist(id = playlistPreview.id, name = playlistPreview.name, thumbnail = playlistPreview.thumbnail))
                                                    }
                                                    context.toast("Playlist deleted")
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                        .animateItem(fadeInSpec = null, fadeOutSpec = null)
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyGridState = lazyGridState,
            icon = R.drawable.search,
            onClick = onSearchClick
        )
    }
}