package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.BrowseResponse
import app.vitune.providers.innertube.models.Context
import app.vitune.providers.innertube.models.MusicCarouselShelfRenderer
import app.vitune.providers.innertube.models.NextResponse
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.utils.findSectionByStrapline
import app.vitune.providers.innertube.utils.findSectionByTitle
import app.vitune.providers.innertube.utils.from
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// Gửi một yêu cầu HTTP để lấy thông tin trang liên quan từ YouTube Music API và
// trích xuất dữ liệu về các bài hát, danh sách phát, album và nghệ sĩ liên quan đến bài hát hiện tại.
// Hàm này sử dụng một đối tượng NextBody để xác định ngữ cảnh và các tham số khác cho yêu cầu.
suspend fun Innertube.relatedPage(body: NextBody) = runCatchingCancellable {
    val nextResponse = client.post(NEXT) {
        setBody(body.copy(context = Context.DefaultWebNoLang)) // Gửi yêu cầu với ngữ cảnh không có ngôn ngữ.
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
        ?.getOrNull(2)
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: return@runCatchingCancellable null

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = browseId,
                context = Context.DefaultWebNoLang
            )
        )
        @Suppress("all")
        mask(
            "contents.sectionListRenderer.contents.musicCarouselShelfRenderer(header.musicCarouselShelfBasicHeaderRenderer(title,strapline),contents($MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK,$MUSIC_TWO_ROW_ITEM_RENDERER_MASK))"
        )
    }.body<BrowseResponse>()

    // Lấy thông tin tiêu đề và mô tả từ phản hồi.
    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    // Lấy danh sách các phần trong phản hồi.
    Innertube.RelatedPage(
        songs = sectionListRenderer
            ?.findSectionByTitle("You might also like")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer) // Lấy danh sách các bài hát từ phần "You might also like"
            ?.mapNotNull(Innertube.SongItem::from),
        playlists = sectionListRenderer
            ?.findSectionByTitle("Recommended playlists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer) // Lấy danh sách các danh sách phát từ phần "Recommended playlists"
            ?.mapNotNull(Innertube.PlaylistItem::from)
            ?.sortedByDescending { it.channel?.name == "YouTube Music" }, // Sắp xếp danh sách phát theo thứ tự giảm dần dựa trên tên kênh
        albums = sectionListRenderer
            ?.findSectionByStrapline("MORE FROM")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer) // Lấy danh sách các album từ phần "MORE FROM"
            ?.mapNotNull(Innertube.AlbumItem::from),
        artists = sectionListRenderer
            ?.findSectionByTitle("Similar artists") // Lấy danh sách các nghệ sĩ từ phần "Similar artists"
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer) // Chuyển đổi các mục thành các nghệ sĩ
            ?.mapNotNull(Innertube.ArtistItem::from) // Chuyển đổi các mục thành các nghệ sĩ
    )
}
