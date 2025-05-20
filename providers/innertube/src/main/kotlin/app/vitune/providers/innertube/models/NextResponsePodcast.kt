package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NextResponsePodcast(
    val contents: Contents?
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer?
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tab>?
                ) {
                    @Serializable
                    data class Tab(
                        val tabRenderer: TabRenderer?
                    ) {
                        @Serializable
                        data class TabRenderer(
                            val content: Content?,
                            val endpoint: NavigationEndpoint?,
                            val title: String?
                        ) {
                            @Serializable
                            data class Content(
                                val musicQueueRenderer: MusicQueueRenderer?
                            )
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class MusicQueueRenderer(
        val content: Content?
    ) {
        @Serializable
        data class Content(
            @JsonNames("playlistPanelContinuation")
            val playlistPanelRenderer: PlaylistPanelRenderer?
        ) {
            @Serializable
            data class PlaylistPanelRenderer(
                val contents: List<Content>?, // Danh sách tập podcast
                val continuations: List<Continuation>?
            ) {
                @Serializable
                data class Content(
                    val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?
                ) {
                    @Serializable
                    data class PlaylistPanelVideoRenderer(
                        val title: Runs?,
                        val longBylineText: Runs?,
                        val shortBylineText: Runs?,
                        val thumbnail: Thumbnail?,
                        val lengthText: Runs?,
                        val videoId: String?,
                        val navigationEndpoint: NavigationEndpoint?
                    ) {
                        // Hàm hỗ trợ lấy browseId hợp lệ
                        fun getPodcastBrowseId(): String? {
                            val browseId = longBylineText?.runs
                                ?.firstOrNull { it.navigationEndpoint?.browseEndpoint != null }
                                ?.navigationEndpoint?.browseEndpoint?.browseId

                            // Kiểm tra nếu browseId không hợp lệ (bắt đầu bằng MPSPPL hoặc không chuẩn)
                            return when {
                                browseId == null -> null
                                browseId.startsWith("MPSPPL") -> null // Bỏ qua browseId không hợp lệ
                                else -> browseId
                            }
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class Continuation(
        val nextContinuationData: NextContinuationData?
    ) {
        @Serializable
        data class NextContinuationData(
            val continuation: String?
        )
    }

    @Serializable
    data class Thumbnail(
        val thumbnails: List<ThumbnailItem>?
    ) {
        @Serializable
        data class ThumbnailItem(
            val url: String?,
            val width: Int?,
            val height: Int?
        )
    }
}