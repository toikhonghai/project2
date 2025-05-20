package app.vitune.android.ui.screens.podcast

import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.PodcastEpisodeEntity
import app.vitune.android.models.PodcastEpisodePlaylistMap
import app.vitune.android.models.PodcastPlaylist
import app.vitune.android.transaction
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.ui.components.themed.PodcastEpisodeMenu
import app.vitune.android.ui.items.PodcastEpisodeItem
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.toast
import app.vitune.compose.persist.persistList
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.LocalAppearance
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.Thumbnail
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class)
@Composable
fun PodcastPlaylistScreen(
    playlist: PodcastPlaylist,
    onSearchClick: () -> Unit
) {
    RouteHandler {
        GlobalRoutes()
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val binder = LocalPlayerServiceBinder.current
        val menuState = LocalMenuState.current
        val (colorPalette) = LocalAppearance.current
        val lazyListState = rememberLazyListState()

        var episodes by persistList<PodcastEpisodeEntity>("podcastScreen/playlist/${playlist.id}/episodes")

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                Database.podcastPlaylistEpisodes(playlist.id).collect { episodeList ->
                    episodes = episodeList?.toImmutableList()
                        ?: emptyList<PodcastEpisodeEntity>().toImmutableList()
                }
            }
        }

        Box(
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            ) {
                item(key = "header", contentType = 0) {
                    Header(title = playlist.name) {
                        IconButton(
                            onClick = pop,
                            icon = R.drawable.chevron_back,
                            color = colorPalette.text,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                items(
                    items = episodes,
                    key = { episode -> episode.videoId }
                ) { episode ->
                    PodcastEpisodeItem(
                        episode = Innertube.PodcastEpisodeItem(
                            info = Innertube.Info(
                                name = episode.title,
                                endpoint = NavigationEndpoint.Endpoint.Watch(videoId = episode.videoId)
                            ),
                            podcast = Innertube.Info(
                                name = episode.podcastId,
                                endpoint = NavigationEndpoint.Endpoint.Browse(browseId = episode.podcastId)
                            ),
                            durationText = episode.durationText,
                            publishedTimeText = episode.publishedTimeText,
                            description = episode.description,
                            thumbnail = episode.thumbnailUrl?.let {
                                Thumbnail(
                                    url = it,
                                    height = null,
                                    width = null
                                )
                            }
                        ),
                        onClick = {
                            scope.launch {
                                try {
                                    binder?.let {
                                        it.stopRadio()
                                        val mediaItems = episodes.map { ep -> ep.asMediaItem() }
                                        val index =
                                            episodes.indexOfFirst { it.videoId == episode.videoId }
                                        if (index != -1) {
                                            it.player.forcePlayAtIndex(mediaItems, index)
                                            if (episode.playPositionMs > 0) {
                                                it.player.seekTo(episode.playPositionMs)
                                            }
                                        } else {
                                            context.toast("Episode not found in Local")
                                        }
                                    } ?: context.toast("Player service is not available")
                                } catch (e: Exception) {
                                    context.toast("Failed to play episode: ${e.message}")
                                }
                            }
                        },
                        onMenuClick = {
                            menuState.display {
                                PodcastEpisodeMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = episode.asMediaItem(),
                                    playlistId = playlist.id, // Sử dụng playlist.id
                                    positionInPlaylist = episodes.indexOfFirst { it.videoId == episode.videoId },
                                    onRemoveFromPlaylist = {
                                        val index =
                                            episodes.indexOfFirst { it.videoId == episode.videoId }
                                        if (index != -1) {
                                            transaction {
                                                Database.move(playlist.id, index, Int.MAX_VALUE)
                                                Database.delete(
                                                    PodcastEpisodePlaylistMap(
                                                        playlist.id,
                                                        episode.videoId,
                                                        Int.MAX_VALUE
                                                    )
                                                )
                                            }
                                            context.toast("Removed from playlist")
                                        } else {
                                            context.toast("Episode not found in playlist")
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .animateItem()
                    )
                }
            }

            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                icon = R.drawable.search,
                onClick = onSearchClick
            )
        }
    }
}