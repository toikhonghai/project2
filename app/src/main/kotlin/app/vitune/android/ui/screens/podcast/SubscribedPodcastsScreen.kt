package app.vitune.android.ui.screens.podcast

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.models.PodcastEntity
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.ShimmerHost
import app.vitune.android.ui.components.themed.ConfirmationDialog
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.items.SongItemPlaceholder
import app.vitune.android.ui.items.SubscribedPodcastItem
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.persist.persist
import app.vitune.compose.persist.persistList
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PodcastSortBy { Title, LastUpdated }

@Composable
fun HeaderPodcastSortBy(
    sortBy: PodcastSortBy,
    setSortBy: (PodcastSortBy) -> Unit,
    sortOrder: SortOrder,
    setSortOrder: (SortOrder) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    HeaderIconButton(
        icon = R.drawable.text,
        enabled = sortBy == PodcastSortBy.Title,
        onClick = { setSortBy(PodcastSortBy.Title) }
    )
    HeaderIconButton(
        icon = R.drawable.time,
        enabled = sortBy == PodcastSortBy.LastUpdated,
        onClick = { setSortBy(PodcastSortBy.LastUpdated) }
    )
    HeaderIconButton(
        icon = R.drawable.arrow_up,
        color = colorPalette.text,
        onClick = { setSortOrder(if (sortOrder == SortOrder.Ascending) SortOrder.Descending else SortOrder.Ascending) }
    )
}

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SubscribedPodcastsScreen(
    onSearchClick: () -> Unit,
    navigationToPodcastDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val (colorPalette, typography) = LocalAppearance.current

    PersistMapCleanup(prefix = "podcastScreen/subscribed/")

    var subscribedPodcasts by persistList<PodcastEntity>(tag = "podcastScreen/subscribedPodcasts")
    var isLoadingSubscribed by persist<Boolean>(tag = "podcastScreen/isLoadingSubscribed", initialValue = false)
    var sortBy by rememberSaveable { mutableStateOf(PodcastSortBy.LastUpdated) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.Descending) }
    var deletingPodcast by rememberSaveable { mutableStateOf<PodcastEntity?>(null) }

    LaunchedEffect(sortBy, sortOrder) {
        isLoadingSubscribed = true
        withContext(Dispatchers.IO) {
            Database.getSubscribedPodcasts().collectLatest { podcasts ->
                subscribedPodcasts = when (sortBy) {
                    PodcastSortBy.Title -> podcasts.sortedBy { it.title.lowercase() }
                    PodcastSortBy.LastUpdated -> podcasts.sortedBy { it.lastUpdated }
                }.let { if (sortOrder == SortOrder.Descending) it.reversed() else it }
                    .distinctBy { it.browseId }
                    .toImmutableList()
                isLoadingSubscribed = false
            }
        }
    }

    Box(modifier = modifier.background(colorPalette.background0).fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues()
        ) {
            item(key = "sortHeader") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicText(
                        text = stringResource(R.string.subscribed_podcasts),
                        style = typography.m.semiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    HeaderPodcastSortBy(sortBy, { sortBy = it }, sortOrder, { sortOrder = it })
                }
            }

            if (isLoadingSubscribed) {
                item(key = "subscribedLoading") {
                    ShimmerHost(modifier = Modifier.padding(horizontal = 16.dp)) {
                        repeat(3) {
                            SongItemPlaceholder(thumbnailSize = 64.dp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            } else if (subscribedPodcasts.isNotEmpty()) {
                items(items = subscribedPodcasts, key = { "subscribed_${it.browseId}" }) { podcast ->
                    if (deletingPodcast?.browseId == podcast.browseId) {
                        ConfirmationDialog(
                            text = stringResource(R.string.confirm_unsubscribe_podcast, podcast.title),
                            onDismiss = { deletingPodcast = null },
                            onConfirm = {
                                scope.launch {
                                    Database.deletePodcast(podcast)
                                    Database.unsubscribeFromPodcast(podcast.browseId)
                                    deletingPodcast = null
                                }
                            }
                        )
                    }
                    SubscribedPodcastItem(
                        podcastEntity = podcast,
                        onClick = { navigationToPodcastDetail(podcast.browseId) },
                        onMenuClick = {
                            menuState.display {
                                NonQueuedMediaItemMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = podcast.asMediaItem(),
                                    onHideFromDatabase = { deletingPodcast = podcast }
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .animateItem()
                    )
                }
            } else {
                item(key = "subscribedEmpty") {
                    BasicText(
                        text = stringResource(R.string.no_subscribed_podcasts),
                        style = typography.xs.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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