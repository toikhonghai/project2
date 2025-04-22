package app.vitune.android.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import app.vitune.android.R
import app.vitune.android.models.toUiMood
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.albumRoute
import app.vitune.android.ui.screens.artistRoute
import app.vitune.android.ui.screens.builtInPlaylistRoute
import app.vitune.android.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import app.vitune.android.ui.screens.localPlaylistRoute
import app.vitune.android.ui.screens.localplaylist.LocalPlaylistScreen
import app.vitune.android.ui.screens.mood.MoodScreen
import app.vitune.android.ui.screens.mood.MoreAlbumsScreen
import app.vitune.android.ui.screens.mood.MoreMoodsScreen
import app.vitune.android.ui.screens.moodRoute
import app.vitune.android.ui.screens.pipedPlaylistRoute
import app.vitune.android.ui.screens.playlistRoute
import app.vitune.android.ui.screens.searchRoute
import app.vitune.android.ui.screens.settingsRoute
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.Route0
import app.vitune.compose.routing.RouteHandler

// Định nghĩa route tĩnh dùng để điều hướng tới màn hình hiển thị thêm mood và album
private val moreMoodsRoute = Route0("moreMoodsRoute")
private val moreAlbumsRoute = Route0("moreAlbumsRoute")

@Route
@Composable
fun HomeScreen() {
    // Lưu trạng thái của từng tab để giữ lại khi chuyển tab
    val saveableStateHolder = rememberSaveableStateHolder()

    // Xóa cache liên quan đến "home/" khi không cần nữa
    PersistMapCleanup("home/")

    // Xử lý điều hướng các route bên trong màn hình Home
    RouteHandler {
        GlobalRoutes() // Route toàn cục (định nghĩa ở nơi khác)

        // Các route chi tiết khác nhau bên trong màn hình Home
        localPlaylistRoute { playlistId -> // playlistId = args[0] as P0
            LocalPlaylistScreen(playlistId = playlistId)
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(builtInPlaylist = builtInPlaylist)
        }

        moodRoute { mood ->
            MoodScreen(mood = mood)
        }

        moreMoodsRoute {
            MoreMoodsScreen()
        }

        moreAlbumsRoute {
            MoreAlbumsScreen()
        }

        // Giao diện nội dung chính của màn hình Home
        Content {
            Scaffold(
                key = "home",
                topIconButtonId = R.drawable.settings, // Nút cài đặt ở góc trên
                onTopIconButtonClick = { settingsRoute() }, // Khi nhấn mở màn hình cài đặt
                tabIndex = UIStatePreferences.homeScreenTabIndex, // Tab đang được chọn
                onTabChange = { UIStatePreferences.homeScreenTabIndex = it }, // Khi chuyển tab
                tabColumnContent = {
                    // Danh sách các tab hiển thị phía trên giao diện
                    tab(0, R.string.quick_picks, R.drawable.sparkles)     // Gợi ý nhanh
                    tab(1, R.string.discover, R.drawable.globe)           // Khám phá
                    tab(2, R.string.songs, R.drawable.musical_notes)      // Bài hát
                    tab(3, R.string.playlists, R.drawable.playlist)       // Playlist
                    tab(4, R.string.artists, R.drawable.person)           // Nghệ sĩ
                    tab(5, R.string.albums, R.drawable.disc)              // Album
                    tab(6, R.string.local, R.drawable.download)           // Nhạc offline
                }
            ) { currentTabIndex ->
                // Mỗi tab giữ lại trạng thái riêng nhờ SaveableStateHolder
                saveableStateHolder.SaveableStateProvider(key = currentTabIndex) {
                    val onSearchClick = { searchRoute("") } // Hành động khi nhấn tìm kiếm

                    // Hiển thị nội dung tương ứng với tab hiện tại
                    when (currentTabIndex) {
                        0 -> QuickPicks(
                            onAlbumClick = { albumRoute(it.key) },
                            onArtistClick = { artistRoute(it.key) },
                            onPlaylistClick = {
                                playlistRoute(
                                    p0 = it.key,
                                    p1 = null,
                                    p2 = null,
                                    p3 = it.channel?.name == "YouTube Music"
                                )
                            },
                            onSearchClick = onSearchClick
                        )

                        1 -> HomeDiscovery(
                            onMoodClick = { mood -> moodRoute(mood.toUiMood()) },
                            onNewReleaseAlbumClick = { albumRoute(it) },
                            onSearchClick = onSearchClick,
                            onMoreMoodsClick = { moreMoodsRoute() },
                            onMoreAlbumsClick = { moreAlbumsRoute() },
                            onPlaylistClick = { playlistRoute(it, null, null, true) }
                        )

                        2 -> HomeSongs(
                            onSearchClick = onSearchClick
                        )

                        3 -> HomePlaylists(
                            onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                            onPlaylistClick = { localPlaylistRoute(it.id) },
                            onPipedPlaylistClick = { session, playlist ->
                                pipedPlaylistRoute(
                                    p0 = session.apiBaseUrl.toString(),
                                    p1 = session.token,
                                    p2 = playlist.id.toString()
                                )
                            },
                            onSearchClick = onSearchClick
                        )

                        4 -> HomeArtistList(
                            onArtistClick = { artistRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        5 -> HomeAlbums(
                            onAlbumClick = { albumRoute(it.id) },
                            onSearchClick = onSearchClick
                        )

                        6 -> HomeLocalSongs(
                            onSearchClick = onSearchClick
                        )
                    }
                }
            }
        }
    }
}
