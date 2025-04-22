package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// sử dụng để ánh xạ phản hồi từ API của YouTube Music.
@Serializable
data class BrowseResponse(
    val contents: Contents?,
    val header: Header?,
    val microformat: Microformat?
) {
    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: Tabs?, //Hiển thị nội dung dạng tab đơn.
        val sectionListRenderer: SectionListRenderer?, // Hiển thị danh sách các phần nội dung.
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer? // Hiển thị nội dung dạng tab đôi.
    )

    @Serializable
    // Lớp này đại diện cho các tab trong phản hồi từ API của YouTube Music.
    @OptIn(ExperimentalSerializationApi::class) // Đánh dấu để sử dụng tính năng chưa ổn định
    data class Header(
        @JsonNames("musicVisualHeaderRenderer") // Đánh dấu để ánh xạ tên trường trong JSON
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?, //  Hiển thị tiêu đề âm nhạc
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer? // Hiển thị tiêu đề chi tiết âm nhạc
    ) {
        @Serializable
        data class MusicDetailHeaderRenderer( // Hiển thị tiêu đề chi tiết âm nhạc
            val title: Runs?,
            val description: Runs?,
            val subtitle: Runs?,
            val secondSubtitle: Runs?,
            val thumbnail: ThumbnailRenderer?
        )

        @Serializable
        data class MusicImmersiveHeaderRenderer( // Hiển thị tiêu đề âm nhạc
            val description: Runs?,
            val playButton: PlayButton?,
            val startRadioButton: StartRadioButton?,
            val thumbnail: ThumbnailRenderer?,
            val foregroundThumbnail: ThumbnailRenderer?,
            val title: Runs?,
            val subscriptionButton: SubscriptionButton?
        ) {
            @Serializable
            data class PlayButton( // Nút phát nhạc
                val buttonRenderer: ButtonRenderer?
            )

            @Serializable
            data class StartRadioButton( // Nút bắt đầu radio
                val buttonRenderer: ButtonRenderer?
            )

            @Serializable
            data class SubscriptionButton( // Nút đăng ký
                val subscribeButtonRenderer: SubscribeButtonRenderer?
            )
        }
    }

    @Serializable
    data class Microformat( // Chứa thông tin định dạng vi mô
        val microformatDataRenderer: MicroformatDataRenderer?
    ) {
        @Serializable
        data class MicroformatDataRenderer( // Chứa thông tin định dạng vi mô
            val urlCanonical: String?
        )
    }
}
