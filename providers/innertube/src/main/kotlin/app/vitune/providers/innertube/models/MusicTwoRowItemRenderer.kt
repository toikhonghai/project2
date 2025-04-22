package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
// Đây là một lớp dữ liệu đại diện cho một mục trong danh sách phát nhạc trên YouTube Music.
data class MusicTwoRowItemRenderer(
    val navigationEndpoint: NavigationEndpoint?, //Điều hướng khi người dùng nhấn vào mục.
    val thumbnailRenderer: ThumbnailRenderer?, //Hình thu nhỏ của mục.
    val title: Runs?, //Tiêu đề của mục.
    val subtitle: Runs?, //Phụ đề của mục, có thể chứa thông tin bổ sung như tên nghệ sĩ hoặc album.
    val thumbnailOverlay: ThumbnailOverlay? //Lớp phủ hình thu nhỏ, có thể chứa các thông tin bổ sung như thời gian phát lại hoặc huy hiệu.
) {
    val isPlaylist: Boolean //Xác định xem mục này có phải là danh sách phát hay không.
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs
            ?.browseEndpointContextMusicConfig?.pageType == "MUSIC_PAGE_TYPE_PLAYLIST"

    val isAlbum: Boolean // Xác định xem mục này có phải là album hay không.
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs
            ?.browseEndpointContextMusicConfig?.pageType == "MUSIC_PAGE_TYPE_ALBUM" ||
                navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig?.pageType == "MUSIC_PAGE_TYPE_AUDIOBOOK"

    val isArtist: Boolean //Xác định xem mục này có phải là nghệ sĩ hay không.
        get() = navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs
            ?.browseEndpointContextMusicConfig?.pageType == "MUSIC_PAGE_TYPE_ARTIST"
}
