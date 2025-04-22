package app.vitune.android.ui.components.themed

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import app.vitune.android.utils.drawCircle
import app.vitune.core.ui.LocalAppearance

@Composable
fun Switch(
    isChecked: Boolean,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current

    val transition = updateTransition(targetState = isChecked, label = null) // Tạo một transition động giữa trạng thái bật/tắt.
    // Transition này sẽ được dùng để animate màu sắc và vị trí khi trạng thái thay đổi.

    // Nếu isChecked = true: màu nền là accent, màu nút là onAccent.
    val backgroundColor by transition.animateColor(label = "") {
        if (it) colorPalette.accent else colorPalette.background1
    }

    val color by transition.animateColor(label = "") {
        if (it) colorPalette.onAccent else colorPalette.textDisabled
    }

    // Di chuyển vị trí nút tròn bên trong switch (trái <-> phải) tuỳ theo trạng thái bật/tắt.
    val offset by transition.animateDp(label = "") {
        if (it) 36.dp else 12.dp
    }

    Canvas(modifier = modifier.size(width = 48.dp, height = 24.dp)) { // Kích thước của switch: chiều rộng 48dp, chiều cao 24dp.
        drawRoundRect( // Vẽ nền bo tròn (góc bo là 12dp), với màu nền được animate ở trên.
            color = backgroundColor,
            cornerRadius = CornerRadius(x = 12.dp.toPx(), y = 12.dp.toPx())
        )

        drawCircle(
            color = color,
            radius = 8.dp.toPx(), // bán kính
            center = size.center.copy(x = offset.toPx()), // size.center Đây là tâm của Canvas hiện tại, Hàm copy() được dùng để thay đổi trục X, giữ nguyên trục Y.
            // Dùng .toPx() để chuyển từ Dp sang pixel vì Canvas hoạt động theo đơn vị pixel.
            shadow = Shadow(
                color = Color.Black.copy(alpha = if (isChecked) 0.4f else 0.1f), // Bật (isChecked = true) → alpha = 0.4f → bóng đậm hơn.
                blurRadius = 8.dp.toPx(), // Bán kính làm mờ bóng đổ.
                offset = Offset(x = -1.dp.toPx(), y = 1.dp.toPx()) // Di chuyển bóng 1dp sang trái và 1dp xuống dưới.
            )
        )
    }
}
