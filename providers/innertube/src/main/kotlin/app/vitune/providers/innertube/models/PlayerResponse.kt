package app.vitune.providers.innertube.models

import app.vitune.providers.innertube.Innertube
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

// sử dụng để biểu diễn phản hồi từ YouTube khi truy vấn thông tin về một video.
// Nó chứa nhiều thông tin liên quan đến trạng thái phát video, cấu hình phát, dữ liệu luồng, và chi tiết video.
@Serializable
data class PlayerResponse( // Chứa thông tin phản hồi từ YouTube khi truy vấn thông tin về một video.
    val playabilityStatus: PlayabilityStatus?,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
    @Transient
    val context: Context? = null,
    @Transient
    val cpn: String? = null
) {
    val reason
        get() = if (playabilityStatus != null && playabilityStatus.status != "OK") buildString {
            appendLine("YouTube responded with status '${playabilityStatus.reason.orEmpty()}'") // Thông báo trạng thái phản hồi từ YouTube
            playabilityStatus.reason?.let { appendLine("Reason: $it") } // Lý do phản hồi
            playabilityStatus.errorScreen?.playerErrorMessageRenderer?.subreason?.text?.let { // Lý do lỗi
                appendLine()
                appendLine(it)
            }
        } else null

    @Serializable
    data class PlayabilityStatus( // Chứa thông tin về trạng thái phát video.
        val status: String? = null,
        val reason: String? = null,
        val errorScreen: ErrorScreen? = null
    )

    @Serializable
    data class PlayerConfig( // Chứa thông tin về cấu hình phát video.
        val audioConfig: AudioConfig?
    ) {
        @Serializable
        data class AudioConfig( // Chứa thông tin về cấu hình âm thanh.
            internal val loudnessDb: Double?, // Độ lớn âm thanh (loudness) của video.
            internal val perceptualLoudnessDb: Double? /// Độ lớn âm thanh cảm nhận được (perceptual loudness) của video.
        ) {
            // For music clients only
            val normalizedLoudnessDb: Float? // Độ lớn âm thanh đã chuẩn hóa (normalized loudness) của video.
                get() = (loudnessDb ?: perceptualLoudnessDb?.plus(7))?.plus(7)?.toFloat() // Chuyển đổi độ lớn âm thanh thành kiểu Float và cộng thêm 7 để chuẩn hóa.
        }
    }

    @Serializable
    data class StreamingData( // Chứa thông tin về dữ liệu luồng video.
        val adaptiveFormats: List<AdaptiveFormat>?, // Danh sách các định dạng luồng thích ứng (adaptive formats).
        val expiresInSeconds: Long? // `Thời gian hết hạn của dữ liệu luồng (expires in seconds).
    ) {
        val highestQualityFormat: AdaptiveFormat? // Chứa định dạng có chất lượng cao nhất trong danh sách adaptiveFormats.
            get() = adaptiveFormats?.filter { it.url != null || it.signatureCipher != null } // Lọc các định dạng có URL hoặc signatureCipher không null.
                ?.let { formats ->
                    formats.findLast { it.itag == 251 || it.itag == 140 } // Tìm định dạng âm thanh (audio) có itag là 251 hoặc 140.
                        ?: formats.maxBy { it.bitrate ?: 0L } // Nếu không tìm thấy định dạng âm thanh, tìm định dạng có bitrate cao nhất.
                }

        @Serializable
        data class AdaptiveFormat( // Chứa thông tin về một định dạng luồng thích ứng.
            val itag: Int, // Mã định dạng (itag) của video.
            val mimeType: String, // Loại MIME của video.
            val bitrate: Long?, // Tốc độ bit (bitrate) của video.
            val averageBitrate: Long?,
            val contentLength: Long?,
            val audioQuality: String?,
            val approxDurationMs: Long?,
            val lastModified: Long?,
            val loudnessDb: Double?,
            val audioSampleRate: Int?,
            val url: String?,
            val signatureCipher: String?
        ) {
            suspend fun findUrl(context: Context) =
                url ?: signatureCipher?.let { Innertube.decodeSignatureCipher(context, it) } // Tìm URL của video, nếu không có thì giải mã signatureCipher để lấy URL.
        }
    }

    @Serializable
    data class VideoDetails( // Chứa thông tin chi tiết về video.
        val videoId: String?
    )
}

@Serializable
data class ErrorScreen(
    val playerErrorMessageRenderer: PlayerErrorMessageRenderer? = null
) {
    @Serializable
    data class PlayerErrorMessageRenderer( // Chứa thông tin về lỗi phát video.
        val subreason: Runs? = null
    )
}
