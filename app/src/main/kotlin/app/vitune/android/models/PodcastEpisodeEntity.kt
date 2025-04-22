package app.vitune.android.models

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import app.vitune.android.Converters
import app.vitune.providers.innertube.Innertube

@Entity(
    tableName = "podcast_episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["browseId"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["podcastId"])]
)
@TypeConverters(Converters::class)
data class PodcastEpisodeEntity(
    @PrimaryKey
    val videoId: String,

    val podcastId: String,

    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val durationText: String?,
    val publishedTimeText: String?,

    val playPositionMs: Long = 0,

    val durationMs: Long? = null,

    val isCompleted: Boolean = false,

    val isDownloaded: Boolean = false,

    val downloadPath: String? = null,

    val lastPlayed: Long? = null,

    val customMetadata: Map<String, String>? = null
){
    companion object {
        fun fromPodcastEpisodeItem(episode: Innertube.PodcastEpisodeItem, podcastId: String): PodcastEpisodeEntity {
            return PodcastEpisodeEntity(
                videoId = episode.info?.endpoint?.videoId.toString(),
                podcastId = podcastId,
                title = episode.info?.name.toString(),
                description = episode.description,
                thumbnailUrl = episode.thumbnail?.url,
                durationText = episode.durationText,
                publishedTimeText = episode.publishedTimeText
            )
        }
    }
    fun toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(podcastId)
            .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
            .setExtras(Bundle().apply {
                putString("durationText", durationText)
                putBoolean("isPodcast", true)
            })
            .build()
        val uri = if (isDownloaded && downloadPath != null) {
            Uri.parse(downloadPath)
        } else {
            Uri.parse("https://youtubei.googleapis.com/youtubei/v1/player?videoId=$videoId")
        }
        return MediaItem.Builder()
            .setMediaId(videoId)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
