package app.vitune.android.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

@Entity
data class EventWithPodcastEpisode(
    @Embedded val event: Event,
    @Relation(parentColumn = "songId", entityColumn = "videoId") val episode: PodcastEpisodeEntity
)