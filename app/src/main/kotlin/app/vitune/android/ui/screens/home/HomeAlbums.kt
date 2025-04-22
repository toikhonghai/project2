package app.vitune.android.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.models.Album
import app.vitune.android.preferences.OrderPreferences
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.items.AlbumItem
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.persist
import app.vitune.core.data.enums.AlbumSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance

@Route // có thể dùng cho custom navigation
@Composable
fun HomeAlbums(
    onAlbumClick: (Album) -> Unit,   // Hàm callback khi click vào một album
    onSearchClick: () -> Unit        // Hàm callback khi nhấn nút tìm kiếm
) = with(OrderPreferences) { // Lấy các giá trị sắp xếp từ OrderPreferences
    val (colorPalette) = LocalAppearance.current  // Lấy bảng màu từ theme hiện tại

    var items by persist<List<Album>>(tag = "home/albums", emptyList()) // Lưu state danh sách album

    // Tự động gọi lại khi thay đổi sortBy hoặc sortOrder
    LaunchedEffect(albumSortBy, albumSortOrder) {
        Database.albums(albumSortBy, albumSortOrder).collect { items = it }
    }

    // Tạo hiệu ứng xoay icon mũi tên khi thay đổi albumSortOrder
    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (albumSortOrder == SortOrder.Ascending) 0f else 180f, // Xoay 0 độ nếu tăng dần, 180 độ nếu giảm dần
        animationSpec = tween(               // Thời gian và cách chạy animation
            durationMillis = 400,           // Thời gian chạy animation: 400 mili giây
            easing = LinearEasing           // Animation tuyến tính (mượt đều)
        ),
        label = ""                          // Nhãn cho debug (để trống vì không cần)
    )

    val lazyListState = rememberLazyListState() // Lưu trạng thái cuộn danh sách

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(), // Padding theo hệ thống
            modifier = Modifier
                .background(colorPalette.background0) // Màu nền danh sách
                .fillMaxSize()
        ) {
            // Phần tiêu đề và các nút sắp xếp
            item(
                key = "header",
                contentType = 0
            ) {
                Header(title = stringResource(R.string.albums)) {
                    // Nút sắp xếp theo năm
                    HeaderIconButton(
                        icon = R.drawable.calendar,
                        enabled = albumSortBy == AlbumSortBy.Year,
                        onClick = { albumSortBy = AlbumSortBy.Year }
                    )

                    // Nút sắp xếp theo tên
                    HeaderIconButton(
                        icon = R.drawable.text,
                        enabled = albumSortBy == AlbumSortBy.Title,
                        onClick = { albumSortBy = AlbumSortBy.Title }
                    )

                    // Nút sắp xếp theo ngày thêm
                    HeaderIconButton(
                        icon = R.drawable.time,
                        enabled = albumSortBy == AlbumSortBy.DateAdded,
                        onClick = { albumSortBy = AlbumSortBy.DateAdded }
                    )

                    Spacer(modifier = Modifier.width(2.dp)) // Khoảng cách nhỏ

                    // Nút để đảo hướng sắp xếp (tăng/giảm)
                    HeaderIconButton(
                        icon = R.drawable.arrow_up,
                        color = colorPalette.text,
                        onClick = { albumSortOrder = !albumSortOrder },
                        modifier = Modifier.graphicsLayer { rotationZ = sortOrderIconRotation } // Xoay icon theo hướng sắp xếp
                    )
                }
            }

            // Danh sách các album
            items(
                items = items,
                key = Album::id
            ) { album ->
                AlbumItem(
                    album = album,
                    thumbnailSize = Dimensions.thumbnails.album,
                    modifier = Modifier
                        .clickable(onClick = { onAlbumClick(album) }) // Xử lý khi nhấn vào album
                        .animateItem() // Hiệu ứng thêm/xoá item
                )
            }
        }

        // Nút FAB tìm kiếm + scroll to top nếu cần
        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            icon = R.drawable.search,
            onClick = onSearchClick
        )
    }
}
