package app.vitune.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service(){
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoId = intent?.getStringExtra("videoId") ?: return START_NOT_STICKY
        val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
        ServiceNotifications.podcastDownload.startForeground(this) {
            setContentTitle(getString(R.string.downloading_podcast))
            setContentText(videoId)
            setSmallIcon(R.drawable.download)
            setOngoing(true)
        }
        scope.launch {
            downloadPodcastEpisode(videoId, url)?.onSuccess { path ->
                Database.updateEpisodeDownloadStatus(videoId, isDownloaded = true, downloadPath = path)
                ServiceNotifications.podcastDownload.cancel(this@DownloadService)
                stopSelf()
            }?.onFailure {
                ServiceNotifications.podcastDownload.sendNotification(this@DownloadService) {
                    setContentTitle(getString(R.string.download_failed))
                    setContentText(it.message)
                    setSmallIcon(R.drawable.alert_circle)
                    setAutoCancel(true)
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun downloadPodcastEpisode(videoId: String, url: String) : Result<String?>? = runCatchingCancellable{
        val file = File(getExternalFilesDir("podcasts")?.also { it.mkdirs() }, "$videoId.mp3")
        Innertube.client.get(url).body<ByteReadChannel>().toByteArray().also { bytes ->
            file.writeBytes(bytes)
        }
        file.absolutePath
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        fun startDownload(context: Context, videoId: String, url: String){
            val intent = Intent(context, DownloadService::class.java).apply{
                putExtra("videoId", videoId)
                putExtra("url", url)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}