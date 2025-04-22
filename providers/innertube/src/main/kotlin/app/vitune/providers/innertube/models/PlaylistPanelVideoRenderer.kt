package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistPanelVideoRenderer( // đại diện cho một video trong danh sách phát trên YouTube Music.
    val title: Runs?,
    val longBylineText: Runs?,
    val shortBylineText: Runs?,
    val lengthText: Runs?,
    val navigationEndpoint: NavigationEndpoint?, // điều hướng đến video hoặc danh sách phát khi nhấp vào tiêu đề hoặc tên nghệ sĩ.
    val thumbnail: ThumbnailRenderer.MusicThumbnailRenderer.Thumbnail?, // chứa thông tin về hình thu nhỏ của video.
    val badges: List<Badge>? // chứa thông tin về huy hiệu của video (ví dụ: huy hiệu "Đã phát hành gần đây", "Đã phát hành trong tuần này",...
)
