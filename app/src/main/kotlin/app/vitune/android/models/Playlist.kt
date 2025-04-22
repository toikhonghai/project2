package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // autoGenerate = true: tự động tăng giá trị id khi thêm bản ghi mới
    val name: String,
    val browseId: String? = null,
    val thumbnail: String? = null
)
