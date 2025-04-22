package app.vitune.android.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.utils.ScrollingInfo
import app.vitune.android.utils.scrollingInfo
import app.vitune.android.utils.smoothScrollToTop
import kotlinx.coroutines.launch

@Composable
fun BoxScope.FloatingActionsContainerWithScrollToTop(
    lazyGridState: LazyGridState,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    @DrawableRes icon: Int? = null,
    @DrawableRes scrollIcon: Int? = R.drawable.chevron_up,
    onClick: (() -> Unit)? = null,
    onScrollToTop: (suspend () -> Unit)? = lazyGridState::smoothScrollToTop,
    reverse: Boolean = false,
    insets: WindowInsets = LocalPlayerAwareWindowInsets.current
) = FloatingActions(
    state = if (visible) lazyGridState.scrollingInfo() else null,
    onScrollToTop = onScrollToTop,
    reverse = reverse,
    icon = icon,
    scrollIcon = scrollIcon,
    onClick = onClick,
    insets = insets,
    modifier = modifier
)

@Composable
fun BoxScope.FloatingActionsContainerWithScrollToTop(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    @DrawableRes icon: Int? = null,
    @DrawableRes scrollIcon: Int? = R.drawable.chevron_up,
    onClick: (() -> Unit)? = null,
    onScrollToTop: (suspend () -> Unit)? = lazyListState::smoothScrollToTop,
    reverse: Boolean = false,
    insets: WindowInsets = LocalPlayerAwareWindowInsets.current
) = FloatingActions(
    state = if (visible) lazyListState.scrollingInfo() else null,
    onScrollToTop = onScrollToTop,
    reverse = reverse,
    icon = icon,
    scrollIcon = scrollIcon,
    onClick = onClick,
    insets = insets,
    modifier = modifier
)

@Composable
fun BoxScope.FloatingActionsContainerWithScrollToTop(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    @DrawableRes icon: Int? = null,
    @DrawableRes scrollIcon: Int? = R.drawable.chevron_up,
    onClick: (() -> Unit)? = null,
    onScrollToTop: (suspend () -> Unit)? = scrollState::smoothScrollToTop,
    reverse: Boolean = false,
    insets: WindowInsets = LocalPlayerAwareWindowInsets.current
) = FloatingActions(
    state = if (visible) scrollState.scrollingInfo() else null,
    onScrollToTop = onScrollToTop,
    reverse = reverse,
    icon = icon,
    scrollIcon = scrollIcon,
    onClick = onClick,
    insets = insets,
    modifier = modifier
)

/*
Floating Action Buttons (FAB) là một thành phần giao diện phổ biến trong thiết kế Material Design — thường là một nút tròn,
nổi lên trên nội dung chính, và được dùng để thực hiện hành động quan trọng, nổi bật nhất trên màn hình hiện tại.
 */
@Composable
private fun BoxScope.FloatingActions(
    state: ScrollingInfo?, // Trạng thái cuộn hiện tại (gồm đang cuộn xuống hay không, đã cuộn xa chưa)
    insets: WindowInsets, // Insets để tính padding (ví dụ: để tránh thanh điều hướng)
    modifier: Modifier = Modifier,
    onScrollToTop: (suspend () -> Unit)? = null, // Hàm được gọi khi nhấn nút scroll to top
    reverse: Boolean = false, // Đảo ngược logic isScrollingDown (dùng cho scroll từ dưới lên)
    @DrawableRes icon: Int? = null, // Icon của nút chính (nút search, thêm,...)
    @DrawableRes scrollIcon: Int? = R.drawable.chevron_up, // Icon của nút scroll to top
    onClick: (() -> Unit)? = null // Sự kiện khi nhấn nút chính
) = Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.Bottom,
    modifier = modifier
        .align(Alignment.BottomEnd) // Đặt FloatingAction ở góc dưới bên phải
        .padding(end = 16.dp) // Padding bên phải cố định
        .padding(
            insets.only(WindowInsetsSides.End).asPaddingValues() // Padding từ WindowInsets (để tránh chạm navigation bar)
        )
) {
    val transition = updateTransition(state, "") // Tạo transition dựa trên trạng thái cuộn
    val bottomPaddingValues = insets.only(WindowInsetsSides.Bottom).asPaddingValues() // Padding dưới tránh nút hệ thống
    val coroutineScope = rememberCoroutineScope() // Coroutine scope để chạy scroll animation

    // Nút Scroll to Top
    onScrollToTop?.let {
        transition.AnimatedVisibility(
            visible = { it != null && it.isScrollingDown == reverse && it.isFar }, // Hiện nếu cuộn xuống (hoặc ngược nếu reverse) và đã cuộn khá xa
            enter = slideInVertically(
                animationSpec = tween(500, if (icon == null) 0 else 100) // Nếu có nút icon thì delay tí cho đẹp
            ) { it },
            exit = slideOutVertically(tween(500, 0)) { it }
        ) {
            SecondaryButton(
                onClick = {
                    coroutineScope.launch { onScrollToTop() } // Gọi suspend scroll về đầu
                },
                iconId = scrollIcon ?: R.drawable.chevron_up, // Icon cuộn lên
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(bottomPaddingValues) // Tránh thanh điều hướng
            )
        }
    }

    // Nút chính (ví dụ: search, thêm mới,...)
    icon?.let {
        onClick?.let {
            transition.AnimatedVisibility(
                visible = { it?.isScrollingDown == false }, // Hiện khi KHÔNG cuộn xuống
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 500, delayMillis = 0),
                    initialOffsetY = { it }
                ),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 500, delayMillis = 100),
                    targetOffsetY = { it }
                )
            ) {
                PrimaryButton(
                    icon = icon,
                    onClick = onClick,
                    enabled = transition.targetState?.isScrollingDown == false,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .padding(bottomPaddingValues)
                )
            }
        }
    }
}
