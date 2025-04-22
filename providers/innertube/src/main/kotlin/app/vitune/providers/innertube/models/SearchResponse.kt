package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents?
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs? // Lưu trữ thông tin về các tab tìm kiếm, mỗi tab có thể chứa nhiều danh sách kết quả tìm kiếm khác nhau.
    )
}
