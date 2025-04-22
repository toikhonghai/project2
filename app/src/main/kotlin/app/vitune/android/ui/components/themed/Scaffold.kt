package app.vitune.android.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import app.vitune.android.R
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.toImmutableList

//Tạo một Scaffold tùy chỉnh với thanh điều hướng bên (NavigationRail) và hiệu ứng chuyển đổi giữa các tab.
/*NavigationRail là một thành phần điều hướng được thiết kế cho màn hình rộng,
chẳng hạn như tablet hoặc màn hình lớn, để hiển thị một thanh điều hướng dọc bên trái.*/
@Composable
fun Scaffold(
    key: String,  // Khóa xác định trạng thái tab đang được lưu trữ
    topIconButtonId: Int,  // ID của nút icon trên thanh điều hướng
    onTopIconButtonClick: () -> Unit,  // Callback khi nút icon được nhấn
    tabIndex: Int,  // Chỉ số tab hiện tại
    onTabChange: (Int) -> Unit,  // Callback khi tab thay đổi
    tabColumnContent: TabsBuilder.() -> Unit,  // Nội dung các tab trong thanh điều hướng
    modifier: Modifier = Modifier,  // Modifier tùy chỉnh
    tabsEditingTitle: String = stringResource(R.string.tabs),  // Tiêu đề chỉnh sửa tab
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit  // Nội dung hiển thị cho tab
) {
    val (colorPalette) = LocalAppearance.current  // Lấy bảng màu từ `LocalAppearance`
    var hiddenTabs by UIStatePreferences.mutableTabStateOf(key)  // Lưu trạng thái ẩn tab trong preferences

    Row(
        modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()
    ) {
        // Thanh điều hướng bên trái (NavigationRail)
        NavigationRail(
            topIconButtonId = topIconButtonId,  // ID của nút trên cùng
            onTopIconButtonClick = onTopIconButtonClick,  // Callback khi nút icon trên cùng được nhấn
            tabIndex = tabIndex,  // Chỉ số tab hiện tại
            onTabIndexChange = onTabChange,  // Callback khi tab thay đổi
            hiddenTabs = hiddenTabs,  // Danh sách tab bị ẩn
            setHiddenTabs = { hiddenTabs = it.toImmutableList() },  // Cập nhật danh sách tab bị ẩn
            tabsEditingTitle = tabsEditingTitle,  // Tiêu đề chỉnh sửa tab
            content = tabColumnContent  // Nội dung danh sách tab
        )

        // Hiệu ứng chuyển đổi giữa các tab
        AnimatedContent(
            targetState = tabIndex,  // Xác định tab hiện tại
            transitionSpec = { // Cấu hình hiệu ứng chuyển đổi
                val slideDirection = if (targetState > initialState) Up else Down  // Xác định hướng trượt
                val animationSpec = spring(  // Cấu hình animation dạng `spring`
                    dampingRatio = 0.9f,  // Giúp animation mượt mà.
                    stiffness = Spring.StiffnessLow,  // Animation diễn ra chậm hơn.
                    visibilityThreshold = IntOffset.VisibilityThreshold  // Giúp xác định animation có hoàn tất không.
                )

                ContentTransform( // Cấu hình hiệu ứng chuyển đổi
                    targetContentEnter = slideIntoContainer(slideDirection, animationSpec),  // Hiệu ứng trượt vào
                    initialContentExit = slideOutOfContainer(slideDirection, animationSpec),  // Hiệu ứng trượt ra
                    sizeTransform = null  // Không thay đổi kích thước khi chuyển đổi
                )
            },
            content = content,  // Nội dung của tab
            label = ""  // Nhãn của animation (có thể dùng để debug)
        )
    }
}