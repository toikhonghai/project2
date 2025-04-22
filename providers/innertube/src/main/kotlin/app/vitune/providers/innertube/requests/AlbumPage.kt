package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.bodies.BrowseBody
import io.ktor.http.Url

// Lưu trữ thông tin về một album, bao gồm tiêu đề, nghệ sĩ, ảnh bìa và danh sách bài hát
/*
Hàm này thực hiện xử lý dữ liệu album trong hệ thống Innertube bằng cách:
✔ Gọi API để lấy dữ liệu album từ playlistPage(body).
✔ Trích xuất thông tin danh sách bài hát (songsPage) của album.
✔ Cập nhật các bài hát trong album, thêm thông tin về tác giả (authors), album (album), và ảnh thumbnail (thumbnail).
 */
suspend fun Innertube.albumPage(body: BrowseBody) = //  Trả về một danh sách album đã được xử lý
    playlistPage(body)
        ?.map { album ->
            album.url?.let {
                playlistPage(body = BrowseBody(browseId = "VL${Url(it).parameters["list"]}")) // Trích xuất list từ URL (bằng Url(it).parameters["list"])., Gọi playlistPage() lại để lấy thông tin playlist chi tiết.
                    ?.getOrNull()
                    ?.let { playlist -> album.copy(songsPage = playlist.songsPage) } // Cập nhật songsPage của album bằng playlist.songsPage.
            } ?: album
        }
        ?.map { album ->
            album.copy( // Sao chép album với các thuộc tính mới
                songsPage = album.songsPage?.copy( // Sao chép songsPage của album với các thuộc tính mới
                    items = album.songsPage.items?.map { song -> // Lặp qua từng bài hát trong danh sách bài hát
                        song.copy( // Sao chép bài hát với các thuộc tính mới
                            authors = song.authors ?: album.authors, // Nếu không có thông tin tác giả trong bài hát, sử dụng thông tin tác giả của album
                            album = Innertube.Info( // Tạo một đối tượng Info cho album
                                name = album.title, // Sử dụng tiêu đề của album
                                endpoint = NavigationEndpoint.Endpoint.Browse( // Tạo một endpoint điều hướng đến album
                                    browseId = body.browseId,
                                    params = body.params
                                )
                            ),
                            thumbnail = album.thumbnail
                        )
                    }
                )
            )
        }
