package app.vitune.core.ui

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Sử dụng API Material 3 chưa ổn định
@OptIn(ExperimentalMaterial3Api::class)
@Composable
// Hàm này tạo ra một cấu hình ripple (hiệu ứng gợn nước) dựa trên màu sắc và chế độ tối/sáng của giao diện
fun rippleConfiguration(appearance: Appearance = LocalAppearance.current) = remember(
    // Ghi nhớ trạng thái dựa trên màu chữ và chế độ tối/sáng của giao diện
    appearance.colorPalette.text,
    appearance.colorPalette.isDark
) {
    val (colorPalette) = appearance // Trích xuất bảng màu từ Appearance

    // Cấu hình hiệu ứng ripple (hiệu ứng gợn nước khi bấm vào UI)
    RippleConfiguration(
        // Màu ripple sẽ là trắng nếu ở chế độ tối và màu chữ có độ sáng thấp
        color = if (colorPalette.isDark && colorPalette.text.luminance() < 0.5) Color.White
        else colorPalette.text,

        // Định nghĩa độ trong suốt của hiệu ứng ripple dựa vào chế độ màu
        rippleAlpha = when {
            colorPalette.isDark -> DarkThemeRippleAlpha // Ripple mờ hơn trong chế độ tối
            colorPalette.text.luminance() > 0.5f -> LightThemeHighContrastRippleAlpha // Độ tương phản cao trong chế độ sáng
            else -> LightThemeLowContrastRippleAlpha // Độ tương phản thấp trong chế độ sáng
        }
    )
}

// Định nghĩa các mức độ trong suốt của hiệu ứng ripple cho giao diện sáng (độ tương phản cao)
private val LightThemeHighContrastRippleAlpha = RippleAlpha(
    pressedAlpha = 0.24f, // Độ trong suốt khi nhấn
    focusedAlpha = 0.24f, // Độ trong suốt khi lấy nét
    draggedAlpha = 0.16f, // Độ trong suốt khi kéo
    hoveredAlpha = 0.08f // Độ trong suốt khi di chuột (trên thiết bị hỗ trợ chuột)
)

// Định nghĩa các mức độ trong suốt của hiệu ứng ripple cho giao diện sáng (độ tương phản thấp)
private val LightThemeLowContrastRippleAlpha = RippleAlpha(
    pressedAlpha = 0.12f,
    focusedAlpha = 0.12f,
    draggedAlpha = 0.08f,
    hoveredAlpha = 0.04f
)

// Định nghĩa mức độ trong suốt của hiệu ứng ripple cho giao diện tối
private val DarkThemeRippleAlpha = RippleAlpha(
    pressedAlpha = 0.10f,
    focusedAlpha = 0.12f,
    draggedAlpha = 0.08f,
    hoveredAlpha = 0.04f
)
