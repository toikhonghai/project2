package app.vitune.android.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.vitune.android.Database
import app.vitune.android.MainActivity
import app.vitune.android.R
import app.vitune.android.models.PodcastEntity
import app.vitune.android.models.PodcastEpisodeEntity
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.requests.loadPodcastPage
import app.vitune.providers.utils.runCatchingCancellable
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class PodcastSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @OptIn(UnstableApi::class)
    override suspend fun doWork(): Result = runCatchingCancellable {
        Log.d("PodcastSyncWorker", "Bắt đầu đồng bộ podcast")
        val subscribedPodcasts = Database.getSubscribedPodcasts().first()

        subscribedPodcasts.forEach { podcast ->
            if (!Database.isSubscribed(podcast.browseId).first()) {
                Log.d("PodcastSyncWorker", "Podcast ${podcast.title} không được đăng ký, bỏ qua")
                return@forEach
            }

            val existingEpisodes = Database.getEpisodesForPodcastAsFlow(podcast.browseId).first()
                .map { it.videoId }.toSet()

            Innertube.loadPodcastPage(podcast.browseId)?.onSuccess { page ->
                val episodes = page.episodes?.items?.map {
                    PodcastEpisodeEntity.fromPodcastEpisodeItem(it, podcast.browseId)
                } ?: emptyList()

                val newEpisodes = episodes.filter { it.videoId !in existingEpisodes }
                if (newEpisodes.isEmpty()) {
                    Log.d("PodcastSyncWorker", "Không có tập mới cho ${podcast.title}")
                    return@onSuccess
                }

                Database.insertEpisodes(newEpisodes)
                Log.d("PodcastSyncWorker", "Đã thêm ${newEpisodes.size} tập mới cho ${podcast.title}")

                Database.updatePodcast(
                    podcast.copy(
                        episodeCount = (page.episodeCount ?: 0) + newEpisodes.size,
                        lastUpdated = System.currentTimeMillis() / 1000
                    )
                )

                if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                    sendNewEpisodeNotification(applicationContext, podcast, newEpisodes)
                } else {
                    Log.d("PodcastSyncWorker", "Thông báo bị tắt cho ứng dụng, bỏ qua thông báo cho ${podcast.title}")
                }
            }?.onFailure { e ->
                Log.e("PodcastSyncWorker", "Đồng bộ podcast ${podcast.title} thất bại: ${e.message}", e)
            }
        }
        Result.success()
    }?.getOrElse {
        Log.e("PodcastSyncWorker", "Đồng bộ thất bại: ${it.message}", it)
        Result.retry()
    } ?: Result.retry()

    @OptIn(UnstableApi::class)
    private suspend fun sendNewEpisodeNotification(
        context: Context,
        podcast: PodcastEntity,
        newEpisodes: List<PodcastEpisodeEntity>
    ) {
        if (newEpisodes.isEmpty() || !Database.isSubscribed(podcast.browseId).first()) {
            Log.d("PodcastSyncWorker", "Không gửi thông báo: Không có tập mới hoặc podcast ${podcast.title} không được đăng ký")
            return
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val latestEpisode = newEpisodes.maxByOrNull { it.publishedTimeText?.let { parseTime(it) } ?: 0L }

        val notificationText = buildString {
            append(latestEpisode?.title ?: "Tập mới")
            if (newEpisodes.size > 1) {
                append(" và ${newEpisodes.size - 1} tập khác")
            }
            latestEpisode?.publishedTimeText?.let {
                append("\nPhát hành: ${dateFormat.format(Date(parseTime(it)))}")
            }
        }

        ServiceNotifications.podcastNewEpisodes.sendNotification(context) {
            setSmallIcon(R.drawable.podcast)
            setContentTitle("${podcast.title} có tập mới")
            setContentText(notificationText)
            setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    podcast.browseId.hashCode(),
                    Intent(context, MainActivity::class.java).apply {
                        putExtra("podcastId", podcast.browseId)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            setAutoCancel(true)
        }
        Log.d("PodcastSyncWorker", "Đã gửi thông báo cho ${newEpisodes.size} tập mới của ${podcast.title}")
    }

    @OptIn(UnstableApi::class)
    private fun parseTime(publishedTimeText: String): Long {
        return try {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            if (publishedTimeText.contains("ago")) {
                val number = publishedTimeText.split(" ")[0].toLongOrNull() ?: 0L
                val unit = publishedTimeText.split(" ")[1]
                val millis = when {
                    unit.startsWith("day") -> number * 24 * 60 * 60 * 1000
                    unit.startsWith("hour") -> number * 60 * 60 * 1000
                    else -> 0L
                }
                System.currentTimeMillis() - millis
            } else {
                dateFormat.parse(publishedTimeText)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e("PodcastSyncWorker", "Không thể phân tích thời gian: $publishedTimeText", e)
            System.currentTimeMillis()
        }
    }
}