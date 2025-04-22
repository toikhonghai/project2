package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable // Đánh dấu lớp là bất biến
@Entity
data class Album(
    @PrimaryKey val id: String,
    val title: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null, // URL của hình thu nhỏ
    val year: String? = null,
    val authorsText: String? = null,
    val shareUrl: String? = null,
    val timestamp: Long? = null, // Thời gian tạo album
    val bookmarkedAt: Long? = null, // Thời gian đánh dấu album
    val otherInfo: String? = null // Thông tin khác về album
)
