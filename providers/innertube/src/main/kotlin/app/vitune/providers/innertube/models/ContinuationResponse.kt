package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ContinuationResponse(  //chứa dữ liệu chính của phản hồi, có thể là tiếp tục tải danh sách nhạc hoặc danh sách phát.
    val continuationContents: ContinuationContents?
) {
    @Serializable
    data class ContinuationContents( //
        @JsonNames("musicPlaylistShelfContinuation")
        val musicShelfContinuation: MusicShelfRenderer?, // Đại diện cho dữ liệu danh sách nhạc tiếp tục tải.
        val playlistPanelContinuation: NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer? // Chứa thông tin về danh sách phát tiếp tục tải.
    )
}
