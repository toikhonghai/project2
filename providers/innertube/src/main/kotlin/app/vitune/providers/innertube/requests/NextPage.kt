package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.ContinuationResponse
import app.vitune.providers.innertube.models.NextResponse
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.utils.from
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// đây là một đoạn mã Kotlin sử dụng thư viện Ktor để thực hiện các yêu cầu HTTP đến API của Innertube.
// Nó định nghĩa một hàm mở rộng cho lớp Innertube, có tên là nextPage.
// Hàm này được sử dụng để lấy thông tin về trang tiếp theo trong danh sách phát hoặc hàng đợi nhạc của YouTube Music.
suspend fun Innertube.nextPage(body: NextBody): Result<Innertube.NextPage>? =
    runCatchingCancellable {
        val response = client.post(NEXT) {
            setBody(body)
            @Suppress("all") // Bỏ qua cảnh báo về việc không sử dụng biến
            mask(
                "contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer.content.musicQueueRenderer.content.playlistPanelRenderer(continuations,contents(automixPreviewVideoRenderer,$PLAYLIST_PANEL_VIDEO_RENDERER_MASK))"
            )
        }.body<NextResponse>() // Gọi hàm body để lấy phản hồi từ yêu cầu HTTP và chuyển đổi nó thành đối tượng NextResponse.

        val tabs = response
            .contents
            ?.singleColumnMusicWatchNextResultsRenderer // Lấy danh sách các phần trong phản hồi.
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs

        val playlistPanelRenderer = tabs
            ?.getOrNull(0)
            ?.tabRenderer
            ?.content
            ?.musicQueueRenderer
            ?.content
            ?.playlistPanelRenderer // thông tin về danh sách phát trong hàng đợi nhạc

        if (body.playlistId == null) {
            val endpoint = playlistPanelRenderer
                ?.contents
                ?.lastOrNull()
                ?.automixPreviewVideoRenderer
                ?.content
                ?.automixPlaylistVideoRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint // Điều hướng đến danh sách phát

            if (endpoint != null) return nextPage(
                body.copy(
                    playlistId = endpoint.playlistId,
                    params = endpoint.params
                )
            )
        }

        Innertube.NextPage( // Trả về một đối tượng NextPage chứa thông tin về danh sách phát
            playlistId = body.playlistId,
            playlistSetVideoId = body.playlistSetVideoId,
            params = body.params,
            itemsPage = playlistPanelRenderer
                ?.toSongsPage() // Chuyển đổi danh sách phát thành một trang bài hát
        )
    }

// Hàm này được sử dụng để lấy thông tin về trang tiếp theo trong danh sách phát hoặc hàng đợi nhạc của YouTube Music.
suspend fun Innertube.nextPage(body: ContinuationBody) = runCatchingCancellable {
    val response = client.post(NEXT) {
        setBody(body)
        @Suppress("all")
        mask(
            "continuationContents.playlistPanelContinuation(continuations,contents.$PLAYLIST_PANEL_VIDEO_RENDERER_MASK)"
        )
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.playlistPanelContinuation
        ?.toSongsPage() // Chuyển đổi danh sách phát thành một trang bài hát
}

// Chuyển đổi một PlaylistPanelRenderer thành một trang bài hát
private fun NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer?.toSongsPage() =
    Innertube.ItemsPage( // Trả về một đối tượng ItemsPage chứa thông tin về danh sách bài hát
        items = this
            ?.contents
            ?.mapNotNull(
                NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer.Content
                ::playlistPanelVideoRenderer // Chuyển đổi danh sách phát thành một trang bài hát
            )?.mapNotNull(Innertube.SongItem::from),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )
