package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThumbnailRenderer( //  Chứa thông tin về hình thu nhỏ của video hoặc bài hát.
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?
) {
    @Serializable
    data class MusicThumbnailRenderer( //  Chứa danh sách các hình thu nhỏ ở các kích thước khác nhau.
        val thumbnail: Thumbnail?
    ) {
        @Serializable
        data class Thumbnail(
            val thumbnails: List<app.vitune.providers.innertube.models.Thumbnail>?
        )
    }
}

@Serializable
data class ThumbnailOverlay(
    val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer // Chứa thông tin về lớp phủ trên hình thu nhỏ của video hoặc bài hát.
) {
    @Serializable
    data class MusicItemThumbnailOverlayRenderer( // Chứa thông tin về lớp phủ trên hình thu nhỏ của video hoặc bài hát.
        val content: Content
    ) {
        @Serializable
        data class Content(
            val musicPlayButtonRenderer: MusicPlayButtonRenderer // Chứa thông tin về nút phát nhạc trên lớp phủ.
        ) {
            @Serializable
            data class MusicPlayButtonRenderer(
                val playNavigationEndpoint: NavigationEndpoint? //  Chứa thông tin điều hướng khi nhấn vào nút phát.
            )
        }
    }
}
