package app.vitune.android.service

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.ServiceConnection
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.android.models.Album
import app.vitune.android.models.PlaylistPreview
import app.vitune.android.models.PodcastEntity
import app.vitune.android.models.PodcastEpisodeEntity
import app.vitune.android.models.Song
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.preferences.OrderPreferences
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.forcePlayAtIndex
import app.vitune.android.utils.forceSeekToNext
import app.vitune.android.utils.forceSeekToPrevious
import app.vitune.android.utils.intent
import app.vitune.core.data.utils.CallValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.MediaDescription as BrowserMediaDescription
import android.media.browse.MediaBrowser.MediaItem as BrowserMediaItem
import android.service.media.MediaBrowserService
import kotlinx.coroutines.runBlocking

class PlayerMediaBrowserService : MediaBrowserService(), ServiceConnection {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastSongs = emptyList<Song>()
    private var lastPodcasts = emptyList<PodcastEntity>()

    private var bound = false

    private val callValidator by lazy {
        CallValidator(applicationContext, R.xml.allowed_media_browser_callers)
    }

    override fun onDestroy() {
        if (bound) unbindService(this)
        super.onDestroy()
    }

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        if (service !is PlayerService.Binder) return
        bound = true
        sessionToken = service.mediaSession.sessionToken
        service.mediaSession.setCallback(SessionCallback(service))
    }

    override fun onServiceDisconnected(name: ComponentName) = Unit

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ) = if (callValidator.canCall(clientPackageName, clientUid)) {
        bindService(intent<PlayerService>(), this, Context.BIND_AUTO_CREATE)
        BrowserRoot(
            MediaId.ROOT.id,
            bundleOf("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT" to 1)
        )
    } else null

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<BrowserMediaItem>>
    ) {
        // Use result.detach() to handle async loading
        result.detach()

        coroutineScope.launch {
            try {
                val items = when (MediaId(parentId)) {
                    MediaId.ROOT -> mutableListOf(
                        songsBrowserMediaItem,
                        playlistsBrowserMediaItem,
                        albumsBrowserMediaItem,
                        podcastsBrowserMediaItem
                    )

                    MediaId.SONGS -> {
                        val songs = Database
                            .songsByPlayTimeDesc(limit = 30)
                            .first()
                            .also { lastSongs = it }
                            .map { song: Song -> song.asBrowserMediaItem }
                        val songList = songs.toMutableList()
                        if (songList.isNotEmpty()) songList.add(0, shuffleBrowserMediaItem)
                        songList
                    }

                    MediaId.PLAYLISTS -> {
                        val playlists = Database
                            .playlistPreviewsByDateAddedDesc()
                            .first()
                            .map { playlist -> playlist.asBrowserMediaItem }
                        val playlistList = playlists.toMutableList()
                        playlistList.add(0, favoritesBrowserMediaItem)
                        playlistList.add(1, offlineBrowserMediaItem)
                        playlistList.add(2, topBrowserMediaItem)
                        playlistList.add(3, localBrowserMediaItem)
                        playlistList
                    }

                    MediaId.ALBUMS -> {
                        val albums = Database
                            .albumsByRowIdDesc()
                            .first()
                            .map { album -> album.asBrowserMediaItem }
                        albums.toMutableList()
                    }

                    MediaId.PODCASTS -> {
                        val podcasts = Database
                            .getSubscribedPodcasts()
                            .first()
                            .also { lastPodcasts = it }
                            .map { podcast: PodcastEntity -> podcast.asBrowserMediaItem }
                        podcasts.toMutableList()
                    }

                    else -> {
                        // Handle podcast episodes for a specific podcast
                        val browseId = parentId.split('/').getOrNull(1)
                        if (browseId != null && parentId.startsWith(MediaId.PODCASTS.id)) {
                            val episodes = Database
                                .getEpisodesForPodcastAsFlow(browseId)
                                .first()
                                .map { episode: PodcastEpisodeEntity -> episode.asBrowserMediaItem }
                            episodes.toMutableList()
                        } else {
                            mutableListOf()
                        }
                    }
                }

                // Send the result on the main thread
                withContext(Dispatchers.Main) {
                    result.sendResult(items)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    result.sendResult(mutableListOf())
                }
            }
        }
    }

    private fun uriFor(@DrawableRes id: Int) = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(resources.getResourcePackageName(id))
        .appendPath(resources.getResourceTypeName(id))
        .appendPath(resources.getResourceEntryName(id))
        .build()

    private val shuffleBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.SHUFFLE.id)
                .setTitle(getString(R.string.shuffle))
                .setIconUri(uriFor(R.drawable.shuffle))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val songsBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.SONGS.id)
                .setTitle(getString(R.string.songs))
                .setIconUri(uriFor(R.drawable.musical_notes))
                .build(),
            BrowserMediaItem.FLAG_BROWSABLE
        )

    private val playlistsBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.PLAYLISTS.id)
                .setTitle(getString(R.string.playlists))
                .setIconUri(uriFor(R.drawable.playlist))
                .build(),
            BrowserMediaItem.FLAG_BROWSABLE
        )

    private val albumsBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.ALBUMS.id)
                .setTitle(getString(R.string.albums))
                .setIconUri(uriFor(R.drawable.disc))
                .build(),
            BrowserMediaItem.FLAG_BROWSABLE
        )

    private val podcastsBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.PODCASTS.id)
                .setTitle(getString(R.string.podcast))
                .setIconUri(uriFor(R.drawable.podcast))
                .build(),
            BrowserMediaItem.FLAG_BROWSABLE
        )

    private val favoritesBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.FAVORITES.id)
                .setTitle(getString(R.string.favorites))
                .setIconUri(uriFor(R.drawable.heart))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val offlineBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.OFFLINE.id)
                .setTitle(getString(R.string.offline))
                .setIconUri(uriFor(R.drawable.airplane))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val topBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.TOP.id)
                .setTitle(
                    getString(
                        R.string.format_my_top_playlist,
                        DataPreferences.topListLength.toString()
                    )
                )
                .setIconUri(uriFor(R.drawable.trending))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val localBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.LOCAL.id)
                .setTitle(getString(R.string.local))
                .setIconUri(uriFor(R.drawable.download))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val Song.asBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId((MediaId.SONGS / id).id)
                .setTitle(title)
                .setSubtitle(artistsText)
                .setIconUri(thumbnailUrl?.toUri())
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val PlaylistPreview.asBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId((MediaId.PLAYLISTS / playlist.id.toString()).id)
                .setTitle(playlist.name)
                .setSubtitle(
                    resources.getQuantityString(
                        R.plurals.song_count_plural,
                        songCount,
                        songCount
                    )
                )
                .setIconUri(uriFor(R.drawable.playlist))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val Album.asBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId((MediaId.ALBUMS / id).id)
                .setTitle(title)
                .setSubtitle(authorsText)
                .setIconUri(thumbnailUrl?.toUri())
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val PodcastEntity.asBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId((MediaId.PODCASTS / browseId).id)
                .setTitle(title)
                .setSubtitle(authorName)
                .setIconUri(thumbnailUrl?.toUri() ?: uriFor(R.drawable.podcast))
                .build(),
            BrowserMediaItem.FLAG_BROWSABLE
        )

    private val PodcastEpisodeEntity.asBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId((MediaId.PODCASTS / podcastId / videoId).id)
                .setTitle(title)
                .setSubtitle(publishedTimeText)
                .setIconUri(thumbnailUrl?.toUri() ?: uriFor(R.drawable.podcast))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private fun PodcastEpisodeEntity.toMediaItem(): MediaItem {
        val podcastTitle = runBlocking {
            Database.getPodcastById(podcastId)?.title ?: "Unknown Podcast"
        }

        return MediaItem.Builder()
            .setMediaId(videoId)
            .setUri(videoId) // Assuming the videoId can be used as the URI or you need to add proper URI handling
            .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(podcastTitle)
                .setArtworkUri(thumbnailUrl?.toUri())
                .build())
            .build()
    }

    private inner class SessionCallback(
        private val binder: PlayerService.Binder
    ) : MediaSession.Callback() {
        override fun onPlay() = binder.player.play()
        override fun onPause() = binder.player.pause()
        override fun onSkipToPrevious() = binder.player.forceSeekToPrevious()
        override fun onSkipToNext() = binder.player.forceSeekToNext()
        override fun onSeekTo(pos: Long) = binder.player.seekTo(pos)
        override fun onSkipToQueueItem(id: Long) = binder.player.seekToDefaultPosition(id.toInt())
        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            if (query.isNullOrBlank()) return
            binder.playFromSearch(query)
        }

        @Suppress("CyclomaticComplexMethod")
        @OptIn(UnstableApi::class)
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            val data = mediaId?.split('/') ?: return
            var index = 0

            coroutineScope.launch {
                // This variable will hold our media items that will eventually be played
                val mediaItems = mutableListOf<MediaItem>()

                // Process based on media ID type
                when (data.getOrNull(0)?.let { MediaId(it) }) {
                    MediaId.SHUFFLE -> {
                        // Handle shuffle - play all songs in random order
                        val songs = lastSongs
                        // Map songs to media items
                        songs.forEach { song ->
                            song.asMediaItem.let { mediaItems.add(it) }
                        }
                    }

                    MediaId.SONGS -> {
                        // Handle playing a specific song from the songs list
                        val songId = data.getOrNull(1)
                        if (songId != null) {
                            // Find the index of the selected song
                            index = lastSongs.indexOfFirst { it.id == songId }
                            if (index >= 0) {
                                // Map all songs to media items
                                lastSongs.forEach { song ->
                                    song.asMediaItem.let { mediaItems.add(it) }
                                }
                            }
                        }
                    }

                    MediaId.FAVORITES -> {
                        // Handle favorites playlist
                        val favorites = Database.favorites().first()
                        val shuffledFavorites = favorites.shuffled()

                        // Map favorite songs to media items
                        shuffledFavorites.forEach { song ->
                            song.asMediaItem.let { mediaItems.add(it) }
                        }
                    }

                    MediaId.OFFLINE -> {
                        // Handle offline songs
                        val offlineSongs = Database.songsWithContentLength().first()
                            .filter { binder.isCached(it) }
                            .shuffled()

                        // Map offline songs to media items
                        offlineSongs.forEach { songWithContentLength ->
                            songWithContentLength.song.asMediaItem.let { mediaItems.add(it) }
                        }
                    }

                    MediaId.TOP -> {
                        // Handle top songs list
                        val duration = DataPreferences.topListPeriod.duration
                        val length = DataPreferences.topListLength

                        val topSongs: List<Song> = if (duration != null) {
                            // Get trending songs for specified period
                            Database.trending(
                                limit = length,
                                period = duration.inWholeMilliseconds
                            ).first()
                        } else {
                            // Get songs by play time if no period specified
                            Database.songsByPlayTimeDesc(limit = length).first()
                        }

                        // Map top songs to media items
                        topSongs.forEach { song ->
                            song.asMediaItem.let { mediaItems.add(it) }
                        }
                    }

                    MediaId.LOCAL -> {
                        // Handle local songs
                        val localSongs = Database.songs(
                            sortBy = OrderPreferences.localSongSortBy,
                            sortOrder = OrderPreferences.localSongSortOrder,
                            isLocal = true
                        ).first()

                        // Filter out songs with 0 duration and map to media items
                        localSongs.filter { it.durationText != "0:00" }.forEach { song ->
                            song.asMediaItem.let { mediaItems.add(it) }
                        }
                    }

                    MediaId.PLAYLISTS -> {
                        // Handle playlist
                        val playlistId = data.getOrNull(1)?.toLongOrNull()
                        if (playlistId != null) {
                            val playlistWithSongs = Database.playlistWithSongs(playlistId).first()
                            val shuffledPlaylistSongs = playlistWithSongs?.songs?.shuffled()

                            // Map playlist songs to media items
                            shuffledPlaylistSongs?.forEach { song ->
                                song.asMediaItem.let { mediaItems.add(it) }
                            }
                        }
                    }

                    MediaId.ALBUMS -> {
                        // Handle album
                        val albumId = data.getOrNull(1)
                        if (albumId != null) {
                            val albumSongs = Database.albumSongs(albumId).first()

                            // Map album songs to media items
                            albumSongs.forEach { song ->
                                song.asMediaItem.let { mediaItems.add(it) }
                            }
                        }
                    }

                    MediaId.PODCASTS -> {
                        // Handle podcasts
                        val browseId = data.getOrNull(1)
                        val videoId = data.getOrNull(2)

                        if (browseId != null && videoId != null) {
                            // Play a specific episode
                            val episode = Database.getEpisodeById(videoId)
                            episode?.toMediaItem()?.let {
                                mediaItems.add(it)
                                index = 0 // Single episode
                            }
                        } else if (browseId != null) {
                            // Play all episodes of a podcast
                            val episodes = Database.getEpisodesForPodcastAsFlow(browseId).first()

                            // Find index of the podcast in lastPodcasts
                            index = lastPodcasts.indexOfFirst { it.browseId == browseId }

                            // Map podcast episodes to media items
                            episodes.forEach { episode ->
                                episode.toMediaItem().let { mediaItems.add(it) }
                            }
                        }
                    }

                    else -> {
                        // Unknown media ID, do nothing
                    }
                }

                if (mediaItems.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        binder.player.forcePlayAtIndex(
                            items = mediaItems,
                            index = index.coerceIn(0, mediaItems.size - 1)
                        )
                    }
                }
            }
        }
    }

    @JvmInline
    private value class MediaId(val id: String) : CharSequence by id {
        companion object {
            val ROOT = MediaId("root")
            val SONGS = MediaId("songs")
            val PLAYLISTS = MediaId("playlists")
            val ALBUMS = MediaId("albums")
            val PODCASTS = MediaId("podcasts")
            val FAVORITES = MediaId("favorites")
            val OFFLINE = MediaId("offline")
            val TOP = MediaId("top")
            val LOCAL = MediaId("local")
            val SHUFFLE = MediaId("shuffle")
        }

        operator fun div(other: String) = MediaId("$id/$other")
    }
}