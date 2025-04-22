package app.vitune.android.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.android.models.Playlist
import app.vitune.android.models.SongPlaylistMap
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.service.PlayerService
import app.vitune.android.transaction
import app.vitune.android.ui.components.BottomSheet
import app.vitune.android.ui.components.BottomSheetState
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.MusicBars
import app.vitune.android.ui.components.themed.BaseMediaItemMenu
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.HorizontalDivider
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.ui.components.themed.Menu
import app.vitune.android.ui.components.themed.MenuEntry
import app.vitune.android.ui.components.themed.QueuedMediaItemMenu
import app.vitune.android.ui.components.themed.ReorderHandle
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.components.themed.TextFieldDialog
import app.vitune.android.ui.components.themed.TextToggle
import app.vitune.android.ui.items.SongItem
import app.vitune.android.ui.items.SongItemPlaceholder
import app.vitune.android.ui.modifiers.swipeToClose
import app.vitune.android.utils.DisposableListener
import app.vitune.android.utils.addNext
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.enqueue
import app.vitune.android.utils.medium
import app.vitune.android.utils.onFirst
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.shouldBePlaying
import app.vitune.android.utils.shuffleQueue
import app.vitune.android.utils.smoothScrollToTop
import app.vitune.android.utils.windows
import app.vitune.compose.persist.persist
import app.vitune.compose.reordering.animateItemPlacement
import app.vitune.compose.reordering.draggedItem
import app.vitune.compose.reordering.rememberReorderingState
import app.vitune.core.data.enums.PlaylistSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.onOverlay
import app.vitune.core.ui.utils.roundedShape
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.requests.nextPage
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Queue(
    layoutState: BottomSheetState, // Trạng thái của BottomSheet (mở/đóng, kéo lên/xuống)
    binder: PlayerService.Binder, // Binder để truy cập player trong service
    beforeContent: @Composable RowScope.() -> Unit, // Composable hiển thị trước danh sách nhạc
    afterContent: @Composable RowScope.() -> Unit,  // Composable hiển thị sau danh sách nhạc
    modifier: Modifier = Modifier, // Modifier để tùy biến bố cục
    shape: RoundedCornerShape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp
    ), // Bo góc trên của giao diện BottomSheet
    scrollConnection: NestedScrollConnection = remember(layoutState::preUpPostDownNestedScrollConnection),
    // Scroll connection để đồng bộ cuộn giữa các phần tử
    windowInsets: WindowInsets = WindowInsets.systemBars // Đệm để tránh overlap với thanh điều hướng/hệ thống
) {
    // Lấy màu sắc và kiểu chữ từ giao diện hiện tại
    val (colorPalette, typography, _, thumbnailShape) = LocalAppearance.current
    val menuState = LocalMenuState.current

    // Padding ngang và dưới để giao diện không bị che khuất bởi system bar
    val horizontalBottomPaddingValues = windowInsets
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .asPaddingValues()

    // Biến lưu danh sách đề xuất nhạc, được lưu lại trong bộ nhớ với tag
    var suggestions by persist<Result<List<MediaItem>?>?>(tag = "queue/suggestions")

    // Chỉ số bài hát hiện tại đang phát, -1 nếu danh sách trống
    var mediaItemIndex by remember {
        mutableIntStateOf(if (binder.player.mediaItemCount == 0) -1 else binder.player.currentMediaItemIndex)
    }

    // Timeline hiện tại của player (chứa danh sách các bài nhạc)
    var windows by remember { mutableStateOf(binder.player.currentTimeline.windows) }

    // Trạng thái player có đang phát hay không
    var shouldBePlaying by remember { mutableStateOf(binder.player.shouldBePlaying) }

    // Trạng thái scroll của danh sách nhạc
    val lazyListState = rememberLazyListState()

    // Trạng thái dùng để reorder danh sách nhạc bằng thao tác kéo thả
    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = windows, // Dựa vào danh sách `windows` làm key để reorder
        onDragEnd = binder.player::moveMediaItem // Gọi hàm di chuyển bài hát trong player khi drag kết thúc
    )

    // Danh sách đề xuất nhạc hiển thị, lọc ra những bài chưa có trong danh sách hiện tại
    val visibleSuggestions by remember {
        derivedStateOf {
            suggestions
                ?.getOrNull()
                .orEmpty()
                .filter { suggestionsItem ->
                    windows.none { window -> window.mediaItem.mediaId == suggestionsItem.mediaId }
                }
        }
    }

    // Kiểm tra xem có cần load thêm gợi ý không, khi item loading xuất hiện trong vùng hiển thị
    val shouldLoadSuggestions by remember {
        derivedStateOf {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }
    }

    // Khi mediaItemIndex hoặc shouldLoadSuggestions thay đổi, block này sẽ được kích hoạt lại
    LaunchedEffect(mediaItemIndex, shouldLoadSuggestions) {
        // Nếu cần load đề xuất...
        if (shouldLoadSuggestions) withContext(Dispatchers.IO) {
            // Gọi API từ luồng IO để tránh block UI
            suggestions = runCatching {
                // Gửi request đến API Innertube (giả định là API của YouTube hoặc dịch vụ nhạc khác)
                Innertube.nextPage(
                    NextBody(videoId = windows[mediaItemIndex].mediaItem.mediaId)
                )?.mapCatching { page ->
                    // Lấy danh sách item đề xuất từ trang trả về và chuyển thành MediaItem
                    page.itemsPage?.items?.map { it.asMediaItem }
                }
            }
                // Ghi log nếu có lỗi
                .also { it.exceptionOrNull()?.printStackTrace() }
                // Lấy giá trị hoặc null nếu thất bại
                .getOrNull()
        }
    }

// Khi chỉ số mediaItem thay đổi, reset lại suggestions về null (dọn dẹp dữ liệu cũ)
    LaunchedEffect(mediaItemIndex) {
        suggestions = null
    }

// Đăng ký một listener lắng nghe sự kiện của Player
    binder.player.DisposableListener {
        object : Player.Listener {

            // Khi chuyển bài hát
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItemIndex =
                    if (binder.player.mediaItemCount == 0) -1
                    else binder.player.currentMediaItemIndex
            }

            // Khi timeline (danh sách bài hát) thay đổi
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                windows = timeline.windows
                mediaItemIndex =
                    if (binder.player.mediaItemCount == 0) -1
                    else binder.player.currentMediaItemIndex
            }

            // Khi trạng thái "play khi sẵn sàng" thay đổi
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            // Khi trạng thái phát nhạc thay đổi (đang phát, tạm dừng, đã dừng, đang buffering, v.v.)
            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    // Tạo BottomSheet chứa danh sách hàng đợi
    BottomSheet(
        state = layoutState, // Trạng thái mở/đóng của BottomSheet
        modifier = modifier.fillMaxSize(), // Chiếm toàn bộ kích thước
        collapsedContent = { innerModifier -> // Phần hiển thị khi BottomSheet bị thu gọn
            Row(
                modifier = Modifier
                    .clip(shape) // Bo góc theo shape truyền vào
                    .background(colorPalette.background2) // Màu nền của phần thu gọn
                    .fillMaxSize() // Chiếm hết không gian
                    .then(innerModifier) // Kết hợp modifier từ hệ thống
                    .padding(horizontalBottomPaddingValues), // Padding ngang và dưới (tránh overlap system bar)
                verticalAlignment = Alignment.CenterVertically, // Căn giữa theo chiều dọc
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Khoảng cách giữa các phần tử
            ) {
                Spacer(modifier = Modifier.width(4.dp)) // Khoảng trắng đầu dòng
                beforeContent() // Composable do người dùng truyền vào (hiển thị bên trái)
                Spacer(modifier = Modifier.weight(1f)) // Đẩy các phần tử về hai bên

                // Icon danh sách phát (playlist)
                Image(
                    painter = painterResource(R.drawable.playlist),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.text), // Tô màu cho icon theo màu chữ hiện tại
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.weight(1f)) // Khoảng trắng để cân đối
                afterContent() // Composable người dùng truyền vào (bên phải)
                Spacer(modifier = Modifier.width(4.dp)) // Khoảng trắng cuối dòng
            }
        }
    ) {
        // Hiệu ứng chuyển động cho cột nhạc (khi reorder sẽ dừng hoạt ảnh)
        val musicBarsTransition = updateTransition(
            targetState = if (reorderingState.isDragging) -1L else mediaItemIndex,
            label = "" // Không đặt label cho transition
        )

        // Khi composable được khởi tạo, tự động scroll tới bài hát đang phát
        LaunchedEffect(Unit) {
            lazyListState.scrollToItem(mediaItemIndex.coerceAtLeast(0)) // Đảm bảo chỉ số >= 0
        }

        Column {
            Box(
                modifier = Modifier
                    .clip(shape) // Bo góc
                    .background(colorPalette.background1) // Màu nền chính
                    .weight(1f) // Chiếm hết phần còn lại trong Column
            ) {
                // Sử dụng LookaheadScope cho khả năng đo trước layout (dùng trong animation)
                LookaheadScope {
                    LazyColumn(
                        state = lazyListState, // Trạng thái scroll
                        contentPadding = windowInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .asPaddingValues(), // Padding tránh thanh trạng thái/hệ thống
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.nestedScroll(scrollConnection) // Kéo liên kết với BottomSheet
                    ) {
                        // Vòng lặp danh sách các bài hát trong hàng đợi
                        itemsIndexed(
                            items = windows,
                            key = { _, window -> window.uid.hashCode() }, // Key duy nhất
                            contentType = { _, _ -> ContentType.Window }
                        ) { i, window ->
                            val isPlayingThisMediaItem = mediaItemIndex == window.firstPeriodIndex

                            SongItem(
                                song = window.mediaItem,
                                thumbnailSize = Dimensions.thumbnails.song,

                                // Overlay ảnh nhỏ khi đang phát (nút play hoặc animation MusicBars)
                                onThumbnailContent = {
                                    musicBarsTransition.AnimatedVisibility(
                                        visible = { it == window.firstPeriodIndex }, // Chỉ hiện nếu là bài đang phát
                                        enter = fadeIn(tween(800)),
                                        exit = fadeOut(tween(800))
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .background(
                                                    color = Color.Black.copy(alpha = 0.25f),
                                                    shape = thumbnailShape
                                                )
                                                .size(Dimensions.thumbnails.song)
                                        ) {
                                            if (shouldBePlaying) MusicBars(
                                                color = colorPalette.onOverlay,
                                                modifier = Modifier.height(24.dp)
                                            ) else Image(
                                                painter = painterResource(R.drawable.play),
                                                contentDescription = null,
                                                colorFilter = ColorFilter.tint(colorPalette.onOverlay),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },

                                // Handle để kéo reorder
                                trailingContent = {
                                    ReorderHandle(
                                        reorderingState = reorderingState,
                                        index = i
                                    )
                                },

                                // Các hành vi khi click hoặc long click
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            menuState.display {
                                                QueuedMediaItemMenu(
                                                    mediaItem = window.mediaItem,
                                                    indexInQueue = if (isPlayingThisMediaItem) null
                                                    else window.firstPeriodIndex,
                                                    onDismiss = menuState::hide
                                                )
                                            }
                                        },
                                        onClick = {
                                            if (isPlayingThisMediaItem) {
                                                // Nếu là bài đang phát: tạm dừng / phát tiếp
                                                if (shouldBePlaying) binder.player.pause()
                                                else binder.player.play()
                                            } else {
                                                // Nếu không phải, chuyển đến bài đó và phát
                                                binder.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                binder.player.playWhenReady = true
                                            }
                                        }
                                    )
                                    .animateItemPlacement(reorderingState) // Hiệu ứng di chuyển
                                    .draggedItem(reorderingState, i) // Hỗ trợ kéo reorder
                                    .background(colorPalette.background1)
                                    .let {
                                        // Vuốt ngang để xóa nếu không phải bài đang phát
                                        if (PlayerPreferences.horizontalSwipeToRemoveItem && !isPlayingThisMediaItem)
                                            it.swipeToClose(
                                                key = windows,
                                                delay = 100.milliseconds,
                                                requireUnconsumed = true
                                            ) {
                                                binder.player.removeMediaItem(window.firstPeriodIndex)
                                            }
                                        else it
                                    },
                                clip = !reorderingState.isDragging, // Không clip khi đang kéo
                                hideExplicit = !isPlayingThisMediaItem && AppearancePreferences.hideExplicit
                            )
                        }

                        // Dấu phân cách giữa hàng đợi và đề xuất
                        item(
                            key = "divider",
                            contentType = { ContentType.Divider }
                        ) {
                            if (visibleSuggestions.isNotEmpty()) HorizontalDivider(
                                modifier = Modifier.padding(start = 28.dp + Dimensions.thumbnails.song)
                            )
                        }

                        // Gợi ý bài hát
                        items(
                            items = visibleSuggestions,
                            key = { "suggestion_${it.mediaId}" },
                            contentType = { ContentType.Suggestion }
                        ) {
                            SongItem(
                                song = it,
                                thumbnailSize = Dimensions.thumbnails.song,
                                modifier = Modifier.clickable {
                                    // Mở menu chọn hành động với bài gợi ý
                                    menuState.display {
                                        BaseMediaItemMenu(
                                            onDismiss = { menuState.hide() },
                                            mediaItem = it,
                                            onEnqueue = { binder.player.enqueue(it) }, // Thêm vào hàng đợi cuối
                                            onPlayNext = { binder.player.addNext(it) } // Phát tiếp theo
                                        )
                                    }
                                },
                                trailingContent = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(
                                            12.dp,
                                            Alignment.End
                                        )
                                    ) {
                                        IconButton(
                                            icon = R.drawable.play_skip_forward,
                                            color = colorPalette.text,
                                            onClick = { binder.player.addNext(it) },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        IconButton(
                                            icon = R.drawable.enqueue,
                                            color = colorPalette.text,
                                            onClick = { binder.player.enqueue(it) },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }

                        // Loading placeholder khi đang load radio hoặc chưa có gợi ý
                        item(
                            key = "loading",
                            contentType = { ContentType.Placeholder }
                        ) {
                            if (binder.isLoadingRadio || suggestions == null)
                                Column(modifier = Modifier.shimmer()) {
                                    repeat(3) { index ->
                                        SongItemPlaceholder(
                                            thumbnailSize = Dimensions.thumbnails.song,
                                            modifier = Modifier
                                                .alpha(1f - index * 0.125f)
                                                .fillMaxWidth()
                                        )
                                    }
                                }
                        }
                    }
                }

                // Nút "shuffle" nổi + scroll to top
                FloatingActionsContainerWithScrollToTop(
                    lazyListState = lazyListState,
                    icon = R.drawable.shuffle,
                    visible = !reorderingState.isDragging,
                    insets = windowInsets.only(WindowInsetsSides.Horizontal),
                    onClick = {
                        reorderingState.coroutineScope.launch {
                            lazyListState.smoothScrollToTop() // Scroll về đầu
                        }.invokeOnCompletion {
                            binder.player.shuffleQueue() // Trộn hàng đợi sau khi scroll xong
                        }
                    }
                )
            }


            Row(
                modifier = Modifier
                    // Khi click vào hàng này thì collapse soft input (bàn phím)
                    .clickable(onClick = layoutState::collapseSoft)
                    .background(colorPalette.background2) // Nền của hàng
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp) // Padding ngang
                    .padding(horizontalBottomPaddingValues) // Padding thêm từ bên ngoài
                    .height(64.dp), // Chiều cao cố định
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút toggle vòng lặp hàng đợi
                TextToggle(
                    state = PlayerPreferences.queueLoopEnabled, // trạng thái hiện tại
                    toggleState = {
                        PlayerPreferences.queueLoopEnabled = !PlayerPreferences.queueLoopEnabled
                    }, // hành vi toggle
                    name = stringResource(R.string.queue_loop) // text hiện lên
                )

                Spacer(modifier = Modifier.weight(1f)) // Đẩy icon ra giữa

                // Icon mũi tên xuống
                Image(
                    painter = painterResource(R.drawable.chevron_down),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.text),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.weight(1f)) // Đẩy phần bên phải qua sát mép

                // Text hiển thị số bài hát trong hàng đợi, có click được
                BasicText(
                    text = pluralStringResource(
                        id = R.plurals.song_count_plural,
                        count = windows.size,
                        windows.size
                    ),
                    style = typography.xxs.medium,
                    modifier = Modifier
                        .clip(16.dp.roundedShape) // Bo tròn nền text
                        .clickable {
                            // HÀM THÊM VÀO PLAYLIST
                            fun addToPlaylist(playlist: Playlist, index: Int) = transaction {
                                val playlistId = Database
                                    .insert(playlist)
                                    .takeIf { it != -1L } ?: playlist.id // Nếu insert thất bại thì dùng id cũ

                                // Thêm từng bài hát vào playlist
                                windows.forEachIndexed { i, window ->
                                    val mediaItem = window.mediaItem
                                    Database.insert(mediaItem)
                                    Database.insert(
                                        SongPlaylistMap(
                                            songId = mediaItem.mediaId,
                                            playlistId = playlistId,
                                            position = index + i
                                        )
                                    )
                                }
                            }

                            // Hiển thị menu chọn playlist
                            menuState.display {
                                var isCreatingNewPlaylist by rememberSaveable { mutableStateOf(false) }

                                // Lấy danh sách playlist từ database, sắp xếp theo thời gian thêm
                                val playlistPreviews by remember {
                                    Database
                                        .playlistPreviews(
                                            sortBy = PlaylistSortBy.DateAdded,
                                            sortOrder = SortOrder.Descending
                                        )
                                        .onFirst { isCreatingNewPlaylist = it.isEmpty() } // Nếu chưa có thì bật tạo mới
                                }.collectAsState(initial = null, context = Dispatchers.IO)

                                // Nếu đang tạo mới playlist thì hiển thị dialog nhập tên
                                if (isCreatingNewPlaylist) TextFieldDialog(
                                    hintText = stringResource(R.string.enter_playlist_name_prompt),
                                    onDismiss = { isCreatingNewPlaylist = false },
                                    onAccept = { text ->
                                        menuState.hide()
                                        addToPlaylist(Playlist(name = text), 0)
                                    }
                                )

                                // Menu chính: chọn playlist hoặc tạo mới
                                Menu {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(horizontal = 24.dp, vertical = 8.dp)
                                            .fillMaxWidth()
                                    ) {
                                        BasicText(
                                            text = stringResource(R.string.add_queue_to_playlist),
                                            style = typography.m.semiBold,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 2,
                                            modifier = Modifier.weight(weight = 2f, fill = false)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        SecondaryTextButton(
                                            text = stringResource(R.string.new_playlist),
                                            onClick = { isCreatingNewPlaylist = true }, // Bật dialog tạo playlist
                                            alternative = true,
                                            modifier = Modifier.weight(weight = 1f, fill = false)
                                        )
                                    }

                                    // Nếu không có playlist nào thì tạo khoảng trống
                                    if (playlistPreviews?.isEmpty() == true)
                                        Spacer(modifier = Modifier.height(160.dp))

                                    // Duyệt danh sách playlist và hiển thị từng cái trong menu
                                    playlistPreviews?.forEach { playlistPreview ->
                                        MenuEntry(
                                            icon = R.drawable.playlist,
                                            text = playlistPreview.playlist.name,
                                            secondaryText = pluralStringResource(
                                                id = R.plurals.song_count_plural,
                                                count = playlistPreview.songCount,
                                                playlistPreview.songCount
                                            ),
                                            onClick = {
                                                menuState.hide()
                                                addToPlaylist(
                                                    playlistPreview.playlist,
                                                    playlistPreview.songCount
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        .background(colorPalette.background1)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// Sử dụng @JvmInline để định nghĩa một inline class – tối ưu hóa hiệu năng, tránh allocation object không cần thiết
@JvmInline
private value class
ContentType private constructor(val value: Int) {
    companion object {
        // Các hằng số đại diện cho từng loại nội dung
        val Window = ContentType(value = 0)        // Đại diện cho kiểu cửa sổ
        val Divider = ContentType(value = 1)       // Đại diện cho kiểu ngăn cách
        val Suggestion = ContentType(value = 2)    // Gợi ý (ví dụ trong UI)
        val Placeholder = ContentType(value = 3)   // Chỗ trống (ví dụ loading hoặc khi không có nội dung)
    }
}
