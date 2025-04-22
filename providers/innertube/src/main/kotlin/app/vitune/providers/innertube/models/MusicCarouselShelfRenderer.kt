package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

// đại diện cho một danh sách nhạc được hiển thị dưới dạng carousel (băng chuyền).
// Đây là một phần của UI, thường xuất hiện trên trang chủ của ứng dụng nghe nhạc.
@Serializable
data class MusicCarouselShelfRenderer(
    val header: Header?,
    val contents: List<Content>?
) {
    @Serializable
    data class Content( // Lưu trữ thông tin của một mục trong danh sách nhạc, có thể là một bài hát hoặc một album.
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?, //Lưu trữ thông tin của một mục trong danh sách nhạc, có thể là một bài hát hoặc một album.
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?, //Lưu trữ thông tin của một mục trong danh sách nhạc, có thể là một bài hát hoặc một album.
        val musicNavigationButtonRenderer: MusicNavigationButtonRenderer? = null // Lưu trữ thông tin của một nút điều hướng trong danh sách nhạc.
    )

    @Serializable
    data class Header(
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?, //Lưu trữ thông tin của một mục trong danh sách nhạc, có thể là một bài hát hoặc một album.
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,// Lưu trữ thông tin của một mục trong danh sách nhạc, có thể là một bài hát hoặc một album.
        val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer? //Lưu trữ thông tin của một tiêu đề trong danh sách nhạc.
    ) {
        @Serializable
        data class MusicCarouselShelfBasicHeaderRenderer(
            val moreContentButton: MoreContentButton?, // Lưu trữ thông tin của một nút điều hướng trong danh sách nhạc.
            val title: Runs?,
            val strapline: Runs? // Lưu trữ thông tin của một tiêu đề trong danh sách nhạc.
        ) {
            @Serializable
            data class MoreContentButton(
                val buttonRenderer: ButtonRenderer? // Lưu trữ thông tin của một nút điều hướng trong danh sách nhạc.
            )
        }
    }
}
