package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.ContinuationResponse
import app.vitune.providers.innertube.models.MusicShelfRenderer
import app.vitune.providers.innertube.models.SearchResponse
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.models.bodies.SearchBody
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// Gửi một yêu cầu HTTP để lấy thông tin trang tìm kiếm từ YouTube Music API và
// chuyển đổi phản hồi thành một đối tượng Innertube.ItemsPage.
// Hàm này sử dụng Generics để cho phép kiểu dữ liệu (T) có thể thay đổi linh hoạt nhưng vẫn bị ràng buộc (bounded) bởi một kiểu cụ thể (Innertube.Item)
// T : Innertube.Item: Đây là một ràng buộc (constraint),
// có nghĩa là T phải kế thừa (:) từ Innertube.Item hoặc là một subclass của nó.
// Hàm này sẽ trả về một đối tượng Result chứa một trang tìm kiếm với các mục được chuyển đổi từ MusicShelfRenderer.
suspend fun <T : Innertube.Item> Innertube.searchPage(
    body: SearchBody,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingCancellable {
    val response = client.post(SEARCH) {
        setBody(body)
        @Suppress("all")
        mask(
            "contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)"
        )
    }.body<SearchResponse>()

    response
        .contents
        ?.tabbedSearchResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.lastOrNull()
        ?.musicShelfRenderer
        ?.toItemsPage(fromMusicShelfRendererContent) // Chuyển đổi nội dung của MusicShelfRenderer thành một trang tìm kiếm
}

// Gửi một yêu cầu HTTP để lấy thông tin trang tìm kiếm từ YouTube Music API và
suspend fun <T : Innertube.Item> Innertube.searchPage(
    body: ContinuationBody, // // Sử dụng ContinuationBody để xác định ngữ cảnh và các tham số khác cho yêu cầu.
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T? // Chuyển đổi nội dung của MusicShelfRenderer thành một trang tìm kiếm
) = runCatchingCancellable {
    val response = client.post(SEARCH) {
        setBody(body)
        @Suppress("all")
        mask(
            "continuationContents.musicShelfContinuation(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)"
        )
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toItemsPage(fromMusicShelfRendererContent)
}

// Chuyển đổi nội dung của MusicShelfRenderer thành một trang tìm kiếm
private fun <T : Innertube.Item> MusicShelfRenderer?.toItemsPage(
    mapper: (MusicShelfRenderer.Content) -> T?
) = Innertube.ItemsPage(
    items = this
        ?.contents
        ?.mapNotNull(mapper),
    continuation = this
        ?.continuations
        ?.firstOrNull()
        ?.nextContinuationData
        ?.continuation
)
