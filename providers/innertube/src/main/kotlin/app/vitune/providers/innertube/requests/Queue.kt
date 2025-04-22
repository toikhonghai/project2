package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.GetQueueResponse
import app.vitune.providers.innertube.models.bodies.QueueBody
import app.vitune.providers.innertube.utils.from
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// đây là một đoạn mã Kotlin sử dụng thư viện Ktor để gửi yêu cầu HTTP POST đến một API của Innertube.
// Nó định nghĩa một hàm mở rộng cho lớp Innertube, có tên là queue.
// Hàm này được sử dụng để lấy thông tin về hàng đợi nhạc (queue) của YouTube Music.
suspend fun Innertube.queue(body: QueueBody) = runCatchingCancellable {
    val response = client.post(QUEUE) {
        setBody(body)
        mask("queueDatas.content.$PLAYLIST_PANEL_VIDEO_RENDERER_MASK")
    }.body<GetQueueResponse>()

    response
        .queueData
        ?.mapNotNull { queueData ->
            queueData
                .content
                ?.playlistPanelVideoRenderer
                ?.let(Innertube.SongItem::from) // Chuyển đổi nội dung thành đối tượng SongItem
        }
}

// Hàm này được sử dụng để lấy thông tin về hàng đợi nhạc (queue) của YouTube Music.
suspend fun Innertube.song(videoId: String): Result<Innertube.SongItem?>? =
    queue(QueueBody(videoIds = listOf(videoId)))?.map { it?.firstOrNull() } //
        // Gọi hàm queue với videoId và lấy phần tử đầu tiên trong danh sách kết quả.
