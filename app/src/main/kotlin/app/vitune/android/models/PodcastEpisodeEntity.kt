package app.vitune.android.models

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import app.vitune.android.Converters
import app.vitune.android.Database
import app.vitune.providers.innertube.Innertube
import kotlinx.coroutines.runBlocking

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

    val customMetadata: Map<String, String>? = null,

    val likedAt: Long? = null
) {
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

    fun toggleLike() = copy(likedAt = if (likedAt == null) System.currentTimeMillis() else null)

    @OptIn(UnstableApi::class)
    fun asMediaItem(): MediaItem {
        val podcast = runBlocking { Database.getPodcastById(podcastId) }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(podcast?.title ?: podcastId)
            .setArtworkUri(thumbnailUrl?.let { Uri.parse(it) })
            .setExtras(
                Bundle().apply {
                    putString("durationText", durationText)
                    putBoolean("isPodcast", true)
                    putString("podcastId", podcastId)
                    putBoolean("isDownloaded", isDownloaded)
                }
            )
            .build()

        val uri = if (isDownloaded && downloadPath != null) {
            Uri.parse(downloadPath)
        } else {
            Uri.parse("https://www.youtube.com/watch?v=$videoId")
        }

        return MediaItem.Builder()
            .setMediaId(videoId)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .setCustomCacheKey(videoId)
            .build()
    }
}