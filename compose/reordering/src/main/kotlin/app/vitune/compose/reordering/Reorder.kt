package app.vitune.compose.reordering

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput

// Modifier mở rộng để thêm khả năng "kéo để reorder"
private fun Modifier.reorder(
    reorderingState: ReorderingState, // trạng thái reorder hiện tại
    index: Int,                        // index của item hiện tại
    detectDragGestures: DetectDragGestures // hàm phát hiện thao tác kéo (drag)
) = this.pointerInput(reorderingState) { // pointerInput để xử lý sự kiện cảm ứng
    with(detectDragGestures) {
        // Gọi hàm detectDragGestures đã truyền vào
        detectDragGestures(
            onDragStart = { reorderingState.onDragStart(index) }, // Khi bắt đầu kéo → set draggingIndex, tính toán offset
            onDrag = reorderingState::onDrag,                     // Khi đang kéo → xử lý logic đổi vị trí
            onDragEnd = reorderingState::onDragEnd,               // Khi kết thúc kéo → animate lại về vị trí
            onDragCancel = reorderingState::onDragEnd             // Nếu bị hủy → cũng kết thúc kéo
        )
    }
}

// Hàm này dùng overload mặc định cho detectDragGestures nếu bạn không cần custom
fun Modifier.reorder(
    reorderingState: ReorderingState,
    index: Int
) = this.reorder(
    reorderingState = reorderingState,
    index = index,
    detectDragGestures = PointerInputScope::detectDragGestures // Sử dụng gesture mặc định của Compose
)

// Đây là 1 functional interface để cho phép bạn tùy biến cách detect gesture
private fun interface DetectDragGestures {
    suspend fun PointerInputScope.detectDragGestures(
        onDragStart: (Offset) -> Unit, // Khi bắt đầu kéo (trả về vị trí con trỏ)
        onDragEnd: () -> Unit,         // Khi kết thúc kéo
        onDragCancel: () -> Unit,      // Khi kéo bị hủy
        onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit // Khi đang kéo
    )
}

