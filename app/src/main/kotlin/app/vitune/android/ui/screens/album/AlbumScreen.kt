package app.vitune.android.ui.screens.album

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.android.models.Album
import app.vitune.android.models.Song
import app.vitune.android.models.SongAlbumMap
import app.vitune.android.query
import app.vitune.android.transaction
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.components.themed.HeaderPlaceholder
import app.vitune.android.ui.components.themed.PlaylistInfo
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.components.themed.adaptiveThumbnailContent
import app.vitune.android.ui.items.AlbumItem
import app.vitune.android.ui.items.AlbumItemPlaceholder
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.searchresult.ItemsPage
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.completed
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.persist.persist
import app.vitune.compose.persist.persistList
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.stateFlowSaver
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.requests.albumPage
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Route
@Composable
fun AlbumScreen(browseId: String) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val tag = "AlbumScreen"

    Log.d(tag, "Loading album with browseId=$browseId")

    val tabIndexState = rememberSaveable(saver = stateFlowSaver()) { MutableStateFlow(0) }
    val tabIndex by tabIndexState.collectAsState()

    var album by persist<Album?>("album/$browseId/album")
    var albumPage by persist<Innertube.PlaylistOrAlbumPage?>("album/$browseId/albumPage")
    var songs by persistList<Song>("album/$browseId/songs")

    PersistMapCleanup(prefix = "album/$browseId/")

    // Lưu trữ coroutine job để hủy khi dispose
    var job by rememberSaveable { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        job = launch {
            Database
                .albumSongs(browseId)
                .distinctUntilChanged()
                .combine(
                    Database
                        .album(browseId)
                        .distinctUntilChanged()
                        .cancellable()
                ) { currentSongs, currentAlbum ->
                    Log.d(tag, "Database: currentSongs.size=${currentSongs.size}, currentAlbum=$currentAlbum")
                    album = currentAlbum
                    songs = currentSongs.toImmutableList()

                    if (currentAlbum?.timestamp != null && currentSongs.isNotEmpty()) {
                        Log.d(tag, "Using cached data for album $browseId")
                        return@combine
                    }

                    withContext(Dispatchers.IO) {
                        Log.d(tag, "Fetching albumPage from API for browseId=$browseId")
                        Innertube.albumPage(BrowseBody(browseId = browseId))
                            ?.completed()
                            ?.onSuccess { newAlbumPage ->
                                Log.d(tag, "API success, newAlbumPage=$newAlbumPage")
                                albumPage = newAlbumPage

                                transaction {
                                    Database.clearAlbum(browseId)
                                    Database.upsert(
                                        album = Album(
                                            id = browseId,
                                            title = newAlbumPage.title,
                                            description = newAlbumPage.description,
                                            thumbnailUrl = newAlbumPage.thumbnail?.url,
                                            year = newAlbumPage.year,
                                            authorsText = newAlbumPage.authors
                                                ?.joinToString("") { it.name.orEmpty() },
                                            shareUrl = newAlbumPage.url,
                                            timestamp = System.currentTimeMillis(),
                                            bookmarkedAt = album?.bookmarkedAt,
                                            otherInfo = newAlbumPage.otherInfo
                                        ),
                                        songAlbumMaps = newAlbumPage
                                            .songsPage
                                            ?.items
                                            ?.map { it.asMediaItem }
                                            ?.onEach { Database.insert(it) }
                                            ?.mapIndexed { position, mediaItem ->
                                                SongAlbumMap(
                                                    songId = mediaItem.mediaId,
                                                    albumId = browseId,
                                                    position = position
                                                )
                                            } ?: emptyList()
                                    )
                                }
                            }?.exceptionOrNull()?.let { error ->
                                Log.d(tag, "API failed for browseId=$browseId, error=$error")
                                error.printStackTrace()
                            }
                    }
                }.collect()
        }
    }

    // Hủy job khi composable bị dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d(tag, "Disposing AlbumScreen, canceling job for browseId=$browseId")
            job?.cancel()
            job = null
        }
    }

    RouteHandler {
        GlobalRoutes()

        Content {
            val headerContent: @Composable (
                beforeContent: (@Composable () -> Unit)?,
                afterContent: (@Composable () -> Unit)?
            ) -> Unit = { beforeContent, afterContent ->
                if (album?.timestamp == null) {
                    Log.d(tag, "Showing HeaderPlaceholder for browseId=$browseId")
                    HeaderPlaceholder(modifier = Modifier.shimmer())
                } else {
                    val (colorPalette) = LocalAppearance.current
                    val context = LocalContext.current

                    Log.d(tag, "Showing Header with title=${album?.title} for browseId=$browseId")
                    Header(title = album?.title ?: stringResource(R.string.unknown)) {
                        beforeContent?.invoke()

                        Spacer(modifier = Modifier.weight(1f))

                        afterContent?.invoke()

                        HeaderIconButton(
                            icon = if (album?.bookmarkedAt == null) R.drawable.bookmark_outline
                            else R.drawable.bookmark,
                            color = colorPalette.accent,
                            onClick = {
                                val bookmarkedAt =
                                    if (album?.bookmarkedAt == null) System.currentTimeMillis() else null
                                Log.d(tag, "Toggling bookmark for browseId=$browseId, bookmarkedAt=$bookmarkedAt")
                                query {
                                    album
                                        ?.copy(bookmarkedAt = bookmarkedAt)
                                        ?.let(Database::update)
                                }
                            }
                        )

                        HeaderIconButton(
                            icon = R.drawable.share_social,
                            color = colorPalette.text,
                            onClick = {
                                album?.shareUrl?.let { url ->
                                    Log.d(tag, "Sharing album URL=$url for browseId=$browseId")
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, url)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, null)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            val thumbnailContent = adaptiveThumbnailContent(
                isLoading = album?.timestamp == null,
                url = album?.thumbnailUrl
            )

            Log.d(tag, "Rendering Scaffold with songs.size=${songs.size} for browseId=$browseId")
            Scaffold(
                key = "album",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = tabIndex,
                onTabChange = { newTab ->
                    Log.d(tag, "Changing tab to $newTab for browseId=$browseId")
                    tabIndexState.update { newTab }
                },
                tabColumnContent = {
                    tab(0, R.string.songs, R.drawable.musical_notes, canHide = false)
                    tab(1, R.string.other_versions, R.drawable.disc)
                }
            ) { currentTabIndex ->
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> {
                            Log.d(tag, "Rendering AlbumSongs with songs.size=${songs.size} for browseId=$browseId")
                            AlbumSongs(
                                songs = songs,
                                headerContent = headerContent,
                                thumbnailContent = thumbnailContent,
                                afterHeaderContent = {
                                    if (album == null) PlaylistInfo(playlist = albumPage)
                                    else PlaylistInfo(playlist = album)
                                }
                            )
                        }

                        1 -> {
                            Log.d(tag, "Rendering ItemsPage for other versions, browseId=$browseId")
                            ItemsPage(
                                tag = "album/$browseId/alternatives",
                                header = headerContent,
                                initialPlaceholderCount = 1,
                                continuationPlaceholderCount = 1,
                                emptyItemsText = stringResource(R.string.no_alternative_version),
                                provider = albumPage?.let {
                                    {
                                        Result.success(
                                            Innertube.ItemsPage(
                                                items = albumPage?.otherVersions,
                                                continuation = null
                                            )
                                        )
                                    }
                                },
                                itemContent = { album ->
                                    AlbumItem(
                                        album = album,
                                        thumbnailSize = Dimensions.thumbnails.album,
                                        modifier = Modifier.clickable {
                                            Log.d(tag, "Navigating to album ${album.key} from browseId=$browseId")
                                            albumRoute(album.key)
                                        }
                                    )
                                },
                                itemPlaceholderContent = {
                                    AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}