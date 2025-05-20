package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.Innertube.Info
import app.vitune.providers.innertube.Innertube.ItemsPage
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
import app.vitune.providers.innertube.models.SearchResponse
import app.vitune.providers.innertube.models.bodies.ContinuationBody
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
            ?.mapNotNull { content ->
                val renderer = content.musicResponsiveListItemRenderer ?: return@mapNotNull null
                val titleRun = renderer.flexColumns.getOrNull(0)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                val subtitleRun = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                val browseEndpoint = renderer.navigationEndpoint?.browseEndpoint
                val pageType = browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType

                if (pageType != "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE") return@mapNotNull null

                val browseId = browseEndpoint?.browseId ?: return@mapNotNull null
//                val playlistId = browseId.let { getPodcastPlaylistId(it) }
                val podcastPage = browseId.let { loadPodcastPage(it)?.getOrNull() }
                val episodeCount = podcastPage?.episodeCount ?: 0

                PodcastItem(
                    info = Info(
                        name = titleRun?.text,
                        endpoint = browseEndpoint
                    ),
                    authors = subtitleRun?.filter { it.navigationEndpoint?.browseEndpoint != null }
                        ?.map { Info<NavigationEndpoint.Endpoint.Browse>(it) },
                    description = subtitleRun?.joinToString("") { it.text.orEmpty() },
                    episodeCount = episodeCount,
                    playlistId = null,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()
                )
            }?.distinctBy { it.key }

        logger.debug("Found ${items?.size ?: 0} podcasts for query: $query")
        ItemsPage(
            items = items,
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs
                ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.continuations
                ?.firstOrNull()?.nextContinuationData?.continuation
        )
    }?.onFailure { logger.error("Search podcasts failed for query $query: ${it.message}", it) }

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

        logger.debug("Sending search request for query: $query")
        val response = client.post(SEARCH) {
            setBody(body)
            body.context.apply()
            mask("contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$PODCAST_EPISODE_RENDERER_MASK)")
        }.body<SearchResponse>()

        logger.debug("Received response with contents: ${response.contents != null}")
        val contents = response.contents?.tabbedSearchResultsRenderer?.tabs
            ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?.flatMap { it.musicShelfRenderer?.contents.orEmpty() }
            ?: emptyList()

        logger.debug("Found ${contents.size} content items in musicShelfRenderer")
        val items = contents.mapNotNull { content ->
            parsePodcastEpisode(content).also { episode ->
                if (episode == null) logger.debug("Failed to parse content: $content")
            }
        }.distinctBy { it.key }

        logger.debug("Found ${items.size} podcast episodes for query: $query")
        ItemsPage(
            items = items,
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs
                ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.continuations
                ?.firstOrNull()?.nextContinuationData?.continuation
        )
    }?.onFailure { logger.error("Search podcast episodes failed for query $query: ${it.message}", it) }

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

    logger.debug("Found ${items?.size ?: 0} podcast episodes for continuation")
    ItemsPage(
        items = items,
        continuation = response.continuationContents?.musicShelfContinuation?.continuations
            ?.firstOrNull()?.nextContinuationData?.continuation
    )
}?.onFailure { logger.error("Search podcast episodes with continuation failed: ${it.message}", it) }

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

    logger.debug("Found ${items?.size ?: 0} podcasts for continuation")
    ItemsPage(
        items = items,
        continuation = response.continuationContents?.musicShelfContinuation?.continuations
            ?.firstOrNull()?.nextContinuationData?.continuation
    )
}?.onFailure { logger.error("Search podcasts with continuation failed: ${it.message}", it) }

private suspend fun parsePodcastItem(content: MusicShelfRenderer.Content): PodcastItem? {
    val renderer = content.musicResponsiveListItemRenderer ?: return null
    val flexColumns = renderer.flexColumns

    val titleRun = flexColumns
        .getOrNull(0)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull()
    if (titleRun == null) {
        logger.warn("titleRun is null for renderer: $renderer")
        return null
    }

    val endpoint = flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull { it.navigationEndpoint?.browseEndpoint != null }
        ?.navigationEndpoint?.endpoint as? NavigationEndpoint.Endpoint.Browse
        ?: renderer.navigationEndpoint?.endpoint as? NavigationEndpoint.Endpoint.Browse
    if (endpoint == null) {
        logger.warn("endpoint is null or not Browse for renderer: $renderer")
        return null
    }

    val pageType = endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
    if (pageType != "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE") return null

    val browseId = endpoint.browseId ?: return null
    val playlistId = browseId.let { getPodcastPlaylistId(it) }
    val podcastPage = browseId.let { Innertube.loadPodcastPage(it)?.getOrNull() }
    val episodeCount = podcastPage?.episodeCount ?: 0

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

    val description = authorRuns?.lastOrNull()?.text

    return PodcastItem(
        info = info,
        authors = authors,
        description = description,
        episodeCount = episodeCount,
        playlistId = playlistId,
        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()
    )
}