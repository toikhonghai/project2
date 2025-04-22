package app.vitune.android.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import app.vitune.android.GlobalPreferencesHolder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// UIStatePreferences: Lưu trữ trạng thái UI của ứng dụng, bao gồm các tab hiện tại và hiển thị dạng lưới.
object UIStatePreferences : GlobalPreferencesHolder() {

    // Chỉ mục tab đang hiển thị trên màn hình chính
    var homeScreenTabIndex by int(0)

    // Chỉ mục tab đang hiển thị trên màn hình kết quả tìm kiếm
    var searchResultScreenTabIndex by int(0)


    // Thuộc tính lưu trữ và theo dõi chỉ mục tab trên màn hình nghệ sĩ
    var artistScreenTabIndexProperty = int(0)
    var artistScreenTabIndex by artistScreenTabIndexProperty

    // Kiểm soát hiển thị danh sách playlist dưới dạng lưới (true) hoặc danh sách (false)
    var playlistsAsGrid by boolean(true)

    // Lưu trạng thái hiển thị của các tab (ẩn/hiện) dưới dạng JSON
    private var visibleTabs by json(mapOf<String, List<String>>())

    /**
     * mutableTabStateOf: Tạo một MutableState để theo dõi trạng thái hiển thị của tab dựa trên key.
     * @param key: Chuỗi đại diện cho tab (ví dụ: "home", "search").
     * @param default: Danh sách mặc định của các tab hiển thị (mặc định là rỗng).
     * @return MutableState chứa danh sách các tab hiển thị dưới dạng ImmutableList.
     */
    @Composable
    fun mutableTabStateOf(
        key: String,
        default: ImmutableList<String> = persistentListOf()
    ): MutableState<ImmutableList<String>> = remember(key, default, visibleTabs) {
        mutableStateOf(
            value = visibleTabs.getOrDefault(key, default).toImmutableList(),
            policy = object : SnapshotMutationPolicy<ImmutableList<String>> {

                /**
                 * Xác định hai danh sách `a` và `b` có tương đương nhau không.
                 * Nếu không, cập nhật `visibleTabs` để lưu trạng thái mới.
                 */
                override fun equivalent(
                    a: ImmutableList<String>,
                    b: ImmutableList<String>
                ): Boolean {
                    val eq = a == b  // So sánh hai danh sách tab
                    if (!eq) visibleTabs += key to b  // Cập nhật nếu có sự thay đổi
                    return eq
                }
            }
        )
    }
}
