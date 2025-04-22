package app.vitune.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vitune.core.ui.utils.roundedShape

// đây là một đoạn mã Kotlin định nghĩa một số enum class và các thuộc tính liên quan đến giao diện người dùng trong ứng dụng Vitune. Các enum class này bao gồm ThumbnailRoundness, ColorSource, ColorMode và Darkness. Mỗi enum class có các giá trị khác nhau để xác định kiểu dáng, nguồn màu sắc, chế độ màu sắc và độ tối của giao diện người dùng.
enum class ThumbnailRoundness(val dp: Dp) {
    None(0.dp),
    Light(2.dp),
    Medium(8.dp),
    Heavy(12.dp),
    Heavier(16.dp),
    Heaviest(18.dp);

    val shape get() = dp.roundedShape
}

enum class ColorSource {
    Default,
    Dynamic,
    MaterialYou
}

enum class ColorMode {
    System,
    Light,
    Dark
}

enum class Darkness {
    Normal,
    AMOLED,
    PureBlack
}
