package app.vitune.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

// Kích thước cho các thành phần UI trong ứng dụng
object Dimensions {
    object Thumbnails {// kích thước hình ảnh cho các mục khác nhau
        val album = 108.dp
        val artist = 92.dp
        val song = 54.dp
        val playlist = album

        val player = Player

        object Player {
            val song
                @Composable get() = with(LocalConfiguration.current) {
                    minOf(screenHeightDp, screenWidthDp)
                }.dp
        }
    }

    val thumbnails = Thumbnails

    object Items { // Kích thước cho các thành phần UI
        val moodHeight = 64.dp
        val headerHeight = 140.dp
        val collapsedPlayerHeight = 64.dp // Chiều cao của trình phát nhạc khi thu gọn

        val verticalPadding = 8.dp
        val horizontalPadding = 8.dp
        val alternativePadding = 12.dp // Khoảng cách giữa các thành phần UI

        val gap = 4.dp // Khoảng cách giữa các thành phần
    }

    val items = Items

    object NavigationRail {// Thanh điều hướng
        val width = 60.dp
        val widthLandscape = 120.dp
        val iconOffset = 6.dp
    }

    val navigationRail = NavigationRail
}
