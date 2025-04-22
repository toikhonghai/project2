package app.vitune.providers.innertube.utils

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.MusicResponsiveListItemRenderer
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.isExplicit

//trích xuất thông tin bài hát từ một MusicResponsiveListItemRenderer
//Hàm mở rộng (extension function) này cho lớp Innertube.SongItem.Companion, cho phép tạo một đối tượng SongItem từ một đối tượng MusicResponsiveListItemRenderer.
fun Innertube.SongItem.Companion.from(renderer: MusicResponsiveListItemRenderer) =
    Innertube.SongItem(
        info = renderer
            .flexColumns
            .getOrNull(0) // lấy cột đầu tiên trong danh sách các cột
            ?.musicResponsiveListItemFlexColumnRenderer // lấy cột đầu tiên trong danh sách các cột
            ?.text // lấy thông tin văn bản từ cột đầu tiên
            ?.runs // lấy danh sách các đoạn văn bản
            ?.getOrNull(0) // lấy đoạn văn bản đầu tiên
            ?.let {
                if (it.navigationEndpoint?.endpoint is NavigationEndpoint.Endpoint.Watch) Innertube.Info( // nếu endpoint là một video
                    name = it.text,
                    endpoint = it.navigationEndpoint.endpoint as NavigationEndpoint.Endpoint.Watch // lấy endpoint từ navigationEndpoint
                ) else null
            },
        authors = renderer
            .flexColumns
            .getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.map { Innertube.Info(name = it.text, endpoint = it.navigationEndpoint?.endpoint) } // lấy tên nghệ sĩ từ cột thứ hai
            ?.filterIsInstance<Innertube.Info<NavigationEndpoint.Endpoint.Browse>>() // lọc các đối tượng có kiểu là Browse
            ?.takeIf(List<Any>::isNotEmpty),
        durationText = renderer
            .fixedColumns
            ?.getOrNull(0)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.getOrNull(0)
            ?.text,
        album = renderer
            .flexColumns
            .getOrNull(2)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.firstOrNull()
            ?.let(Innertube::Info), // lấy tên album từ cột thứ ba
        explicit = renderer.badges.isExplicit,
        thumbnail = renderer
            .thumbnail
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.firstOrNull()
    ).takeIf { it.info?.endpoint?.videoId != null } // kiểm tra xem video có chứa nội dung người lớn hay không
