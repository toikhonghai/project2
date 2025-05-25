package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.Innertube.Info
import app.vitune.providers.innertube.Innertube.ItemsPage
import app.vitune.providers.innertube.Innertube.PLAYER
import app.vitune.providers.innertube.Innertube.PodcastEpisodeItem
import app.vitune.providers.innertube.Innertube.PodcastItem
import app.vitune.providers.innertube.Innertube.SEARCH
import app.vitune.providers.innertube.Innertube.SearchFilter
import app.vitune.providers.innertube.Innertube.client
import app.vitune.providers.innertube.Innertube.logger
import app.vitune.providers.innertube.models.Context
import app.vitune.providers.innertube.models.ContinuationResponse
import app.vitune.providers.innertube.models.MusicShelfRenderer
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.PlayerResponse
import app.vitune.providers.innertube.models.SearchResponse
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.models.bodies.PlayerBody
import app.vitune.providers.innertube.models.bodies.SearchBody
import app.vitune.providers.innertube.utils.toItemsPage
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

suspend fun Innertube.searchPodcasts(query: String): Result<ItemsPage<PodcastItem>>? =
    runCatchingCancellable {
        val body = SearchBody(
            query = query,
            params = SearchFilter.Podcast.value,
            context = Context(
                client = Context.Client(
                    clientName = "WEB_REMIX",
                    clientVersion = "1.20241028.01.00"
                ),
                user = Context.User()
            )
        )

        val response = client.post(SEARCH) {
            setBody(body)
            body.context.apply()
            mask("contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.musicResponsiveListItemRenderer(flexColumns,thumbnail,navigationEndpoint))")
        }.body<SearchResponse>()

        val items = response.contents?.tabbedSearchResultsRenderer?.tabs
            ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.flatMap { it.musicShelfRenderer?.contents.orEmpty() }
            ?.mapNotNull { parsePodcastItem(it) }
            ?.distinctBy { it.key }

        logger.debug("Found ${items?.size ?: 0} podcasts for query: $query")
        ItemsPage(
            items = items,
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs
                ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.continuations
                ?.firstOrNull()?.nextContinuationData?.continuation
        )
    }?.onFailure { logger.error("Tìm kiếm kênh podcast thất bại cho truy vấn $query: ${it.message}", it) }

suspend fun Innertube.searchPodcastEpisodes(query: String): Result<ItemsPage<PodcastEpisodeItem>>? =
    runCatchingCancellable {
        val body = SearchBody(
            query = query,
            params = SearchFilter.PodcastEpisode.value,
            context = Context(
                client = Context.Client(
                    clientName = "WEB_REMIX",
                    clientVersion = "1.20241028.01.00"
                ),
                user = Context.User()
            )
        )

        logger.debug("Gửi yêu cầu tìm kiếm cho truy vấn: $query")
        val response = client.post(SEARCH) {
            setBody(body)
            body.context.apply()
            mask("contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$PODCAST_EPISODE_RENDERER_MASK)")
        }.body<SearchResponse>()

        logger.debug("Nhận phản hồi với nội dung: ${response.contents != null}")
        val contents = response.contents?.tabbedSearchResultsRenderer?.tabs
            ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.flatMap { it.musicShelfRenderer?.contents.orEmpty() }
            ?: emptyList()

        logger.debug("Tìm thấy ${contents.size} mục trong musicShelfRenderer")
        val items = contents.mapNotNull { content ->
            parsePodcastEpisode(content).also { episode ->
                if (episode == null) logger.debug("Không thể phân tích nội dung: $content")
            }
        }.distinctBy { it.key }

        logger.debug("Tìm thấy ${items.size} tập podcast cho truy vấn: $query")
        ItemsPage(
            items = items,
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs
                ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.continuations
                ?.firstOrNull()?.nextContinuationData?.continuation
        )
    }?.onFailure { logger.error("Tìm kiếm tập podcast thất bại cho truy vấn $query: ${it.message}", it) }

suspend fun Innertube.searchPodcastEpisodesWithContinuation(
    continuationToken: String
) = runCatchingCancellable {
    val body = ContinuationBody(continuation = continuationToken)

    val response = client.post(SEARCH) {
        setBody(body)
        mask("continuationContents.musicShelfContinuation(continuations,contents.$PODCAST_EPISODE_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    val items = response.continuationContents?.musicShelfContinuation?.contents
        ?.mapNotNull { parsePodcastEpisode(it) }
        ?.distinctBy { it.key }

    logger.debug("Tìm thấy ${items?.size ?: 0} tập podcast cho continuation")
    ItemsPage(
        items = items,
        continuation = response.continuationContents?.musicShelfContinuation?.continuations
            ?.firstOrNull()?.nextContinuationData?.continuation
    )
}?.onFailure { logger.error("Tìm kiếm tập podcast với continuation thất bại: ${it.message}", it) }

suspend fun Innertube.searchPodcastsWithContinuation(
    continuationToken: String
) = runCatchingCancellable {
    val body = ContinuationBody(
        continuation = continuationToken
    )

    val response = client.post(SEARCH) {
        setBody(body)
        mask("continuationContents.musicShelfContinuation(continuations,contents.musicResponsiveListItemRenderer(flexColumns,thumbnail,navigationEndpoint))")
    }.body<ContinuationResponse>()

    val items = response.continuationContents?.musicShelfContinuation?.contents
        ?.mapNotNull { parsePodcastItem(it) }
        ?.distinctBy { it.key }

    logger.debug("Tìm thấy ${items?.size ?: 0} kênh podcast cho continuation")
    ItemsPage(
        items = items,
        continuation = response.continuationContents?.musicShelfContinuation?.continuations
            ?.firstOrNull()?.nextContinuationData?.continuation
    )
}?.onFailure { logger.error("Tìm kiếm kênh podcast với continuation thất bại: ${it.message}", it) }

private fun parsePodcastItem(content: MusicShelfRenderer.Content): PodcastItem? {
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    val flexColumns = renderer.flexColumns

    val titleRun = flexColumns
        .getOrNull(0)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull() ?: return null

    val endpoint = renderer.navigationEndpoint?.endpoint as? NavigationEndpoint.Endpoint.Browse
        ?: return null

    val pageType = endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
    if (pageType != "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE") return null

    val browseId = endpoint.browseId ?: return null

    val info = Info(titleRun.text, endpoint)

    val authorRuns = flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs

    val authors = authorRuns
        ?.filter { it.navigationEndpoint?.browseEndpoint != null }
        ?.map { Info<NavigationEndpoint.Endpoint.Browse>(it) }
        ?: emptyList()

    val description = authorRuns?.joinToString("") { it.text.orEmpty() }

    return PodcastItem(
        info = info,
        authors = authors,
        description = description,
        episodeCount = null, // Bỏ episodeCount để tránh gọi mạng
        playlistId = null, // Không cần playlistId trong tìm kiếm
        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()
    )
}

suspend fun Innertube.getPodcastStreamUrl(context: Context, videoId: String): String? = runCatchingCancellable {
    val body = PlayerBody(
        videoId = videoId,
        context = context // Sử dụng context được truyền vào
    )

    val response = client.post(PLAYER) {
        setBody(body)
        body.context.apply()
    }.body<PlayerResponse>()

    val audioFormat = response.streamingData?.adaptiveFormats
        ?.filter { it.mimeType.startsWith("audio/") && !it.mimeType.contains("video/") }
        ?.maxByOrNull { it.bitrate ?: 0 }

    audioFormat?.url
}?.onFailure { logger.error("Failed to get stream URL for videoId $videoId: ${it.message}", it) }?.getOrNull()