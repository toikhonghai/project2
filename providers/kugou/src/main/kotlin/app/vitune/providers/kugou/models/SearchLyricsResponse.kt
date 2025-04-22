package app.vitune.providers.kugou.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
// Lớp này được sử dụng để phân tích cú pháp phản hồi từ API của Kugou khi tìm kiếm lời bài hát
internal class SearchLyricsResponse(
    val candidates: List<Candidate>
) {
    @Serializable
    internal class Candidate( // Lớp này đại diện cho một ứng viên lời bài hát
        val id: Long,
        @SerialName("accesskey") val accessKey: String,
        val duration: Long
    )
}
