package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SectionListRenderer(
    val contents: List<Content>?,
    val continuations: List<Continuation>?
) {
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?,
        @JsonNames("musicPlaylistShelfRenderer")
        val musicShelfRenderer: MusicShelfRenderer?,
        val gridRenderer: GridRenderer?,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer?
    ) {
        @Serializable
        data class MusicDescriptionShelfRenderer(
            val description: Runs?
        )

        @Serializable
        data class MusicResponsiveHeaderRenderer(
            val title: Runs?,
            val description: MusicDescriptionShelfRenderer?,
            val subtitle: Runs?,
            val secondSubtitle: Runs?,
            val thumbnail: ThumbnailRenderer?,
            val straplineTextOne: Runs?,
            val buttons: List<Button>?
        ) {
            @Serializable
            data class Button(
                val buttonRenderer: ButtonRenderer? = null,
                val toggleButtonRenderer: ToggleButtonRenderer? = null,
                val menuRenderer: MenuRenderer? = null
            )

            @Serializable
            data class ToggleButtonRenderer(
                val isToggled: Boolean? = null,
                val isDisabled: Boolean? = null,
                val defaultIcon: Icon? = null,
                val defaultText: Runs? = null, // Làm tùy chọn
                val toggledIcon: Icon? = null,
                val toggledText: Runs? = null, // Làm tùy chọn
                val trackingParams: String? = null,
                val defaultNavigationEndpoint: NavigationEndpoint? = null
            )

            @Serializable
            data class MenuRenderer(
                val items: List<MenuItem>?
            )

            @Serializable
            data class MenuItem(
                val toggleMenuServiceItemRenderer: ToggleMenuServiceItemRenderer? = null
            )

            @Serializable
            data class ToggleMenuServiceItemRenderer(
                val defaultText: Runs? = null, // Làm tùy chọn
                val defaultIcon: Icon? = null,
                val defaultServiceEndpoint: NavigationEndpoint? = null,
                val toggledText: Runs? = null, // Làm tùy chọn
                val toggledIcon: Icon? = null,
                val toggledServiceEndpoint: LikeEndpointWrapper? = null,
                val trackingParams: String? = null
            )

            @Serializable
            data class LikeEndpointWrapper(
                val clickTrackingParams: String? = null,
                val likeEndpoint: LikeEndpoint? = null
            )

            @Serializable
            data class LikeEndpoint(
                val status: String? = null,
                val target: Target? = null
            )

            @Serializable
            data class Target(
                val playlistId: String? = null
            )

            @Serializable
            data class Icon(
                val iconType: String? = null
            )
        }
    }
}