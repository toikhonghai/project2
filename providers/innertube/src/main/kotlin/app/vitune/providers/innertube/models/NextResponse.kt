package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

//Sử dụng để ánh xạ phản hồi JSON từ API YouTube Music, chủ yếu liên quan đến danh sách phát tiếp theo, hàng đợi nhạc và Automix.
@OptIn(ExperimentalSerializationApi::class)
@Serializable
// Lớp này đại diện cho phản hồi từ API YouTube Music, bao gồm thông tin về hàng đợi nhạc và các video trong danh sách phát.
data class NextResponse(
    val contents: Contents?
) {
    @Serializable
    data class MusicQueueRenderer( // Lưu trữ thông tin về hàng đợi nhạc trong ứng dụng YouTube Music.
        val content: Content?
    ) {
        @Serializable
        data class Content(
            @JsonNames("playlistPanelContinuation") // Đánh dấu để ánh xạ tên trường trong JSON
            val playlistPanelRenderer: PlaylistPanelRenderer? // Lưu trữ thông tin về danh sách phát trong hàng đợi nhạc.
        ) {
            @Serializable
            data class PlaylistPanelRenderer( // Chứa danh sách phát trong hàng đợi
                val contents: List<Content>?, // Lưu trữ danh sách các video trong danh sách phát.
                val continuations: List<Continuation>? // Lưu trữ thông tin về các phần tiếp theo trong danh sách phát.
            ) {
                @Serializable
                data class Content(
                    val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?, // Lưu trữ thông tin về video trong danh sách phát.
                    val automixPreviewVideoRenderer: AutomixPreviewVideoRenderer? // Lưu trữ thông tin về video xem trước Automix.
                ) {

                    @Serializable
                    data class AutomixPreviewVideoRenderer(
                        val content: Content?
                    ) {
                        @Serializable
                        data class Content(
                            val automixPlaylistVideoRenderer: AutomixPlaylistVideoRenderer?
                        ) {
                            @Serializable
                            data class AutomixPlaylistVideoRenderer(
                                val navigationEndpoint: NavigationEndpoint? // Điều hướng khi người dùng nhấn vào video xem trước Automix.
                            )
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class Contents( // Lưu trữ thông tin về nội dung trong phản hồi.
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer( // Lưu trữ thông tin về kết quả tìm kiếm âm nhạc trong ứng dụng YouTube Music.
            val tabbedRenderer: TabbedRenderer?
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer? // Lưu trữ thông tin về các tab trong kết quả tìm kiếm âm nhạc.
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer( // Lưu trữ thông tin về các tab trong kết quả tìm kiếm âm nhạc.
                    val tabs: List<Tab>?
                ) {
                    @Serializable
                    data class Tab( // Lưu trữ thông tin về một tab trong kết quả tìm kiếm âm nhạc.
                        val tabRenderer: TabRenderer?
                    ) {
                        @Serializable
                        data class TabRenderer( // Lưu trữ thông tin về một tab trong kết quả tìm kiếm âm nhạc.
                            val content: Content?,
                            val endpoint: NavigationEndpoint?,
                            val title: String?
                        ) {
                            @Serializable
                            data class Content( // Lưu trữ thông tin về nội dung trong tab.
                                val musicQueueRenderer: MusicQueueRenderer?
                            )
                        }
                    }
                }
            }
        }
    }
}
