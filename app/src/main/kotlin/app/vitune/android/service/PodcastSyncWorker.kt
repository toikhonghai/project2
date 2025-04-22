package app.vitune.android.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
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

class PodcastSyncWorker(context: Context, params: WorkerParameters): CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatchingCancellable {
        val podcasts = Database.getSubscribedPodcasts().first()
        podcasts.forEach { podcast ->
            Innertube.loadPodcastPage(podcast.browseId)?.onSuccess { page ->
                val episodes = page.episodes?.items?.map { PodcastEpisodeEntity.fromPodcastEpisodeItem(it, podcast.browseId) }
                episodes?.let { Database.insertEpisodes(it) }
                Database.updatePodcast(podcast.copy(
                    episodeCount = episodes?.size,
                    lastUpdated = System.currentTimeMillis()/1000
                ))
                if(episodes?.isNotEmpty() == true){
                    sendNewEpisodeNotification(applicationContext, podcast, episodes)
                }
            }
        }
        Result.success()
    }?.getOrElse {
        Result.retry()
    } ?: Result.retry() // fallback nếu bị CancellationException
    private suspend fun sendNewEpisodeNotification(context: Context, podcast: PodcastEntity, newEpisodes: List<PodcastEpisodeEntity>) {
        if (newEpisodes.isEmpty() || !Database.isSubscribed(podcast.browseId).first()) return
        ServiceNotifications.podcastNewEpisodes.sendNotification(context) {
            setSmallIcon(R.drawable.podcast)
            setContentTitle("${podcast.title} has ${newEpisodes.size} new episodes")
            setContentText(newEpisodes.first().title)
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        putExtra("podcastId", podcast.browseId)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            setAutoCancel(true)
        }
    }
}