package app.vitune.core.ui

import android.app.Activity
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import app.vitune.core.ui.utils.isAtLeastAndroid6
import app.vitune.core.ui.utils.isAtLeastAndroid8
import app.vitune.core.ui.utils.isCompositionLaunched
import app.vitune.core.ui.utils.roundedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

// Đánh dấu class Appearance là Parcelable để có thể lưu trữ và truyền qua Bundle dễ dàng.
@Parcelize
@Immutable
// lớp này được sử dụng để định nghĩa giao diện của ứng dụng, bao gồm các thuộc tính như bảng màu, phông chữ và hình dạng của hình thu nhỏ.
data class Appearance(
    val colorPalette: ColorPalette, // Bảng màu của giao diện
    val typography: Typography, // Phông chữ của giao diện
    val thumbnailShapeCorners: ParcelableDp // Độ bo tròn của hình thu nhỏ
) : Parcelable {
    @IgnoredOnParcel
    val thumbnailShape = thumbnailShapeCorners.roundedShape // Tạo hình dạng từ góc bo tròn
    operator fun component4() = thumbnailShape // Hỗ trợ destructuring để truy cập dễ dàng
}

// Biến CompositionLocal cung cấp thông tin về Appearance trong toàn bộ ứng dụng
val LocalAppearance = staticCompositionLocalOf<Appearance> {
    error("No appearance provided") // Báo lỗi nếu Appearance chưa được cung cấp
}

// Ghi nhớ giao diện Appearance theo trạng thái hiện tại
@Composable
inline fun rememberAppearance(
    vararg keys: Any = arrayOf(Unit), // Các khóa thay đổi sẽ kích hoạt recompose
    isDark: Boolean = isSystemInDarkTheme(), // Xác định chế độ tối
    crossinline provide: (isSystemInDarkTheme: Boolean) -> Appearance // Hàm cung cấp Appearance
) = rememberSaveable(keys, isCompositionLaunched(), isDark) {
    mutableStateOf(provide(isDark)) // Lưu trạng thái của Appearance
}

// Hàm tạo một Appearance dựa trên các thông số đầu vào
@Composable
fun appearance(
    source: ColorSource, // Nguồn màu (mặc định, động, Material You)
    mode: ColorMode, // Chế độ màu (Sáng, Tối, Theo hệ thống)
    darkness: Darkness, // Độ tối của chủ đề
    materialAccentColor: Color?, // Màu nhấn Material You
    sampleBitmap: Bitmap?, // Ảnh dùng để lấy màu chủ đạo (nếu có)
    fontFamily: BuiltInFontFamily, // Phông chữ sử dụng
    applyFontPadding: Boolean, // Có áp dụng padding cho font không
    thumbnailRoundness: Dp, // Độ bo tròn của hình thu nhỏ
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme() // Có đang ở chế độ tối theo hệ thống không
): Appearance {
    // Xác định chế độ tối dựa trên hệ thống hoặc chế độ được chỉ định
    val isDark = remember(mode, isSystemInDarkTheme) {
        mode == ColorMode.Dark || (mode == ColorMode.System && isSystemInDarkTheme)
    }

    // Tạo bảng màu dựa trên thông tin đã chọn
    val colorPalette = rememberSaveable(
        source,
        darkness,
        isDark,
        materialAccentColor,
        sampleBitmap
    ) {
        colorPaletteOf(
            source = source,
            darkness = darkness,
            isDark = isDark,
            materialAccentColor = materialAccentColor,
            sampleBitmap = sampleBitmap
        )
    }

    // Trả về một Appearance với các thuộc tính được thiết lập
    return rememberAppearance(
        colorPalette,
        fontFamily,
        applyFontPadding,
        thumbnailRoundness,
        isDark = isDark
    ) {
        Appearance(
            colorPalette = colorPalette,
            typography = typographyOf(
                color = colorPalette.text, // Màu chữ theo bảng màu
                fontFamily = fontFamily, // Font chữ được chọn
                applyFontPadding = applyFontPadding // Áp dụng padding cho font
            ),
            thumbnailShapeCorners = thumbnailRoundness // Góc bo tròn của hình thu nhỏ
        )
    }.value
}

// Thiết lập giao diện thanh trạng thái và thanh điều hướng theo chế độ sáng/tối
fun Activity.setSystemBarAppearance(isDark: Boolean) {
    with(WindowCompat.getInsetsController(window, window.decorView.rootView)) {
        isAppearanceLightStatusBars = !isDark // Đặt màu cho thanh trạng thái
        isAppearanceLightNavigationBars = !isDark // Đặt màu cho thanh điều hướng
    }

    // Xác định màu cho thanh trạng thái và thanh điều hướng dựa vào chế độ sáng/tối
    val color = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()

    // TODO: Android yêu cầu nền đằng sau thanh hệ thống
    @Suppress("DEPRECATION")
    if (!isAtLeastAndroid6) window.statusBarColor = color // Đặt màu thanh trạng thái cho Android < 6
    @Suppress("DEPRECATION")
    if (!isAtLeastAndroid8) window.navigationBarColor = color // Đặt màu thanh điều hướng cho Android < 8
}

// Composable để thiết lập giao diện thanh hệ thống khi bảng màu thay đổi
@Composable
fun Activity.SystemBarAppearance(palette: ColorPalette) = LaunchedEffect(palette) {
    withContext(Dispatchers.Main) {
        setSystemBarAppearance(palette.isDark) // Cập nhật thanh hệ thống theo chế độ sáng/tối
    }
}
