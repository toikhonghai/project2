package app.vitune.android.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState

// Hàm extension để cuộn mượt về đầu danh sách trong LazyVerticalGrid (LazyGridState)
suspend fun LazyGridState.smoothScrollToTop() {
    // Nếu mục đầu tiên hiển thị đang nằm ngoài kích thước danh sách hiển thị
    if (firstVisibleItemIndex > layoutInfo.visibleItemsInfo.size)
        scrollToItem(layoutInfo.visibleItemsInfo.size) // Cuộn nhanh về gần đầu trước (tránh giật lag)

    animateScrollToItem(0) // Cuộn mượt về item đầu tiên
}

// Hàm extension để cuộn mượt về đầu danh sách trong LazyColumn/LazyRow (LazyListState)
suspend fun LazyListState.smoothScrollToTop() {
    if (firstVisibleItemIndex > layoutInfo.visibleItemsInfo.size)
        scrollToItem(layoutInfo.visibleItemsInfo.size)

    animateScrollToItem(0)
}

// Cuộn mượt về đầu trong các Scrollable đơn giản (ví dụ: Column có verticalScroll)
suspend fun ScrollState.smoothScrollToTop() = animateScrollTo(0)

// Cuộn mượt về cuối trong các Scrollable đơn giản
suspend fun ScrollState.smoothScrollToBottom() = animateScrollTo(maxValue)

