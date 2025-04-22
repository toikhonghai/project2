package app.vitune.android.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.times
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.ui.modifiers.pressable

val LocalMenuState = staticCompositionLocalOf { MenuState() } // Tạo một CompositionLocal cho phép bạn truy cập MenuState ở bất kỳ đâu trong cây Composable.

@Stable
// MenuState là một lớp đại diện cho trạng thái của menu trong ứng dụng.
class MenuState {
    var isDisplayed by mutableStateOf(false) // Có đang hiển thị không?
        private set

    var content by mutableStateOf<@Composable () -> Unit>({}) //  Nội dung hiển thị trong bottom sheet (Composable lambda).
        private set

    fun display(content: @Composable () -> Unit) { // Hàm này được sử dụng để hiển thị menu với nội dung mới.
        this.content = content
        isDisplayed = true
    }

    fun hide() { // Hàm này được sử dụng để ẩn menu.
        isDisplayed = false
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope") // cảnh báo này là do việc sử dụng BoxWithConstraints mà không có nội dung bên trong
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState = LocalMenuState.current
) = BoxWithConstraints(modifier = modifier) { // BoxWithConstraints: Cho phép truy cập maxHeight, maxWidth, dùng để tính chiều cao của sheet.
    val windowInsets = LocalPlayerAwareWindowInsets.current // Lấy WindowInsets hiện tại từ LocalPlayerAwareWindowInsets

    val height = 0.8f * maxHeight //  Sheet sẽ chiếm 80% chiều cao màn hình.

    val bottomSheetState = rememberBottomSheetState( // Tạo một trạng thái cho BottomSheet
        dismissedBound = -windowInsets // Là toàn bộ thông tin về phần bị hệ thống chiếm dụng (status bar, navigation bar, v.v.).
            .only(WindowInsetsSides.Bottom) // Chỉ lấy phần insets ở phía dưới màn hình (thanh điều hướng hoặc bàn phím ảo).
            .asPaddingValues() // Chuyển đổi thành PaddingValues
            .calculateBottomPadding(), // Trả về Dp đại diện cho chiều cao thanh điều hướng (navigation bar) hoặc phần bị che phía dưới.
        expandedBound = height // Chiều cao tối đa của bottom sheet
    )

    // LaunchedEffect là một composable giúp thực hiện các tác vụ bất đồng bộ trong Compose.
    LaunchedEffect(state.isDisplayed) { // Khi trạng thái hiển thị thay đổi, kiểm tra xem có cần mở hoặc đóng bottom sheet không.
        if (state.isDisplayed) bottomSheetState.expandSoft()
        else bottomSheetState.dismissSoft()
    }

    LaunchedEffect(bottomSheetState.collapsed) { // Khi sheet tự đóng (người dùng vuốt xuống), trạng thái trong MenuState cũng cập nhật.
        if (bottomSheetState.collapsed) state.hide() // Nếu sheet đã được đóng, gọi hàm hide() trong MenuState để ẩn menu.
    }

    AnimatedVisibility( // AnimatedVisibility: Hiển thị hoặc ẩn một composable với hiệu ứng chuyển tiếp.
        visible = state.isDisplayed,
        enter = fadeIn(), // Hiệu ứng khi hiển thị
        exit = fadeOut() // Hiệu ứng khi ẩn
    ) {
        Spacer(
            modifier = Modifier
                .pressable(onRelease = state::hide) // Khi nhấn vào không gian trống, gọi hàm hide() trong MenuState để ẩn menu.
                .alpha(bottomSheetState.progress * 0.5f)
                .background(Color.Black)
                .fillMaxSize()
        )
    }

    CompositionLocalProvider(LocalOverscrollConfiguration provides null) { // Vô hiệu hóa overscroll effect (hiệu ứng bounce/kéo-xuống-giới-hạn) khi cuộn nội dung bên trong BottomSheet.
        if (!bottomSheetState.dismissed) BottomSheet( // This way the back gesture gets handled correctly
            state = bottomSheetState,
            collapsedContent = { },
            onDismiss = { state.hide() },
            indication = null, // Vô hiệu hóa hiệu ứng chỉ báo khi kéo
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .sizeIn(maxHeight = height)
                    .nestedScroll(bottomSheetState.preUpPostDownNestedScrollConnection) // xử lý nested scroll (cuộn lồng nhau) trong BottomSheet,
                        // giúp sheet phản ứng mượt mà khi cuộn một nội dung bên trong, ví dụ: cuộn LazyColumn mà đồng thời sheet cũng bị kéo.
            ) {
                state.content()
            }
        }
    }
}
