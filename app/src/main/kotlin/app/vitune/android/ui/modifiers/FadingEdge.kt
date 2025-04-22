package app.vitune.android.ui.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

/*
tạo hiệu ứng mờ dần (fade) ở các cạnh (top/bottom hoặc left/right) của một thành phần trong Jetpack Compose.
Hiệu ứng này rất hữu ích khi bạn muốn thể hiện rằng nội dung có thể cuộn tiếp, như trong danh sách scrollable.
 */
private fun Modifier.fadingEdge(
    start: Boolean, //  có làm mờ ở phía đầu không? (top nếu vertical, left nếu horizontal)
    middle: Int, // số lượng màu đen giữa các màu trong suốt
    end: Boolean, // có làm mờ ở phía cuối không? (bottom nếu vertical, right nếu horizontal)
    alpha: Float, // độ mờ của màu trong suốt (0f là hoàn toàn trong suốt, 1f là hoàn toàn không trong suốt)
    isHorizontal: Boolean // xác định xem hiệu ứng là theo chiều ngang hay chiều dọc
) = this
    .graphicsLayer(alpha = 0.99f) // đặt alpha gần 1 để tránh bị mờ
    .drawWithContent {
        drawContent() // cho phép bạn vẽ nội dung gốc trước (drawContent()), sau đó vẽ đè một lớp mờ lên nội dung này.
        val gradient = buildList { // tạo một danh sách màu sắc cho gradient
            val transparentColor = Color(red = 0f, green = 0f, blue = 0f, alpha = 1f - alpha) // Tạo màu đen trong suốt, dùng để mờ dần.

            add(if (start) transparentColor else Color.Black) // Mờ ở đầu (nếu start = true)
            repeat(middle) { add(Color.Black) } // Thêm màu đen vào giữa
            add(if (end) transparentColor else Color.Black) // Mờ ở cuối (nếu end = true)
        }
        // Vẽ một lớp Rect với gradient theo chiều ngang hoặc dọc.
        drawRect( // vẽ một hình chữ nhật với gradient
            brush = if (isHorizontal) Brush.horizontalGradient(gradient) // nếu là chiều ngang thì dùng Brush.horizontalGradient
            else Brush.verticalGradient(gradient), // nếu là chiều dọc thì dùng Brush.verticalGradient
            blendMode = BlendMode.DstIn // giúp áp dụng hiệu ứng mờ như mặt nạ (mask): càng trong suốt, càng mờ.
        )
    }

// Hàm mở rộng này cho phép bạn thêm hiệu ứng mờ dần vào các thành phần trong Jetpack Compose.
fun Modifier.verticalFadingEdge(
    top: Boolean = true,
    middle: Int = 3,
    bottom: Boolean = true,
    alpha: Float = 1f
) = fadingEdge(start = top, middle = middle, end = bottom, alpha = alpha, isHorizontal = false)

// Hàm mở rộng này cho phép bạn thêm hiệu ứng mờ dần vào các thành phần trong Jetpack Compose.
fun Modifier.horizontalFadingEdge(
    left: Boolean = true,
    middle: Int = 3,
    right: Boolean = true,
    alpha: Float = 1f
) = fadingEdge(start = left, middle = middle, end = right, alpha = alpha, isHorizontal = true)
