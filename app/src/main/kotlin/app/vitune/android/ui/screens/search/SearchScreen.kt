package app.vitune.android.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.vitune.android.R
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.secondary
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.ui.LocalAppearance

@Route // @Route là
@Composable
fun SearchScreen(
    initialTextInput: String,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit
) {
    // Tạo một holder để lưu trạng thái có thể lưu lại (saveable) theo từng tab
    val saveableStateHolder = rememberSaveableStateHolder()

    // Lưu trạng thái tab hiện tại (0: Online, 1: Library)
    val (tabIndex, onTabChanged) = rememberSaveable { mutableIntStateOf(0) }

    // Lưu trạng thái của TextField (bao gồm text và vị trí con trỏ)
    val (textFieldValue, onTextFieldValueChanged) = rememberSaveable(
        initialTextInput,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length) // Đặt con trỏ ở cuối chuỗi
            )
        )
    }

    // Hàm dọn dẹp các trạng thái PersistMap có prefix là "search/"
    PersistMapCleanup(prefix = "search/")

    // Điều hướng chính của màn hình này
    RouteHandler {
        GlobalRoutes() // Các route con chung có thể dùng ở nhiều màn

        // UI content của màn
        Content {
            // Tạo decoration box cho TextField (ví dụ hiển thị placeholder động)
            val decorationBox: @Composable (@Composable () -> Unit) -> Unit = { innerTextField ->
                Box {
                    // Hiển thị placeholder nếu text rỗng
                    AnimatedVisibility(
                        visible = textFieldValue.text.isEmpty(),
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        BasicText(
                            text = stringResource(R.string.search_placeholder),
                            maxLines = 1,
                            style = LocalAppearance.current.typography.xxl.secondary
                        )
                    }

                    // Nội dung thật sự của TextField (do hệ thống gọi)
                    innerTextField()
                }
            }

            // Giao diện scaffold với thanh trên, tab, và nội dung tab
            Scaffold(
                key = "search", // Khóa để xác định scaffold này
                topIconButtonId = R.drawable.chevron_back, // Icon nút quay lại
                onTopIconButtonClick = pop, // Khi nhấn nút quay lại thì pop ra màn trước
                tabIndex = tabIndex,
                onTabChange = onTabChanged,
                tabColumnContent = {
                    // Các tab: Online và Library
                    tab(0, R.string.online, R.drawable.globe, canHide = false)
                    tab(1, R.string.library, R.drawable.library)
                }
            ) { currentTabIndex ->
                // Mỗi tab có thể có trạng thái riêng biệt (giữ được khi chuyển tab)
                saveableStateHolder.SaveableStateProvider(currentTabIndex) {
                    when (currentTabIndex) {
                        0 -> OnlineSearch(
                            textFieldValue = textFieldValue,
                            onTextFieldValueChange = onTextFieldValueChanged,
                            onSearch = onSearch,
                            onViewPlaylist = onViewPlaylist,
                            decorationBox = decorationBox,
                            focused = child == null // Tự động focus nếu không có route con
                        )

                        1 -> LocalSongSearch(
                            textFieldValue = textFieldValue,
                            onTextFieldValueChange = onTextFieldValueChanged,
                            decorationBox = decorationBox
                        )
                    }
                }
            }
        }
    }
}
