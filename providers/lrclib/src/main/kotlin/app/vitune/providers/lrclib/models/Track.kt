package app.vitune.providers.lrclib.models

import app.vitune.providers.lrclib.LrcParser
import app.vitune.providers.lrclib.toLrcFile
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.time.Duration

@Serializable
// Lớp này đại diện cho một bài hát với các thuộc tính như id, tên bài hát, tên nghệ sĩ, thời gian phát, lời bài hát không đồng bộ và lời bài hát đồng bộ.
data class Track(
    val id: Int,
    val trackName: String,
    val artistName: String,
    val duration: Double,
    val plainLyrics: String?,
    val syncedLyrics: String?
) {
    val lrc by lazy { syncedLyrics?.let { LrcParser.parse(it)?.toLrcFile() } }
}

// Hàm mở rộng này được sử dụng để tìm kiếm bài hát tốt nhất phù hợp với tiêu đề và thời gian đã cho trong danh sách các bài hát.
internal fun List<Track>.bestMatchingFor(title: String, duration: Duration) =
    firstOrNull { it.duration.toLong() == duration.inWholeSeconds } // Tìm bài hát có thời gian phát chính xác
        ?: minByOrNull { abs(it.trackName.length - title.length) } // Nếu không tìm thấy, tìm bài hát có độ dài tên gần nhất với tiêu đề đã cho
