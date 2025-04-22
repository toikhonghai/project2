package app.vitune.android.ui.screens.home

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Song
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.android.preferences.OrderPreferences
import app.vitune.android.query
import app.vitune.android.service.isLocal
import app.vitune.android.transaction
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.ConfirmationDialog
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.components.themed.InHistoryMediaItemMenu
import app.vitune.android.ui.components.themed.TextField
import app.vitune.android.ui.items.SongItem
import app.vitune.android.ui.modifiers.swipeToClose
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.center
import app.vitune.android.utils.color
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.formatted
import app.vitune.android.utils.playingSong
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.compose.persist.persistList
import app.vitune.core.data.enums.SongSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.onOverlay
import app.vitune.core.ui.overlay
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

private val Song.formattedTotalPlayTime @Composable get() = totalPlayTimeMs.milliseconds.formatted

@Composable
fun HomeSongs(
    onSearchClick: () -> Unit
) = with(OrderPreferences) {
    HomeSongs(
        onSearchClick = onSearchClick,
        songProvider = {
            Database.songs(songSortBy, songSortOrder)
                .map { songs -> songs.filter { it.totalPlayTimeMs > 0L } }
        },
        sortBy = songSortBy,
        setSortBy = { songSortBy = it },
        sortOrder = songSortOrder,
        setSortOrder = { songSortOrder = it },
        title = stringResource(R.string.songs)
    )
}

// Cho phép sử dụng API đang ở trạng thái experimental (ví dụ LazyColumn nâng cao)
@kotlin.OptIn(ExperimentalFoundationApi::class)
@OptIn(UnstableApi::class)
@Route // Dùng để đánh dấu đây là một màn hình điều hướng trong custom navigation
@Composable
fun HomeSongs(
    onSearchClick: () -> Unit, // Callback khi người dùng bấm nút tìm kiếm nổi
    songProvider: () -> Flow<List<Song>>, // Nguồn cung cấp danh sách bài hát (luồng dữ liệu)
    sortBy: SongSortBy, // Kiểu sắp xếp hiện tại
    setSortBy: (SongSortBy) -> Unit, // Callback thay đổi kiểu sắp xếp
    sortOrder: SortOrder, // Thứ tự sắp xếp (tăng/giảm)
    setSortOrder: (SortOrder) -> Unit, // Callback thay đổi thứ tự sắp xếp
    title: String // Tiêu đề hiển thị trên đầu danh sách
) {
    val (colorPalette, typography, _, thumbnailShape) = LocalAppearance.current // Lấy màu và font từ theme
    val binder = LocalPlayerServiceBinder.current // Truy cập player để điều khiển phát nhạc
    val menuState = LocalMenuState.current // Truy cập menu context
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var filter: String? by rememberSaveable { mutableStateOf(null) } // Bộ lọc tên bài hát
    var items by persistList<Song>("home/songs") // Lưu danh sách bài hát giữa các recomposition

    // Lọc bài hát theo `filter`
    val filteredItems by remember {
        derivedStateOf {
            filter?.lowercase()?.ifBlank { null }?.let { f ->
                items.filter {
                    f in it.title.lowercase() || f in it.artistsText?.lowercase().orEmpty()
                }.sortedBy { it.title }
            } ?: items
        }
    }

    var hidingSong: String? by rememberSaveable { mutableStateOf(null) } // ID bài hát đang định ẩn (hiện dialog)

    // Lấy danh sách bài hát từ provider
    LaunchedEffect(sortBy, sortOrder, songProvider) {
        songProvider().collect { items = it.toPersistentList() }
    }

    val lazyListState = rememberLazyListState() // Lưu trạng thái scroll

    val (currentMediaId, playing) = playingSong(binder) // Lấy bài đang phát

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
            // HEADER
            item(key = "header", contentType = 0) {
                Header(title = title) {
                    var searching by rememberSaveable { mutableStateOf(false) }

                    AnimatedContent(targetState = searching, label = "") { state ->
                        if (state) {
                            val focusRequester = remember { FocusRequester() }

                            LaunchedEffect(Unit) { focusRequester.requestFocus() }

                            // Thanh tìm kiếm khi người dùng nhấn nút search
                            TextField(
                                value = filter.orEmpty(),
                                onValueChange = { filter = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (filter.isNullOrBlank()) filter = ""
                                    focusManager.clearFocus()
                                }),
                                hintText = stringResource(R.string.filter_placeholder),
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        if (!it.hasFocus) {
                                            keyboardController?.hide()
                                            if (filter?.isBlank() == true) {
                                                filter = null
                                                searching = false
                                            }
                                        }
                                    }
                            )
                        } else Row(verticalAlignment = Alignment.CenterVertically) {
                            // Nút mở thanh tìm kiếm
                            HeaderIconButton(
                                onClick = { searching = true },
                                icon = R.drawable.search,
                                color = colorPalette.text
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Hiển thị số lượng bài hát
                            if (items.isNotEmpty()) BasicText(
                                text = pluralStringResource(
                                    R.plurals.song_count_plural,
                                    items.size,
                                    items.size
                                ),
                                style = typography.xs.secondary.semiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f)) // đẩy icon về bên phải

                    // Nút sắp xếp
                    HeaderSongSortBy(sortBy, setSortBy, sortOrder, setSortOrder)
                }
            }

            // Danh sách bài hát
            items(
                items = filteredItems,
                key = { song -> song.id }
            ) { song ->
                // Nếu người dùng đang ẩn bài này => hiện dialog xác nhận
                if (hidingSong == song.id) HideSongDialog(
                    song = song,
                    onDismiss = { hidingSong = null },
                    onConfirm = {
                        hidingSong = null
                        menuState.hide()
                    }
                )

                // Mỗi item bài hát
                SongItem(
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                keyboardController?.hide()
                                menuState.display {
                                    InHistoryMediaItemMenu(
                                        song = song,
                                        onDismiss = menuState::hide,
                                        onHideFromDatabase = { hidingSong = song.id }
                                    )
                                }
                            },
                            onClick = {
                                keyboardController?.hide()
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(
                                    items.map(Song::asMediaItem),
                                    items.indexOf(song)
                                )
                            }
                        )
                        .animateItem() // Hiệu ứng cuộn mượt
                        .let {
                            if (AppearancePreferences.swipeToHideSong) it.swipeToClose(
                                key = filteredItems,
                                requireUnconsumed = true
                            ) { animationJob ->
                                if (AppearancePreferences.swipeToHideSongConfirm)
                                    hidingSong = song.id
                                else {
                                    if (!song.isLocal) binder?.cache?.removeResource(song.id)
                                    transaction { Database.delete(song) }
                                }
                                animationJob.join()
                            } else it
                        },
                    song = song,
                    thumbnailSize = Dimensions.thumbnails.song,
                    onThumbnailContent = if (sortBy == SongSortBy.PlayTime) {
                        {
                            BasicText(
                                text = song.formattedTotalPlayTime,
                                style = typography.xxs.semiBold.center.color(colorPalette.onOverlay),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, colorPalette.overlay)
                                        ),
                                        shape = thumbnailShape.copy(
                                            topStart = CornerSize(0.dp),
                                            topEnd = CornerSize(0.dp)
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.BottomCenter)
                            )
                        }
                    } else null,
                    isPlaying = playing && currentMediaId == song.id
                )
            }
        }

        // Nút tìm kiếm nổi + cuộn lên đầu danh sách
        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            icon = R.drawable.search,
            onClick = onSearchClick
        )
    }
}

// Chú thích cho phép sử dụng API chưa ổn định (unstable)
@OptIn(UnstableApi::class)
@Composable
fun HideSongDialog(
    song: Song,                          // Bài hát sẽ bị ẩn (xóa)
    onDismiss: () -> Unit,              // Callback khi người dùng đóng dialog
    onConfirm: () -> Unit,              // Callback khi người dùng xác nhận
    modifier: Modifier = Modifier       // Cho phép chỉnh sửa giao diện ngoài
) {
    val binder = LocalPlayerServiceBinder.current // Truy cập player service để quản lý cache

    ConfirmationDialog(
        text = stringResource(R.string.confirm_hide_song), // Text trong dialog
        onDismiss = onDismiss,                             // Sự kiện đóng
        onConfirm = {
            onConfirm() // Gọi callback bên ngoài

            // Xử lý xóa bài hát khỏi cache và database
            query {
                runCatching {
                    // Nếu bài hát không phải local => xóa khỏi cache
                    if (!song.isLocal) binder?.cache?.removeResource(song.id)
                    // Xóa khỏi database
                    Database.delete(song)
                }
            }
        },
        modifier = modifier
    )
}

// Row content, for convenience, doesn't need modifier/receiver
// Một hàng chứa các nút sắp xếp, sử dụng trong Header (ví dụ như danh sách bài hát)
// Không dùng modifier cho RowScope => bỏ cảnh báo
@Suppress("UnusedReceiverParameter", "ModifierMissing")
@Composable
fun RowScope.HeaderSongSortBy(
    sortBy: SongSortBy,                    // Kiểu sắp xếp hiện tại (Title, PlayTime, v.v.)
    setSortBy: (SongSortBy) -> Unit,       // Hàm để cập nhật kiểu sắp xếp
    sortOrder: SortOrder,                  // Thứ tự sắp xếp (Ascending / Descending)
    setSortOrder: (SortOrder) -> Unit      // Hàm để cập nhật thứ tự sắp xếp
) {
    val (colorPalette) = LocalAppearance.current // Lấy màu sắc hiện tại từ theme

    // Tạo hiệu ứng xoay cho icon dựa trên sortOrder (0 độ nếu tăng, 180 độ nếu giảm)
    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = ""
    )

    // Nút sắp xếp theo thời gian nghe
    HeaderIconButton(
        icon = R.drawable.trending,
        enabled = sortBy == SongSortBy.PlayTime, // Sáng nút nếu đang sắp xếp theo mục này
        onClick = { setSortBy(SongSortBy.PlayTime) }
    )

    // Nút sắp xếp theo tên
    HeaderIconButton(
        icon = R.drawable.text,
        enabled = sortBy == SongSortBy.Title,
        onClick = { setSortBy(SongSortBy.Title) }
    )

    // Nút sắp xếp theo ngày thêm vào
    HeaderIconButton(
        icon = R.drawable.time,
        enabled = sortBy == SongSortBy.DateAdded,
        onClick = { setSortBy(SongSortBy.DateAdded) }
    )

    // Nút đảo thứ tự sắp xếp (tăng/giảm), có hiệu ứng xoay mũi tên
    HeaderIconButton(
        icon = R.drawable.arrow_up,
        color = colorPalette.text, // Màu icon theo theme
        onClick = { setSortOrder(!sortOrder) }, // Đảo chiều sắp xếp
        modifier = Modifier.graphicsLayer { rotationZ = sortOrderIconRotation } // Xoay icon
    )
}
