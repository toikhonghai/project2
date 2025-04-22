package app.vitune.android.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Song
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.query
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.ShimmerHost
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.components.themed.TextPlaceholder
import app.vitune.android.ui.items.AlbumItem
import app.vitune.android.ui.items.AlbumItemPlaceholder
import app.vitune.android.ui.items.ArtistItem
import app.vitune.android.ui.items.ArtistItemPlaceholder
import app.vitune.android.ui.items.PlaylistItem
import app.vitune.android.ui.items.PlaylistItemPlaceholder
import app.vitune.android.ui.items.SongItem
import app.vitune.android.ui.items.SongItemPlaceholder
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.center
import app.vitune.android.utils.forcePlay
import app.vitune.android.utils.playingSong
import app.vitune.android.utils.rememberSnapLayoutInfo
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.compose.persist.persist
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.isLandscape
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.requests.relatedPage
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("UnusedBoxWithConstraintsScope") // Tắt cảnh báo về phạm vi BoxWithConstraints không sử dụng
@OptIn(ExperimentalFoundationApi::class) // Cho phép sử dụng các API đang trong giai đoạn thử nghiệm
@Route // Gắn hàm này vào hệ thống định tuyến của ứng dụng
@Composable
fun QuickPicks(
    // Callback khi người dùng click vào album
    onAlbumClick: (Innertube.AlbumItem) -> Unit,
    // Callback khi người dùng click vào nghệ sĩ
    onArtistClick: (Innertube.ArtistItem) -> Unit,
    // Callback khi người dùng click vào playlist
    onPlaylistClick: (Innertube.PlaylistItem) -> Unit,
    // Callback khi người dùng click vào thanh tìm kiếm
    onSearchClick: () -> Unit
) {
    // Lấy màu sắc và kiểu chữ từ theme hiện tại
    val (colorPalette, typography) = LocalAppearance.current

    // Lấy binder để tương tác với PlayerService
    val binder = LocalPlayerServiceBinder.current

    // Trạng thái menu hiện tại (được dùng để điều khiển giao diện toàn cục)
    val menuState = LocalMenuState.current

    // Insets của màn hình có tính đến player (ví dụ như tránh player ở dưới khi cuộn)
    val windowInsets = LocalPlayerAwareWindowInsets.current

    // Biến lưu bài hát thịnh hành được chọn (dùng `persist` để giữ giá trị khi recomposition)
    var trending by persist<Song?>("home/trending")

    // Kết quả dữ liệu liên quan (albums, playlists, nghệ sĩ, bài hát...) dùng cho phần Quick Picks
    var relatedPageResult by persist<Result<Innertube.RelatedPage?>?>(tag = "home/relatedPageResult")

    // Ghi đè cache nếu người dùng cho phép cache quick picks
    LaunchedEffect(relatedPageResult, DataPreferences.shouldCacheQuickPicks) {
        if (DataPreferences.shouldCacheQuickPicks)
            relatedPageResult?.getOrNull()?.let { DataPreferences.cachedQuickPicks = it }
        else
            DataPreferences.cachedQuickPicks = Innertube.RelatedPage()
    }

    // Khi người dùng thay đổi nguồn dữ liệu (Trending / LastInteraction)
    LaunchedEffect(DataPreferences.quickPicksSource) {
        // Nếu đã có cache và được phép dùng cache thì dùng lại
        if (
            DataPreferences.shouldCacheQuickPicks &&
            !DataPreferences.cachedQuickPicks.let {
                it.albums.isNullOrEmpty() &&
                        it.artists.isNullOrEmpty() &&
                        it.playlists.isNullOrEmpty() &&
                        it.songs.isNullOrEmpty()
            }
        ) {
            relatedPageResult = Result.success(DataPreferences.cachedQuickPicks)
        }

        // Hàm nội bộ để xử lý khi có bài hát mới
        suspend fun handleSong(song: Song?) {
            // Nếu kết quả chưa có hoặc bài hát khác trước thì gọi API lấy dữ liệu liên quan
            if (relatedPageResult == null || trending?.id != song?.id) {
                relatedPageResult = Innertube.relatedPage(
                    body = NextBody(videoId = (song?.id ?: "J7p4bzqLvCw")) // ID mặc định nếu null
                )
            }
            trending = song // Ghi lại bài hát hiện tại
        }

        // Tùy theo lựa chọn nguồn dữ liệu, gọi flow tương ứng
        when (DataPreferences.quickPicksSource) {
            // Nếu là "Trending" thì lắng nghe dữ liệu trending từ database
            DataPreferences.QuickPicksSource.Trending ->
                Database.trending()
                    .distinctUntilChanged() // Tránh xử lý lại nếu không có thay đổi
                    .collect { handleSong(it.firstOrNull()) }

            // Nếu là "LastInteraction" thì lắng nghe dữ liệu tương tác gần nhất
            DataPreferences.QuickPicksSource.LastInteraction ->
                Database.events()
                    .distinctUntilChanged()
                    .collect { handleSong(it.firstOrNull()?.song) }
        }
    }

    // Scroll state cho giao diện dạng cuộn
    val scrollState = rememberScrollState()

    // Scroll state cho lưới hiển thị quick picks
    val quickPicksLazyGridState = rememberLazyGridState()

    // Padding chỉ tính phần "end" (bên phải hoặc trái, tuỳ RTL/LTR)
    val endPaddingValues = windowInsets.only(WindowInsetsSides.End).asPaddingValues()

    // Modifier dùng cho các tiêu đề section, có padding đồng đều và padding bên phải
    val sectionTextModifier = Modifier
        .padding(horizontal = 16.dp)
        .padding(top = 24.dp, bottom = 8.dp)
        .padding(endPaddingValues)

    // Lấy bài hát hiện tại đang phát và trạng thái có đang phát hay không
    val (currentMediaId, playing) = playingSong(binder)

    // Layout dạng Box với thông tin kích thước tối đa
    BoxWithConstraints {
        // Tính toán chiều rộng item trong LazyGrid tuỳ theo landscape hay portrait
        val quickPicksLazyGridItemWidthFactor =
            if (isLandscape && maxWidth * 0.475f >= 320.dp) 0.475f else 0.75f

        // Provider giúp "snap" (cuộn mượt) tới item gần nhất ở giữa màn hình
        val snapLayoutInfoProvider = rememberSnapLayoutInfo(
            lazyGridState = quickPicksLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize * quickPicksLazyGridItemWidthFactor / 2f - itemSize / 2f)
            }
        )

        // Chiều rộng từng item theo phần trăm tính toán ở trên
        val itemInHorizontalGridWidth = maxWidth * quickPicksLazyGridItemWidthFactor

        // Cột dọc chứa toàn bộ giao diện Quick Picks
        Column(
            modifier = Modifier
                .background(colorPalette.background0) // Đặt màu nền theo theme
                .fillMaxSize() // Chiếm toàn bộ chiều cao và chiều rộng
                .verticalScroll(scrollState) // Cho phép cuộn dọc nội dung
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Vertical) // Chỉ lấy phần padding trên/dưới (tránh player chẳng hạn)
                        .asPaddingValues()
                )
        ) {
            // Phần header với tiêu đề "Quick Picks"
            Header(
                title = stringResource(R.string.quick_picks), // Lấy chuỗi từ resource
                modifier = Modifier.padding(endPaddingValues) // Padding bên phải để tránh va chạm layout
            )

            // Nếu dữ liệu liên quan đã được load thành công
            relatedPageResult?.getOrNull()?.let { related ->
                trending?.let {
                    BasicText(
                        text = stringResource(R.string.trending), // Cần thêm string resource này
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )
                }
                // Lưới ngang hiển thị các mục được đề xuất (bài hát, album, v.v.)
                LazyHorizontalGrid(
                    state = quickPicksLazyGridState, // Trạng thái cuộn cho lưới
                    rows = GridCells.Fixed(4), // 4 hàng cố định
                    flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider), // Snap về giữa khi cuộn
                    contentPadding = endPaddingValues, // Padding bên phải
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            (Dimensions.thumbnails.song + Dimensions.items.verticalPadding * 2) * 4
                        ) // Chiều cao phù hợp với 4 hàng thumbnail
                ) {
                    // Nếu có bài hát thịnh hành thì hiển thị ở mục đầu tiên
                    trending?.let { song ->
                        item {
                            SongItem(
                                modifier = Modifier
                                    .combinedClickable(
                                        onLongClick = {
                                            // Hiện menu khi nhấn giữ bài hát
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem,
                                                    onRemoveFromQuickPicks = {
                                                        // Xóa khỏi tương tác gần đây
                                                        query {
                                                            Database.clearEventsFor(song.id)
                                                        }
                                                    }
                                                )
                                            }
                                        },
                                        onClick = {
                                            // Phát bài hát khi click
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio() // Dừng radio hiện tại
                                            binder?.player?.forcePlay(mediaItem) // Phát bài mới
                                            binder?.setupRadio(
                                                NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                            ) // Cài lại radio
                                        }
                                    )
                                    .animateItem(
                                        fadeInSpec = null,
                                        fadeOutSpec = null
                                    ) // Tắt hiệu ứng fade khi hiển thị
                                    .width(itemInHorizontalGridWidth), // Chiều rộng theo tỉ lệ đã tính toán trước
                                song = song, // Truyền dữ liệu bài hát vào
                                thumbnailSize = Dimensions.thumbnails.song, // Kích thước ảnh đại diện
                                trailingContent = {
                                    // Biểu tượng "star" ở góc bài hát đang nổi bật
                                    Image(
                                        painter = painterResource(R.drawable.star),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(colorPalette.accent),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                showDuration = false, // Không hiển thị thời lượng bài hát
                                isPlaying = playing && currentMediaId == song.id // Hiển thị đang phát nếu đúng bài này
                            )
                        }
                    }


                    items(
                        items = related.songs?.dropLast(if (trending == null) 0 else 1)
                            ?: emptyList(),
                        key = Innertube.SongItem::key
                    ) { song ->
                        SongItem(
                            song = song,
                            thumbnailSize = Dimensions.thumbnails.song,
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem
                                            )
                                        }
                                    },
                                    onClick = {
                                        val mediaItem = song.asMediaItem
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(mediaItem)
                                        binder?.setupRadio(
                                            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                        )
                                    }
                                )
                                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                                .width(itemInHorizontalGridWidth),
                            showDuration = false,
                            isPlaying = playing && currentMediaId == song.key
                        )
                    }
                }

                related.albums?.let { albums ->
                    BasicText(
                        text = stringResource(R.string.related_albums),
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = albums,
                            key = Innertube.AlbumItem::key
                        ) { album ->
                            AlbumItem(
                                album = album,
                                thumbnailSize = Dimensions.thumbnails.album,
                                alternative = true,
                                modifier = Modifier.clickable { onAlbumClick(album) }
                            )
                        }
                    }
                }

                related.artists?.let { artists ->
                    BasicText(
                        text = stringResource(R.string.similar_artists),
                        style = typography.m.semiBold,
                        modifier = sectionTextModifier
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = artists,
                            key = Innertube.ArtistItem::key
                        ) { artist ->
                            ArtistItem(
                                artist = artist,
                                thumbnailSize = Dimensions.thumbnails.artist,
                                alternative = true,
                                modifier = Modifier.clickable { onArtistClick(artist) }
                            )
                        }
                    }
                }

                related.playlists?.let { playlists ->
                    BasicText(
                        text = stringResource(R.string.recommended_playlists),
                        style = typography.m.semiBold,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 8.dp)
                    )

                    LazyRow(contentPadding = endPaddingValues) {
                        items(
                            items = playlists,
                            key = Innertube.PlaylistItem::key
                        ) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                thumbnailSize = Dimensions.thumbnails.playlist,
                                alternative = true,
                                modifier = Modifier.clickable { onPlaylistClick(playlist) }
                            )
                        }
                    }
                }

                Unit
            } ?: relatedPageResult?.exceptionOrNull()?.let {
                BasicText(
                    text = stringResource(R.string.error_message),
                    style = typography.s.secondary.center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(all = 16.dp)
                )
            } ?: ShimmerHost {
                repeat(4) {
                    SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        AlbumItemPlaceholder(
                            thumbnailSize = Dimensions.thumbnails.album,
                            alternative = true
                        )
                    }
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        ArtistItemPlaceholder(
                            thumbnailSize = Dimensions.thumbnails.album,
                            alternative = true
                        )
                    }
                }

                TextPlaceholder(modifier = sectionTextModifier)

                Row {
                    repeat(2) {
                        PlaylistItemPlaceholder(
                            thumbnailSize = Dimensions.thumbnails.album,
                            alternative = true
                        )
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            scrollState = scrollState,
            icon = R.drawable.search,
            onClick = onSearchClick
        )
    }
}
