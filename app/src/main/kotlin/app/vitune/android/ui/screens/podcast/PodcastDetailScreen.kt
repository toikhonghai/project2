package app.vitune.android.ui.screens.podcast

import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.PodcastEntity
import app.vitune.android.models.PodcastEpisodeEntity
import app.vitune.android.models.PodcastSubscriptionEntity
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.ShimmerHost
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.ui.components.themed.PodcastEpisodeMenu
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.items.PodcastEpisodeItem
import app.vitune.android.ui.items.SongItemPlaceholder
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.persist.persist
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.shimmer
import app.vitune.core.ui.utils.isLandscape
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.Thumbnail
import app.vitune.providers.innertube.requests.getPodcastPlaylistId
import app.vitune.providers.innertube.requests.loadPodcastEpisodesNext
import app.vitune.providers.innertube.requests.loadPodcastPage
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class)
@Composable
fun PodcastDetailScreen(
    browseId: String,
    modifier: Modifier = Modifier
) {
    RouteHandler {
        GlobalRoutes()
        val scope = rememberCoroutineScope()
        val menuState = LocalMenuState.current
        val context = LocalContext.current
        val binder = LocalPlayerServiceBinder.current
        val (colorPalette, typography) = LocalAppearance.current
        val lazyListState = rememberLazyListState()

        PersistMapCleanup(prefix = "podcastDetail/$browseId")

        var podcast by persist<PodcastEntity?>(tag = "podcastDetail/$browseId/podcast")
        var episodes by persist<List<PodcastEpisodeEntity>?>(tag = "podcastDetail/$browseId/episodes")
        var isLoading by persist<Boolean>(
            tag = "podcastDetail/$browseId/isLoading",
            initialValue = true
        )
        var isSubscribed by persist<Boolean>(
            tag = "podcastDetail/$browseId/isSubscribed",
            initialValue = false
        )

        LaunchedEffect(Unit) {
            Database.isSubscribed(browseId).collectLatest { isSubscribed = it }
        }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val dbPodcast = Database.getPodcastWithEpisodes(browseId)
                if (dbPodcast != null && dbPodcast.podcast != null && dbPodcast.episodes.isNotEmpty()) {
                    podcast = dbPodcast.podcast
                    episodes = dbPodcast.episodes
                    Log.d(
                        "PodcastDetailScreen",
                        "Loaded podcast ${dbPodcast.podcast.title} with ${dbPodcast.episodes.size} episodes from database"
                    )
                } else {
                    Innertube.loadPodcastPage(browseId)?.onSuccess { page ->
                        val playlistId = getPodcastPlaylistId(browseId) // Lấy playlistId ở đây
                        val podcastEntity = PodcastEntity(
                            browseId = browseId,
                            title = page.title ?: "Unknown",
                            description = page.description,
                            authorName = page.author?.name,
                            authorBrowseId = page.author?.endpoint?.browseId,
                            thumbnailUrl = page.thumbnail?.url,
                            episodeCount = page.episodes?.items?.size ?: 0,
                            playlistId = playlistId,
                            lastUpdated = System.currentTimeMillis()
                        )
                        val episodeEntities = page.episodes?.items?.map { episode ->
                            PodcastEpisodeEntity.fromPodcastEpisodeItem(episode, browseId)
                        } ?: emptyList()

                        Database.insertPodcast(podcastEntity)
                        episodeEntities.forEach { Database.insertEpisode(it) }

                        podcast = podcastEntity
                        episodes = episodeEntities
                        Log.d(
                            "PodcastDetailScreen",
                            "Fetched podcast ${page.title} with ${episodeEntities.size} episodes from API"
                        )
                    }?.onFailure {
                        Log.e("PodcastDetailScreen", "Failed to load podcast: ${it.message}", it)
                        context.toast("Failed to load podcast: ${it.message}")
                    }
                }
                isLoading = false
            }
        }

        Box(modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            ) {
                if (isLoading || podcast == null || episodes == null) {
                    item(key = "headerLoading") {
                        ShimmerHost(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(colorPalette.shimmer)
                            )
                        }
                    }
                    item(key = "podcastInfoLoading") {
                        ShimmerHost(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isLandscape) 120.dp else 160.dp)
                                        .background(colorPalette.shimmer)
                                        .padding(top = 16.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(20.dp)
                                            .background(colorPalette.shimmer)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(16.dp)
                                            .background(colorPalette.shimmer)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .background(colorPalette.shimmer)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .height(14.dp)
                                            .background(colorPalette.shimmer)
                                    )
                                }
                            }
                        }
                    }
                    item(key = "episodesHeaderLoading") {
                        ShimmerHost(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.3f)
                                    .height(20.dp)
                                    .background(colorPalette.shimmer)
                            )
                        }
                    }
                    item(key = "episodesLoading") {
                        ShimmerHost(modifier = Modifier.padding(horizontal = 16.dp)) {
                            repeat(3) {
                                SongItemPlaceholder(
                                    thumbnailSize = 64.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    item(key = "header") {
                        Header(title = podcast?.title ?: "Podcast") {
                            IconButton(
                                onClick = pop,
                                icon = R.drawable.chevron_back,
                                color = colorPalette.text,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            SecondaryTextButton(
                                text = if (isSubscribed) stringResource(R.string.unsubscribe) else stringResource(
                                    R.string.subscribe
                                ),
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            if (isSubscribed) {
                                                Database.unsubscribeFromPodcast(browseId)
                                                withContext(Dispatchers.Main) {
                                                    context.toast("Unsubscribed from podcast")
                                                }
                                            } else {
                                                if (browseId.isBlank()) {
                                                    Log.e("PodcastDetailScreen", "Invalid browseId")
                                                    withContext(Dispatchers.Main) {
                                                        context.toast("Cannot subscribe: Invalid podcast ID")
                                                    }
                                                    return@launch
                                                }

                                                val playlistId =
                                                    Database.getPodcastPlaylistId(browseId)
                                                if (playlistId == null) {
                                                    Log.w(
                                                        "PodcastDetailScreen",
                                                        "No playlistId found for browseId: $browseId"
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        context.toast("Cannot subscribe: Playlist ID not found")
                                                    }
                                                    return@launch
                                                }

                                                Database.insertSubscription(
                                                    PodcastSubscriptionEntity(
                                                        browseId = browseId,
                                                        channelId = playlistId
                                                    )
                                                )
                                                withContext(Dispatchers.Main) {
                                                    context.toast("Subscribed to podcast")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "PodcastDetailScreen",
                                                "Error during subscription: ${e.message}",
                                                e
                                            )
                                            withContext(Dispatchers.Main) {
                                                context.toast("Failed to subscribe: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item(key = "podcastInfo") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = podcast?.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(if (isLandscape) 120.dp else 160.dp)
                                    .padding(end = 16.dp)
                            )
                            Column {
                                Text(
                                    text = podcast?.title ?: "",
                                    style = typography.l.semiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = podcast?.authorName ?: "",
                                    style = typography.m.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                podcast?.description?.let {
                                    Text(
                                        text = it,
                                        style = typography.s.secondary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    item(key = "episodesHeader") {
                        val episodeCount = podcast?.episodeCount ?: episodes?.size ?: 0
                        BasicText(
                            text = pluralStringResource(
                                R.plurals.podcast_episode_count_plural,
                                episodeCount,
                                episodeCount
                            ),
                            style = typography.m.semiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    episodes?.let { episodeList ->
                        items(items = episodeList, key = { it.videoId }) { episode ->
                            PodcastEpisodeItem(
                                episode = Innertube.PodcastEpisodeItem(
                                    info = Innertube.Info(
                                        name = episode.title,
                                        endpoint = NavigationEndpoint.Endpoint.Watch(videoId = episode.videoId)
                                    ),
                                    podcast = Innertube.Info(
                                        name = podcast?.title,
                                        endpoint = podcast?.authorBrowseId?.let {
                                            NavigationEndpoint.Endpoint.Browse(
                                                browseId = it
                                            )
                                        }
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
                                                withContext(Dispatchers.Main) {
                                                    it.stopRadio()
                                                    val mediaItem = episode.asMediaItem()
                                                    it.player.forcePlay(mediaItem)
                                                    it.setupRadio(
                                                        NavigationEndpoint.Endpoint.Watch(videoId = episode.videoId)
                                                    )
                                                    // Xếp hàng các tập liên quan
                                                    val playlistId = podcast?.playlistId
                                                    if (playlistId != null) {
                                                        val episodesPage = Innertube.loadPodcastEpisodesNext(playlistId)?.getOrNull()
                                                        val mediaItems = episodesPage?.items?.map { it.asMediaItem() } ?: emptyList()
                                                        it.player.addMediaItems(mediaItems)
                                                        Log.d(
                                                            "PodcastDetailScreen",
                                                            "Playing podcast episode: ${episode.title} with queue of ${mediaItems.size} episodes"
                                                        )
                                                    } else {
                                                        Log.d(
                                                            "PodcastDetailScreen",
                                                            "Playing single podcast episode: ${episode.title}"
                                                        )
                                                    }
                                                }
                                            } ?: withContext(Dispatchers.Main) {
                                                context.toast("Player service is not available")
                                                Log.e("PodcastDetailScreen", "PlayerService binder is null")
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                context.toast("Failed to play podcast: ${e.message}")
                                                Log.e("PodcastDetailScreen", "Error playing podcast episode: ${e.message}", e)
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
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .animateItem()
                            )
                        }
                    } ?: item(key = "episodesLoading") {
                        ShimmerHost(modifier = Modifier.padding(horizontal = 16.dp)) {
                            repeat(3) {
                                SongItemPlaceholder(
                                    thumbnailSize = 64.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!isLoading && podcast != null && episodes != null) {
                FloatingActionsContainerWithScrollToTop(
                    lazyListState = lazyListState,
                    icon = R.drawable.shuffle,
                    onClick = {}
                )
            }
        }
    }
}