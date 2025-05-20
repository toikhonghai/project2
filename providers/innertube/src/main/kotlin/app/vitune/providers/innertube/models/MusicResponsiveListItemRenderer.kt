package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MusicResponsiveListItemRenderer(
    val fixedColumns: List<FlexColumn>?,
    val flexColumns: List<FlexColumn>,
    val thumbnail: ThumbnailRenderer?,
    val navigationEndpoint: NavigationEndpoint?,
    val badges: List<Badge>?,
    val playlistItemData: PlaylistItemData? = null,
    val overlay: Overlay? = null,
    val menu: Menu? = null
) {
    @Serializable
    data class FlexColumn(
        @JsonNames("musicResponsiveListItemFixedColumnRenderer")
        val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer?
    ) {
        @Serializable
        data class MusicResponsiveListItemFlexColumnRenderer(
            val text: Runs?
        )
    }

    @Serializable
    data class PlaylistItemData(
        val videoId: String?
    )

    @Serializable
    data class Overlay(
        val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer?
    ) {
        @Serializable
        data class MusicItemThumbnailOverlayRenderer(
            val content: Content?
        ) {
            @Serializable
            data class Content(
                val musicPlayButtonRenderer: MusicPlayButtonRenderer?
            ) {
                @Serializable
                data class MusicPlayButtonRenderer(
                    val playNavigationEndpoint: NavigationEndpoint?
                )
            }
        }
    }

    @Serializable
    data class Menu(
        val menuRenderer: MenuRenderer?
    ) {
        @Serializable
        data class MenuRenderer(
            val items: List<MenuItem>?
        ) {
            @Serializable
            data class MenuItem(
                val menuServiceItemRenderer: MenuServiceItemRenderer? = null,
                val menuNavigationItemRenderer: MenuNavigationItemRenderer? = null
            ) {
                @Serializable
                data class MenuServiceItemRenderer(
                    val text: Runs?,
                    val icon: Icon?,
                    val serviceEndpoint: ServiceEndpoint?
                ) {
                    @Serializable
                    data class Icon(
                        val iconType: String?
                    )

                    @Serializable
                    data class ServiceEndpoint(
                        val queueAddEndpoint: QueueAddEndpoint? = null,
                        val offlineVideoEndpoint: OfflineVideoEndpoint? = null
                    ) {
                        @Serializable
                        data class QueueAddEndpoint(
                            val queueTarget: QueueTarget?
                        ) {
                            @Serializable
                            data class QueueTarget(
                                val videoId: String?
                            )
                        }

                        @Serializable
                        data class OfflineVideoEndpoint(
                            val videoId: String?
                        )
                    }
                }

                @Serializable
                data class MenuNavigationItemRenderer(
                    val text: Runs?,
                    val icon: Icon?,
                    val navigationEndpoint: NavigationEndpoint?
                ) {
                    @Serializable
                    data class Icon(
                        val iconType: String?
                    )
                }
            }
        }
    }
}