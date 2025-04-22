package app.vitune.compose.routing

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut

// Hiệu ứng chuyển khi "đẩy màn hình mới vào" (stacking transition)
// - Màn hình cũ: scale nhỏ lại + mờ dần
// - Màn hình mới: fadeIn
// - Z-index cao hơn → nổi lên trên
val defaultStacking = ContentTransform(
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(), // thu nhỏ + mờ đi
    targetContentEnter = fadeIn(), // màn hình mới xuất hiện mượt
    targetContentZIndex = 1f // nằm trên cùng
)

// Hiệu ứng chuyển khi "pop màn hình ra" (unstacking transition)
// - Màn hình cũ: scale to lên + mờ đi
// - Màn hình mới: không có hiệu ứng (giữ nguyên)
// - Z-index thấp hơn → trở về bên dưới
val defaultUnstacking = ContentTransform(
    initialContentExit = scaleOut(targetScale = 1.1f) + fadeOut(), // phóng to + mờ đi
    targetContentEnter = EnterTransition.None, // không hiệu ứng cho content phía dưới
    targetContentZIndex = 0f // nằm bên dưới
)

// Hiệu ứng chuyển khi cả 2 state là null → thường dùng cho hiển thị ban đầu
val defaultStill = ContentTransform(
    initialContentExit = scaleOut(targetScale = 0.9f) + fadeOut(),
    targetContentEnter = fadeIn(),
    targetContentZIndex = 1f
)

// Extension cho TransitionScope: kiểm tra xem đang trong quá trình "stacking"
// tức là thêm màn hình mới (initialState null → targetState có)
val TransitionScope<*>.isStacking: Boolean
    get() = initialState == null && targetState != null

// Extension kiểm tra quá trình "unstacking"
// tức là pop màn hình ra (initialState có → targetState null)
val TransitionScope<*>.isUnstacking: Boolean
    get() = initialState != null && targetState == null

// Extension kiểm tra trạng thái không thay đổi
val TransitionScope<*>.isStill: Boolean
    get() = initialState == null && targetState == null

