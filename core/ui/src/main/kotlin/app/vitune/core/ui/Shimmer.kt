package app.vitune.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.valentinilk.shimmer.defaultShimmerTheme

/**
 * Hàm tạo hiệu ứng shimmer (hiệu ứng nhấp nháy như tải dữ liệu).
 * Dùng để làm hiệu ứng tải dữ liệu trên UI.
 */
@Composable
// Hàm này tạo ra một chủ đề shimmer tùy chỉnh với các thông số cụ thể.
fun shimmerTheme() = remember {
    defaultShimmerTheme.copy(
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800, // Hiệu ứng diễn ra trong 800ms
                easing = LinearEasing, // Chuyển động tuyến tính (đồng đều)
                delayMillis = 250 // Độ trễ trước khi bắt đầu hiệu ứng
            ),
            repeatMode = RepeatMode.Restart // Lặp lại từ đầu sau mỗi chu kỳ
        ),
        shaderColors = listOf(
            Color.Unspecified.copy(alpha = 0.25f), // Màu bán trong suốt
            Color.White.copy(alpha = 0.50f), // Màu trắng sáng hơn
            Color.Unspecified.copy(alpha = 0.25f) // Quay lại màu bán trong suốt
        )
    )
}
