package app.vitune.android.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System

@Entity(tableName = "podcast_subscriptions")
data class PodcastSubscriptionEntity(
    @PrimaryKey
    val browseId: String,
    val channelId: String?, // Sử dụng để lưu playlistId
    val subscribedAt: Long = System.now().epochSeconds,
    val notificationEnabled: Boolean = false
)