package app.vitune.android.ui.modifiers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlin.math.abs

@JvmInline
// hàm này tạo ra một lớp PinchDirection để xác định hướng của cử chỉ pinch (phóng to hoặc thu nhỏ)
value class PinchDirection private constructor(private val out: Boolean) {
    companion object {
        // Phóng to (Zoom out: hai ngón tay kéo xa nhau)
        val Out = PinchDirection(out = true)

        // Thu nhỏ (Zoom in: hai ngón tay kéo gần lại)
        val In = PinchDirection(out = false)
    }

    // Kiểm tra xem pinch đã vượt qua ngưỡng chưa
    fun reachedThreshold(
        value: Float,     // scale hiện tại
        threshold: Float  // ngưỡng kích hoạt
    ) = when (this) {
        Out -> value >= threshold // nếu pinch ra, cần scale >= ngưỡng
        In -> value <= threshold  // nếu pinch vào, cần scale <= ngưỡng
        else -> error("Unreachable") // sẽ không xảy ra, dùng để đảm bảo an toàn
    }
}

// hàm này thêm một cử chỉ pinch vào Modifier
fun Modifier.pinchToToggle(
    direction: PinchDirection,        // hướng pinch cần kích hoạt (In hoặc Out)
    threshold: Float,                 // ngưỡng scale cần đạt để gọi callback
    key: Any? = Unit,                 // key để nhớ gesture (trong remember)
    onPinch: (scale: Float) -> Unit   // callback khi pinch đủ mạnh
) = this.pointerInput(key) {
    coroutineScope {
        awaitEachGesture {
            // touchSlop giúp bỏ qua các cử chỉ nhỏ, chỉ nhận khi thật sự có gesture mạnh
            val touchSlop = viewConfiguration.touchSlop / 2

            var scale = 1f              // scale ban đầu
            var touchSlopReached = false

            awaitFirstDown(requireUnconsumed = false) // đợi một ngón tay chạm xuống

            @Suppress("LoopWithTooManyJumpStatements")
            while (isActive) {
                val event = awaitPointerEvent()

                // Nếu bất kỳ pointer nào bị consume thì hủy gesture
                if (event.changes.fastAny { it.isConsumed }) break

                // Nếu không còn ngón nào đang nhấn thì tiếp tục đợi
                if (!event.changes.fastAny { it.pressed }) continue

                // Tính scale của lần zoom mới, nhân vào tổng scale
                scale *= event.calculateZoom()

                // Nếu chưa đạt touchSlop, kiểm tra xem khoảng cách pinch có vượt chưa
                if (!touchSlopReached) {
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    if (abs(1 - scale) * centroidSize > touchSlop) touchSlopReached = true
                }

                // Nếu đã đạt touchSlop thì consume các thay đổi vị trí
                if (touchSlopReached) event.changes.fastForEach {
                    if (it.positionChanged()) it.consume()
                }

                // Nếu scale đã vượt qua ngưỡng mong muốn theo đúng hướng thì gọi callback
                if (
                    direction.reachedThreshold(
                        value = scale,
                        threshold = threshold
                    )
                ) {
                    onPinch(scale)
                    break
                }
            }
        }
    }
}

