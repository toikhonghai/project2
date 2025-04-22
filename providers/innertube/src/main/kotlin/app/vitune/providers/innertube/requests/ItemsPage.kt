package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.BrowseResponse
import app.vitune.providers.innertube.models.ContinuationResponse
import app.vitune.providers.innertube.models.GridRenderer
import app.vitune.providers.innertube.models.MusicResponsiveListItemRenderer
import app.vitune.providers.innertube.models.MusicShelfRenderer
import app.vitune.providers.innertube.models.MusicTwoRowItemRenderer
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// Gửi yêu cầu đến API của YouTube Music để duyệt nội dung dựa trên một ID duyệt (browseId).
/*
Nó có nghĩa là hàm này sử dụng Generics để cho phép kiểu dữ liệu (T) có thể
thay đổi linh hoạt nhưng vẫn bị ràng buộc (bounded) bởi một kiểu cụ thể (Innertube.Item)
T : Innertube.Item: Đây là một ràng buộc (constraint),
có nghĩa là T phải kế thừa (:) từ Innertube.Item hoặc là một subclass của nó.
 */
suspend fun <T : Innertube.Item> Innertube.itemsPage(
    body: BrowseBody,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null }
) = runCatchingCancellable {
    val response = client.post(BROWSE) {
        setBody(body)
    }.body<BrowseResponse>()

    // Lấy danh sách các phần trong phản hồi.
    val sectionListRendererContent = response
        .contents
        ?.singleColumnBrowseResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()

    // Chuyển đổi danh sách các phần thành danh sách các mục BrowseResult.Item.
    itemsPageFromMusicShelRendererOrGridRenderer(
        musicShelfRenderer = sectionListRendererContent
            ?.musicShelfRenderer,
        gridRenderer = sectionListRendererContent
            ?.gridRenderer,
        fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
        fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer
    )
}

suspend fun <T : Innertube.Item> Innertube.itemsPage(
    body: ContinuationBody,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null }
) = runCatchingCancellable {
    val response = client.post(BROWSE) {
        setBody(body)
    }.body<ContinuationResponse>()

    itemsPageFromMusicShelRendererOrGridRenderer(
        musicShelfRenderer = response
            .continuationContents
            ?.musicShelfContinuation,
        gridRenderer = null,
        fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
        fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer
    )
}

private fun <T : Innertube.Item> itemsPageFromMusicShelRendererOrGridRenderer(
    musicShelfRenderer: MusicShelfRenderer?,
    gridRenderer: GridRenderer?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T?,
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T?
) = when {
    musicShelfRenderer != null -> Innertube.ItemsPage(
        continuation = musicShelfRenderer
            .continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation,
        items = musicShelfRenderer
            .contents
            ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer) // Lấy danh sách các mục từ phản hồi.
            ?.mapNotNull(fromMusicResponsiveListItemRenderer) // Chuyển đổi các mục thành các mục Innertube.Item.
    )

    gridRenderer != null -> Innertube.ItemsPage(
        continuation = null,
        items = gridRenderer
            .items
            ?.mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer) // Lấy danh sách các mục từ phản hồi.
            ?.mapNotNull(fromMusicTwoRowItemRenderer) // Chuyển đổi các mục thành các mục Innertube.Item.
    )

    else -> null
}
