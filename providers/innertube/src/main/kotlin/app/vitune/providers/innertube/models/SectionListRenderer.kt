package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SectionListRenderer( // Lớp này đại diện cho một danh sách các phần nội dung trong phản hồi từ API của YouTube Music.
    val contents: List<Content>?,
    val continuations: List<Continuation>?
) {
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?, // Lớp này đại diện cho một danh sách các phần nội dung trong phản hồi từ API của YouTube Music.
        @JsonNames("musicPlaylistShelfRenderer")
        val musicShelfRenderer: MusicShelfRenderer?, // đại diện cho một danh sách nhạc (music shelf) trên ứng dụng nghe nhạc.
        val gridRenderer: GridRenderer?, // đại diện cho một danh sách các video trong một lưới (grid).
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?, // đại diện cho một danh sách mô tả âm nhạc.
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer? // đại diện cho một tiêu đề phản hồi âm nhạc.
    ) {
        @Serializable
        data class MusicDescriptionShelfRenderer(
            val description: Runs?
        )

        @Serializable
        data class MusicResponsiveHeaderRenderer(
            val title: Runs?,
            val description: MusicDescriptionShelfRenderer?,
            val subtitle: Runs?,
            val secondSubtitle: Runs?,
            val thumbnail: ThumbnailRenderer?, // chứa thông tin về hình thu nhỏ của video.
            val straplineTextOne: Runs? // chứa thông tin về văn bản mô tả một.
        )
    }
}
