package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.Innertube.toMood
import app.vitune.providers.innertube.models.BrowseResponse
import app.vitune.providers.innertube.models.GridRenderer
import app.vitune.providers.innertube.models.MusicCarouselShelfRenderer
import app.vitune.providers.innertube.models.MusicNavigationButtonRenderer
import app.vitune.providers.innertube.models.MusicResponsiveListItemRenderer
import app.vitune.providers.innertube.models.MusicTwoRowItemRenderer
import app.vitune.providers.innertube.models.SectionListRenderer
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.utils.from
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// Gửi yêu cầu đến API của YouTube Music để duyệt nội dung dựa trên một ID duyệt (browseId).
suspend fun Innertube.browse(body: BrowseBody) = runCatchingCancellable { // Gọi hàm runCatchingCancellable để thực hiện một tác vụ có thể bị hủy bỏ và bắt lỗi nếu có.
    val response = client.post(BROWSE) {
        setBody(body)
    }.body<BrowseResponse>() // Gọi hàm body để lấy phản hồi từ yêu cầu HTTP và chuyển đổi nó thành đối tượng BrowseResponse.

    BrowseResult( // Tạo một đối tượng BrowseResult từ phản hồi.
        title = response
            .header
            ?.musicImmersiveHeaderRenderer // Lấy tiêu đề từ phản hồi, nếu có.
            ?.title
            ?.text // Nếu không có tiêu đề, lấy tiêu đề từ phần header của phản hồi.
            ?: response
                .header
                ?.musicDetailHeaderRenderer // Lấy tiêu đề từ phần header của phản hồi.
                ?.title
                ?.text,
        items = response // Lấy danh sách các mục từ phản hồi.
            .contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull() // Lấy tab đầu tiên trong danh sách các tab.
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer // Lấy nội dung của tab đầu tiên.
            ?.toBrowseItems() // Chuyển đổi danh sách các mục thành danh sách các mục BrowseResult.Item.
            .orEmpty()
    )
}

// Lưu trữ thông tin về các tab trong giao diện người dùng của YouTube Music.
fun SectionListRenderer.toBrowseItems() = contents?.mapNotNull { content -> // mapNotNull sẽ tự động loại bỏ null trong kết quả.
    when {
        content.gridRenderer != null -> content.gridRenderer.toBrowseItem() // Chuyển đổi nội dung thành một mục lưới.
        content.musicCarouselShelfRenderer != null -> content.musicCarouselShelfRenderer.toBrowseItem() // Chuyển đổi nội dung thành một mục băng chuyền.
        else -> null
    }
}

fun GridRenderer.toBrowseItem() = BrowseResult.Item(
    title = header
        ?.gridHeaderRenderer // Lấy tiêu đề từ phần header của lưới.
        ?.title
        ?.runs
        ?.firstOrNull()
        ?.text,
    items = items
        ?.mapNotNull {
            it.musicTwoRowItemRenderer?.toItem() ?: it.musicNavigationButtonRenderer?.toItem() // Chuyển đổi các mục trong lưới thành các mục Innertube.Item.
        }
        .orEmpty()
)

fun MusicCarouselShelfRenderer.toBrowseItem( // Chuyển đổi một MusicCarouselShelfRenderer thành một BrowseResult.Item.
    fromResponsiveListItemRenderer: ((MusicResponsiveListItemRenderer) -> Innertube.Item?)? = null // Hàm chuyển đổi tùy chọn để chuyển đổi MusicResponsiveListItemRenderer thành Innertube.Item.
) = BrowseResult.Item(
    title = header
        ?.musicCarouselShelfBasicHeaderRenderer
        ?.title
        ?.runs
        ?.firstOrNull()
        ?.text,
    items = contents
        ?.mapNotNull {
            it.musicResponsiveListItemRenderer?.let { renderer ->
                fromResponsiveListItemRenderer?.invoke(renderer) // Chuyển đổi MusicResponsiveListItemRenderer thành Innertube.Item nếu có hàm chuyển đổi.
            } ?: it.musicTwoRowItemRenderer?.toItem() // nếu musicResponsiveListItemRenderer null thử Chuyển đổi các mục trong băng chuyền thành các mục Innertube.Item.
                ?: it.musicNavigationButtonRenderer?.toItem() // Chuyển đổi các nút điều hướng thành các mục Innertube.Item.
        }
        .orEmpty()
)

data class BrowseResult( // Lớp này đại diện cho kết quả của một yêu cầu duyệt (browse) trong ứng dụng YouTube Music.
    val title: String?,
    val items: List<Item>
) {
    data class Item(
        val title: String?,
        val items: List<Innertube.Item>
    )
}

fun MusicTwoRowItemRenderer.toItem() = when { // Chuyển đổi một MusicTwoRowItemRenderer thành một mục Innertube.Item.
    isAlbum -> Innertube.AlbumItem.from(this) // Chuyển đổi thành một mục album.
    isPlaylist -> Innertube.PlaylistItem.from(this) // Chuyển đổi thành một mục danh sách phát.
    isArtist -> Innertube.ArtistItem.from(this) // Chuyển đổi thành một mục nghệ sĩ.
    else -> null
}

fun MusicNavigationButtonRenderer.toItem() = when { // Chuyển đổi một MusicNavigationButtonRenderer thành một mục Innertube.Item.
    isMood -> toMood()
    else -> null
}
