@file:Suppress("NOTHING_TO_INLINE")

package app.vitune.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

@JvmInline // Dùng Inline class để tối ưu bộ nhớ (Java không thấy đây là một class riêng biệt).
value class Px(val value: Int) {

    // Chuyển đổi từ Px sang Dp trong Composable bằng LocalDensity
    inline val dp @Composable get() = dp(LocalDensity.current)

    // Chuyển đổi từ Px sang Dp với một Density cụ thể
    inline fun dp(density: Density) = with(density) { value.toDp() }
}

inline val Int.px inline get() = Px(value = this)
inline val Float.px inline get() = roundToInt().px

inline val Dp.px: Int
    @Composable
    inline get() = with(LocalDensity.current) { roundToPx() }

inline val TextUnit.dp
    @Composable
    inline get() = dp(LocalDensity.current)

inline fun TextUnit.dp(density: Density) = with(density) { toDp() }
