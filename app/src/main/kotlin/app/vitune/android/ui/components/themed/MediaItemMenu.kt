package app.vitune.android.ui.components.themed

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Info
import app.vitune.android.models.Playlist
import app.vitune.android.models.PodcastEntity
import app.vitune.android.models.PodcastEpisodeEntity
import app.vitune.android.models.PodcastEpisodePlaylistMap
import app.vitune.android.models.PodcastPlaylist
import app.vitune.android.models.Song
import app.vitune.android.models.SongPlaylistMap
import app.vitune.android.query
import app.vitune.android.service.DownloadService
import app.vitune.android.service.PrecacheService
import app.vitune.android.service.isLocal
import app.vitune.android.transaction
import app.vitune.android.ui.items.SongItem
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.artistRoute
import app.vitune.android.ui.screens.home.HideSongDialog
import app.vitune.android.utils.addNext
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.enqueue
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.formatAsDuration
import app.vitune.android.utils.isCached
import app.vitune.android.utils.launchYouTubeMusic
import app.vitune.android.utils.medium
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.core.data.enums.PlaylistSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.favoritesIcon
import app.vitune.core.ui.utils.px
import app.vitune.core.ui.utils.roundedShape
import app.vitune.core.ui.utils.songBundle
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.requests.getPodcastStreamUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File


@Composable
fun InHistoryMediaItemMenu(
    onDismiss: () -> Unit,
    song: Song,
    modifier: Modifier = Modifier
) {
    var isHiding by rememberSaveable { mutableStateOf(false) }

    if (isHiding) HideSongDialog(
        song = song,
        onDismiss = { isHiding = false },
        onConfirm = onDismiss
    )

    InHistoryMediaItemMenu(
        onDismiss = onDismiss,
        song = song,
        onHideFromDatabase = { isHiding = true },
        modifier = modifier
    )
}

@Composable
fun InHistoryMediaItemMenu(
    onDismiss: () -> Unit,
    song: Song,
    onHideFromDatabase: () -> Unit,
    modifier: Modifier = Modifier
) = NonQueuedMediaItemMenu(
    mediaItem = song.asMediaItem,
    onDismiss = onDismiss,
    onHideFromDatabase = onHideFromDatabase,
    modifier = modifier
)

@Composable
fun InPlaylistMediaItemMenu(
    onDismiss: () -> Unit,
    playlistId: Long,
    positionInPlaylist: Int,
    song: Song,
    modifier: Modifier = Modifier
) = NonQueuedMediaItemMenu(
    mediaItem = song.asMediaItem,
    onDismiss = onDismiss,
    onRemoveFromPlaylist = {
        transaction {
            Database.move(playlistId, positionInPlaylist, Int.MAX_VALUE)
            Database.delete(SongPlaylistMap(song.id, playlistId, Int.MAX_VALUE))
        }
    },
    modifier = modifier
)

@Composable
fun NonQueuedMediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onHideFromDatabase: (() -> Unit)? = null,
    onRemoveFromQuickPicks: (() -> Unit)? = null
) {
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloaded by remember { mutableStateOf(false) }

    LaunchedEffect(mediaItem) {
        Database.getDownloadedSongs().collect { songs ->
            isDownloaded = songs.any { it.id == mediaItem.mediaId }
        }
    }

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onStartRadio = {
            binder?.stopRadio()
            binder?.player?.forcePlay(mediaItem)
            binder?.setupRadio(
                NavigationEndpoint.Endpoint.Watch(
                    videoId = mediaItem.mediaId,
                    playlistId = mediaItem.mediaMetadata.extras?.getString("playlistId")
                )
            )
        },
        onPlayNext = { binder?.player?.addNext(mediaItem) },
        onEnqueue = { binder?.player?.enqueue(mediaItem) },
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        onHideFromDatabase = onHideFromDatabase,
        onRemoveFromQuickPicks = onRemoveFromQuickPicks,
        onDownload = if (!isDownloaded && !mediaItem.isLocal) {
            {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val song = Database.getDownloadedSongs().first().find { it.id == mediaItem.mediaId }
                        song?.downloadPath?.let { path ->
                            File(path).delete()
                        }
                        Database.updateSongDownloadStatus(
                            songId = mediaItem.mediaId,
                            isDownloaded = false,
                            downloadPath = null
                        )
                        withContext(Dispatchers.Main) {
                            context.toast("Đã xóa bài hát đã tải")
                        }
                    } catch (e: Exception) {
                        Log.e("NonQueuedMediaItemMenu", "Xóa bài hát đã tải thất bại: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            context.toast("Xóa bài hát đã tải thất bại: ${e.message}")
                        }
                    }
                }
            }
        } else null,
        onDeleteDownload = if (isDownloaded) {
            {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val song = Database.getDownloadedSongs().first().find { it.id == mediaItem.mediaId }
                        song?.downloadPath?.let { path ->
                            File(path).delete()
                        }
                        Database.updateSongDownloadStatus(
                            songId = mediaItem.mediaId,
                            isDownloaded = false,
                            downloadPath = null
                        )
                        withContext(Dispatchers.Main) {
                            context.toast("Đã xóa bài hát đã tải")
                        }
                    } catch (e: Exception) {
                        Log.e("NonQueuedMediaItemMenu", "Xóa bài hát đã tải thất bại: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            context.toast("Xóa bài hát đã tải thất bại: ${e.message}")
                        }
                    }
                }
            }
        } else null,
        modifier = modifier
    )
}

@Composable
fun QueuedMediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    indexInQueue: Int?,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current

    BaseMediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onRemoveFromQueue = indexInQueue?.let { index -> { binder?.player?.removeMediaItem(index) } },
        modifier = modifier
    )
}

@Composable
fun BaseMediaItemMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    onGoToEqualizer: (() -> Unit)? = null,
    onShowSleepTimer: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onEnqueue: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onHideFromDatabase: (() -> Unit)? = null,
    onRemoveFromQuickPicks: (() -> Unit)? = null,
    onShowSpeedDialog: (() -> Unit)? = null,
    onShowNormalizationDialog: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null // Thêm tham số mới
) {
    val context = LocalContext.current

    MediaItemMenu(
        mediaItem = mediaItem,
        onDismiss = onDismiss,
        onGoToEqualizer = onGoToEqualizer,
        onShowSleepTimer = onShowSleepTimer,
        onStartRadio = onStartRadio,
        onPlayNext = onPlayNext,
        onEnqueue = onEnqueue,
        onAddToPlaylist = { playlist, position ->
            transaction {
                Database.insert(mediaItem)
                Database.insert(
                    SongPlaylistMap(
                        songId = mediaItem.mediaId,
                        playlistId = Database.insert(playlist).takeIf { it != -1L } ?: playlist.id,
                        position = position
                    )
                )
            }
        },
        onHideFromDatabase = onHideFromDatabase,
        onRemoveFromPlaylist = onRemoveFromPlaylist,
        onRemoveFromQueue = onRemoveFromQueue,
        onGoToAlbum = albumRoute::global,
        onGoToArtist = artistRoute::global,
        onShare = {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "https://music.youtube.com/watch?v=${mediaItem.mediaId}"
                )
            }
            context.startActivity(Intent.createChooser(sendIntent, null))
        },
        onRemoveFromQuickPicks = onRemoveFromQuickPicks,
        onShowSpeedDialog = onShowSpeedDialog,
        onShowNormalizationDialog = onShowNormalizationDialog,
        onDownload = onDownload,
        onDeleteDownload = onDeleteDownload, // Truyền tham số mới
        modifier = modifier
    )
}

@Composable
fun MediaItemMenu(
    mediaItem: MediaItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onGoToEqualizer: (() -> Unit)? = null,
    onShowSleepTimer: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onEnqueue: (() -> Unit)? = null,
    onHideFromDatabase: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onAddToPlaylist: ((Playlist, Int) -> Unit)? = null,
    onGoToAlbum: ((String) -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null,
    onRemoveFromQuickPicks: (() -> Unit)? = null,
    onShowSpeedDialog: (() -> Unit)? = null,
    onShowNormalizationDialog: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null
) {
    val (colorPalette, typography) = LocalAppearance.current
    val density = LocalDensity.current
    val uriHandler = LocalUriHandler.current
    val binder = LocalPlayerServiceBinder.current
    val context = LocalContext.current

    val isLocal by remember { derivedStateOf { mediaItem.isLocal } }

    var isViewingPlaylists by remember { mutableStateOf(false) }
    var height by remember { mutableStateOf(0.dp) }
    var likedAt by remember { mutableStateOf<Long?>(null) }
    var isBlacklisted by remember { mutableStateOf(false) }

    val extras = remember(mediaItem) { mediaItem.mediaMetadata.extras?.songBundle }

    var albumInfo by remember {
        mutableStateOf(
            extras?.albumId?.let {
                Info(id = it, name = null)
            }
        )
    }

    var artistsInfo by remember {
        mutableStateOf(
            extras?.artistNames?.let { names ->
                extras.artistIds?.let { ids ->
                    names.zip(ids) { name, id -> Info(id, name) }
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (albumInfo == null) albumInfo = Database.songAlbumInfo(mediaItem.mediaId)
            if (artistsInfo == null) artistsInfo = Database.songArtistInfo(mediaItem.mediaId)

            launch {
                Database.likedAt(mediaItem.mediaId).collect { likedAt = it }
            }
            launch {
                Database.blacklisted(mediaItem.mediaId).collect { isBlacklisted = it }
            }
        }
    }

    AnimatedContent(
        targetState = isViewingPlaylists,
        transitionSpec = {
            val animationSpec = tween<IntOffset>(400)
            val slideDirection = if (targetState) Left else Right
            slideIntoContainer(slideDirection, animationSpec) togetherWith
                    slideOutOfContainer(slideDirection, animationSpec)
        },
        label = ""
    ) { currentIsViewingPlaylists ->
        if (currentIsViewingPlaylists) {
            val playlistPreviews by remember {
                Database.playlistPreviews(
                    sortBy = PlaylistSortBy.DateAdded,
                    sortOrder = SortOrder.Descending
                )
            }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

            var isCreatingNewPlaylist by rememberSaveable { mutableStateOf(false) }

            if (isCreatingNewPlaylist && onAddToPlaylist != null) TextFieldDialog(
                hintText = stringResource(R.string.enter_playlist_name_prompt),
                onDismiss = { isCreatingNewPlaylist = false },
                onAccept = { text ->
                    onDismiss()
                    onAddToPlaylist(Playlist(name = text), 0)
                }
            )

            BackHandler { isViewingPlaylists = false }

            Menu(modifier = modifier.requiredHeight(height)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { isViewingPlaylists = false },
                        icon = R.drawable.chevron_back,
                        color = colorPalette.textSecondary,
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )

                    if (onAddToPlaylist != null) SecondaryTextButton(
                        text = stringResource(R.string.new_playlist),
                        onClick = { isCreatingNewPlaylist = true },
                        alternative = true
                    )
                }

                onAddToPlaylist?.let { onAddToPlaylist ->
                    playlistPreviews.forEach { playlistPreview ->
                        MenuEntry(
                            icon = R.drawable.playlist,
                            text = playlistPreview.playlist.name,
                            secondaryText = pluralStringResource(
                                id = R.plurals.song_count_plural,
                                count = playlistPreview.songCount,
                                playlistPreview.songCount
                            ),
                            onClick = {
                                onDismiss()
                                onAddToPlaylist(playlistPreview.playlist, playlistPreview.songCount)
                            }
                        )
                    }
                }
            }
        } else Menu(
            modifier = modifier.onPlaced {
                height = it.size.height.px.dp(density)
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                SongItem(
                    song = mediaItem,
                    thumbnailSize = Dimensions.thumbnails.song,
                    modifier = Modifier.weight(1f),
                    showDuration = false
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        icon = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                        color = colorPalette.favoritesIcon,
                        onClick = {
                            query {
                                if (
                                    Database.like(
                                        songId = mediaItem.mediaId,
                                        likedAt = if (likedAt == null) System.currentTimeMillis() else null
                                    ) != 0
                                ) return@query

                                Database.insert(mediaItem, Song::toggleLike)
                            }
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(18.dp)
                    )

                    if (!isLocal) IconButton(
                        icon = R.drawable.share_social,
                        color = colorPalette.text,
                        onClick = {
                            onDismiss()
                            onShare()
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(17.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .alpha(0.5f)
                    .padding(vertical = 8.dp)
            )

            onPlayNext?.let {
                MenuEntry(
                    icon = R.drawable.play_skip_forward,
                    text = stringResource(R.string.play_next),
                    onClick = {
                        onDismiss()
                        onPlayNext()
                    }
                )
            }

            onEnqueue?.let {
                MenuEntry(
                    icon = R.drawable.enqueue,
                    text = stringResource(R.string.enqueue),
                    onClick = {
                        onDismiss()
                        onEnqueue()
                    }
                )
            }

            if (!isLocal) onStartRadio?.let {
                MenuEntry(
                    icon = R.drawable.radio,
                    text = stringResource(R.string.start_radio),
                    onClick = {
                        onDismiss()
                        onStartRadio()
                    }
                )
            }

            onAddToPlaylist?.let {
                MenuEntry(
                    icon = R.drawable.playlist,
                    text = stringResource(R.string.add_to_playlist),
                    onClick = { isViewingPlaylists = true },
                    trailingContent = {
                        Image(
                            painter = painterResource(R.drawable.chevron_forward),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            onGoToEqualizer?.let {
                MenuEntry(
                    icon = R.drawable.equalizer,
                    text = stringResource(R.string.equalizer),
                    onClick = {
                        onDismiss()
                        onGoToEqualizer()
                    }
                )
            }

            onShowSpeedDialog?.let {
                MenuEntry(
                    icon = R.drawable.speed,
                    text = stringResource(R.string.playback_settings),
                    onClick = {
                        onDismiss()
                        onShowSpeedDialog()
                    }
                )
            }

            onShowNormalizationDialog?.let {
                MenuEntry(
                    icon = R.drawable.volume_up,
                    text = stringResource(R.string.volume_boost),
                    onClick = {
                        onDismiss()
                        onShowNormalizationDialog()
                    }
                )
            }

            onShowSleepTimer?.let {
                var isShowingSleepTimerDialog by remember { mutableStateOf(false) }
                var sleepTimerMillisLeft by remember { mutableLongStateOf(0L) }

                LaunchedEffect(binder, binder?.sleepTimerMillisLeft) {
                    binder?.sleepTimerMillisLeft?.collectLatest {
                        sleepTimerMillisLeft = it ?: 0L
                    } ?: run { sleepTimerMillisLeft = 0L }
                }

                val stopAfterSong = {
                    runCatching {
                        binder?.startSleepTimer(
                            binder.player.duration - binder.player.contentPosition
                        )
                    }
                    isShowingSleepTimerDialog = false
                }

                if (isShowingSleepTimerDialog) {
                    if (sleepTimerMillisLeft == 0L) DefaultDialog(
                        onDismiss = { isShowingSleepTimerDialog = false }
                    ) {
                        var amount by remember { mutableIntStateOf(1) }

                        BasicText(
                            text = stringResource(R.string.set_sleep_timer),
                            style = typography.s.semiBold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 16.dp,
                                alignment = Alignment.CenterHorizontally
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .alpha(if (amount <= 1) 0.5f else 1f)
                                    .clip(CircleShape)
                                    .clickable(enabled = amount > 1) { amount-- }
                                    .size(48.dp)
                                    .background(colorPalette.background0)
                            ) {
                                BasicText(
                                    text = "-",
                                    style = typography.xs.semiBold
                                )
                            }

                            Box(contentAlignment = Alignment.Center) {
                                BasicText(
                                    text = "88h 88m", // invisible placeholder, no need to localize
                                    style = typography.s.semiBold,
                                    modifier = Modifier.alpha(0f)
                                )
                                BasicText(
                                    text = "${stringResource(R.string.format_hours, amount / 6)} ${
                                        stringResource(
                                            R.string.format_minutes,
                                            (amount % 6) * 10
                                        )
                                    }",
                                    style = typography.s.semiBold
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .alpha(if (amount >= 60) 0.5f else 1f)
                                    .clip(CircleShape)
                                    .clickable(enabled = amount < 60) { amount++ }
                                    .size(48.dp)
                                    .background(colorPalette.background0)
                            ) {
                                BasicText(
                                    text = "+",
                                    style = typography.xs.semiBold
                                )
                            }
                        }

                        SecondaryTextButton(
                            text = stringResource(R.string.sleep_timer_until_song_end),
                            onClick = stopAfterSong,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DialogTextButton(
                                text = stringResource(R.string.cancel),
                                onClick = { isShowingSleepTimerDialog = false }
                            )

                            DialogTextButton(
                                text = stringResource(R.string.set),
                                enabled = amount > 0,
                                primary = true,
                                onClick = {
                                    binder?.startSleepTimer(amount * 10 * 60 * 1000L)
                                    isShowingSleepTimerDialog = false
                                }
                            )
                        }
                    } else ConfirmationDialog(
                        text = stringResource(R.string.stop_sleep_timer_prompt),
                        cancelText = stringResource(R.string.no),
                        confirmText = stringResource(R.string.stop),
                        onDismiss = { isShowingSleepTimerDialog = false },
                        onConfirm = { binder?.cancelSleepTimer() }
                    )
                }

                MenuEntry(
                    icon = R.drawable.alarm,
                    text = stringResource(R.string.sleep_timer),
                    onClick = { isShowingSleepTimerDialog = true },
                    onLongClick = stopAfterSong,
                    trailingContent = {
                        AnimatedVisibility(
                            visible = sleepTimerMillisLeft != 0L,
                            label = "",
                            enter = fadeIn() + expandIn(),
                            exit = fadeOut() + shrinkOut()
                        ) {
                            BasicText(
                                text = stringResource(
                                    R.string.format_time_left,
                                    formatAsDuration(sleepTimerMillisLeft)
                                ),
                                style = typography.xxs.medium,
                                modifier = Modifier
                                    .background(
                                        color = colorPalette.background0,
                                        shape = 16.dp.roundedShape
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .animateContentSize()
                            )
                        }
                    }
                )
            }

            if (!isLocal) onGoToAlbum?.let {
                albumInfo?.let { (albumId) ->
                    MenuEntry(
                        icon = R.drawable.disc,
                        text = stringResource(R.string.go_to_album),
                        onClick = {
                            onDismiss()
                            onGoToAlbum(albumId)
                        }
                    )
                }
            }

            if (!isLocal) onGoToArtist?.let {
                artistsInfo?.forEach { (id, name) ->
                    name?.let {
                        MenuEntry(
                            icon = R.drawable.person,
                            text = stringResource(R.string.format_go_to_artist, name),
                            onClick = {
                                onDismiss()
                                onGoToArtist(id)
                            }
                        )
                    }
                }
            }

            if (!isLocal) MenuEntry(
                icon = R.drawable.play,
                text = stringResource(R.string.watch_on_youtube),
                onClick = {
                    onDismiss()
                    binder?.player?.pause()
                    uriHandler.openUri("https://youtube.com/watch?v=${mediaItem.mediaId}")
                }
            )

            if (!isLocal) MenuEntry(
                icon = R.drawable.musical_notes,
                text = stringResource(R.string.open_in_youtube_music),
                onClick = {
                    onDismiss()
                    binder?.player?.pause()
                    if (!launchYouTubeMusic(context, "watch?v=${mediaItem.mediaId}"))
                        context.toast(context.getString(R.string.youtube_music_not_installed))
                }
            )

            if (!isLocal) onDownload?.let {
                MenuEntry(
                    icon = R.drawable.download,
                    text = stringResource(R.string.download),
                    onClick = {
                        onDismiss()
                        onDownload()
                    }
                )
            }

            if (!isLocal) onDeleteDownload?.let {
                MenuEntry(
                    icon = R.drawable.trash,
                    text = stringResource(R.string.delete_download),
                    onClick = {
                        onDismiss()
                        onDeleteDownload()
                    }
                )
            }

            if (!mediaItem.isLocal) AnimatedContent(
                targetState = isBlacklisted,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = ""
            ) { blacklisted ->
                MenuEntry(
                    icon = R.drawable.remove_circle_outline,
                    text = if (blacklisted) stringResource(R.string.remove_from_blacklist)
                    else stringResource(R.string.add_to_blacklist),
                    onClick = {
                        transaction {
                            Database.insert(mediaItem)
                            Database.toggleBlacklist(mediaItem.mediaId)
                        }
                    }
                )
            }

            onRemoveFromQueue?.let {
                MenuEntry(
                    icon = R.drawable.trash,
                    text = stringResource(R.string.remove_from_queue),
                    onClick = {
                        onDismiss()
                        onRemoveFromQueue()
                    }
                )
            }

            onRemoveFromPlaylist?.let {
                MenuEntry(
                    icon = R.drawable.trash,
                    text = stringResource(R.string.remove_from_playlist),
                    onClick = {
                        onDismiss()
                        onRemoveFromPlaylist()
                    }
                )
            }

            onHideFromDatabase?.let {
                MenuEntry(
                    icon = R.drawable.trash,
                    text = stringResource(R.string.hide),
                    onClick = onHideFromDatabase
                )
            }

            if (!isLocal) onRemoveFromQuickPicks?.let {
                MenuEntry(
                    icon = R.drawable.trash,
                    text = stringResource(R.string.hide_from_quick_picks),
                    onClick = {
                        onDismiss()
                        onRemoveFromQuickPicks()
                    }
                )
            }
        }
    }
}

@Composable
fun PodcastEpisodeMenu(
    onDismiss: () -> Unit,
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
    playlistId: Long? = null,
    positionInPlaylist: Int? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val (colorPalette, typography) = LocalAppearance.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val favoritesPlaylistName = stringResource(R.string.favorites)

    var isViewingPlaylists by remember { mutableStateOf(false) }
    var height by remember { mutableStateOf(0.dp) }
    var likedAt by remember { mutableStateOf<Long?>(null) }
    var isBlacklisted by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            Database.podcastEpisodeLikedAt(mediaItem.mediaId).collect { likedAt = it }
        }
        launch {
            Database.blacklisted(mediaItem.mediaId).collect { isBlacklisted = it }
        }
        launch {
            Database.getDownloadedEpisodes().collect { episodes ->
                isDownloaded = episodes.any { it.videoId == mediaItem.mediaId }
            }
        }
    }

    AnimatedContent(
        targetState = isViewingPlaylists,
        transitionSpec = {
            val animationSpec = tween<IntOffset>(400)
            val slideDirection = if (targetState) Left else Right
            slideIntoContainer(slideDirection, animationSpec) togetherWith
                    slideOutOfContainer(slideDirection, animationSpec)
        },
        label = ""
    ) { currentIsViewingPlaylists ->
        if (currentIsViewingPlaylists) {
            val playlistPreviews by Database.podcastPlaylistPreviews(
                sortBy = PlaylistSortBy.DateAdded,
                sortOrder = SortOrder.Descending
            ).collectAsState(initial = emptyList(), context = Dispatchers.IO)

            var isCreatingNewPlaylist by rememberSaveable { mutableStateOf(false) }

            if (isCreatingNewPlaylist) TextFieldDialog(
                hintText = stringResource(R.string.enter_podcast_playlist_name_prompt),
                onDismiss = { isCreatingNewPlaylist = false },
                onAccept = { text ->
                    coroutineScope.launch {
                        onDismiss()
                        transaction {
                            val playlistId = Database.insert(PodcastPlaylist(name = text))
                            runBlocking(Dispatchers.IO) {
                                if (!Database.episodeExists(mediaItem.mediaId)) {
                                    Database.insertEpisode(
                                        PodcastEpisodeEntity(
                                            videoId = mediaItem.mediaId,
                                            podcastId = mediaItem.mediaMetadata.extras?.getString("podcastId") ?: "",
                                            title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                                            thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
                                            durationText = mediaItem.mediaMetadata.extras?.getString("durationText"),
                                            description = null,
                                            publishedTimeText = null
                                        )
                                    )
                                }
                            }
                            Database.insert(
                                PodcastEpisodePlaylistMap(
                                    episodeId = mediaItem.mediaId,
                                    playlistId = playlistId,
                                    position = 0
                                )
                            )
                        }
                        context.toast("Added to podcast playlist")
                    }
                }
            )

            BackHandler { isViewingPlaylists = false }

            Menu(modifier = modifier.requiredHeight(height)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { isViewingPlaylists = false },
                        icon = R.drawable.chevron_back,
                        color = colorPalette.textSecondary,
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(20.dp)
                    )

                    SecondaryTextButton(
                        text = stringResource(R.string.new_podcast_playlist),
                        onClick = { isCreatingNewPlaylist = true },
                        alternative = true
                    )
                }

                playlistPreviews.forEach { playlistPreview ->
                    MenuEntry(
                        icon = R.drawable.playlist,
                        text = playlistPreview.name,
                        secondaryText = pluralStringResource(
                            id = R.plurals.podcast_count_plural,
                            count = playlistPreview.episodeCount,
                            playlistPreview.episodeCount
                        ),
                        onClick = {
                            coroutineScope.launch {
                                onDismiss()
                                transaction {
                                    runBlocking(Dispatchers.IO) {
                                        if (!Database.episodeExists(mediaItem.mediaId)) {
                                            Database.insertEpisode(
                                                PodcastEpisodeEntity(
                                                    videoId = mediaItem.mediaId,
                                                    podcastId = mediaItem.mediaMetadata.extras?.getString("podcastId") ?: "",
                                                    title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                                                    thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
                                                    durationText = mediaItem.mediaMetadata.extras?.getString("durationText"),
                                                    description = null,
                                                    publishedTimeText = null
                                                )
                                            )
                                        }
                                    }
                                    Database.insert(
                                        PodcastEpisodePlaylistMap(
                                            episodeId = mediaItem.mediaId,
                                            playlistId = playlistPreview.id,
                                            position = playlistPreview.episodeCount
                                        )
                                    )
                                }
                                context.toast("Added to podcast playlist")
                            }
                        }
                    )
                }
            }
        } else Menu(
            modifier = modifier.onPlaced {
                height = it.size.height.px.dp(density)
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                SongItem(
                    song = mediaItem,
                    thumbnailSize = Dimensions.thumbnails.song,
                    modifier = Modifier.weight(1f),
                    showDuration = false
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        icon = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart,
                        color = colorPalette.favoritesIcon,
                        onClick = {
                            query {
                                runBlocking(Dispatchers.IO) {
                                    val podcastId = mediaItem.mediaMetadata.extras?.getString("podcastId")
                                    if (podcastId.isNullOrEmpty()) {
                                        context.toast("Cannot add to Favorites: Invalid podcast ID")
                                        return@runBlocking
                                    }

                                    // Kiểm tra và chèn podcast nếu cần
                                    val podcastExists = Database.getPodcastById(podcastId) != null
                                    if (!podcastExists) {
                                        Database.insertPodcast(
                                            PodcastEntity(
                                                browseId = podcastId,
                                                title = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Podcast",
                                                description = null,
                                                authorName = null,
                                                authorBrowseId = null,
                                                thumbnailUrl = null,
                                                episodeCount = null,
                                                lastUpdated = System.currentTimeMillis()
                                            )
                                        )
                                    }

                                    // Chèn hoặc cập nhật tập podcast
                                    if (!Database.episodeExists(mediaItem.mediaId)) {
                                        Database.insertEpisode(
                                            PodcastEpisodeEntity(
                                                videoId = mediaItem.mediaId,
                                                podcastId = podcastId,
                                                title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                                                thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
                                                durationText = mediaItem.mediaMetadata.extras?.getString("durationText"),
                                                description = null,
                                                publishedTimeText = null,
                                                likedAt = System.currentTimeMillis() // Đặt likedAt ngay khi chèn
                                            )
                                        )
                                    } else {
                                        Database.likePodcastEpisode(
                                            mediaItem.mediaId,
                                            if (likedAt == null) System.currentTimeMillis() else null
                                        )
                                    }

                                    // Xử lý playlist Favorites
                                    val favoritesPlaylist = Database.getPodcastPlaylistByNameSync(favoritesPlaylistName)
                                    favoritesPlaylist?.let {
                                        if (likedAt == null) {
                                            Database.insert(
                                                PodcastEpisodePlaylistMap(
                                                    episodeId = mediaItem.mediaId,
                                                    playlistId = it.id,
                                                    position = 0
                                                )
                                            )
                                        } else {
                                            Database.delete(
                                                PodcastEpisodePlaylistMap(
                                                    episodeId = mediaItem.mediaId,
                                                    playlistId = it.id,
                                                    position = 0
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(18.dp)
                    )

                    IconButton(
                        icon = R.drawable.share_social,
                        color = colorPalette.text,
                        onClick = {
                            onDismiss()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://www.youtube.com/watch?v=${mediaItem.mediaId}"
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        },
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .size(17.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .alpha(0.5f)
                    .padding(vertical = 8.dp)
            )

            MenuEntry(
                icon = R.drawable.play_skip_forward,
                text = stringResource(R.string.play_next),
                onClick = {
                    onDismiss()
                    binder?.player?.addNext(mediaItem)
                }
            )

            MenuEntry(
                icon = R.drawable.enqueue,
                text = stringResource(R.string.enqueue),
                onClick = {
                    onDismiss()
                    binder?.player?.enqueue(mediaItem)
                }
            )

            MenuEntry(
                icon = R.drawable.radio,
                text = stringResource(R.string.start_radio),
                onClick = {
                    onDismiss()
                    binder?.stopRadio()
                    binder?.player?.forcePlay(mediaItem)
                    binder?.setupRadio(
                        NavigationEndpoint.Endpoint.Watch(
                            videoId = mediaItem.mediaId,
                            playlistId = mediaItem.mediaMetadata.extras?.getString("playlistId")
                        )
                    )
                }
            )

            MenuEntry(
                icon = R.drawable.playlist,
                text = stringResource(R.string.add_to_playlist),
                onClick = { isViewingPlaylists = true },
                trailingContent = {
                    Image(
                        painter = painterResource(R.drawable.chevron_forward),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            if (!isDownloaded) MenuEntry(
                icon = R.drawable.download,
                text = stringResource(R.string.download),
                onClick = {
                    onDismiss()
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val streamUrl = Innertube.getPodcastStreamUrl(mediaItem.mediaId)
                                ?: throw Exception("Không tìm thấy URL luồng podcast")
                            if (!Database.episodeExists(mediaItem.mediaId)) {
                                Database.insertEpisode(
                                    PodcastEpisodeEntity(
                                        videoId = mediaItem.mediaId,
                                        podcastId = mediaItem.mediaMetadata.extras?.getString("podcastId") ?: "",
                                        title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                                        thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
                                        durationText = mediaItem.mediaMetadata.extras?.getString("durationText") ?: "0:00",
                                        description = null,
                                        publishedTimeText = null
                                    )
                                )
                            }
                            DownloadService.startDownload(context.applicationContext, mediaItem.mediaId, streamUrl)
                            withContext(Dispatchers.Main) {
                                context.toast("Đã bắt đầu tải podcast")
                            }
                        } catch (e: Exception) {
                            Log.e("PodcastEpisodeMenu", "Tải podcast thất bại: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                context.toast("Tải podcast thất bại: ${e.message}")
                            }
                        }
                    }
                }
            )

            if (isDownloaded) MenuEntry(
                icon = R.drawable.trash,
                text = stringResource(R.string.delete_download),
                onClick = {
                    onDismiss()
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val episode = Database.getDownloadedEpisodes().first().find { it.videoId == mediaItem.mediaId }
                            episode?.downloadPath?.let { path ->
                                File(path).delete()
                            }
                            Database.updateEpisodeDownloadStatus(
                                videoId = mediaItem.mediaId,
                                isDownloaded = false,
                                downloadPath = null
                            )
                            withContext(Dispatchers.Main) {
                                context.toast("Đã xóa podcast đã tải")
                            }
                        } catch (e: Exception) {
                            Log.e("PodcastEpisodeMenu", "Xóa podcast đã tải thất bại: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                context.toast("Xóa podcast đã tải thất bại: ${e.message}")
                            }
                        }
                    }
                }
            )

            onRemoveFromPlaylist?.let {
                if (playlistId != null && positionInPlaylist != null) {
                    MenuEntry(
                        icon = R.drawable.trash,
                        text = stringResource(R.string.remove_from_playlist),
                        onClick = {
                            onDismiss()
                            onRemoveFromPlaylist()
                        }
                    )
                }
            }

            MenuEntry(
                icon = R.drawable.play,
                text = stringResource(R.string.watch_on_youtube),
                onClick = {
                    onDismiss()
                    binder?.player?.pause()
                    uriHandler.openUri("https://youtube.com/watch?v=${mediaItem.mediaId}")
                }
            )

            MenuEntry(
                icon = R.drawable.musical_notes,
                text = stringResource(R.string.open_in_youtube_music),
                onClick = {
                    onDismiss()
                    binder?.player?.pause()
                    if (!launchYouTubeMusic(context, "watch?v=${mediaItem.mediaId}"))
                        context.toast(context.getString(R.string.youtube_music_not_installed))
                }
            )

            AnimatedContent(
                targetState = isBlacklisted,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = ""
            ) { blacklisted ->
                MenuEntry(
                    icon = R.drawable.remove_circle_outline,
                    text = if (blacklisted) stringResource(R.string.remove_from_blacklist)
                    else stringResource(R.string.add_to_blacklist),
                    onClick = {
                        transaction {
                            Database.insert(mediaItem)
                            Database.toggleBlacklist(mediaItem.mediaId)
                        }
                    }
                )
            }
        }
    }
}