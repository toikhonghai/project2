package app.vitune.providers.innertube.utils

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.Innertube.PodcastItem
import app.vitune.providers.innertube.models.MusicResponsiveListItemRenderer
import app.vitune.providers.innertube.models.MusicShelfRenderer

// ✅ Hàm mở rộng để chuyển từ MusicResponsiveListItemRenderer sang PodcastItem
fun PodcastItem.Companion.from(renderer: MusicResponsiveListItemRenderer) = PodcastItem(
    info = renderer.flexColumns
        .getOrNull(0)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.firstOrNull()
        ?.let(Innertube::Info),

    authors = renderer.flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.filter { it.navigationEndpoint?.browseEndpoint != null }
        ?.map(Innertube::Info),

    description = renderer.flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.lastOrNull()
        ?.text,

    episodeCount = renderer.flexColumns
        .getOrNull(1)
        ?.musicResponsiveListItemFlexColumnRenderer
        ?.text
        ?.runs
        ?.lastOrNull()
        ?.text
        ?.let { "\\d+".toRegex().find(it)?.value?.toIntOrNull() },

    thumbnail = renderer.thumbnail
        ?.musicThumbnailRenderer
        ?.thumbnail
        ?.thumbnails
        ?.firstOrNull()
).takeIf { it.info?.endpoint?.browseId != null }


//// ✅ Hàm mở rộng để chuyển MusicShelfRenderer sang ItemsPage<PodcastItem>
//fun MusicShelfRenderer.toItemsPage(): Innertube.ItemsPage<PodcastItem> {
//    val items = contents?.mapNotNull {
//        it.musicResponsiveListItemRenderer?.let(PodcastItem.Companion::from)
//    } ?: emptyList()
//
//    val continuation = continuations?.firstOrNull()
//        ?.nextContinuationData?.continuation
//
//    return Innertube.ItemsPage(items, continuation)
//}
/**
 * Chuyển đổi MusicShelfRenderer thành ItemsPage chứa danh sách PodcastItem
 * @param mapper Hàm chuyển đổi từng Content thành Item cụ thể
 */
fun <T : Innertube.Item> MusicShelfRenderer?.toItemsPage(
    mapper: (MusicShelfRenderer.Content) -> T?
) = Innertube.ItemsPage(
    items = this
        ?.contents
        ?.mapNotNull(mapper), // Lọc bỏ null, giữ các item hợp lệ
    continuation = this
        ?.continuations
        ?.firstOrNull()
        ?.nextContinuationData
        ?.continuation // Token để tải thêm dữ liệu (nếu có)
)
