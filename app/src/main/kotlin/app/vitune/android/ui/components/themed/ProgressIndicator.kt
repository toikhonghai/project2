package app.vitune.android.ui.components.themed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import app.vitune.core.ui.LocalAppearance

@Composable
// hàm này tạo ra một thanh tiến trình hình tròn
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    animateProgress: Boolean = progress != null, // biến này xác định xem có nên hoạt ảnh cho thanh tiến trình hay không
    strokeCap: StrokeCap? = null // để chỉ định kiểu của đầu thanh tiến trình (tròn hay vuông)
) {
    // Lấy màu sắc từ theme hiện tại (có thể là theme light/dark)
    val (colorPalette) = LocalAppearance.current

    // Nếu không có giá trị tiến trình (progress == null), tức là kiểu "vô định" (indeterminate)
    if (progress == null) {
        androidx.compose.material3.CircularProgressIndicator( // kiểu đầu thanh tiến trình mặc định là tròn
            modifier = modifier, // Tùy chỉnh layout
            color = colorPalette.accent, // Màu sắc chính của thanh tiến trình
            strokeCap = strokeCap ?: ProgressIndicatorDefaults.CircularIndeterminateStrokeCap
            // Nếu không truyền strokeCap thì dùng kiểu đầu tròn mặc định
        )
    } else {
        // Nếu có progress cụ thể, animate nếu cần
        val animatedProgress by (
                if (animateProgress)
                    animateFloatAsState(targetValue = progress) // Animate khi giá trị progress thay đổi
                else
                    remember { derivedStateOf { progress } } // Nếu không animate thì giữ nguyên giá trị tĩnh
                )

        // Vẽ thanh tiến trình dạng xác định (determinate)
        androidx.compose.material3.CircularProgressIndicator(
            modifier = modifier,
            color = colorPalette.accent,
            strokeCap = strokeCap ?: ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
            progress = { animatedProgress } // Truyền giá trị tiến trình đã (hoặc không) animate
        )
    }
}

@Composable
// hàm này tạo ra một thanh tiến trình hình chữ nhật
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    animateProgress: Boolean = progress != null,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap
) {
    val (colorPalette) = LocalAppearance.current

    if (progress == null) androidx.compose.material3.LinearProgressIndicator(
        modifier = modifier,
        color = colorPalette.accent,
        trackColor = colorPalette.background1,
        strokeCap = strokeCap
    ) else {
        val animatedProgress by (
                if (animateProgress) animateFloatAsState(targetValue = progress)
                else remember { derivedStateOf { progress } }
                )

        androidx.compose.material3.LinearProgressIndicator(
            modifier = modifier,
            color = colorPalette.accent,
            trackColor = colorPalette.background1,
            strokeCap = strokeCap,
            progress = { animatedProgress }
        )
    }
}
