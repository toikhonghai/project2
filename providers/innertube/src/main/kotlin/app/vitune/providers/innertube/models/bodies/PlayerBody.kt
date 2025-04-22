package app.vitune.providers.innertube.models.bodies

import app.vitune.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
// Chứa thông tin cần thiết để phát video trên YouTube Music.
data class PlayerBody(
    val context: Context = Context.DefaultAndroidMusic,
    val videoId: String,
    val playlistId: String? = null,
    val cpn: String? = null, // Mã xác thực nội dung (CPN) được sử dụng để xác thực quyền truy cập vào video.
    val contentCheckOk: String = "true",
    val racyCheckOn: String = "true", // Kiểm tra nội dung nhạy cảm.
    val playbackContext: PlaybackContext? = null
) {
    @Serializable
    data class PlaybackContext( // Chứa thông tin về ngữ cảnh phát video.
        val contentPlaybackContext: ContentPlaybackContext? = null
    ) {
        @Serializable
        data class ContentPlaybackContext( // Chứa thông tin về ngữ cảnh phát video.
            val signatureTimestamp: String? = null
        )
    }
}
