package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.BrowseResponse
import app.vitune.providers.innertube.models.MusicTwoRowItemRenderer
import app.vitune.providers.innertube.models.bodies.BrowseBody
import app.vitune.providers.innertube.models.oddElements
import app.vitune.providers.innertube.models.splitBySeparator
import app.vitune.providers.innertube.utils.from
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

// đây là một đoạn mã Kotlin sử dụng thư viện Ktor để gửi yêu cầu HTTP POST đến một API của YouTube.
// Nó định nghĩa một hàm mở rộng cho lớp Innertube, có tên là discoverPage.
// Hàm này được sử dụng để lấy thông tin về trang khám phá (discover page) của YouTube Music.
suspend fun Innertube.discoverPage() = runCatchingCancellable {
    val response = client.post(BROWSE) {
        setBody(BrowseBody(browseId = "FEmusic_explore"))
        mask("contents") // chỉ lấy phần "contents" trong phản hồi.
    }.body<BrowseResponse>() // Gọi hàm body để lấy phản hồi từ yêu cầu HTTP và chuyển đổi nó thành đối tượng BrowseResponse.

    val sections = response
        .contents
        ?.singleColumnBrowseResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents // Lấy danh sách các phần trong phản hồi.

    Innertube.DiscoverPage(
        // Lấy danh sách các mục từ phản hồi.
        newReleaseAlbums = sections?.find {
            it.musicCarouselShelfRenderer
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
                ?.browseId == "FEmusic_new_releases_albums"
        }?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull { it.musicTwoRowItemRenderer?.toNewReleaseAlbumPage() }
            .orEmpty(),
        // Lấy danh sách các bài hát từ phản hồi.
        moods = sections?.find {
            it.musicCarouselShelfRenderer
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
                ?.browseId == "FEmusic_moods_and_genres"
        }?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull { it.musicNavigationButtonRenderer?.toMood() }
            .orEmpty(),
        trending = run { // khởi tạo một khối lệnh mới để tránh xung đột với biến sections
            val renderer = sections?.find {
                it.musicCarouselShelfRenderer
                    ?.header
                    ?.musicCarouselShelfBasicHeaderRenderer
                    ?.moreContentButton
                    ?.buttonRenderer
                    ?.navigationEndpoint
                    ?.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig
                    ?.pageType == "MUSIC_PAGE_TYPE_PLAYLIST"
            }?.musicCarouselShelfRenderer // tìm kiếm trong danh sách nội dung

            Innertube.DiscoverPage.Trending( // tạo một đối tượng DiscoverPage.Trending từ phản hồi.
                songs = renderer
                    ?.toBrowseItem(Innertube.SongItem::from) // chuyển đổi nội dung thành một mục lưới.
                    ?.items
                    ?.filterIsInstance<Innertube.SongItem>()  // lọc các mục trong danh sách để chỉ lấy các mục là bài hát.
                    ?.map { song -> // Why, YouTube, why
                        song.copy(
                            authors = song.authors?.firstOrNull()?.let { listOf(it) } ?: emptyList() // lấy danh sách các tác giả từ bài hát.
                        )
                    }
                    .orEmpty(),
                endpoint = renderer
                    ?.header
                    ?.musicCarouselShelfBasicHeaderRenderer
                    ?.moreContentButton
                    ?.buttonRenderer
                    ?.navigationEndpoint
                    ?.browseEndpoint
            )
        }
    )
}

// hàm mở rộng này chuyển đổi một đối tượng MusicTwoRowItemRenderer thành một đối tượng AlbumItem trong Innertube.
fun MusicTwoRowItemRenderer.toNewReleaseAlbumPage() = Innertube.AlbumItem(
    info = Innertube.Info(
        name = title?.text,
        endpoint = navigationEndpoint?.browseEndpoint
    ),
    authors = subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.oddElements()?.map {
        Innertube.Info(
            name = it.text,
            endpoint = it.navigationEndpoint?.browseEndpoint
        )
    },
    year = subtitle?.runs?.lastOrNull()?.text,
    thumbnail = thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails?.firstOrNull()
)
