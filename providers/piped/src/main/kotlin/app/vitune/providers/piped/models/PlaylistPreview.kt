package app.vitune.providers.piped.models

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
// nó chứa thông tin về một playlist đã được tạo
data class CreatedPlaylist(
    @SerialName("playlistId")
    val id: UUIDString
)

@Serializable
// nó chứa thông tin về một playlist, bao gồm id, tên, mô tả ngắn, url hình thu nhỏ và số lượng video trong playlist
data class PlaylistPreview(
    val id: UUIDString,
    val name: String,
    @SerialName("shortDescription")
    val description: String? = null,
    @SerialName("thumbnail")
    val thumbnailUrl: UrlString,
    @SerialName("videos")
    val videoCount: Int
)

@Serializable
// nó chứa thông tin về một playlist, bao gồm tên, url hình thu nhỏ, mô tả, url banner, số lượng video và danh sách video trong playlist
data class Playlist(
    val name: String,
    val thumbnailUrl: UrlString,
    val description: String? = null,
    val bannerUrl: UrlString? = null,
    @SerialName("videos")
    val videoCount: Int,
    @SerialName("relatedStreams")
    val videos: List<Video>
) {
    @Serializable
    // nó chứa thông tin về một video trong playlist, bao gồm url, tiêu đề, url hình thu nhỏ, tên người tải lên, url người tải lên và thời gian video
    data class Video(
        val url: String, // not a real url, why?
        val title: String,
        @SerialName("thumbnail")
        val thumbnailUrl: UrlString,
        val uploaderName: String,
        val uploaderUrl: String, // not a real url either
        @SerialName("uploaderAvatar")
        val uploaderAvatarUrl: UrlString,
        @SerialName("duration")
        val durationSeconds: Long
    ) {
        val id
            get() = if (url.startsWith("/watch?v=")) url.substringAfter("/watch?v=")
            else Url(url).parameters["v"]?.firstOrNull()?.toString()
        /*
        ví dụ:
        val url1 = "/watch?v=abc123"
        val url2 = "https://youtube.com/watch?v=xyz789"

        println(url1.substringAfter("/watch?v=")) // 👉 Output: abc123
        println(Url(url2).parameters["v"]?.firstOrNull()) // 👉 Output: xyz789
         */
        val uploaderId
            get() = if (uploaderUrl.startsWith("/channel/")) uploaderUrl.substringAfter("/channel/")
            else Url(uploaderUrl).segments.lastOrNull()

        val duration get() = durationSeconds.seconds // chuyển đổi giây thành Duration
    }
}
