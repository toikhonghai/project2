package app.vitune.android.models.ui

import android.os.Bundle
import androidx.media3.common.MediaItem
import app.vitune.core.ui.utils.songBundle

data class UiMedia(
    val id: String,        // ID của media item
    val title: String,     // Tiêu đề của bài hát hoặc video
    val artist: String,    // Tên nghệ sĩ
    val duration: Long,    // Thời lượng của media (tính bằng mili giây)
    val explicit: Boolean,  // Nếu bài hát có chứa nội dung người lớn (explicit)
    val extras: Bundle? = null
)

fun MediaItem.toUiMedia(duration: Long) = UiMedia( // Hàm mở rộng để chuyển đổi MediaItem thành UiMedia
    id = mediaId,  // ID của MediaItem
    title = mediaMetadata.title?.toString().orEmpty(),  // Lấy tiêu đề của bài hát, nếu không có thì trả về chuỗi rỗng
    artist = mediaMetadata.artist?.toString().orEmpty(),  // Lấy tên nghệ sĩ, nếu không có thì trả về chuỗi rỗng
    duration = duration,  // Thời gian của media item
    explicit = mediaMetadata.extras?.songBundle?.explicit == true,  // Kiểm tra xem bài hát có phải là explicit không
    extras = mediaMetadata.extras
)

