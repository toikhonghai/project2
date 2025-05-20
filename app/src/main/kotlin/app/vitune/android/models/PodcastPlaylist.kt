package app.vitune.android.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "PodcastPlaylist")
data class PodcastPlaylist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val thumbnail: String? = null
)

@Entity(
    tableName = "PodcastEpisodePlaylistMap",
    foreignKeys = [
        ForeignKey(
            entity = PodcastPlaylist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PodcastEpisodeEntity::class,
            parentColumns = ["videoId"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["playlistId", "episodeId"]
)
data class PodcastEpisodePlaylistMap(
    val playlistId: Long,
    val episodeId: String,
    val position: Int
)

data class PodcastPlaylistPreview(
    val id: Long, // Thêm id
    val name: String, // Thêm name
    val episodeCount: Int,
    val thumbnail: String?
)