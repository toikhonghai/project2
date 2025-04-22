package app.vitune.providers.innertube.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
//sử dụng để biểu diễn phản hồi của API về hàng đợi nhạc hiện tại của
// người dùng trên YouTube Music
@Serializable
// Chứa thông tin về hàng đợi nhạc hiện tại của người dùng.
data class GetQueueResponse( // Chứa thông tin về hàng đợi nhạc hiện tại của người dùng.
    @SerialName("queueDatas")
    val queueData: List<QueueData>?
) {
    @Serializable
    data class QueueData(
        val content: NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer.Content? // Chứa thông tin về danh sách phát nhạc (PlaylistPanelRenderer.Content).
    )
}
