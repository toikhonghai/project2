package app.vitune.compose.reordering

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

// được dùng để thêm hiệu ứng chuyển động khi vị trí item trong danh sách thay đổi – đặc biệt là trong thao tác kéo và thả (drag & drop)
context(LazyItemScope) // được sử dụng để chỉ định rằng animateItemPlacement chỉ có thể được gọi trong một LazyItemScope
fun Modifier.animateItemPlacement(reorderingState: ReorderingState) = this.composed { // composed là một API cho phép tạo một modifier có logic động trong Compose.
    // dùng nó để tính toán modifier dựa trên điều kiện hoặc trạng thái, ví dụ như đang kéo (draggingIndex != -1) hay không
    if (reorderingState.draggingIndex == -1) this.animateItem( // nếu không có thao tác kéo thì không cần hiệu ứng chuyển động
        fadeInSpec = null, // không cần hiệu ứng fade in
        fadeOutSpec = null // không cần hiệu ứng fade out
    ) else this // ngược lại, nếu có thao tác kéo thì không cần hiệu ứng chuyển động
}
