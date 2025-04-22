package app.vitune.android.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.vitune.providers.innertube.Innertube
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val browseId: String,

    val title: String,
    val description: String?,
    val authorName: String?,
    val authorBrowseId: String?,
    val thumbnailUrl: String?,
    val episodeCount: Int?,

    val lastUpdated: Long = System.now().epochSeconds
){
    companion object {
        fun fromPodcastItem(item: Innertube.PodcastItem): PodcastEntity{
            return PodcastEntity(
                browseId = item.info?.endpoint?.browseId.toString(),
                title = item.info?.name.toString(),
                description = item.description,
                authorName = item.authors?.firstOrNull()?.name,
                authorBrowseId = item.authors?.firstOrNull()?.endpoint?.browseId,
                thumbnailUrl = item.thumbnail?.url,
                episodeCount = item.episodeCount
            )
        }
    }
}
