package app.vitune.android.ui.screens.searchresult

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.PodcastEntity
import app.vitune.android.models.PodcastEpisodeEntity
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.components.themed.PodcastEpisodeMenu
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.items.AlbumItem
import app.vitune.android.ui.items.AlbumItemPlaceholder
import app.vitune.android.ui.items.ArtistItem
import app.vitune.android.ui.items.ArtistItemPlaceholder
import app.vitune.android.ui.items.PlaylistItem
import app.vitune.android.ui.items.PlaylistItemPlaceholder
import app.vitune.android.ui.items.PodcastEpisodeItem
import app.vitune.android.ui.items.PodcastItem
import app.vitune.android.ui.items.SongItem
import app.vitune.android.ui.items.SongItemPlaceholder
import app.vitune.android.ui.items.VideoItem
import app.vitune.android.ui.items.VideoItemPlaceholder
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.artistRoute
import app.vitune.android.ui.screens.playlistRoute
import app.vitune.android.ui.screens.podcastRoute
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.playingSong
import app.vitune.android.utils.toast
import app.vitune.compose.persist.LocalPersistMap
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.Dimensions
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.models.bodies.SearchBody
import app.vitune.providers.innertube.requests.getPodcastPlaylistId
import app.vitune.providers.innertube.requests.loadPodcastEpisodesNext
import app.vitune.providers.innertube.requests.loadPodcastPage
import app.vitune.providers.innertube.requests.searchPage
import app.vitune.providers.innertube.requests.searchPodcastEpisodes
import app.vitune.providers.innertube.requests.searchPodcastEpisodesWithContinuation
import app.vitune.providers.innertube.requests.searchPodcasts
import app.vitune.providers.innertube.requests.searchPodcastsWithContinuation
import app.vitune.providers.innertube.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Route
@Composable
fun SearchResultScreen(query: String, onSearchAgain: () -> Unit) {
    val persistMap = LocalPersistMap.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val saveableStateHolder = rememberSaveableStateHolder()

    PersistMapCleanup(prefix = "searchResults/$query/")

    val (currentMediaId, playing) = playingSong(binder)

    RouteHandler {
        GlobalRoutes()

        Content {
            val headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit = {
                Header(
                    title = query,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            persistMap?.clean("searchResults/$query/")
                            onSearchAgain()
                        }
                    }
                )
            }

            Scaffold(
                key = "searchresult",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = UIStatePreferences.searchResultScreenTabIndex,
                onTabChange = { UIStatePreferences.searchResultScreenTabIndex = it },
                tabColumnContent = {
                    tab(0, R.string.songs, R.drawable.musical_notes)
                    tab(1, R.string.albums, R.drawable.disc)
                    tab(2, R.string.artists, R.drawable.person)
                    tab(3, R.string.videos, R.drawable.film)
                    tab(4, R.string.playlists, R.drawable.playlist)
                    tab(5, R.string.podcast, R.drawable.podcast)
                    tab(6, R.string.podcast_channels, R.drawable.podcast)
                }
            ) { tabIndex ->
                saveableStateHolder.SaveableStateProvider(tabIndex) {
                    when (tabIndex) {
                        0 -> ItemsPage(
                            tag = "searchResults/$query/songs",
                            provider = { continuation ->
                                if (continuation == null) Innertube.searchPage(
                                    body = SearchBody(
                                        query = query,
                                        params = Innertube.SearchFilter.Song.value
                                    ),
                                    fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                ) else Innertube.searchPage(
                                    body = ContinuationBody(continuation = continuation),
                                    fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                )
                            },
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { song ->
                                SongItem(
                                    song = song,
                                    thumbnailSize = Dimensions.thumbnails.song,
                                    modifier = Modifier.combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem
                                                )
                                            }
                                        },
                                        onClick = {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(song.asMediaItem)
                                            binder?.setupRadio(song.info?.endpoint)
                                        }
                                    ),
                                    isPlaying = playing && currentMediaId == song.key
                                )
                            },
                            itemPlaceholderContent = {
                                SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                            }
                        )

                        1 -> ItemsPage(
                            tag = "searchResults/$query/albums",
                            provider = { continuation ->
                                if (continuation == null) {
                                    Innertube.searchPage(
                                        body = SearchBody(
                                            query = query,
                                            params = Innertube.SearchFilter.Album.value
                                        ),
                                        fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                    )
                                } else {
                                    Innertube.searchPage(
                                        body = ContinuationBody(continuation = continuation),
                                        fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                    )
                                }
                            },
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { album ->
                                AlbumItem(
                                    album = album,
                                    thumbnailSize = Dimensions.thumbnails.album,
                                    modifier = Modifier.clickable(onClick = { albumRoute(album.key) })
                                )
                            },
                            itemPlaceholderContent = {
                                AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album)
                            }
                        )

                        2 -> ItemsPage(
                            tag = "searchResults/$query/artists",
                            provider = { continuation ->
                                if (continuation == null) {
                                    Innertube.searchPage(
                                        body = SearchBody(
                                            query = query,
                                            params = Innertube.SearchFilter.Artist.value
                                        ),
                                        fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                    )
                                } else {
                                    Innertube.searchPage(
                                        body = ContinuationBody(continuation = continuation),
                                        fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                    )
                                }
                            },
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { artist ->
                                ArtistItem(
                                    artist = artist,
                                    thumbnailSize = 64.dp,
                                    modifier = Modifier
                                        .clickable(onClick = { artistRoute(artist.key) })
                                )
                            },
                            itemPlaceholderContent = {
                                ArtistItemPlaceholder(thumbnailSize = 64.dp)
                            }
                        )

                        3 -> ItemsPage(
                            tag = "searchResults/$query/videos",
                            provider = { continuation ->
                                if (continuation == null) Innertube.searchPage(
                                    body = SearchBody(
                                        query = query,
                                        params = Innertube.SearchFilter.Video.value
                                    ),
                                    fromMusicShelfRendererContent = Innertube.VideoItem::from
                                ) else Innertube.searchPage(
                                    body = ContinuationBody(continuation = continuation),
                                    fromMusicShelfRendererContent = Innertube.VideoItem::from
                                )
                            },
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { video ->
                                VideoItem(
                                    video = video,
                                    thumbnailWidth = 128.dp,
                                    thumbnailHeight = 72.dp,
                                    modifier = Modifier.combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    mediaItem = video.asMediaItem,
                                                    onDismiss = menuState::hide
                                                )
                                            }
                                        },
                                        onClick = {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(video.asMediaItem)
                                            binder?.setupRadio(video.info?.endpoint)
                                        }
                                    )
                                )
                            },
                            itemPlaceholderContent = {
                                VideoItemPlaceholder(
                                    thumbnailWidth = 128.dp,
                                    thumbnailHeight = 72.dp
                                )
                            }
                        )

                        4 -> ItemsPage(
                            tag = "searchResults/$query/playlists",
                            provider = { continuation ->
                                if (continuation == null) Innertube.searchPage(
                                    body = SearchBody(
                                        query = query,
                                        params = Innertube.SearchFilter.CommunityPlaylist.value
                                    ),
                                    fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                ) else Innertube.searchPage(
                                    body = ContinuationBody(continuation = continuation),
                                    fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                )
                            },
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    thumbnailSize = Dimensions.thumbnails.playlist,
                                    modifier = Modifier.clickable {
                                        playlistRoute(playlist.key, null, null, false)
                                    }
                                )
                            },
                            itemPlaceholderContent = {
                                PlaylistItemPlaceholder(thumbnailSize = Dimensions.thumbnails.playlist)
                            }
                        )

                        5 -> ItemsPage(
                            tag = "searchResults/$query/podcast_episodes",
                            provider = { continuation ->
                                if (continuation == null) Innertube.searchPodcastEpisodes(
                                    query = query
                                ) else Innertube.searchPodcastEpisodesWithContinuation(
                                    continuationToken = continuation
                                )
                            },
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { episode ->
                                PodcastEpisodeItem(
                                    episode = episode,
                                    onClick = {
                                        scope.launch {
                                            try {
                                                binder?.let {
                                                    it.stopRadio()
                                                    val mediaItem = episode.asMediaItem()
                                                    it.player.forcePlay(mediaItem)
                                                    it.setupRadio(episode.info?.endpoint)

                                                    // Load and queue related episodes
                                                    val podcastId = episode.podcast?.endpoint?.browseId
                                                    if (podcastId != null) {
                                                        val playlistId = getPodcastPlaylistId(podcastId)
                                                        if (playlistId != null) {
                                                            val episodesPage = Innertube.loadPodcastEpisodesNext(playlistId)?.getOrNull()
                                                            val mediaItems = episodesPage?.items?.map { it.asMediaItem() } ?: emptyList()
                                                            withContext(Dispatchers.Main) {
                                                                it.player.addMediaItems(mediaItems)
                                                            }
                                                        }
                                                    }
                                                } ?: withContext(Dispatchers.Main) {
                                                    context.toast("Player service is not available")
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Log.e("SearchResultScreen", "Error playing podcast episode: ${e.message}", e)
                                                    context.toast("Failed to play podcast: ${e.message}")
                                                }
                                            }
                                        }
                                    },
                                    onMenuClick = {
                                        menuState.display {
                                            PodcastEpisodeMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = episode.asMediaItem()
                                            )
                                        }
                                    }
                                )
                            },
                            itemPlaceholderContent = {
                                SongItemPlaceholder(thumbnailSize = 64.dp)
                            }
                        )

                        6 -> ItemsPage(
                            tag = "searchResults/$query/podcast_channels",
                            provider = { continuation ->
                                if (continuation == null) Innertube.searchPodcasts(query)
                                else Innertube.searchPodcastsWithContinuation(continuation)
                            },
                            emptyItemsText = stringResource(R.string.no_search_results),
                            header = headerContent,
                            itemContent = { podcast ->
                                PodcastItem(
                                    podcast = podcast,
                                    onClick = {
                                        podcastRoute(podcast.key)
                                    },
                                    onMenuClick = {
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clickable {
                                            podcastRoute(podcast.key)
                                        }
                                )
                            },
                            itemPlaceholderContent = {
                                AlbumItemPlaceholder(thumbnailSize = Dimensions.thumbnails.album) // Hoặc tạo PodcastItemPlaceholder
                            }
                        )
                    }
                }
            }
        }
    }
}