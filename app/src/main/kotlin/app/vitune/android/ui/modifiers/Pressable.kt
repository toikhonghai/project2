package app.vitune.android.ui.modifiers

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

// tạo ra một modifier cho phép xử lý các tương tác liên quan đến việc nhấn và thả
fun Modifier.pressable(
    onPress: () -> Unit = {}, // khi người dùng bắt đầu nhấn xuống (chưa nhả).
    onCancel: () -> Unit = {}, // khi người dùng hủy nhấn (rời tay ra trước khi kích hoạt hành động).
    onRelease: () -> Unit = {}, //  khi người dùng nhấn xong (click hoàn chỉnh).
    indication: Indication? = null // để chỉ định hiệu ứng hình ảnh khi nhấn (như ripple effect).
) = this.composed { // composed là một cách để định nghĩa Modifier tùy chỉnh có thể sử dụng các hàm Compose như remember, LaunchedEffect, v.v.
    val interactionSource = remember { MutableInteractionSource() } // là một nguồn để theo dõi trạng thái tương tác (ví dụ như pressed, focused, hovered).
    val isPressed by interactionSource.collectIsPressedAsState() //  là một biến trạng thái (state) phản ánh việc nút đang được nhấn hay không.

    LaunchedEffect(isPressed) {
        if (isPressed) onPress() else onCancel()
    }

    this.clickable(
        interactionSource = interactionSource, //  để theo dõi trạng thái tương tác của nút.
        indication = indication, // để chỉ định hiệu ứng hình ảnh khi nhấn (như ripple effect).
        onClick = onRelease //  để xử lý sự kiện nhấn nút (click event).
    )
}
