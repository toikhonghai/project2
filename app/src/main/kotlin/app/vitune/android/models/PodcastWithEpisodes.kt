package app.vitune.android.models

import androidx.room.Embedded
import androidx.room.Relation

data class PodcastWithEpisodes(
    @Embedded
    val podcast: PodcastEntity,

    @Relation(
        parentColumn = "browseId",
        entityColumn = "podcastId"
    )
    val episodes: List<PodcastEpisodeEntity>
)
