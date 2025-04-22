package app.vitune.core.ui

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

// Định nghĩa alias để sử dụng @WriteWith, giúp hỗ trợ Parcelable cho Color và Dp.
typealias ParcelableColor = @WriteWith<ColorParceler> Color
typealias ParcelableDp = @WriteWith<DpParceler> Dp

// Sử dụng @Parcelize để tự động sinh code Parcelable cho class này.
// @Immutable đảm bảo class này không thể bị thay đổi sau khi được tạo.
@Parcelize
@Immutable
data class ColorPalette(
    val background0: ParcelableColor,  // Màu nền chính
    val background1: ParcelableColor,  // Màu nền phụ
    val background2: ParcelableColor,  // Màu nền phụ thứ hai
    val accent: ParcelableColor,       // Màu nhấn (accent)
    val onAccent: ParcelableColor,     // Màu chữ trên màu nhấn
    val red: ParcelableColor = Color(0xffbf4040),  // Màu đỏ mặc định
    val blue: ParcelableColor = Color(0xff4472cf), // Màu xanh mặc định
    val yellow: ParcelableColor = Color(0xfffff176), // Màu vàng mặc định
    val text: ParcelableColor,         // Màu văn bản chính
    val textSecondary: ParcelableColor, // Màu văn bản phụ
    val textDisabled: ParcelableColor, // Màu văn bản bị vô hiệu hóa
    val isDefault: Boolean,             // Xác định đây có phải là bảng màu mặc định không
    val isDark: Boolean                 // Xác định đây có phải là bảng màu tối hay không
) : Parcelable

// Màu accent mặc định, sử dụng mô hình màu HSL (Hue, Saturation, Lightness).
private val defaultAccentColor = Color(0xff3e44ce).hsl

// Định nghĩa bảng màu mặc định cho chế độ sáng
val defaultLightPalette = ColorPalette(
    background0 = Color(0xfffdfdfe), // Màu nền sáng
    background1 = Color(0xfff8f8fc), // Màu nền phụ
    background2 = Color(0xffeaeaf5), // Màu nền phụ thứ hai
    text = Color(0xff212121),       // Màu văn bản chính (đen nhạt)
    textSecondary = Color(0xff656566), // Màu văn bản phụ (xám)
    textDisabled = Color(0xff9d9d9d),  // Màu văn bản bị vô hiệu hóa (xám nhạt)
    accent = defaultAccentColor.color, // Màu nhấn
    onAccent = Color.White,            // Màu văn bản trên màu nhấn (trắng)
    isDefault = true,                   // Đây là bảng màu mặc định
    isDark = false                       // Đây là chế độ sáng
)

// Định nghĩa bảng màu mặc định cho chế độ tối
val defaultDarkPalette = ColorPalette(
    background0 = Color(0xff16171d), // Màu nền tối
    background1 = Color(0xff1f2029), // Màu nền phụ
    background2 = Color(0xff2b2d3b), // Màu nền phụ thứ hai
    text = Color(0xffe1e1e2),       // Màu văn bản chính (trắng xám)
    textSecondary = Color(0xffa3a4a6), // Màu văn bản phụ (xám sáng)
    textDisabled = Color(0xff6f6f73),  // Màu văn bản bị vô hiệu hóa (xám đậm)
    accent = defaultAccentColor.color, // Màu nhấn
    onAccent = Color.White,            // Màu văn bản trên màu nhấn (trắng)
    isDefault = true,                   // Đây là bảng màu mặc định
    isDark = true                        // Đây là chế độ tối
)

// Hàm tạo bảng màu sáng từ giá trị HSL của màu nhấn
private fun lightColorPalette(accent: Hsl) = lightColorPalette(
    hue = accent.hue,           // Lấy giá trị Hue từ HSL
    saturation = accent.saturation // Lấy giá trị Saturation từ HSL
)

private fun lightColorPalette(hue: Float, saturation: Float) = ColorPalette(
    background0 = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.1f),
        lightness = 0.925f
    ),
    background1 = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.3f),
        lightness = 0.90f
    ),
    background2 = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.4f),
        lightness = 0.85f
    ),
    text = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.02f),
        lightness = 0.12f
    ),
    textSecondary = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.1f),
        lightness = 0.40f
    ),
    textDisabled = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.2f),
        lightness = 0.65f
    ),
    accent = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.5f),
        lightness = 0.5f
    ),
    onAccent = Color.White,
    isDefault = false,
    isDark = false
)

private fun darkColorPalette(accent: Hsl, darkness: Darkness) = darkColorPalette(
    hue = accent.hue,
    saturation = accent.saturation,
    darkness = darkness
)

private fun darkColorPalette(
    hue: Float,
    saturation: Float,
    darkness: Darkness
) = ColorPalette(
    background0 = if (darkness == Darkness.Normal) Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.1f),
        lightness = 0.10f
    ) else Color.Black,
    background1 = if (darkness == Darkness.Normal) Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.3f),
        lightness = 0.15f
    ) else Color.Black,
    background2 = if (darkness == Darkness.Normal) Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.4f),
        lightness = 0.2f
    ) else Color.Black,
    text = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.02f),
        lightness = 0.88f
    ),
    textSecondary = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.1f),
        lightness = 0.65f
    ),
    textDisabled = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(0.2f),
        lightness = 0.40f
    ),
    accent = Color.hsl(
        hue = hue,
        saturation = saturation.coerceAtMost(if (darkness == Darkness.AMOLED) 0.4f else 0.5f),
        lightness = 0.5f
    ),
    onAccent = Color.White,
    isDefault = false,
    isDark = true
)

fun accentColorOf(
    source: ColorSource,
    isDark: Boolean,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?
) = when (source) {
    // Nếu nguồn là mặc định, sử dụng màu accent mặc định
    ColorSource.Default -> defaultAccentColor

    // Nếu nguồn là Dynamic, lấy màu từ ảnh nếu có, nếu không thì dùng màu mặc định
    ColorSource.Dynamic -> sampleBitmap?.let { dynamicAccentColorOf(it, isDark) } ?: defaultAccentColor

    // Nếu nguồn là Material You, sử dụng màu accent từ Material You nếu có, nếu không thì dùng màu mặc định
    ColorSource.MaterialYou -> materialAccentColor?.hsl ?: defaultAccentColor
}

fun dynamicAccentColorOf(
    bitmap: Bitmap,
    isDark: Boolean
): Hsl? {
    val palette = Palette
        .from(bitmap)
        .maximumColorCount(8) // Giới hạn bảng màu tối đa 8 màu
        // Nếu chế độ tối, loại bỏ các màu có hue trong khoảng 36f đến 100f (vàng - xanh lá)
        .addFilter(if (isDark) ({ _, hsl -> hsl[0] !in 36f..100f }) else null)
        .generate()

    val hsl = if (isDark) {
        // Nếu chế độ tối, ưu tiên lấy màu chủ đạo, nếu không có thì tạo lại
        palette.dominantSwatch ?: Palette
            .from(bitmap)
            .maximumColorCount(8)
            .generate()
            .dominantSwatch
    } else {
        // Nếu chế độ sáng, lấy màu chủ đạo từ bảng màu
        palette.dominantSwatch
    }?.hsl ?: return null // Nếu không tìm thấy màu hợp lệ, trả về null

    val arr = if (hsl[1] < 0.08) // Nếu độ bão hòa < 0.08 (màu quá nhạt)
        palette.swatches
            .map(Palette.Swatch::getHsl) // Lấy danh sách các màu
            .sortedByDescending(FloatArray::component2) // Sắp xếp theo độ bão hòa giảm dần
            .find { it[1] != 0f } // Chọn màu đầu tiên có độ bão hòa khác 0
            ?: hsl // Nếu không có màu nào phù hợp, giữ nguyên màu ban đầu
    else hsl

    return arr.hsl // Trả về màu đã xử lý dưới dạng Hsl
}

fun ColorPalette.amoled() = if (isDark) {
    val (hue, saturation) = accent.hsl // Lấy hue và saturation của màu accent

    copy(
        background0 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.1f), // Giới hạn độ bão hòa tối đa 0.1
            lightness = 0.10f // Đặt độ sáng 10%
        ),
        background1 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.3f), // Giới hạn độ bão hòa tối đa 0.3
            lightness = 0.15f // Đặt độ sáng 15%
        ),
        background2 = Color.hsl(
            hue = hue,
            saturation = saturation.coerceAtMost(0.4f), // Giới hạn độ bão hòa tối đa 0.4
            lightness = 0.2f // Đặt độ sáng 20%
        )
    )
} else this // Nếu không ở chế độ tối, giữ nguyên bảng màu

// Hàm tạo ColorPalette dựa trên nguồn màu, độ tối và các tham số khác
fun colorPaletteOf(
    source: ColorSource, // Nguồn màu
    darkness: Darkness, // Mức độ tối
    isDark: Boolean, // Chế độ tối hay sáng
    materialAccentColor: Color?, // Màu sắc từ Material You (nếu có)
    sampleBitmap: Bitmap? // Bitmap để lấy màu động (nếu có)
): ColorPalette {
    // Xác định màu nhấn (accent color) dựa trên nguồn màu
    val accentColor = accentColorOf(
        source = source,
        isDark = isDark,
        materialAccentColor = materialAccentColor,
        sampleBitmap = sampleBitmap
    )

    // Trả về bảng màu phù hợp với chế độ tối/sáng và kiểm tra nếu đó là màu mặc định
    return (if (isDark) darkColorPalette(accentColor, darkness) else lightColorPalette(accentColor))
        .copy(isDefault = accentColor == defaultAccentColor)
}

// Kiểm tra xem bảng màu có nền đen thuần túy hay không
inline val ColorPalette.isPureBlack get() = background0 == Color.Black

// Màu của thanh tiến trình trong trình phát nhạc thu gọn
inline val ColorPalette.collapsedPlayerProgressBar
    get() = if (isPureBlack) defaultDarkPalette.background0 else background2

// Màu của biểu tượng yêu thích (nếu là mặc định thì màu đỏ, ngược lại là màu accent)
inline val ColorPalette.favoritesIcon get() = if (isDefault) red else accent

// Màu của hiệu ứng shimmer (hiệu ứng lấp lánh khi tải nội dung)
inline val ColorPalette.shimmer get() = if (isDefault) Color(0xff838383) else accent

// Màu nền của các bề mặt UI (tùy thuộc vào chế độ tối/sáng)
inline val ColorPalette.surface get() = if (isPureBlack) Color(0xff272727) else background2

// Màu overlay (màu nền trong suốt để phủ lên nội dung khác)
@Suppress("UnusedReceiverParameter")
inline val ColorPalette.overlay get() = Color.Black.copy(alpha = 0.75f)

// Màu chữ trên overlay
@Suppress("UnusedReceiverParameter")
inline val ColorPalette.onOverlay get() = defaultDarkPalette.text

// Màu shimmer trên overlay
@Suppress("UnusedReceiverParameter")
inline val ColorPalette.onOverlayShimmer get() = defaultDarkPalette.shimmer

// Parceler cho đối tượng Color (dùng để lưu trữ và khôi phục trong Parcelable)
object ColorParceler : Parceler<Color> {
    override fun Color.write(parcel: Parcel, flags: Int) = parcel.writeLong(value.toLong())
    override fun create(parcel: Parcel) = Color(parcel.readLong())
}

// Parceler cho đối tượng Dp (đơn vị đo kích thước trong Compose)
object DpParceler : Parceler<Dp> {
    override fun Dp.write(parcel: Parcel, flags: Int) = parcel.writeFloat(value)
    override fun create(parcel: Parcel) = parcel.readFloat().dp
}
