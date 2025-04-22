package app.vitune.compose.reordering

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// Modifier để áp dụng hiệu ứng kéo thả lên 1 item trong LazyColumn / LazyRow
fun Modifier.draggedItem(
    reorderingState: ReorderingState, // trạng thái reorder hiện tại
    index: Int,                        // index của item hiện tại
    draggedElevation: Dp = 4.dp       // độ nổi lên khi đang kéo (shadow)
): Modifier = when (reorderingState.draggingIndex) {

    // Nếu không có item nào đang được kéo → trả lại modifier gốc
    -1 -> this

    // Nếu item hiện tại là item đang bị kéo
    index -> offset {
        // Tính offset (vị trí lệch) theo hướng kéo
        when (reorderingState.lazyListState.layoutInfo.orientation) {
            Orientation.Vertical -> IntOffset(0, reorderingState.offset.value)
            Orientation.Horizontal -> IntOffset(reorderingState.offset.value, 0)
        }
    }
        // Z-index cao để nó nằm trên các item khác
        .zIndex(1f)

    // Các item còn lại (không phải item kéo cũng không phải idle)
    else -> offset {
        // Tính toán độ lệch (offset) nếu có hiệu ứng chuyển động
        val offset = when (index) {
            // Nếu item đang nằm trong danh sách có hiệu ứng animate
            in reorderingState.indexesToAnimate ->
                reorderingState.indexesToAnimate.getValue(index).value

            // Nếu item nằm trong đoạn bị đẩy xuống (kéo từ trên xuống)
            in (reorderingState.draggingIndex + 1)..reorderingState.reachedIndex ->
                -reorderingState.draggingItemSize

            // Nếu item bị đẩy lên (kéo từ dưới lên)
            in reorderingState.reachedIndex..<reorderingState.draggingIndex ->
                reorderingState.draggingItemSize

            // Còn lại thì không dịch chuyển
            else -> 0
        }

        // Áp dụng offset theo hướng danh sách
        when (reorderingState.lazyListState.layoutInfo.orientation) {
            Orientation.Vertical -> IntOffset(0, offset)
            Orientation.Horizontal -> IntOffset(offset, 0)
        }
    }
}

// Sử dụng composed để kết hợp hiệu ứng shadow và pinning
    .composed {
        // Lấy context để ghim item (pin) trong khi đang kéo
        val container = LocalPinnableContainer.current

        // Tạo hiệu ứng animate cho shadow theo dragging state
        val elevation by animateDpAsState(
            targetValue = if (reorderingState.draggingIndex == index) draggedElevation else 0.dp,
            label = ""
        )

        // Pin item lại khi đang kéo, unpin khi thả ra
        DisposableEffect(reorderingState.draggingIndex) {
            val handle = if (reorderingState.draggingIndex == index) container?.pin() else null

            onDispose {
                handle?.release()
            }
        }

        // Thêm shadow cho item đang được kéo
        this.shadow(elevation = elevation)
    }
