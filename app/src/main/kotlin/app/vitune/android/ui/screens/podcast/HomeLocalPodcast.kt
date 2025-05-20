package app.vitune.android.ui.screens.podcast

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.PodcastEpisodeEntity
import app.vitune.android.preferences.OrderPreferences
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.PodcastEpisodeMenu
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.items.PodcastEpisodeItem
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.hasPermission
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.isAtLeastAndroid13
import app.vitune.core.ui.utils.isCompositionLaunched
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.Thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Quyền truy cập bộ nhớ cho Android
private val permission = if (isAtLeastAndroid13)
    Manifest.permission.READ_MEDIA_AUDIO
else
    Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun HomeLocalPodcast(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) = with(OrderPreferences) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val (colorPalette, typography) = LocalAppearance.current
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Kiểm tra quyền truy cập
    var hasPermission by remember(isCompositionLaunched()) {
        mutableStateOf(context.applicationContext.hasPermission(permission))
    }

    // Xử lý yêu cầu quyền
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasPermission = it }
    )

    // Lấy danh sách podcast đã tải xuống
    val episodes by Database.getDownloadedEpisodes().collectAsState(initial = emptyList())

    if (hasPermission) {
        Box(
            modifier = modifier
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
                    Header(title = stringResource(R.string.local)) {
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
                                        val mediaItems = episodes.map { ep -> ep.asMediaItem() }
                                        val index =
                                            episodes.indexOfFirst { it.videoId == episode.videoId }
                                        if (index != -1) {
                                            it.player.forcePlayAtIndex(mediaItems, index)
                                            if (episode.playPositionMs > 0) {
                                                it.player.seekTo(episode.playPositionMs)
                                            }
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
    } else {
        LaunchedEffect(Unit) { launcher.launch(permission) }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                text = stringResource(R.string.media_permission_declined),
                modifier = Modifier.fillMaxWidth(0.75f),
                style = typography.m.medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryTextButton(
                text = stringResource(R.string.open_settings),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            setData(Uri.fromParts("package", context.packageName, null))
                        }
                    )
                }
            )
        }
    }
}