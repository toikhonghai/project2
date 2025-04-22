package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/*
Đây là một bảng dùng để lưu lại danh sách các bài hát đang chờ phát (queue),
giống như khi bạn thêm bài hát vào hàng đợi trong Spotify hoặc YouTube Music.
 */
@Immutable
@Entity
class QueuedMediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val mediaItem: MediaItem, // BLOB là kiểu dữ liệu nhị phân lớn, dùng để lưu trữ các đối tượng phức tạp như MediaItem
    var position: Long? // Vị trí phát bài hát trong hàng đợi (queue), có thể là null nếu không xác định
)
