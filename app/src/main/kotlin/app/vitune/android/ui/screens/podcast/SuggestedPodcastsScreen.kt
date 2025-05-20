package app.vitune.android.ui.screens.podcast

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.models.PodcastEntity
import app.vitune.android.models.PodcastSubscriptionEntity
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.ShimmerHost
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.items.PodcastItem
import app.vitune.android.ui.items.SongItemPlaceholder
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.persist.persist
import app.vitune.compose.persist.persistList
import app.vitune.core.ui.LocalAppearance
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.Thumbnail
import app.vitune.providers.innertube.requests.loadPodcastPage
import app.vitune.providers.innertube.requests.searchPodcasts
import app.vitune.providers.innertube.requests.searchPodcastsWithContinuation
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun setLastRefreshed(context: Context, value: Long) {
    context.getSharedPreferences("PodcastScreenPrefs", Context.MODE_PRIVATE)
        .edit()
        .putLong("lastRefreshed", value)
        .apply()
}

private fun getLastRefreshed(context: Context): Long {
    return context.getSharedPreferences("PodcastScreenPrefs", Context.MODE_PRIVATE)
        .getLong("lastRefreshed", 0L)
}

@OptIn(UnstableApi::class)
suspend fun fetchPodcastWithEpisodeCount(podcast: Innertube.PodcastItem, context: android.content.Context): PodcastEntity {
    val browseId = podcast.info?.endpoint?.browseId
    if (browseId == null) {
        Log.w("SuggestedPodcastsScreen", "Invalid browseId for podcast: ${podcast.info?.name}")
        return PodcastEntity.fromPodcastItem(podcast.copy(episodeCount = 0))
    }

    Log.d("SuggestedPodcastsScreen", "Fetching episode count for podcast: ${podcast.info?.name}, browseId: $browseId")
    return Innertube.loadPodcastPage(browseId)?.onSuccess { podcastPage ->
        val entity = PodcastEntity(
            browseId = podcast.key,
            title = podcastPage.title ?: podcast.info?.name ?: "",
            description = podcastPage.description,
            authorName = podcastPage.author?.name,
            authorBrowseId = podcastPage.author?.endpoint?.browseId,
            thumbnailUrl = podcastPage.thumbnail?.url,
            episodeCount = podcastPage.episodeCount ?: 0,
            playlistId = podcastPage.playlistId,
            lastUpdated = System.currentTimeMillis()
        )
        Database.insertPodcast(entity)
        Log.d("SuggestedPodcastsScreen", "Fetched podcast: ${podcastPage.title}, episodeCount: ${podcastPage.episodeCount}")
    }?.getOrNull()?.let { podcastPage ->
        PodcastEntity(
            browseId = podcast.key,
            title = podcastPage.title ?: podcast.info?.name ?: "",
            description = podcastPage.description,
            authorName = podcastPage.author?.name,
            authorBrowseId = podcastPage.author?.endpoint?.browseId,
            thumbnailUrl = podcastPage.thumbnail?.url,
            episodeCount = podcastPage.episodeCount ?: 0,
            playlistId = podcastPage.playlistId,
            lastUpdated = System.currentTimeMillis()
        )
    } ?: PodcastEntity.fromPodcastItem(podcast.copy(episodeCount = 0))
}

suspend fun resolvePodcastIdentifier(podcast: PodcastEntity): String? {
    val browseId = podcast.browseId
    if (browseId.isEmpty()) {
        Log.w("SuggestedPodcastsScreen", "Podcast has no browseId: ${podcast.title}")
        return null
    }

    Log.d("SuggestedPodcastsScreen", "Using browseId for podcast: ${podcast.title}, browseId: $browseId")
    return browseId
}

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class, ExperimentalAnimationApi::class)
@Composable
fun SuggestedPodcastsScreen(
    onSearchClick: () -> Unit,
    navigationToPodcastDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val (colorPalette, typography) = LocalAppearance.current

    PersistMapCleanup(prefix = "podcastScreen/suggested/")

    var suggestedPodcasts by persistList<PodcastEntity>(tag = "podcastScreen/suggestedPodcasts")
    var isLoadingSuggested by persist<Boolean>(tag = "podcastScreen/isLoadingSuggested", initialValue = true)
    var continuationToken by persist<String?>(tag = "podcastScreen/continuationToken")

    LaunchedEffect(Unit) {
        isLoadingSuggested = true
        withContext(Dispatchers.IO) {
            // Lấy danh sách podcast từ cơ sở dữ liệu
            val dbPodcasts = Database.getPodcasts(20).distinctBy { it.browseId }
            // Lấy danh sách browseId của các podcast đã đăng ký
            val subscribedBrowseIds = Database.getSubscribedPodcasts().first().map { it.browseId }.toSet()
            // Lọc bỏ các podcast đã đăng ký
            val filteredDbPodcasts = dbPodcasts.filter { it.browseId !in subscribedBrowseIds }
            if (filteredDbPodcasts.isNotEmpty()) {
                suggestedPodcasts = filteredDbPodcasts.toImmutableList()
                Log.d("SuggestedPodcastsScreen", "Loaded ${filteredDbPodcasts.size} podcasts from database")
            }

            val currentTime = System.currentTimeMillis()
            val lastRefreshed = getLastRefreshed(context)
            if (currentTime - lastRefreshed > 60 * 60 * 1000) {
                val queries = listOf("Podcast")
                var podcasts = listOf<PodcastEntity>()
                for (query in queries) {
                    Log.d("SuggestedPodcastsScreen", "Trying query: $query")
                    val result = Innertube.searchPodcasts(query)
                    result?.onSuccess { page ->
                        val newPodcasts = page?.items?.filterNot { podcast ->
                            // Lọc bỏ podcast đã đăng ký và trùng lặp
                            podcasts.any { it.browseId == podcast.key } || podcast.key in subscribedBrowseIds
                        }?.mapNotNull { podcast ->
                            fetchPodcastWithEpisodeCount(podcast, context)
                        }?.distinctBy { it.browseId } ?: emptyList()
                        podcasts = podcasts + newPodcasts
                        continuationToken = page?.continuation
                        if (podcasts.size >= 20) break
                    }?.onFailure { e ->
                        Log.e("SuggestedPodcastsScreen", "Error with query $query: ${e.message}", e)
                    }
                }
                podcasts.distinctBy { it.browseId }.forEach { Database.insertPodcast(it) }
                if (podcasts.isNotEmpty()) {
                    // Lọc lại danh sách để đảm bảo không có podcast đã đăng ký
                    suggestedPodcasts = podcasts.filter { it.browseId !in subscribedBrowseIds }.distinctBy { it.browseId }.toImmutableList()
                }
                if (podcasts.isEmpty() && filteredDbPodcasts.isEmpty()) {
                    Log.e("SuggestedPodcastsScreen", "All queries failed and database is empty")
                    context.toast("No podcasts found. Please try searching for a specific podcast.")
                }
                setLastRefreshed(context, currentTime)
            }
            isLoadingSuggested = false
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                if (lastVisibleItem != null && lastVisibleItem.index >= suggestedPodcasts.size - 2 && continuationToken != null) {
                    withContext(Dispatchers.IO) {
                        // Lấy danh sách browseId của các podcast đã đăng ký
                        val subscribedBrowseIds = Database.getSubscribedPodcasts().first().map { it.browseId }.toSet()
                        Innertube.searchPodcastsWithContinuation(continuationToken!!)?.onSuccess { page ->
                            val newPodcasts = page?.items?.filterNot { podcast ->
                                // Lọc bỏ podcast đã đăng ký và trùng lặp
                                suggestedPodcasts.any { it.browseId == podcast.key } || podcast.key in subscribedBrowseIds
                            }?.mapNotNull { podcast ->
                                fetchPodcastWithEpisodeCount(podcast, context)
                            }?.distinctBy { it.browseId } ?: emptyList()
                            newPodcasts.forEach { Database.insertPodcast(it) }
                            suggestedPodcasts = (suggestedPodcasts + newPodcasts).distinctBy { it.browseId }.toImmutableList()
                            continuationToken = page?.continuation
                        }?.onFailure { e ->
                            Log.e("SuggestedPodcastsScreen", "Failed to load more podcasts: ${e.message}", e)
                            context.toast("Failed to load more podcasts: ${e.message}")
                        }
                    }
                }
            }
    }

    // Phần còn lại của composable giữ nguyên
    Box(modifier = modifier.background(colorPalette.background0).fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues()
        ) {
            item(key = "suggestedHeader") {
                BasicText(
                    text = stringResource(R.string.suggested_podcasts),
                    style = typography.m.semiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (isLoadingSuggested) {
                item(key = "suggestedLoading") {
                    ShimmerHost(modifier = Modifier.padding(horizontal = 16.dp)) {
                        repeat(3) {
                            SongItemPlaceholder(thumbnailSize = 64.dp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            } else if (suggestedPodcasts.isNotEmpty()) {
                items<PodcastEntity>(
                    items = suggestedPodcasts,
                    key = { "suggested_${it.browseId}" }
                ) { podcast ->
                    PodcastItem(
                        podcast = Innertube.PodcastItem(
                            info = Innertube.Info(
                                name = podcast.title,
                                endpoint = NavigationEndpoint.Endpoint.Browse(browseId = podcast.browseId)
                            ),
                            authors = listOfNotNull(podcast.authorName?.let { name ->
                                Innertube.Info(
                                    name = name,
                                    endpoint = podcast.authorBrowseId?.let { browseId ->
                                        NavigationEndpoint.Endpoint.Browse(browseId = browseId)
                                    }
                                )
                            }),
                            description = podcast.description,
                            episodeCount = podcast.episodeCount,
                            playlistId = podcast.playlistId,
                            thumbnail = podcast.thumbnailUrl?.let { url ->
                                Thumbnail(url = url, height = null, width = null)
                            }
                        ),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val identifier = resolvePodcastIdentifier(podcast)
                                if (identifier != null) {
                                    navigationToPodcastDetail(identifier)
                                } else {
                                    context.toast("Unable to navigate to podcast: ${podcast.title}")
                                }
                            }
                        },
                        onMenuClick = {
                            menuState.display {
                                NonQueuedMediaItemMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = podcast.asMediaItem()
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
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            icon = R.drawable.search,
            onClick = onSearchClick
        )
    }
}