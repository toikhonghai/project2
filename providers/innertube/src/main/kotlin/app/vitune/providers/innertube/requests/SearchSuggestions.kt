package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.SearchSuggestionsResponse
import app.vitune.providers.innertube.models.bodies.SearchSuggestionsBody
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// Gửi yêu cầu đến API của YouTube Music để lấy gợi ý tìm kiếm dựa trên một truy vấn tìm kiếm.
suspend fun Innertube.searchSuggestions(body: SearchSuggestionsBody) = runCatchingCancellable {
    val response = client.post(SEARCH_SUGGESTIONS) {
        setBody(body) // gửi dữ liệu lên server
        @Suppress("all")
        mask(
            "contents.searchSuggestionsSectionRenderer.contents.searchSuggestionRenderer.navigationEndpoint.searchEndpoint.query"
        )
    }.body<SearchSuggestionsResponse>() // nhận phản hồi từ server

    response
        .contents
        ?.firstOrNull()
        ?.searchSuggestionsSectionRenderer
        ?.contents
        ?.mapNotNull { content ->
            content
                .searchSuggestionRenderer
                ?.navigationEndpoint
                ?.searchEndpoint
                ?.query
        }
}
