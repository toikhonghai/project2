package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded

@Immutable // Đánh dấu lớp là bất biến
// lớp này được sử dụng để kết hợp thông tin của bài hát và độ dài nội dung
data class SongWithContentLength(
    @Embedded val song: Song, // Lớp Song được nhúng vào lớp SongWithContentLength
    val contentLength: Long?
)
