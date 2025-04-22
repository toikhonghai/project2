package app.vitune.android.ui.screens.player

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.ui.modifiers.PinchDirection
import app.vitune.android.ui.modifiers.onSwipe
import app.vitune.android.ui.modifiers.pinchToToggle
import app.vitune.android.utils.FullScreenState
import app.vitune.android.utils.forceSeekToNext
import app.vitune.android.utils.forceSeekToPrevious
import app.vitune.android.utils.thumbnail
import app.vitune.android.utils.windowState
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.px
import coil3.compose.AsyncImage

@SuppressLint("UnusedBoxWithConstraintsScope") // Bỏ cảnh báo về phạm vi BoxWithConstraints
@Composable
fun LyricsDialog(
    onDismiss: () -> Unit,               // Callback khi dialog bị đóng
    modifier: Modifier = Modifier        // Modifier tùy chỉnh giao diện
) = Dialog(onDismissRequest = onDismiss) { // Hiển thị một Dialog (toàn màn hình)

    val currentOnDismiss by rememberUpdatedState(onDismiss) // Giữ phiên bản mới nhất của onDismiss

    // Cấu hình để hiển thị toàn màn hình (ẩn thanh trạng thái nếu cần)
    FullScreenState(shown = PlayerPreferences.lyricsShowSystemBars)

    // Lấy bảng màu và hình dạng bo góc từ giao diện hiện tại
    val (colorPalette, _, _, thumbnailShape) = LocalAppearance.current

    // Lấy player hiện tại từ service, nếu không có thì không tiếp tục
    val player = LocalPlayerServiceBinder.current?.player ?: return@Dialog

    // Lấy thông tin cửa sổ phát và lỗi nếu có
    val (window, error) = windowState()

    // Nếu không có window hoặc có lỗi thì tự động đóng dialog
    LaunchedEffect(window, error) {
        if (window == null || error != null) currentOnDismiss()
    }

    // Nếu window vẫn null thì dừng luôn tại đây
    window ?: return@Dialog

    // AnimatedContent hiển thị hiệu ứng chuyển giữa các bài hát
    AnimatedContent(
        targetState = window, // Mỗi lần thay đổi bài hát sẽ trigger lại composable bên trong
        transitionSpec = {
            // Nếu bài hát giống nhau thì không cần hiệu ứng gì
            if (initialState.mediaItem.mediaId == targetState.mediaItem.mediaId)
                return@AnimatedContent ContentTransform(
                    targetContentEnter = EnterTransition.None,
                    initialContentExit = ExitTransition.None
                )

            // Xác định hướng chuyển tiếp: trượt trái nếu bài sau, phải nếu bài trước
            val direction = if (targetState.firstPeriodIndex > initialState.firstPeriodIndex)
                AnimatedContentTransitionScope.SlideDirection.Left
            else AnimatedContentTransitionScope.SlideDirection.Right

            // Áp dụng hiệu ứng trượt khi chuyển bài hát
            ContentTransform(
                targetContentEnter = slideIntoContainer(
                    towards = direction,
                    animationSpec = tween(500) // thời gian 500ms
                ),
                initialContentExit = slideOutOfContainer(
                    towards = direction,
                    animationSpec = tween(500)
                ),
                sizeTransform = null
            )
        },
        label = "" // Nhãn mô tả, có thể dùng cho debugging/tracing
    ) { currentWindow -> // Bắt đầu nội dung của AnimatedContent với trạng thái cửa sổ hiện tại
        BoxWithConstraints(
            modifier = modifier
                .padding(all = 36.dp) // Padding toàn bộ để tạo khoảng cách với viền dialog
                .padding(vertical = 32.dp) // Padding thêm theo chiều dọc
                .clip(thumbnailShape) // Bo góc theo hình dạng thumbnail (ví dụ: bo tròn)
                .fillMaxSize() // Chiếm toàn bộ kích thước còn lại
                .background(colorPalette.background1) // Nền của box với màu giao diện
                .pinchToToggle( // Cho phép dùng cử chỉ pinch để đóng dialog
                    direction = PinchDirection.In, // Chỉ kích hoạt khi pinch vào trong
                    threshold = 0.9f, // Ngưỡng độ co để nhận pinch
                    onPinch = { onDismiss() } // Khi pinch đủ sâu, gọi hàm đóng dialog
                )
                .onSwipe( // Cho phép vuốt để điều khiển trình phát
                    onSwipeLeft = {
                        player.forceSeekToNext() // Vuốt trái để phát bài tiếp theo
                    },
                    onSwipeRight = {
                        player.seekToDefaultPosition() // Trở về đầu bài hiện tại
                        player.forceSeekToPrevious() // Phát bài trước
                    }
                )
        ) {
            // Nếu bài hát có ảnh bìa (artwork), hiển thị nó làm nền mờ
            if (currentWindow.mediaItem.mediaMetadata.artworkUri != null) AsyncImage(
                model = currentWindow.mediaItem.mediaMetadata.artworkUri.thumbnail((maxHeight - 64.dp).px), // Lấy ảnh thu nhỏ
                contentDescription = null, // Không cần mô tả nội dung
                contentScale = ContentScale.Crop, // Cắt ảnh để lấp đầy khung
                modifier = Modifier
                    .fillMaxSize() // Ảnh nền bao phủ toàn bộ
                    .background(colorPalette.background0) // Nền phía sau ảnh nếu cần
                    .blur(radius = 8.dp) // Làm mờ ảnh nền để làm nổi bật phần lời
            )

            // Thành phần hiển thị lời bài hát
            Lyrics(
                mediaId = currentWindow.mediaItem.mediaId, // ID của bài hát
                isDisplayed = true, // Đánh dấu rằng lyrics đang được hiển thị
                onDismiss = { }, // Không làm gì khi đóng trong component này
                mediaMetadataProvider = currentWindow.mediaItem::mediaMetadata, // Hàm cung cấp metadata bài hát
                durationProvider = player::getDuration, // Hàm cung cấp thời lượng bài hát
                ensureSongInserted = { Database.insert(currentWindow.mediaItem) }, // Đảm bảo bài hát được thêm vào database
                onMenuLaunch = onDismiss, // Khi mở menu, cũng sẽ đóng dialog (nếu cần)
                modifier = Modifier.height(maxHeight), // Chiều cao = chiều cao của BoxWithConstraints
                shouldKeepScreenAwake = false, // Không giữ màn hình luôn sáng (mặc định)
                shouldUpdateLyrics = false // Không tự động cập nhật lyrics khi đang hiển thị
            )
        }
    }
}
