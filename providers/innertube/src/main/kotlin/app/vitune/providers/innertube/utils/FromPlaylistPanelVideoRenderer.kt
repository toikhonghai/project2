package app.vitune.providers.innertube.utils

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.PlaylistPanelVideoRenderer
import app.vitune.providers.innertube.models.isExplicit

// hàm mở rộng để chuyển đổi từ PlaylistPanelVideoRenderer sang Innertube.SongItem
fun Innertube.SongItem.Companion.from(renderer: PlaylistPanelVideoRenderer) = Innertube.SongItem(
    info = Innertube.Info( // tạo một đối tượng Info từ renderer
        name = renderer
            .title
            ?.text,
        endpoint = renderer
            .navigationEndpoint
            ?.watchEndpoint // lấy endpoint từ navigationEndpoint
    ),
    authors = renderer
        .longBylineText
        ?.splitBySeparator()
        ?.getOrNull(0)
        ?.map(Innertube::Info), // lấy tên nghệ sĩ từ longBylineText
    album = renderer
        .longBylineText
        ?.splitBySeparator()
        ?.getOrNull(1)
        ?.getOrNull(0)
        ?.let(Innertube::Info), // lấy tên album từ longBylineText
    thumbnail = renderer
        .thumbnail
        ?.thumbnails
        ?.getOrNull(0),
    durationText = renderer
        .lengthText
        ?.text, // lấy thời gian phát từ lengthText
    explicit = renderer.badges.isExplicit // kiểm tra xem video có chứa nội dung người lớn hay không
).takeIf { it.info?.endpoint?.videoId != null }
