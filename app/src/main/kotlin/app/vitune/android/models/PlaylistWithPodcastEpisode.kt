package app.vitune.android.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.Relation

@Entity
data class PlaylistWithPodcastEpisode(
    @Embedded val playlist: PodcastPlaylist,
    @Relation(
        parentColumn = "id",
        entityColumn = "videoId",
        associateBy = Junction(
            value = PodcastEpisodePlaylistMap::class,
            parentColumn = "playlistId",
            entityColumn = "episodeId"
        )
    )
    val episodes: List<PodcastEpisodeEntity>
)