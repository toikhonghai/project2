package app.vitune.android.models

import androidx.compose.runtime.Immutable

@Immutable
data class PlaylistPreview( // lớp dữ liệu dùng để đại diện cho một bản tóm tắt của playlist,
    val id: Long,
    val name: String,
    val songCount: Int,
    val thumbnail: String?
) {
    val playlist by lazy {
        Playlist(
            id = id,
            name = name,
            browseId = null
        )
    }
}
