package app.vitune.android.ui.screens.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import app.vitune.android.models.Artist
import app.vitune.android.preferences.OrderPreferences
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.HeaderIconButton
import app.vitune.android.ui.items.ArtistItem
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.persistList
import app.vitune.core.data.enums.ArtistSortBy
import app.vitune.core.data.enums.SortOrder
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.toImmutableList

@Route
@Composable
fun HomeArtistList(
    onArtistClick: (Artist) -> Unit, // Hàm callback khi nhấn vào 1 artist
    onSearchClick: () -> Unit // Hàm callback khi nhấn icon tìm kiếm
) = with(OrderPreferences) { // Truy cập và sử dụng trạng thái sắp xếp artist từ OrderPreferences
    val (colorPalette) = LocalAppearance.current // Lấy ra theme màu hiện tại

    var items by persistList<Artist>("home/artists") // Lưu danh sách artist với key để giữ trạng thái

    // Mỗi khi thay đổi kiểu sắp xếp hoặc thứ tự, gọi lại database để lấy danh sách mới
    LaunchedEffect(artistSortBy, artistSortOrder) {
        Database
            .artists(artistSortBy, artistSortOrder)
            .collect { items = it.toImmutableList() }
    }

    // Animation xoay icon mũi tên lên xuống khi thay đổi sort order
    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (artistSortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(
            durationMillis = 400,
            easing = LinearEasing
        ),
        label = ""
    )

    val lazyGridState = rememberLazyGridState() // Trạng thái scroll của lưới

    Box {
        LazyVerticalGrid(
            state = lazyGridState,
            // Tự động điều chỉnh số cột dựa trên chiều rộng của thumbnail
            columns = GridCells.Adaptive(
                Dimensions.thumbnails.song * 2 + Dimensions.items.verticalPadding * 2
            ),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues(), // Thêm padding để tránh phần tử giao diện
            horizontalArrangement = Arrangement.Center, // Canh giữa các item
            modifier = Modifier
                .background(colorPalette.background0) // Nền của danh sách
                .fillMaxSize()
        ) {
            // Header: thanh tiêu đề + các nút sắp xếp
            item(
                key = "header",
                contentType = 0,
                span = { GridItemSpan(maxLineSpan) } // Header chiếm full hàng
            ) {
                Header(title = stringResource(R.string.artists)) {
                    HeaderIconButton(
                        icon = R.drawable.text,
                        enabled = artistSortBy == ArtistSortBy.Name,
                        onClick = { artistSortBy = ArtistSortBy.Name } // Sắp xếp theo tên
                    )

                    HeaderIconButton(
                        icon = R.drawable.time,
                        enabled = artistSortBy == ArtistSortBy.DateAdded,
                        onClick = { artistSortBy = ArtistSortBy.DateAdded } // Sắp xếp theo thời gian thêm
                    )

                    Spacer(modifier = Modifier.width(2.dp)) // Khoảng trống giữa nút

                    HeaderIconButton(
                        icon = R.drawable.arrow_up, // Mũi tên xoay lên/xuống
                        color = colorPalette.text,
                        onClick = { artistSortOrder = !artistSortOrder }, // Đảo thứ tự sắp xếp
                        modifier = Modifier.graphicsLayer {
                            rotationZ = sortOrderIconRotation // Gán animation xoay
                        }
                    )
                }
            }

            // Hiển thị từng artist trong danh sách
            items(items = items, key = Artist::id) { artist ->
                ArtistItem(
                    artist = artist,
                    thumbnailSize = Dimensions.thumbnails.song * 2, // Kích thước thumbnail
                    alternative = true, // Giao diện thay thế
                    modifier = Modifier
                        .clickable(onClick = { onArtistClick(artist) }) // Khi nhấn vào artist
                        .animateItem(fadeInSpec = null, fadeOutSpec = null) // Hiệu ứng animation từng item
                )
            }
        }

        // Nút nổi có thể scroll to top và mở tìm kiếm
        FloatingActionsContainerWithScrollToTop(
            lazyGridState = lazyGridState, // Truyền trạng thái cuộn
            icon = R.drawable.search, // Icon nút chính
            onClick = onSearchClick // Khi nhấn tìm kiếm
        )
    }
}

