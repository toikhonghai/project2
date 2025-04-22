package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Tabs( // Lưu trữ thông tin về các tab trong giao diện người dùng của YouTube Music.
    val tabs: List<Tab>?
) {
    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer?
    ) {
        @Serializable
        data class TabRenderer(
            val content: Content?,
            val title: String?,
            val tabIdentifier: String? // Định danh duy nhất cho tab này, có thể được sử dụng để xác định tab trong các tình huống khác nhau.
        ) {
            @Serializable
            data class Content(
                val sectionListRenderer: SectionListRenderer? // Chứa thông tin về danh sách các phần trong tab này.
            )
        }
    }
}

@Serializable
data class TwoColumnBrowseResultsRenderer( // Lưu trữ thông tin về giao diện hai cột trong YouTube Music.
    val tabs: List<Tabs.Tab>?, // Danh sách các tab trong giao diện hai cột.
    val secondaryContents: Tabs.Tab.TabRenderer.Content? // Chứa thông tin về nội dung phụ trong giao diện hai cột (nếu có).
)
