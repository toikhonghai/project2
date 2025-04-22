package app.vitune.android.preferences

import app.vitune.android.GlobalPreferencesHolder
import app.vitune.core.data.enums.AlbumSortBy
import app.vitune.core.data.enums.ArtistSortBy
import app.vitune.core.data.enums.PlaylistSortBy
import app.vitune.core.data.enums.SongSortBy
import app.vitune.core.data.enums.SortOrder

// đây là một file Kotlin chứa các thuộc tính và phương thức để quản lý các tùy chọn sắp xếp cho các đối tượng như bài hát, album, nghệ sĩ và danh sách phát trong ứng dụng Vitune.
object OrderPreferences : GlobalPreferencesHolder() {
    var songSortOrder by enum(SortOrder.Descending)
    var localSongSortOrder by enum(SortOrder.Descending)
    var playlistSortOrder by enum(SortOrder.Descending)
    var albumSortOrder by enum(SortOrder.Descending)
    var artistSortOrder by enum(SortOrder.Descending)

    var songSortBy by enum(SongSortBy.DateAdded)
    var localSongSortBy by enum(SongSortBy.DateAdded)
    var playlistSortBy by enum(PlaylistSortBy.DateAdded)
    var albumSortBy by enum(AlbumSortBy.DateAdded)
    var artistSortBy by enum(ArtistSortBy.DateAdded)
}
