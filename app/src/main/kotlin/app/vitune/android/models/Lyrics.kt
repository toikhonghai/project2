package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Immutable
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Song::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE // Xóa tất cả các bản ghi liên quan trong bảng Lyrics khi bản ghi trong bảng Song bị xóa
        )
    ]
)
data class Lyrics(
    @PrimaryKey val songId: String,
    val fixed: String?, // Lời bài hát đã sửa lỗi
    val synced: String?, // Lời bài hát đã đồng bộ hóa
    val startTime: Long? = null
)
