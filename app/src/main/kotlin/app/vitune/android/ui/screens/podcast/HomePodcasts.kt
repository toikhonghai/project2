package app.vitune.android.ui.screens.podcast

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.components.themed.PodcastEpisodeMenu
import app.vitune.android.ui.items.PodcastEpisodeItem
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.compose.persist.persistList
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.Thumbnail
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomePodcasts(
    onSearchClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val (colorPalette, typography) = LocalAppearance.current
    val lazyListState = rememberLazyListState()

    var episodes by persistList<app.vitune.android.models.PodcastEpisodeEntity>("podcastScreen/recentEpisodes")
    var sortBy by rememberSaveable { mutableStateOf(PodcastSortBy.LastUpdated) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.Descending) }

    LaunchedEffect(sortBy, sortOrder) {
        withContext(Dispatchers.IO) {
            Database.getRecentlyPlayedEpisodes(limit = 20).collect { episodeList ->
                episodes = when (sortBy) {
                    PodcastSortBy.Title -> episodeList.sortedBy { it.title.lowercase() }
                    PodcastSortBy.LastUpdated -> episodeList.sortedBy { it.lastPlayed }
                }.let { if (sortOrder == SortOrder.Descending) it.reversed() else it }
                    .distinctBy { it.videoId }
                    .toImmutableList()
            }
        }
    }

    // Lưu vị trí phát khi người dùng tạm dừng hoặc chuyển tập
    LaunchedEffect(binder?.player?.currentMediaItem) {
        binder?.player?.currentMediaItem?.let { mediaItem ->
            val isPodcast = mediaItem.mediaMetadata.extras?.getBoolean("isPodcast") == true
            if (isPodcast) {
                val videoId = mediaItem.mediaId
                val positionMs = binder.player.currentPosition
                scope.launch(Dispatchers.IO) {
                    Database.updateEpisodePosition(videoId, positionMs)
                    Database.updateEpisode( // Cập nhật lastPlayed
                        Database.getEpisodeById(videoId)?.copy(lastPlayed = System.currentTimeMillis()) ?: return@launch
                    )
                }
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
                Header(title = stringResource(R.string.podcasts)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            text = pluralStringResource(
                                R.plurals.podcast_count_plural,
                                episodes.size,
                                episodes.size
                            ),
                            style = typography.xs.secondary.semiBold,
                            maxLines = 1
                        )
                    }
                    HeaderPodcastSortBy(sortBy, { sortBy = it }, sortOrder, { sortOrder = it })
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
                            name = episode.title,
                            endpoint = NavigationEndpoint.Endpoint.Browse(browseId = episode.podcastId)
                        ),
                        durationText = episode.durationText,
                        publishedTimeText = episode.publishedTimeText,
                        description = episode.description,
                        thumbnail = episode.thumbnailUrl?.let { Thumbnail(url = it, height = null, width = null) }
                    ),
                    onClick = {
                        scope.launch {
                            try {
                                binder?.let {
                                    it.stopRadio()
                                    val mediaItem = episode.asMediaItem()
                                    it.player.seekTo(episode.playPositionMs)
                                    it.player.forcePlay(mediaItem)
                                    it.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = episode.videoId))
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
                                mediaItem = episode.asMediaItem()
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