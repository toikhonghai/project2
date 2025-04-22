package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
/// Dùng để lưu trữ thông tin về hình thu nhỏ (thumbnail) của video, playlist hoặc album trong ứng dụng YouTube Music.
data class Thumbnail(
    val url: String,
    val height: Int?,
    val width: Int?
) {
    fun size(size: Int) = when {
        url.startsWith("https://lh3.googleusercontent.com") -> "$url-w$size-h$size" //iúp thay đổi kích thước của ảnh dựa trên URL.
        url.startsWith("https://yt3.ggpht.com") -> "$url-s$size" //  giúp thay đổi kích thước của ảnh dựa trên URL.
        else -> url
    }
}
