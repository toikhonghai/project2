package app.vitune.android.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.handleUrl
import app.vitune.android.models.Mood
import app.vitune.android.models.SearchQuery
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.query
import app.vitune.android.ui.screens.album.AlbumScreen
import app.vitune.android.ui.screens.artist.ArtistScreen
import app.vitune.android.ui.screens.pipedplaylist.PipedPlaylistScreen
import app.vitune.android.ui.screens.playlist.PlaylistScreen
import app.vitune.android.ui.screens.search.SearchScreen
import app.vitune.android.ui.screens.searchresult.SearchResultScreen
import app.vitune.android.ui.screens.settings.LogsScreen
import app.vitune.android.ui.screens.settings.SettingsScreen
import app.vitune.android.utils.toast
import app.vitune.compose.routing.Route0
import app.vitune.compose.routing.Route1
import app.vitune.compose.routing.Route3
import app.vitune.compose.routing.Route4
import app.vitune.compose.routing.RouteHandlerScope
import app.vitune.core.data.enums.BuiltInPlaylist
import io.ktor.http.Url
import java.util.UUID

/**
 * Marker class for linters that a composable is a route and should not be handled like a regular
 * composable, but rather as an entrypoint.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Route // Đánh dấu cho các hàm composable là route, không nên xử lý như các composable thông thường mà nên coi như là điểm vào (entrypoint) của ứng dụng.

val albumRoute = Route1<String>("albumRoute")
val artistRoute = Route1<String>("artistRoute")
val builtInPlaylistRoute = Route1<BuiltInPlaylist>("builtInPlaylistRoute")
val localPlaylistRoute = Route1<Long>("localPlaylistRoute")
val logsRoute = Route0("logsRoute")
val pipedPlaylistRoute = Route3<String, String, String>("pipedPlaylistRoute")
val playlistRoute = Route4<String, String?, Int?, Boolean>("playlistRoute")
val moodRoute = Route1<Mood>("moodRoute")
val searchResultRoute = Route1<String>("searchResultRoute")
val searchRoute = Route1<String>("searchRoute")
val settingsRoute = Route0("settingsRoute")

@Composable
fun RouteHandlerScope.GlobalRoutes() { // Hàm extension Composable mở rộng cho RouteHandlerScope để định nghĩa các route toàn cục.

    val context = LocalContext.current // Lấy context hiện tại (thường dùng để gọi intent, toast, v.v.)
    val binder = LocalPlayerServiceBinder.current // Lấy binder để tương tác với player service nếu cần

    // Khi gọi albumRoute(browseId), sẽ hiển thị màn hình AlbumScreen với browseId được truyền
    albumRoute { browseId ->
        AlbumScreen(browseId = browseId)
    }

    // Khi gọi artistRoute(browseId), sẽ hiển thị màn hình ArtistScreen với browseId được truyền
    artistRoute { browseId ->
        ArtistScreen(browseId = browseId)
    }

    // Khi gọi logsRoute(), hiển thị màn hình Logs
    logsRoute {
        LogsScreen()
    }

    // Khi gọi pipedPlaylistRoute(apiBaseUrl, sessionToken, playlistId), hiển thị màn hình playlist từ dịch vụ Piped
    pipedPlaylistRoute { apiBaseUrl, sessionToken, playlistId ->
        PipedPlaylistScreen(
            apiBaseUrl = runCatching { Url(apiBaseUrl) }.getOrNull()
                ?: error("Invalid apiBaseUrl: $apiBaseUrl is not a valid Url"), // kiểm tra và ép apiBaseUrl thành kiểu Url
            sessionToken = sessionToken,
            playlistId = runCatching {
                UUID.fromString(playlistId)
            }.getOrNull() ?: error("Invalid playlistId: $playlistId is not a valid UUID") // kiểm tra playlistId có hợp lệ UUID không
        )
    }

    // Khi gọi playlistRoute(browseId, params, maxDepth, shouldDedup), hiển thị màn hình Playlist với các tham số
    playlistRoute { browseId, params, maxDepth, shouldDedup ->
        PlaylistScreen(
            browseId = browseId,
            params = params,
            maxDepth = maxDepth,
            shouldDedup = shouldDedup
        )
    }

    // Khi gọi settingsRoute(), hiển thị màn hình cài đặt
    settingsRoute {
        SettingsScreen()
    }

    // Khi gọi searchRoute(initialTextInput), hiển thị màn hình tìm kiếm với input ban đầu là initialTextInput
    searchRoute { initialTextInput ->
        SearchScreen(
            initialTextInput = initialTextInput,

            // Khi người dùng tìm kiếm, hiển thị SearchResultScreen tương ứng
            onSearch = { query ->
                searchResultRoute(query)

                // Nếu không tắt lưu lịch sử tìm kiếm, thì lưu lại vào database
                if (!DataPreferences.pauseSearchHistory) query {
                    Database.insert(SearchQuery(query = query))
                }
            },

            // Khi người dùng nhấn vào playlist chứa URL, xử lý mở URL
            onViewPlaylist = { url ->
                with(context) {
                    runCatching {
                        handleUrl(url.toUri(), binder) // Chuyển URL sang URI và xử lý mở
                    }.onFailure {
                        toast(getString(R.string.error_url, url)) // Nếu lỗi, hiện thông báo
                    }
                }
            }
        )
    }

    // Khi gọi searchResultRoute(query), hiển thị màn hình kết quả tìm kiếm
    searchResultRoute { query ->
        SearchResultScreen(
            query = query,
            onSearchAgain = { searchRoute(query) } // Cho phép tìm lại query cũ
        )
    }
}
