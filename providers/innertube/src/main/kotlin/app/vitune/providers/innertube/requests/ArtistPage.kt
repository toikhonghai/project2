package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.BrowseResponse
import app.vitune.providers.innertube.models.Context
import app.vitune.providers.innertube.models.MusicCarouselShelfRenderer
import app.vitune.providers.innertube.models.MusicShelfRenderer
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.utils.findSectionByTitle
import app.vitune.providers.innertube.utils.from
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext

///  gửi một yêu cầu HTTP để lấy thông tin trang nghệ sĩ từ YouTube Music API và
// trích xuất dữ liệu về tên, mô tả, hình ảnh, và các danh mục như bài hát, album, đĩa đơn.
suspend fun Innertube.artistPage(body: BrowseBody) = runCatchingCancellable { // Gọi hàm runCatchingCancellable để thực hiện một tác vụ có thể bị hủy bỏ và bắt lỗi nếu có.
    val ctx = currentCoroutineContext() //Lưu lại CoroutineContext hiện tại để đảm bảo các coroutine con sử dụng cùng một context.
    val response = client.post(BROWSE) {
        setBody(body) // Gửi yêu cầu với nội dung là body đã được truyền vào.
        mask("contents,header") //để chỉ lấy các phần "contents" và "header" trong phản hồi.
    }.body<BrowseResponse>() // Gọi hàm body để lấy phản hồi từ yêu cầu HTTP và chuyển đổi nó thành đối tượng BrowseResponse.

    val responseNoLang by lazy {
        CoroutineScope(ctx).async(start = CoroutineStart.LAZY) { // chạy coroutine trì hoãn (LAZY), chỉ thực hiện khi gọi await().
            client.post(BROWSE) {
                setBody(body.copy(context = Context.DefaultWebNoLang)) // Gửi yêu cầu với ngữ cảnh không có ngôn ngữ.
                mask("contents,header") // để chỉ lấy các phần "contents" và "header" trong phản hồi.
            }.body<BrowseResponse>()
        }
    }

    suspend fun findSectionByTitle(text: String) = response //  Hàm này tìm kiếm một phần (section) có tiêu đề text.
        .contents
        ?.singleColumnBrowseResultsRenderer
        ?.tabs
        ?.get(0)
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.findSectionByTitle(text) ?: responseNoLang.await() // Nếu không tìm thấy, nó sẽ gọi hàm await() trên responseNoLang để lấy phản hồi không có ngôn ngữ.
        .contents
        ?.singleColumnBrowseResultsRenderer // Lấy danh sách các phần trong phản hồi.
        ?.tabs
        ?.get(0) // Lấy tab đầu tiên trong danh sách các tab.
        ?.tabRenderer // Lấy thông tin của tab đầu tiên.
        ?.content
        ?.sectionListRenderer // Lấy nội dung của tab đầu tiên.
        ?.findSectionByTitle(text)

    val songsSection = findSectionByTitle("Songs")?.musicShelfRenderer // tìm kiếm trong danh sách nội dung
    logger.info("Songs section: $songsSection, Contents: ${songsSection?.contents?.size}")
    val albumsSection = findSectionByTitle("Albums")?.musicCarouselShelfRenderer // tìm kiếm trong danh sách nội dung
    val singlesSection = findSectionByTitle("Singles")?.musicCarouselShelfRenderer

    // tìm kiếm trong danh sách nội dung
    Innertube.ArtistPage(
        name = response
            .header
            ?.musicImmersiveHeaderRenderer // Lấy tên nghệ sĩ từ phần header của phản hồi.
            ?.title
            ?.text,
        description = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.description
            ?.text,
        thumbnail = (
                response
                    .header
                    ?.musicImmersiveHeaderRenderer
                    ?.foregroundThumbnail
                    ?: response
                        .header
                        ?.musicImmersiveHeaderRenderer
                        ?.thumbnail
                )
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.getOrNull(0),
        shuffleEndpoint = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.playButton
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.watchEndpoint,
        radioEndpoint = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.startRadioButton
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.watchEndpoint,
        songs = songsSection
            ?.contents
            ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        songsEndpoint = songsSection
            ?.bottomEndpoint
            ?.browseEndpoint,
        albums = albumsSection
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        albumsEndpoint = albumsSection
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.moreContentButton
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.browseEndpoint,
        singles = singlesSection
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        singlesEndpoint = singlesSection
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.moreContentButton
            ?.buttonRenderer
            ?.navigationEndpoint
            ?.browseEndpoint,
        subscribersCountText = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.subscriptionButton
            ?.subscribeButtonRenderer
            ?.subscriberCountText
            ?.text
    )
}
