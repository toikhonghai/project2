package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable // Đánh dấu class là bất biến (immutable), không thể thay đổi sau khi khởi tạo
@Entity // Đánh dấu class là một entity trong Room Database
data class Song(
    @PrimaryKey val id: String, // ID của bài hát
    val title: String, // Tên bài hát
    val artistsText: String? = null, // Tên nghệ sĩ của bài hát
    val durationText: String?, // Thời gian bài hát
    val thumbnailUrl: String?, // URL của ảnh bìa bài hát
    val likedAt: Long? = null, // Thời gian bài hát được thích (thích = like)
    val totalPlayTimeMs: Long = 0, // Tổng thời gian phát bài hát (tính bằng mili giây)
    val loudnessBoost: Float? = null, // Độ lớn âm thanh của bài hát
    @ColumnInfo(defaultValue = "false") // Đánh dấu trường này là không có giá trị mặc định
    val blacklisted: Boolean = false, // Đánh dấu bài hát là bị chặn (blacklisted)
    @ColumnInfo(defaultValue = "false") // Đánh dấu trường này là không có giá trị mặc định
    val explicit: Boolean = false, // Đánh dấu bài hát là có nội dung nhạy cảm (explicit)
    @ColumnInfo(defaultValue = "false") // Thêm cột mới
    val isDownloaded: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") // Thêm cột mới
    val downloadPath: String? = null
) {
    fun toggleLike() = copy(likedAt = if (likedAt == null) System.currentTimeMillis() else null) // Hàm này sẽ sao chép đối tượng hiện tại và thay đổi trạng thái likedAt
}
