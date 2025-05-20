package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.models.BrowseResponse.Header.MusicDetailHeaderRenderer
import app.vitune.providers.innertube.models.BrowseResponse.Header.MusicImmersiveHeaderRenderer
import app.vitune.providers.innertube.models.Runs
import app.vitune.providers.innertube.models.ThumbnailRenderer

interface PodcastHeader {
    val title: Runs?
    val description: Runs?
    val thumbnail: ThumbnailRenderer?
    val subtitle: Runs? // May be null for immersiveHeader
}

data class DetailPodcastHeader(val renderer: MusicDetailHeaderRenderer) : PodcastHeader {
    override val title: Runs? get() = renderer.title
    override val description: Runs? get() = renderer.description
    override val thumbnail: ThumbnailRenderer? get() = renderer.thumbnail
    override val subtitle: Runs? get() = renderer.subtitle
}

data class ImmersivePodcastHeader(val renderer: MusicImmersiveHeaderRenderer) : PodcastHeader {
    override val title: Runs? get() = renderer.title
    override val description: Runs? get() = renderer.description
    override val thumbnail: ThumbnailRenderer? get() = renderer.thumbnail
    override val subtitle: Runs? get() = null // No subtitle in immersiveHeader
}