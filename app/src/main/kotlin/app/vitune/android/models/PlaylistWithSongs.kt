package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    /*
    @Relation giúp xác định mối quan hệ giữa các bảng trong Room. Cụ thể ở đây,
    @Relation được dùng để chỉ rằng có một quan hệ một-nhiều giữa bảng Playlist và bảng Song,
    thông qua bảng trung gian SortedSongPlaylistMap.
     */
    @Relation(
        entity = Song::class, // bảng mà bạn muốn lấy dữ liệu từ đó
        parentColumn = "id", // cột khóa chính của bảng Playlist
        entityColumn = "id", // cột khóa chính của bảng Song
        associateBy = Junction( // Junction được sử dụng khi bạn có mối quan hệ nhiều-nhiều giữa các bảng,
            // và bạn cần một bảng trung gian để lưu trữ mối quan hệ đó.
            value = SortedSongPlaylistMap::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<Song> // Danh sách bài hát trong playlist này
)
