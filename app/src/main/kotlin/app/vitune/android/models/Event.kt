package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
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
            onDelete = ForeignKey.CASCADE
        )
    ]
)
// lớp này lưu trữ thông tin về các sự kiện phát nhạc
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val songId: String,
    val timestamp: Long, // thời gian xảy ra sự kiện
    val playTime: Long, // thời gian phát nhạc (tính bằng giây) tại thời điểm sự kiện xảy ra
    val entityType: String
)
