package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Immutable
@Entity(
    primaryKeys = ["songId", "albumId"],
    foreignKeys = [
        ForeignKey(
            entity = Song::class, // Tham chiếu đến entity Song
            parentColumns = ["id"], // Cột khóa chính trong bảng cha (Song)
            childColumns = ["songId"], // Cột khóa ngoại trong bảng con (SongAlbumMap)
            onDelete = ForeignKey.CASCADE // Xóa tất cả các bản ghi trong bảng con khi bản ghi trong bảng cha bị xóa
        ),
        ForeignKey(
            entity = Album::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SongAlbumMap( // Lớp này ánh xạ giữa bài hát và album
    @ColumnInfo(index = true) val songId: String, // COlumnInfo đánh dấu cột này là khóa ngoại
    @ColumnInfo(index = true) val albumId: String,
    val position: Int?
)
