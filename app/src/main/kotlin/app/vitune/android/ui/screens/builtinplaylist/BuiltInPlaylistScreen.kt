package app.vitune.android.ui.screens.builtinplaylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import app.vitune.android.R
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.RouteHandler
import app.vitune.core.data.enums.BuiltInPlaylist

// Object đại diện cho màn hình hiển thị các playlist hệ thống (Favorites, Offline, Top, History)
object BuiltInPlaylistScreen {
    internal const val KEY = "builtinplaylist" // Khóa dùng để lưu trạng thái UI (ví dụ tabs ẩn/hiện)

    // Trả về danh sách các BuiltInPlaylist đang được hiển thị, dựa trên trạng thái tab đã ẩn
    @Composable
    fun shownPlaylistsAsState(): State<List<BuiltInPlaylist>> {
        // Lấy danh sách tab đã ẩn từ UIStatePreferences (được lưu dưới dạng ordinal dưới dạng String)
        val hiddenPlaylistTabs by UIStatePreferences.mutableTabStateOf(KEY)

        return remember {
            // derivedStateOf giúp tạo state phụ thuộc vào hiddenPlaylistTabs
            derivedStateOf {
                BuiltInPlaylist.entries.filter { it.ordinal.toString() !in hiddenPlaylistTabs } // Lọc ra các playlist không bị ẩn
            }
        }
    }
}


// Hàm hiển thị màn hình chi tiết cho từng BuiltInPlaylist (Favorites, Offline, v.v.)
@Route
@Composable
fun BuiltInPlaylistScreen(builtInPlaylist: BuiltInPlaylist) {
    val saveableStateHolder = rememberSaveableStateHolder() // Giữ trạng thái UI khi chuyển tab
    val (tabIndex, onTabIndexChanged) = rememberSaveable { mutableIntStateOf(builtInPlaylist.ordinal) }
    // -> tabIndex: tab hiện tại đang hiển thị, mặc định theo playlist truyền vào

    // Cleanup các biến được lưu trữ trong PersistMap với prefix là tên playlist (để tránh trùng key)
    PersistMapCleanup(prefix = "${builtInPlaylist.name}/")

    RouteHandler {
        GlobalRoutes() // Xử lý các route chung toàn cục (ví dụ: điều hướng trong app)

        Content {
            // Lấy tiêu đề cho tab "Top" (dạng "Top 100", "Top 50", ...)
            val topTabTitle = stringResource(R.string.format_top_playlist, DataPreferences.topListLength)

            // Giao diện Scaffold có hỗ trợ tab và nút back
            Scaffold(
                key = BuiltInPlaylistScreen.KEY, // Key cho UI state (dùng trong Preferences)
                topIconButtonId = R.drawable.chevron_back, // Icon back ở góc trên
                onTopIconButtonClick = pop, // Hành động khi nhấn nút back
                tabIndex = tabIndex, // Tab hiện tại
                onTabChange = onTabIndexChanged, // Callback khi người dùng chuyển tab

                // Nội dung của cột tab (tên và icon)
                tabColumnContent = {
                    tab(0, R.string.favorites, R.drawable.heart)
                    tab(1, R.string.offline, R.drawable.airplane)
                    tab(2, topTabTitle, R.drawable.trending_up)
                    tab(3, R.string.history, R.drawable.history)
                },

                // Tiêu đề màn hình khi chỉnh sửa tab
                tabsEditingTitle = stringResource(R.string.playlists)
            ) { currentTabIndex ->
                // Mỗi tab được bọc trong SaveableStateProvider để giữ UI state riêng biệt giữa các tab
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    // Hiển thị nội dung cho tab tương ứng (nếu có)
                    BuiltInPlaylist
                        .entries
                        .getOrNull(currentTabIndex)
                        ?.let { BuiltInPlaylistSongs(builtInPlaylist = it) }
                }
            }
        }
    }
}
