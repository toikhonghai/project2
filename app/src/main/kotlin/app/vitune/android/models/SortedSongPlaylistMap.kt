package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@Immutable
@DatabaseView("SELECT * FROM SongPlaylistMap ORDER BY position") // Tạo một DatabaseView để truy vấn dữ liệu từ bảng SongPlaylistMap và sắp xếp theo vị trí
data class SortedSongPlaylistMap(
    @ColumnInfo(index = true) val songId: String,
    @ColumnInfo(index = true) val playlistId: Long,
    val position: Int
)
