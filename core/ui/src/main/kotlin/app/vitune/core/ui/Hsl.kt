package app.vitune.core.ui

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

// Suppress cảnh báo "NOTHING_TO_INLINE" vì chúng ta sử dụng `inline` để tối ưu hiệu suất
@Suppress("NOTHING_TO_INLINE")

// Sử dụng `@JvmInline` để tối ưu bộ nhớ, biến `Hsl` thành một giá trị kiểu `value class`
@JvmInline
// Định nghĩa lớp `Hsl` để đại diện cho màu sắc trong không gian HSL (Hue, Saturation, Lightness)
value class Hsl(@PublishedApi internal val raw: FloatArray) {

    // Định nghĩa một Saver để lưu trữ và khôi phục đối tượng Hsl khi cần thiết
    object Saver : androidx.compose.runtime.saveable.Saver<Hsl, FloatArray> {
        // Khôi phục một đối tượng Hsl từ mảng FloatArray
        override fun restore(value: FloatArray) = value.hsl

        // Lưu trữ đối tượng Hsl thành một FloatArray
        override fun SaverScope.save(value: Hsl) = value.raw
    }

    // Khởi tạo: Kiểm tra xem mảng `raw` có đúng 3 phần tử không (H, S, L)
    init {
        assert(raw.size == 3) { "Invalid Hsl value! Expected size: 3, actual size: ${raw.size}" }
    }

    // Các thuộc tính inline giúp truy xuất nhanh các giá trị Hue, Saturation, Lightness
    inline val hue get() = raw[0] // Hue (màu sắc)
    inline val saturation get() = raw[1] // Độ bão hòa màu
    inline val lightness get() = raw[2] // Độ sáng màu

    // Chuyển đổi HSL thành Color
    inline val color
        get() = Color.hsl(
            hue = hue,
            saturation = saturation,
            lightness = lightness
        )

    // Hỗ trợ tính năng destructuring, cho phép sử dụng như:
    // val (h, s, l) = someHslObject
    inline operator fun component1() = hue
    inline operator fun component2() = saturation
    inline operator fun component3() = lightness
}

// Hàm mở rộng giúp chuyển đổi một FloatArray thành một đối tượng Hsl
val FloatArray.hsl get() = Hsl(raw = this)

// Hàm mở rộng giúp chuyển đổi một đối tượng Color thành HSL
val Color.hsl
    get() = FloatArray(3) // Tạo một mảng 3 phần tử (H, S, L)
        .apply { ColorUtils.colorToHSL(this@Color.toArgb(), this) } // Chuyển đổi từ ARGB sang HSL
        .hsl // Trả về một đối tượng Hsl

