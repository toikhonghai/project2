package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.Innertube.PodcastEpisodeItem
import app.vitune.providers.innertube.Innertube.PodcastPage
import app.vitune.providers.innertube.models.BrowseResponse
import app.vitune.providers.innertube.models.ContinuationResponse
import app.vitune.providers.innertube.models.MusicShelfRenderer
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.utils.toItemsPage
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Tải thông tin trang podcast từ YouTube Music API
 */
// Tải trang podcast từ YouTube Music
suspend fun Innertube.loadPodcastPage(
    browseId: String
) = runCatchingCancellable {
    // Tạo body gửi kèm (chỉ cần browseId vì context có sẵn trong client)
    val body = BrowseBody(browseId = browseId)

    // Gửi POST request để lấy thông tin chi tiết của podcast
    val response = client.post(BROWSE) {
        setBody(body)
        mask(
            "header.musicDetailHeaderRenderer," +
                    "contents.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$PODCAST_EPISODE_RENDERER_MASK)"
        )
    }.body<BrowseResponse>()

    val header = response.header
    val detailHeader = header?.musicDetailHeaderRenderer
    val immersiveHeader = header?.musicImmersiveHeaderRenderer

    //  Lấy tiêu đề
    val title = detailHeader?.title?.runs?.firstOrNull()?.text

    // Lấy mô tả (gộp nhiều đoạn runs thành một chuỗi)
    val description = detailHeader?.description?.runs?.joinToString("") { it.text.orEmpty() }

    // 👤 Lấy thông tin tác giả từ subtitle (dòng có navigationEndpoint)
    val author = detailHeader?.subtitle?.runs
        ?.firstOrNull { it.navigationEndpoint?.browseEndpoint != null }
        ?.let { Innertube.Info<NavigationEndpoint.Endpoint.Browse>(it) }

    //  Lấy ảnh thumbnail đầu tiên
    val thumbnail = detailHeader
        ?.thumbnail
        ?.musicThumbnailRenderer
        ?.thumbnail
        ?.thumbnails
        ?.firstOrNull()

    // Lấy nút đăng ký (hiện chỉ có subscriberCountText do API giới hạn)
    val subscriptionButton = immersiveHeader
        ?.subscriptionButton
        ?.subscribeButtonRenderer
        ?.subscriberCountText
        ?.runs
        ?.firstOrNull()
        ?.text
        ?.let { subscriberText ->
            PodcastPage.SubscriptionButton(
                subscribed = false,
                subscribedButtonText = subscriberText,
                unsubscribedButtonText = null,
                channelId = null
            )
        }

    // Lấy danh sách các tập podcast
    val episodes = response
        .contents
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.musicShelfRenderer
        ?.toItemsPage(::parsePodcastEpisode)

    // Trả về đối tượng PodcastPage chứa toàn bộ thông tin
    PodcastPage(
        title = title,
        description = description,
        author = author,
        thumbnail = thumbnail,
        subscriptionButton = subscriptionButton,
        episodes = episodes
    )
}

// Hàm tải thêm tập podcast khi có continuation token
suspend fun Innertube.loadMorePodcastEpisodes(
    continuationToken: String
) = runCatchingCancellable {
    val body = ContinuationBody(continuation = continuationToken)

    val response = client.post(BROWSE) {
        setBody(body)
        mask("continuationContents.musicShelfContinuation(continuations,contents.$PODCAST_EPISODE_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toItemsPage(::parsePodcastEpisode)
}

// Parse 1 tập podcast từ renderer thành PodcastEpisodeItem
private fun parsePodcastEpisode(content: MusicShelfRenderer.Content): PodcastEpisodeItem? {
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    val flexColumns = renderer.flexColumns

    // Lấy tiêu đề và endpoint để tạo info
    val titleRun = flexColumns
        .getOrNull(0)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull() ?: return null

    val endpoint = titleRun.navigationEndpoint?.endpoint as? NavigationEndpoint.Endpoint.Watch
        ?: return null

    val info = Innertube.Info(titleRun.text, endpoint)

    // Lấy thông tin podcast gốc
    val podcastRun = flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs

    val podcast = podcastRun
        ?.firstOrNull { it.navigationEndpoint?.browseEndpoint != null }
        ?.let { Innertube.Info<NavigationEndpoint.Endpoint.Browse>(it) }

    val publishedTimeText = podcastRun?.lastOrNull()?.text

    // ️ Thời lượng tập
    val durationText = renderer.fixedColumns
        ?.firstOrNull()
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull()
        ?.text

    // Mô tả
    val description = flexColumns
        .getOrNull(2)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.joinToString("") { it.text.orEmpty() }

    // Thumbnail tập podcast
    val thumbnail = renderer.thumbnail
        ?.musicThumbnailRenderer
        ?.thumbnail
        ?.thumbnails
        ?.firstOrNull()

    return PodcastEpisodeItem(
        info = info,
        podcast = podcast,
        durationText = durationText,
        publishedTimeText = publishedTimeText,
        description = description,
        thumbnail = thumbnail
    )
}

//// Chuyển MusicShelfRenderer thành danh sách PodcastEpisodeItem + continuation
//private fun <T : Innertube.Item> MusicShelfRenderer?.toItemsPage(
//    mapper: (MusicShelfRenderer.Content) -> T?
//) = Innertube.ItemsPage(
//    items = this?.contents?.mapNotNull(mapper),
//    continuation = this?.continuations?.firstOrNull()?.nextContinuationData?.continuation
//)
