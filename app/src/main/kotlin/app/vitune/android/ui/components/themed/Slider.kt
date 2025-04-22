package app.vitune.android.ui.components.themed

import androidx.annotation.IntRange
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.surface

@Composable
//  tạo một Slider (thanh trượt) tùy chỉnh
fun Slider(
    state: Float,                           // Giá trị hiện tại của Slider
    setState: (Float) -> Unit,              // Hàm callback để cập nhật giá trị
    onSlideComplete: () -> Unit,            // Gọi khi người dùng buông tay khỏi Slider
    range: ClosedFloatingPointRange<Float>, // Khoảng giá trị hợp lệ
    modifier: Modifier = Modifier,          // Modifier tùy chỉnh UI
    @IntRange(from = 0) steps: Int = 0,     // Số bước chia nhỏ giữa range
    showTicks: Boolean = steps != 0         // Hiển thị các "vạch nhỏ" hay không
) {
    val (colorPalette) = LocalAppearance.current

    androidx.compose.material3.Slider(
        value = state,
        onValueChange = setState,
        onValueChangeFinished = onSlideComplete,
        valueRange = range,
        modifier = modifier,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = colorPalette.onAccent,                          // Màu chấm tròn kéo
            activeTrackColor = colorPalette.accent,                      // Màu phần track đã trượt qua
            inactiveTrackColor = colorPalette.surface.copy(alpha = 0.75f), // Phần track chưa trượt tới
            activeTickColor = if (showTicks) colorPalette.surface else Color.Transparent, // Màu vạch nhỏ đã trượt
            inactiveTickColor = if (showTicks) colorPalette.accent else Color.Transparent // Màu vạch nhỏ chưa trượt tới
        )
    )
}
