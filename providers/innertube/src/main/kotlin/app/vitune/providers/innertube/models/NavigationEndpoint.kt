package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

/**
 * watchPlaylistEndpoint: params, playlistId
 * watchEndpoint: params, playlistId, videoId, index
 * browseEndpoint: params, browseId
 * searchEndpoint: params, query
 */
//Sử dụng để quản lý các endpoint (điểm điều hướng) trong ứng dụng nhạc,
// giúp xác định khi người dùng nhấn vào một mục trong danh sách nhạc, hệ thống sẽ điều hướng đến đâu.
@Serializable
data class NavigationEndpoint(
    val watchEndpoint: Endpoint.Watch?,
    val watchPlaylistEndpoint: Endpoint.WatchPlaylist?,
    val browseEndpoint: Endpoint.Browse?,
    val searchEndpoint: Endpoint.Search?
) {
    val endpoint get() = watchEndpoint ?: browseEndpoint ?: watchPlaylistEndpoint ?: searchEndpoint // Lấy endpoint đầu tiên không null từ các endpoint khác nhau

    @Serializable
    sealed class Endpoint { // Lớp cha cho các endpoint khác nhau
        @Serializable
        data class Watch(
            val params: String? = null,
            val playlistId: String? = null,
            val videoId: String? = null,
            val index: Int? = null,
            val playlistSetVideoId: String? = null,
            val watchEndpointMusicSupportedConfigs: WatchEndpointMusicSupportedConfigs? = null
        ) : Endpoint() {
            @Serializable
            data class WatchEndpointMusicSupportedConfigs( // Lớp này chứa các cấu hình hỗ trợ cho video nhạc
                val watchEndpointMusicConfig: WatchEndpointMusicConfig?
            ) {
                @Serializable
                data class WatchEndpointMusicConfig(
                    val musicVideoType: String? //  Xác định loại video nhạc (ví dụ: "MUSIC_VIDEO_TYPE_ATV" cho video chính thức).
                )
            }
        }

        @Serializable
        data class WatchPlaylist( // Điều hướng đến danh sách phát
            val params: String?,
            val playlistId: String?
        ) : Endpoint()

        @Serializable
        data class Browse( // Điều hướng đến trang duyệt (nghệ sĩ, album)
            val params: String? = null,
            val browseId: String? = null,
            val browseEndpointContextSupportedConfigs: BrowseEndpointContextSupportedConfigs? = null
        ) : Endpoint() {
            val type: String?
                get() = browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig
                    ?.pageType

            @Serializable
            data class BrowseEndpointContextSupportedConfigs(
                val browseEndpointContextMusicConfig: BrowseEndpointContextMusicConfig
            ) {
                @Serializable
                data class BrowseEndpointContextMusicConfig( // Xác định trang này thuộc loại gì (album, nghệ sĩ, danh mục...).
                    val pageType: String
                )
            }
        }

        @Serializable
        data class Search(
            val params: String?,
            val query: String
        ) : Endpoint()
    }
}
