package app.vitune.android.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.Log
import app.vitune.android.R
import app.vitune.android.models.toUiMood
import app.vitune.android.preferences.UIStatePreferences
import app.vitune.android.ui.components.themed.MainTab
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.components.themed.TabsBuilder
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.accountSettingsRoute
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
import app.vitune.android.ui.screens.podcast.HomeLocalPodcast
import app.vitune.android.ui.screens.podcast.HomePodcastPlaylists
import app.vitune.android.ui.screens.podcast.HomePodcasts
import app.vitune.android.ui.screens.podcast.PodcastPlaylistScreen
import app.vitune.android.ui.screens.podcast.SubscribedPodcastsScreen
import app.vitune.android.ui.screens.podcast.SuggestedPodcastsScreen
import app.vitune.android.ui.screens.podcastPlaylistRoute
import app.vitune.android.ui.screens.podcastRoute
import app.vitune.android.ui.screens.searchRoute
import app.vitune.android.ui.screens.settingsRoute
import app.vitune.compose.persist.LocalPersistMap
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.Route0
import app.vitune.compose.routing.RouteHandler

private val moreMoodsRoute = Route0("moreMoodsRoute")
private val moreAlbumsRoute = Route0("moreAlbumsRoute")

@Route
@Composable
fun HomeScreen() {
    val saveableStateHolder = rememberSaveableStateHolder()

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Music) }
    var musicTabIndex by rememberSaveable { mutableStateOf(UIStatePreferences.homeScreenTabIndex) }
    var podcastTabIndex by rememberSaveable { mutableStateOf(0) }

    val persistMap = LocalPersistMap.current

    LaunchedEffect(selectedTab, persistMap) {
        if (selectedTab == MainTab.Music) {
            persistMap?.clean("podcastScreen/")
            persistMap?.clean("podcastDetail/")
            persistMap?.clean("home/trending") // Thêm dòng này
            persistMap?.clean("home/quickPicks/relatedPageResult") // Thêm dòng này
        } else {
            persistMap?.clean("home/")
        }
    }

    RouteHandler {
        GlobalRoutes()

        localPlaylistRoute { playlistId ->
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

        podcastPlaylistRoute { playlist ->
            PodcastPlaylistScreen(
                playlist = playlist,
                onSearchClick = { searchRoute("") }
            )
        }

        Content {
            val onSearchClick = { searchRoute("") }

            val musicTabColumnContent: TabsBuilder.() -> Unit = {
                tab(0, R.string.quick_picks, R.drawable.sparkles)
                tab(1, R.string.discover, R.drawable.globe)
                tab(2, R.string.songs, R.drawable.musical_notes)
                tab(3, R.string.playlists, R.drawable.playlist)
                tab(4, R.string.artists, R.drawable.person)
                tab(5, R.string.albums, R.drawable.disc)
                tab(6, R.string.local, R.drawable.download)
            }

            val podcastTabColumnContent: TabsBuilder.() -> Unit = {
                tab(0, R.string.subscribed_podcasts, R.drawable.podcast, canHide = false)
                tab(1, R.string.suggested_podcasts, R.drawable.sparkles, canHide = false)
                tab(2, R.string.podcasts, R.drawable.musical_notes, canHide = false)
                tab(3, R.string.playlists, R.drawable.playlist, canHide = false)
                tab(4, R.string.local, R.drawable.download, canHide = false)
            }

            Scaffold(
                key = if (selectedTab == MainTab.Music) "home-music" else "home-podcast",
                topIconButtonId = R.drawable.settings,
                iconAccountButtonId = R.drawable.account,
                onTopIconButtonClick = { settingsRoute() },
                tabIndex = if (selectedTab == MainTab.Music) musicTabIndex else podcastTabIndex,
                onTabChange = { index ->
                    if (selectedTab == MainTab.Music) {
                        musicTabIndex = index
                        UIStatePreferences.homeScreenTabIndex = index
                    } else {
                        podcastTabIndex = index
                    }
                },
                tabColumnContent = if (selectedTab == MainTab.Music) musicTabColumnContent else podcastTabColumnContent,
                showMainTabs = true,
                selectedMainTab = selectedTab,
                onMainTabSelected = { newTab ->
                    if (selectedTab != newTab) {
                        selectedTab = newTab
                        Log.e("screen podcast", "HomeScreen: selectedTab updated to $newTab")
                    }
                },
                onAccountIconClick = {
                    Log.d("HomeScreen", "Account icon clicked, navigating to AccountSettingsScreen")
                    accountSettingsRoute() }
            ) { currentIndex ->
                saveableStateHolder.SaveableStateProvider(key = "${selectedTab}_$currentIndex") {
                    when (selectedTab) {
                        MainTab.Music -> {
                            when (currentIndex) {
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
                        MainTab.Podcast -> {
                            when (currentIndex) {
                                0 -> SubscribedPodcastsScreen(
                                    onSearchClick = onSearchClick,
                                    navigationToPodcastDetail = { browseId -> podcastRoute(browseId) }
                                )
                                1 -> SuggestedPodcastsScreen(
                                    onSearchClick = onSearchClick,
                                    navigationToPodcastDetail = { browseId -> podcastRoute(browseId) }
                                )
                                2 -> HomePodcasts(
                                    onSearchClick = onSearchClick
                                )
                                3 -> HomePodcastPlaylists(
                                    onPlaylistClick = { podcastPlaylistRoute(it) },
                                    onSearchClick = onSearchClick
                                )
                                4 -> HomeLocalPodcast(
                                    onSearchClick = onSearchClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}