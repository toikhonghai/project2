package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

//đại diện cho một danh sách nhạc (music shelf) trên ứng dụng nghe nhạc.
// Danh sách này có thể là một phần của trang chủ hoặc một danh sách phát.
@Serializable
data class MusicShelfRenderer(
    val bottomEndpoint: NavigationEndpoint?, // điểm đến khi nhấn vào nút "Xem tất cả" (View All) trong danh sách nhạc.
    val contents: List<Content>?, // danh sách các mục trong danh sách nhạc.
    val continuations: List<Continuation>?, // danh sách các phần tiếp theo (continuation) để tải thêm nội dung.
    val title: Runs?
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? // đại diện cho một mục trong danh sách nhạc.
    ) {
        val runs: Pair<List<Runs.Run>, List<List<Runs.Run>>>
            get() = musicResponsiveListItemRenderer
                ?.flexColumns // Lấy danh sách các cột linh hoạt trong mục danh sách nhạc.
                ?.firstOrNull() // Lấy cột đầu tiên trong danh sách các cột linh hoạt.
                ?.musicResponsiveListItemFlexColumnRenderer // Lấy thông tin của cột đầu tiên.
                ?.text
                ?.runs
                .orEmpty() to
                    musicResponsiveListItemRenderer // Lấy danh sách các cột linh hoạt trong mục danh sách nhạc.
                        ?.flexColumns
                        ?.let { it.getOrNull(1) ?: it.lastOrNull() }
                        ?.musicResponsiveListItemFlexColumnRenderer // Lấy thông tin của cột thứ hai hoặc cột cuối cùng trong danh sách các cột linh hoạt.
                        ?.text
                        ?.splitBySeparator() // Chia danh sách các đối tượng Run thành các nhóm dựa trên ký tự phân tách (SEPARATOR).
                        .orEmpty()
        /*
            Kết quả: Trả về một Pair, trong đó:

            Phần đầu chứa danh sách các đoạn văn bản của tiêu đề.

            Phần sau chứa danh sách danh sách các đoạn văn bản của phụ đề (nếu có).
         */

        val thumbnail: Thumbnail?
            get() = musicResponsiveListItemRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.thumbnail
                ?.thumbnails
                ?.firstOrNull()
        //Truy xuất từ musicResponsiveListItemRenderer → thumbnail → musicThumbnailRenderer → thumbnail → thumbnails.
    }
}
