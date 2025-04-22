package app.vitune.android.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant

@Entity(tableName = "podcast_subscriptions")
data class PodcastSubscriptionEntity(
    @PrimaryKey
    val browseId: String,
    val channelId: String?,
    val subscribedAt: Long = System.now().epochSeconds,
    val notificationEnabled: Boolean = false
)
