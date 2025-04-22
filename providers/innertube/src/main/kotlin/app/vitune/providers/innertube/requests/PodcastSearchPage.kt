package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.Innertube.PodcastItem
import app.vitune.providers.innertube.Innertube.SearchFilter
import app.vitune.providers.innertube.models.ContinuationResponse
import app.vitune.providers.innertube.models.MusicShelfRenderer
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.SearchResponse
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.models.bodies.SearchBody
import app.vitune.providers.innertube.utils.toItemsPage
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
* Tìm kiếm podcast từ YouTube Music API
* @param query Chuỗi tìm kiếm
* @param filter Bộ lọc tìm kiếm, mặc định là Podcast
* @return Một đối tượng Result chứa trang kết quả tìm kiếm podcast
*/
suspend fun Innertube.searchPodcasts(
    query: String,
    filter: SearchFilter = SearchFilter.Podcast
) = runCatchingCancellable {
    // Tạo body gửi kèm POST request
    val body = SearchBody(
        query = query,
        params = filter.value
    )

    // Gửi POST để tìm kiếm với bộ lọc podcast, dùng mask để giới hạn dữ liệu trả về
    val response = client.post(SEARCH) {
        setBody(body)
        mask("contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$PODCAST_RENDERER_MASK)")
    }.body<SearchResponse>()

    // Trích dữ liệu kết quả podcast từ response
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
        ?.toItemsPage(::parsePodcastItem)
}

/**
 * Tải thêm kết quả tìm kiếm podcast từ continuation token
 * @param continuationToken Token để tiếp tục tải thêm kết quả
 * @return Một đối tượng Result chứa trang kết quả tìm kiếm podcast tiếp theo
 */
suspend fun Innertube.searchPodcastsWithContinuation(
    continuationToken: String
) = runCatchingCancellable {
    val body = ContinuationBody(
        continuation = continuationToken
    )

    // Gửi POST với continuation token để lấy thêm kết quả
    val response = client.post(SEARCH) {
        setBody(body)
        mask("continuationContents.musicShelfContinuation(continuations,contents.$PODCAST_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    // Trích danh sách kết quả mới từ response
    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toItemsPage(::parsePodcastItem)
}

/**
 * Chuyển đổi một MusicShelfRenderer.Content thành một PodcastItem
 * @param content Đối tượng MusicShelfRenderer.Content từ API response
 * @return PodcastItem được tạo từ dữ liệu trong content, hoặc null nếu dữ liệu không hợp lệ
 */
private fun parsePodcastItem(content: MusicShelfRenderer.Content): PodcastItem? {
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    val flexColumns = renderer.flexColumns

    // Lấy thông tin tiêu đề podcast và endpoint để tạo đối tượng Info
    val titleRun = flexColumns
        .getOrNull(0)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull() ?: return null

    val endpoint = titleRun.navigationEndpoint?.endpoint as? NavigationEndpoint.Endpoint.Browse
        ?: return null
    val info = Innertube.Info(titleRun.text, endpoint)

    // Lấy danh sách tác giả (các run có navigationEndpoint)
    val authorRuns = flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs

    val authors = authorRuns
        ?.filter { it.navigationEndpoint?.browseEndpoint != null }
        ?.map { Innertube.Info<NavigationEndpoint.Endpoint.Browse>(it) }

    // Mô tả thường nằm cuối danh sách tác giả
    val description = authorRuns?.lastOrNull()?.text

    // Trích số lượng tập từ chuỗi mô tả nếu có (ví dụ "12 tập")
    val episodeCount = description?.let {
        "\\d+".toRegex().find(it)?.value?.toIntOrNull()
    }

    // Lấy thumbnail đầu tiên trong danh sách
    val thumbnail = renderer.thumbnail
        ?.musicThumbnailRenderer
        ?.thumbnail
        ?.thumbnails
        ?.firstOrNull()

    // Trả về đối tượng PodcastItem đã được tạo
    return PodcastItem(
        info = info,
        authors = authors,
        description = description,
        episodeCount = episodeCount,
        thumbnail = thumbnail
    )
}

///**
// * Chuyển đổi MusicShelfRenderer thành ItemsPage chứa danh sách PodcastItem
// * @param mapper Hàm chuyển đổi từng Content thành Item cụ thể
// */
//private fun <T : Innertube.Item> MusicShelfRenderer?.toItemsPage(
//    mapper: (MusicShelfRenderer.Content) -> T?
//) = Innertube.ItemsPage(
//    items = this
//        ?.contents
//        ?.mapNotNull(mapper), // Lọc bỏ null, giữ các item hợp lệ
//    continuation = this
//        ?.continuations
//        ?.firstOrNull()
//        ?.nextContinuationData
//        ?.continuation // Token để tải thêm dữ liệu (nếu có)
//)