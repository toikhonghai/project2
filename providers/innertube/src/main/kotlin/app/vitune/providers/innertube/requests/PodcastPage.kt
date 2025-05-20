package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.Innertube.BROWSE
import app.vitune.providers.innertube.Innertube.PodcastEpisodeItem
import app.vitune.providers.innertube.Innertube.PodcastItem
import app.vitune.providers.innertube.Innertube.PodcastPage
import app.vitune.providers.innertube.Innertube.client
import app.vitune.providers.innertube.Innertube.logger
import app.vitune.providers.innertube.models.BrowseResponse
import app.vitune.providers.innertube.models.Context
import app.vitune.providers.innertube.models.ContinuationResponse
import app.vitune.providers.innertube.models.MusicShelfRenderer
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.NextResponse
import app.vitune.providers.innertube.models.SearchResponse
import app.vitune.providers.innertube.models.Thumbnail
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.models.bodies.ContinuationBody
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.models.bodies.SearchBody
import app.vitune.providers.innertube.utils.toItemsPage
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.headers
import org.slf4j.LoggerFactory

/**
 * Fetch podcast metadata and episodes from YouTube Music API
 */
suspend fun Innertube.loadPodcastPage(
    browseId: String
) = runCatchingCancellable {
    logger.info("Fetching podcast page with browseId: $browseId")

    // Lấy metadata
    val metadata = loadPodcastMetadata(browseId)?.getOrThrow()

    // Lấy playlistId và danh sách tập
    val playlistId = getPodcastPlaylistId(browseId)
    val episodes = playlistId?.let { loadPodcastEpisodesNext(it)?.getOrNull() } ?: metadata?.episodes

    // Log kết quả
    logger.info("Podcast Title: ${metadata?.title ?: "Unknown"}")
    logger.info("Playlist ID: ${playlistId ?: "None"}")
    logger.info("Episode Count: ${episodes?.items?.size ?: 0}")

    PodcastPage(
        title = metadata?.title,
        description = metadata?.description,
        author = metadata?.author,
        thumbnail = metadata?.thumbnail,
        subscriptionButton = metadata?.subscriptionButton,
        episodes = episodes,
        episodeCount = episodes?.items?.size ?: metadata?.episodeCount,
        playlistId = playlistId // Thêm playlistId vào PodcastPage
    )
}

suspend fun Innertube.loadPodcastMetadata(browseId: String) = runCatchingCancellable {
    val body = BrowseBody(
        browseId = browseId,
        context = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.20241028.01.00",
                gl = "VN",
                hl = "vi"
            ),
            user = Context.User()
        )
    )

    val response = client.post(BROWSE) {
        setBody(body)
        body.context.apply()
    }.body<BrowseResponse>()
    val header = response.contents?.twoColumnBrowseResultsRenderer?.tabs
        ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        ?.firstOrNull()?.musicResponsiveHeaderRenderer

    val title = header?.title?.runs?.firstOrNull()?.text
    val description = header?.description?.description?.runs?.joinToString("") { it.text.orEmpty() }
    val author = header?.straplineTextOne?.runs
        ?.firstOrNull { it.navigationEndpoint?.browseEndpoint != null }
        ?.let { Innertube.Info<NavigationEndpoint.Endpoint.Browse>(it) }
    val thumbnail = header?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
        ?.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }
    val episodes = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents
        ?.sectionListRenderer?.contents
        ?.firstOrNull { it.musicShelfRenderer != null }
        ?.musicShelfRenderer
        ?.toItemsPage(::parsePodcastEpisode)
    val playlistId = getPodcastPlaylistId(browseId)

    PodcastPage(
        title = title,
        description = description,
        author = author,
        thumbnail = thumbnail,
        subscriptionButton = null,
        episodes = episodes,
        episodeCount = episodes?.items?.size ?: 0,
        playlistId = playlistId // Lưu playlistId
    )
}
suspend fun getPodcastPlaylistId(browseId: String): String? = runCatchingCancellable {
    val body = BrowseBody(
        browseId = browseId,
        context = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.20241028.01.00",
                gl = "VN", // Khu vực Việt Nam
                hl = "vi" // Ngôn ngữ tiếng Việt
            ),
            user = Context.User()
        )
    )

    val response = client.post(BROWSE) {
        setBody(body)
        body.context.apply()
    }.body<BrowseResponse>()

    response.contents
        ?.twoColumnBrowseResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.musicResponsiveHeaderRenderer
        ?.buttons
        ?.flatMap { button -> button.menuRenderer?.items.orEmpty() }
        ?.firstOrNull { item -> item.toggleMenuServiceItemRenderer?.toggledServiceEndpoint?.likeEndpoint?.target?.playlistId != null }
        ?.toggleMenuServiceItemRenderer
        ?.toggledServiceEndpoint
        ?.likeEndpoint
        ?.target
        ?.playlistId
}?.onFailure { logger.error("Failed to retrieve playlistId for browseId $browseId: ${it.message}", it) }?.getOrNull()

suspend fun Innertube.loadPodcastEpisodesNext(playlistId: String) = runCatchingCancellable {
    val body = NextBody(
        playlistId = playlistId,
        context = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.20241028.01.00",
                gl = "VN", // Khu vực Việt Nam
                hl = "vi" // Ngôn ngữ tiếng Việt
            ),
            user = Context.User()
        )
    )

    val response = client.post(NEXT) {
        setBody(body)
        body.context.apply()
    }.body<NextResponse>()
    val playlistPanelRenderer = response.contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.musicQueueRenderer
        ?.content
        ?.playlistPanelRenderer

    val episodes = playlistPanelRenderer?.contents
        ?.mapNotNull { it.playlistPanelVideoRenderer }
        ?.map { renderer ->
            PodcastEpisodeItem(
                info = renderer.title?.runs?.firstOrNull()?.let { Innertube.Info(it.text, renderer.navigationEndpoint?.watchEndpoint) },
                podcast = renderer.longBylineText?.runs
                    ?.firstOrNull { it.navigationEndpoint?.browseEndpoint != null }
                    ?.let { Innertube.Info<NavigationEndpoint.Endpoint.Browse>(it) },
                durationText = renderer.lengthText?.runs?.firstOrNull()?.text,
                publishedTimeText = null,
                description = null,
                thumbnail = renderer.thumbnail?.thumbnails?.firstOrNull()
            )
        }

    Innertube.ItemsPage(
        items = episodes,
        continuation = playlistPanelRenderer?.continuations?.firstOrNull()?.nextContinuationData?.continuation
    )
}


// Load more podcast episodes using a continuation token
suspend fun Innertube.loadMorePodcastEpisodes(
    continuationToken: String
) = runCatchingCancellable {
    val body = ContinuationBody(continuation = continuationToken)

    val response = client.post(BROWSE) {
        setBody(body)
        parameter("continuation", body.continuation)
        parameter("ctoken", body.continuation)
    }.body<ContinuationResponse>().also { response ->
        logger.info("Continuation response: $response")
    }

    response.continuationContents?.musicShelfContinuation?.toItemsPage(::parsePodcastEpisode)
}

// Fetch podcast episodes using BrowseBody
suspend fun Innertube.loadPodcastEpisodes(
    browseId: String
) = runCatchingCancellable {
    logger.info("Fetching podcast episodes with browseId: $browseId")
    val body = BrowseBody(
        browseId = browseId,
        context = Context(
            client = Context.Client(
                clientName = "WEB_REMIX",
                clientVersion = "1.20241028.01.00",
                gl = "VN", // Khu vực Việt Nam
                hl = "vi" // Ngôn ngữ tiếng Việt
            ),
            user = Context.User()
        )
    )

    val response = client.post(BROWSE) {
        setBody(body)
    }.body<BrowseResponse>()

    logger.info("Browse response for episodes: $response")
    val contents = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        ?: response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents

    val musicShelfRenderer = contents?.firstOrNull { it.musicShelfRenderer != null }?.musicShelfRenderer

    val episodesPage = musicShelfRenderer?.toItemsPage(::parsePodcastEpisode)

    logger.debug("Fetched ${episodesPage?.items?.size ?: 0} episodes for browseId $browseId")
    episodesPage
}?.onFailure { error ->
    logger.error("Error in loadPodcastEpisodes for browseId $browseId: ${error.message}", error)
}

internal fun parsePodcastEpisode(content: MusicShelfRenderer.Content): PodcastEpisodeItem? {
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    val flexColumns = renderer.flexColumns

    val titleRun = flexColumns
        .getOrNull(0)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull() ?: return null

    // Lấy videoId từ playlistItemData hoặc browseId
    val videoId = renderer.playlistItemData?.videoId
        ?: titleRun.navigationEndpoint?.browseEndpoint?.browseId?.removePrefix("MPED")
        ?: return null

    // Tạo Watch endpoint từ videoId
    val endpoint = NavigationEndpoint.Endpoint.Watch(
        videoId = videoId,
        watchEndpointMusicSupportedConfigs = NavigationEndpoint.Endpoint.Watch.WatchEndpointMusicSupportedConfigs(
            watchEndpointMusicConfig = NavigationEndpoint.Endpoint.Watch.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig(
                musicVideoType = "MUSIC_VIDEO_TYPE_PODCAST_EPISODE"
            )
        )
    )

    // Kiểm tra pageType để đảm bảo là podcast episode
    val isPodcastEpisode = titleRun.navigationEndpoint?.browseEndpoint
        ?.browseEndpointContextSupportedConfigs
        ?.browseEndpointContextMusicConfig
        ?.pageType == "MUSIC_PAGE_TYPE_NON_MUSIC_AUDIO_TRACK_PAGE"
    if (!isPodcastEpisode) {
        logger.debug("Not a podcast episode: ${titleRun.text}")
        return null
    }

    val info = Innertube.Info(titleRun.text, endpoint)

    val podcastRun = flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs

    // Tìm podcast với browseEndpoint
    val podcast = podcastRun
        ?.firstOrNull { it.navigationEndpoint?.browseEndpoint != null }
        ?.let { Innertube.Info<NavigationEndpoint.Endpoint.Browse>(it) }

    // Lấy publishedTimeText từ runs đầu tiên hoặc cuối cùng
    val publishedTimeText = podcastRun
        ?.firstOrNull { it.text?.matches("\\d{1,2} [A-Za-z]+ \\d{4}|\\d+ (days?|hours?) ago".toRegex()) == true }
        ?.text ?: podcastRun?.lastOrNull()?.text

    val durationText = renderer.fixedColumns
        ?.firstOrNull()
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull()
        ?.text

    val description = flexColumns
        .getOrNull(2)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.joinToString("") { it.text.orEmpty() }

    val thumbnail = renderer.thumbnail
        ?.musicThumbnailRenderer
        ?.thumbnail
        ?.thumbnails
        ?.firstOrNull()
        ?.let {
            Thumbnail(
                url = it.url ?: "",
                height = it.height,
                width = it.width
            )
        }

    return PodcastEpisodeItem(
        info = info,
        podcast = podcast,
        durationText = durationText,
        publishedTimeText = publishedTimeText,
        description = description,
        thumbnail = thumbnail
    ).also {
        logger.debug("Parsed podcast episode: ${it.info?.name}, videoId: $videoId")
    }
}