package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Immutable
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Song::class, // Bảng cha là bảng Song
            parentColumns = ["id"], //  Khóa chính của bảng cha (bảng Song)
            childColumns = ["songId"], // Trường trong bảng hiện tại (Format) dùng để tạo liên kết.
            onDelete = ForeignKey.CASCADE // Xóa tất cả các bản ghi trong bảng hiện tại khi bản ghi trong bảng cha bị xóa
        )
    ]
)
data class Format( // Lớp này lưu trữ thông tin về định dạng của bài hát
    @PrimaryKey val songId: String, // Khóa chính của bài hát
    val itag: Int? = null, // Mã định dạng của bài hát
    val mimeType: String? = null, // Kiểu MIME của bài hát
    val bitrate: Long? = null, // Tốc độ bit của bài hát
    val contentLength: Long? = null, // Độ dài nội dung của bài hát
    val lastModified: Long? = null,// Thời gian sửa đổi cuối cùng của bài hát
    val loudnessDb: Float? = null // Độ lớn âm thanh của bài hát (tính bằng decibel
)
