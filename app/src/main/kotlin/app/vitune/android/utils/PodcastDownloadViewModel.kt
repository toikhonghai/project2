package app.vitune.android.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vitune.android.Database
import app.vitune.android.models.PodcastEpisodeEntity
import app.vitune.android.service.DownloadService
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.Context as InnertubeContext // Alias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.media3.common.util.Log
import app.vitune.android.models.PodcastEntity
import app.vitune.providers.innertube.requests.getPodcastStreamUrl
import app.vitune.providers.innertube.requests.loadPodcastMetadata

class PodcastDownloadViewModel : ViewModel() {

    fun downloadPodcastEpisode(androidContext: Context, mediaItem: androidx.media3.common.MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val innertubeContext = InnertubeContext(
                    client = InnertubeContext.Client(
                        clientName = "WEB_REMIX",
                        clientVersion = "1.20241028.01.00"
                    ),
                    user = InnertubeContext.User()
                )

                val streamUrl = Innertube.getPodcastStreamUrl(innertubeContext, mediaItem.mediaId)
                    ?: throw Exception("Không tìm thấy URL luồng podcast")

                val podcastId = mediaItem.mediaMetadata.extras?.getString("podcastId") ?: throw Exception("Podcast ID không tìm thấy")

                // BƯỚC MỚI: Đảm bảo PodcastEntity (podcast cha) tồn tại
                val existingPodcast = Database.getPodcastById(podcastId)
                if (existingPodcast == null) {
                    // Lấy thông tin podcast từ API nếu chưa có trong DB
                    val podcastMetadata = Innertube.loadPodcastMetadata(podcastId)?.getOrNull()
                    if (podcastMetadata != null) {
                        val newPodcast = PodcastEntity(
                            browseId = podcastId,
                            title = podcastMetadata.title ?: "Unknown Podcast",
                            description = podcastMetadata.description,
                            authorName = podcastMetadata.author?.name,
                            authorBrowseId = podcastMetadata.author?.endpoint?.browseId,
                            thumbnailUrl = podcastMetadata.thumbnail?.url,
                            episodeCount = podcastMetadata.episodeCount,
                            playlistId = podcastMetadata.playlistId,
                            lastUpdated = System.currentTimeMillis()
                        )
                        Database.insertPodcast(newPodcast)
                        Log.d("PodcastDownloadVM", "Đã chèn Podcast mới: ${newPodcast.title}")
                    }
                } else {
                    // Cập nhật thông tin podcast nếu cần (ví dụ: cập nhật playlistId nếu mới lấy được)
                    // Hoặc chỉ đơn giản là chạm vào để cập nhật lastUpdated
                    Database.updatePodcast(existingPodcast.copy(lastUpdated = System.currentTimeMillis()))
                    Log.d("PodcastDownloadVM", "Đã cập nhật Podcast đã tồn tại: ${existingPodcast.title}")
                }

                // Kiểm tra và chèn/cập nhật PodcastEpisodeEntity vào DB trước khi tải
                val existingEpisode = Database.getEpisodeById(mediaItem.mediaId)
                if (existingEpisode == null) {
                    Database.insertEpisode(
                        PodcastEpisodeEntity(
                            videoId = mediaItem.mediaId,
                            podcastId = mediaItem.mediaMetadata.extras?.getString("podcastId") ?: "",
                            title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                            thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
                            durationText = mediaItem.mediaMetadata.extras?.getString("durationText") ?: "0:00",
                            description = null,
                            publishedTimeText = null
                        )
                    )
                } else {
                    Database.updateEpisodeMetadata(
                        videoId = mediaItem.mediaId,
                        title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                        description = existingEpisode.description, // Giữ lại description cũ nếu không có cái mới
                        thumbnailUrl = mediaItem.mediaMetadata.artworkUri?.toString(),
                        durationText = mediaItem.mediaMetadata.extras?.getString("durationText") ?: "0:00",
                        publishedTimeText = existingEpisode.publishedTimeText // Giữ lại publishedTimeText cũ
                    )
                }

                DownloadService.startDownload(androidContext.applicationContext, mediaItem.mediaId, streamUrl)
                withContext(Dispatchers.Main) {
                    androidContext.toast("Đã bắt đầu tải podcast")
                }
            } catch (e: Exception) {
                Log.e("PodcastDownloadVM", "Tải podcast thất bại: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    androidContext.toast("Tải podcast thất bại: ${e.message}")
                }
            }
        }
    }
}