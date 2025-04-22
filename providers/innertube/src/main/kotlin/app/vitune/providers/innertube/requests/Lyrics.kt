package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.BrowseResponse
import app.vitune.providers.innertube.models.NextResponse
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// đây là một đoạn mã Kotlin sử dụng thư viện Ktor để gửi yêu cầu HTTP POST đến một API của Innertube.
// Nó định nghĩa một hàm mở rộng cho lớp Innertube, có tên là lyrics.
// Hàm này được sử dụng để lấy lời bài hát từ YouTube Music API.
suspend fun Innertube.lyrics(body: NextBody) = runCatchingCancellable {
    val nextResponse = client.post(NEXT) {
        setBody(body)
        @Suppress("all")
        mask(
            "contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer(endpoint,title)"
        )
    }.body<NextResponse>()

    // Lấy browseId từ phản hồi.
    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.getOrNull(1)
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: return@runCatchingCancellable null // Nếu không tìm thấy browseId, trả về null.

    // Gửi yêu cầu đến API của YouTube Music để lấy thông tin mô tả bài hát dựa trên browseId.
    val response = client.post(BROWSE) {
        setBody(BrowseBody(browseId = browseId))
        mask("contents.sectionListRenderer.contents.musicDescriptionShelfRenderer.description")
    }.body<BrowseResponse>()

    // Lấy mô tả bài hát từ phản hồi.
    response.contents
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.musicDescriptionShelfRenderer
        ?.description
        ?.text
}
