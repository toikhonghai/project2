package app.vitune.providers.innertube.utils

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.MusicShelfRenderer
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.isExplicit

// Possible configurations:
// "song" • author(s) • album • duration
// "song" • author(s) • duration
// author(s) • album • duration
// author(s) • duration

// đây là một hàm mở rộng cho lớp Innertube.SongItem.Companion, nó sẽ nhận vào một đối tượng MusicShelfRenderer.Content và trả về một đối tượng Innertube.SongItem
fun Innertube.SongItem.Companion.from(content: MusicShelfRenderer.Content) = runCatching {
    val (mainRuns, otherRuns) = content.runs

    val album: Innertube.Info<NavigationEndpoint.Endpoint.Browse>? = otherRuns
        .getOrNull(otherRuns.lastIndex - 1)
        ?.firstOrNull()
        ?.takeIf { run ->
            run
                .navigationEndpoint
                ?.browseEndpoint
                ?.type == "MUSIC_PAGE_TYPE_ALBUM"
        }
        ?.let(Innertube::Info)

    Innertube.SongItem(
        info = mainRuns
            .firstOrNull()
            ?.let(Innertube::Info),
        authors = otherRuns
            .getOrNull(otherRuns.lastIndex - if (album == null) 1 else 2)
            ?.map(Innertube::Info),
        album = album,
        durationText = otherRuns
            .lastOrNull()
            ?.firstOrNull()
            ?.text
            ?.takeIf { ':' in it }
            ?: otherRuns
                .getOrNull(otherRuns.size - 2)
                ?.firstOrNull()
                ?.text,
        explicit = content.musicResponsiveListItemRenderer?.badges.isExplicit,
        thumbnail = content.thumbnail
    ).takeIf { it.info?.endpoint?.videoId != null }
}.getOrNull()

// đây là một hàm mở rộng cho lớp Innertube.VideoItem.Companion, nó sẽ nhận vào một đối tượng MusicShelfRenderer.Content và trả về một đối tượng Innertube.VideoItem
fun Innertube.VideoItem.Companion.from(content: MusicShelfRenderer.Content) = runCatching {
    val (mainRuns, otherRuns) = content.runs

    Innertube.VideoItem(
        info = mainRuns
            .firstOrNull()
            ?.let(Innertube::Info),
        authors = otherRuns
            .getOrNull(otherRuns.lastIndex - 2)
            ?.map(Innertube::Info),
        viewsText = otherRuns
            .getOrNull(otherRuns.lastIndex - 1)
            ?.firstOrNull()
            ?.text,
        durationText = otherRuns
            .getOrNull(otherRuns.lastIndex)
            ?.firstOrNull()
            ?.text,
        thumbnail = content.thumbnail
    ).takeIf { it.info?.endpoint?.videoId != null }
}.getOrNull()

fun Innertube.AlbumItem.Companion.from(content: MusicShelfRenderer.Content) = runCatching {
    val (mainRuns, otherRuns) = content.runs

    Innertube.AlbumItem(
        info = Innertube.Info(
            name = mainRuns
                .firstOrNull()
                ?.text,
            endpoint = content
                .musicResponsiveListItemRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
        ),
        authors = otherRuns
            .getOrNull(otherRuns.lastIndex - 1)
            ?.map(Innertube::Info),
        year = otherRuns
            .getOrNull(otherRuns.lastIndex)
            ?.firstOrNull()
            ?.text,
        thumbnail = content.thumbnail
    ).takeIf { it.info?.endpoint?.browseId != null }
}.getOrNull()

//
fun Innertube.ArtistItem.Companion.from(content: MusicShelfRenderer.Content) = runCatching {
    val (mainRuns, otherRuns) = content.runs

    Innertube.ArtistItem(
        info = Innertube.Info(
            name = mainRuns
                .firstOrNull()
                ?.text,
            endpoint = content
                .musicResponsiveListItemRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
        ),
        subscribersCountText = otherRuns
            .lastOrNull()
            ?.last()
            ?.text,
        thumbnail = content.thumbnail
    ).takeIf { it.info?.endpoint?.browseId != null }
}.getOrNull()

// đây là một hàm mở rộng cho lớp Innertube.PlaylistItem.Companion, nó sẽ nhận vào một đối tượng MusicShelfRenderer.Content và trả về một đối tượng Innertube.PlaylistItem
fun Innertube.PlaylistItem.Companion.from(content: MusicShelfRenderer.Content) = runCatching {
    val (mainRuns, otherRuns) = content.runs

    Innertube.PlaylistItem(
        info = Innertube.Info(
            name = mainRuns
                .firstOrNull()
                ?.text,
            endpoint = content
                .musicResponsiveListItemRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
        ),
        channel = otherRuns
            .firstOrNull()
            ?.firstOrNull()
            ?.let(Innertube::Info),
        songCount = otherRuns
            .lastOrNull()
            ?.firstOrNull()
            ?.text
            ?.split(' ')
            ?.firstOrNull()
            ?.toIntOrNull(),
        thumbnail = content.thumbnail
    ).takeIf { it.info?.endpoint?.browseId != null }
}.getOrNull()
