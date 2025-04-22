package app.vitune.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.vitune.android.ui.modifiers.horizontalFadingEdge

/*
Hàm FadingRow bạn chia sẻ là một @Composable tùy biến trong Jetpack Compose, có chức năng tạo một hàng ngang (Row) có hiệu ứng fade (mờ dần)
ở hai bên khi có thể cuộn – thường dùng để hiển thị rõ ràng rằng danh sách có thể scroll được.
Fade là một hiệu ứng trực quan giúp người dùng nhận biết rằng có nhiều nội dung hơn bên ngoài vùng nhìn thấy.
 */
@Composable
inline fun FadingRow(
    modifier: Modifier = Modifier,
    segments: Int = 12,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    val scrollState = rememberScrollState() // Tạo một trạng thái cuộn mới
    val alphaLeft by animateFloatAsState(
        targetValue = if (scrollState.canScrollBackward) 1f else 0f, // true nếu có thể cuộn sang trái.
        label = ""
    )
    val alphaRight by animateFloatAsState(
        targetValue = if (scrollState.canScrollForward) 1f else 0f, // true nếu có thể cuộn sang phải.
        label = ""
    )

    Row(
        modifier = modifier
            .horizontalFadingEdge( // Thêm hiệu ứng fade cho hàng
                left = true,
                middle = segments - 2,
                right = false,
                alpha = alphaLeft
            )
            .horizontalFadingEdge(
                left = false,
                middle = segments - 2,
                right = true,
                alpha = alphaRight
            )
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = verticalAlignment,
        content = content
    )
}
