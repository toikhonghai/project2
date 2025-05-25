package app.vitune.android.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.Log
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.requests.getPodcastStreamUrl
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.seconds
import app.vitune.providers.innertube.models.Context as InnertubeContext // Alias

class DownloadService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoId = intent?.getStringExtra("videoId") ?: return START_NOT_STICKY
        val initialUrl = intent.getStringExtra("url") ?: return START_NOT_STICKY

        ServiceNotifications.podcastDownload.startForeground(this) {
            setContentTitle(getString(R.string.downloading_podcast))
            setContentText(videoId)
            setSmallIcon(R.drawable.download)
            setOngoing(true)
        }

        scope.launch {
            var currentUrl = initialUrl
            var retryCount = 0
            val maxRetries = 2

            while (retryCount <= maxRetries) {
                val result = downloadPodcastEpisode(videoId, currentUrl)
                if (result?.isSuccess == true) {
                    result.onSuccess { path ->
                        Database.updateEpisodeDownloadStatus(videoId, isDownloaded = true, downloadPath = path)
                        Log.d("DownloadService", "Podcast tải thành công: $videoId, lưu tại: $path")
                        withContext(Dispatchers.Main) {
                            ServiceNotifications.podcastDownload.cancel(this@DownloadService)
                            stopSelf()
                        }
                        return@launch
                    }
                } else {
                    result?.onFailure { exception ->
                        Log.e("DownloadService", "Lỗi khi tải podcast: ${exception.message}", exception)
                        if (exception is ClientRequestException && exception.response.status == HttpStatusCode.Forbidden) {
                            Log.w("DownloadService", "Gặp lỗi 403 Forbidden, thử lấy URL mới...")
                            retryCount++
                            if (retryCount <= maxRetries) {
                                val newStreamUrl = Innertube.getPodcastStreamUrl(
                                    context = InnertubeContext(
                                        client = InnertubeContext.Client(
                                            clientName = "WEB_REMIX",
                                            clientVersion = "1.20241028.01.00",
                                            gl = "VN",
                                            hl = "vi"
                                        ),
                                        user = InnertubeContext.User()
                                    ),
                                    videoId = videoId
                                )
                                if (newStreamUrl != null) {
                                    currentUrl = newStreamUrl
                                    Log.d("DownloadService", "Đã lấy được URL mới, thử lại...")
                                    kotlinx.coroutines.delay(1.seconds)
                                } else {
                                    Log.e("DownloadService", "Không thể lấy URL mới sau lỗi 403.")
                                    break
                                }
                            }
                        } else {
                            // Lỗi khác không phải 403, không retry URL
                            break
                        }
                    }
                }
            }

            // Nếu thoát khỏi vòng lặp mà chưa thành công
            withContext(Dispatchers.Main) {
                ServiceNotifications.podcastDownload.sendNotification(this@DownloadService) {
                    setContentTitle(getString(R.string.download_failed))
                    setContentText("Tải xuống thất bại sau nhiều lần thử.")
                    setSmallIcon(R.drawable.alert_circle)
                    setAutoCancel(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun downloadPodcastEpisode(videoId: String, url: String): Result<String?>? = runCatchingCancellable {
        val file = File(getExternalFilesDir("podcasts")?.also { it.mkdirs() }, "$videoId.mp3")
        // Sử dụng client.get(url) trực tiếp, nó đã có OriginInterceptor
        Innertube.client.get(url).body<ByteReadChannel>().toByteArray().also { bytes ->
            file.writeBytes(bytes)
        }
        file.absolutePath
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun startDownload(context: Context, videoId: String, url: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra("videoId", videoId)
                putExtra("url", url)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}