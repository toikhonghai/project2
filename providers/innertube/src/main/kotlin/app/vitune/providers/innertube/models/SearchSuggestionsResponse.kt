package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsResponse(
    val contents: List<Content>?
) {
    @Serializable
    data class Content(
        val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer? //
    ) {
        @Serializable
        data class SearchSuggestionsSectionRenderer( // Lưu trữ thông tin về một phần gợi ý tìm kiếm, bao gồm danh sách các gợi ý tìm kiếm
            val contents: List<Content>?
        ) {
            @Serializable
            data class Content(
                val searchSuggestionRenderer: SearchSuggestionRenderer?  // Lưu trữ thông tin về một gợi ý tìm kiếm, bao gồm tiêu đề và hành động khi bấm vào gợi ý
            ) {
                @Serializable
                data class SearchSuggestionRenderer(
                    val navigationEndpoint: NavigationEndpoint? // Lưu trữ thông tin về hành động khi bấm vào nút
                )
            }
        }
    }
}
