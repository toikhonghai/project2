package app.vitune.providers.kugou

import app.vitune.providers.kugou.models.DownloadLyricsResponse
import app.vitune.providers.kugou.models.SearchLyricsResponse
import app.vitune.providers.kugou.models.SearchSongResponse
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.decodeBase64String
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

// đây là một đoạn mã Kotlin sử dụng thư viện Ktor để gửi yêu cầu HTTP đến API của KuGou.
// Nó định nghĩa một đối tượng KuGou với các hàm để tìm kiếm lời bài hát và tải xuống lời bài hát từ KuGou.
// Đối tượng này sử dụng thư viện Ktor để thực hiện các yêu cầu HTTP và xử lý phản hồi từ API.
object KuGou {
    @OptIn(ExperimentalSerializationApi::class)
    private val client by lazy { // Khởi tạo một HttpClient với các plugin và cấu hình cần thiết
        HttpClient(OkHttp) { //  Dùng HttpClient với OkHttp để gửi HTTP request.
            BrowserUserAgent() // Sử dụng User-Agent của trình duyệt để giả lập yêu cầu từ trình duyệt.

            expectSuccess = true // Khi gửi yêu cầu, nếu server trả về mã trạng thái không thành công (4xx, 5xx), Ktor sẽ ném ra ngoại lệ.

            install(ContentNegotiation) { // Cài đặt plugin ContentNegotiation để xử lý nội dung trả về từ server.
                val feature = Json {
                    ignoreUnknownKeys = true // Bỏ qua các khóa không xác định trong JSON
                    explicitNulls = false // Không sử dụng null cho các trường không xác định
                    encodeDefaults = true // Mã hóa các giá trị mặc định
                }

                json(feature) // Đăng ký JSON serializer với cấu hình mặc định
                json(feature, ContentType.Text.Html) // Đăng ký JSON serializer với ContentType là HTML
                json(feature, ContentType.Text.Plain) // Đăng ký JSON serializer với ContentType là Plain Text
            }

            install(ContentEncoding) { // Cài đặt plugin ContentEncoding để xử lý nén nội dung trả về từ server.
                gzip() // Sử dụng gzip để nén nội dung.
                deflate() // Sử dụng deflate để nén nội dung.
            }

            defaultRequest { // Cài đặt các giá trị mặc định cho tất cả các yêu cầu HTTP.
                url("https://krcs.kugou.com")
            }
        }
    }

    suspend fun lyrics(artist: String, title: String, duration: Long) = runCatchingCancellable { // Tìm kiếm lời bài hát dựa trên tên nghệ sĩ, tiêu đề và thời gian
        val keyword = keyword(artist, title) // Tạo từ khóa tìm kiếm từ tên nghệ sĩ và tiêu đề bài hát
        val infoByKeyword = searchSong(keyword) // Tìm kiếm bài hát bằng từ khóa

        if (infoByKeyword.isNotEmpty()) {
            var tolerance = 0 // Biến để điều chỉnh độ chính xác của thời gian

            while (tolerance <= 5) {
                for (info in infoByKeyword) { // Duyệt qua danh sách bài hát tìm được
                    if (info.duration >= duration - tolerance && info.duration <= duration + tolerance) { // Kiểm tra thời gian bài hát
                        searchLyricsByHash(info.hash).firstOrNull()?.let { candidate -> // Tìm kiếm lời bài hát bằng hash
                            return@runCatchingCancellable downloadLyrics( // Tải xuống lời bài hát
                                candidate.id, // ID của bài hát
                                candidate.accessKey // Khóa truy cập để tải xuống lời bài hát
                            ).normalize() // Chuẩn hóa lời bài hát
                        }
                    }
                }

                tolerance++
            }
        }

        // Nếu không tìm thấy lời bài hát bằng hash, tìm kiếm lời bài hát bằng từ khóa
        searchLyricsByKeyword(keyword).firstOrNull()?.let { candidate ->
            return@runCatchingCancellable downloadLyrics( // Tải xuống lời bài hát
                candidate.id,
                candidate.accessKey
            ).normalize()
        }

        null
    }

    private suspend fun downloadLyrics(id: Long, accessKey: String) = client.get("/download") { // Tải xuống lời bài hát từ KuGou
        parameter("ver", 1) // Phiên bản API
        parameter("man", "yes") // Tham số xác định loại yêu cầu
        parameter("client", "pc") // Loại client
        parameter("fmt", "lrc")
        parameter("id", id) // ID của bài hát
        parameter("accesskey", accessKey) // Khóa truy cập để tải xuống lời bài hát
    }.body<DownloadLyricsResponse>().content.decodeBase64String().let(KuGou::Lyrics) // Giải mã nội dung lời bài hát từ định dạng Base64 và tạo đối tượng Lyrics

    private suspend fun searchLyricsByHash(hash: String) = client.get("/search") { // Tìm kiếm lời bài hát bằng hash
        parameter("ver", 1)
        parameter("man", "yes")
        parameter("client", "mobi")
        parameter("hash", hash)
    }.body<SearchLyricsResponse>().candidates // Trả về danh sách candidates (các phiên bản lời bài hát có thể đúng với bài hát này).

    private suspend fun searchLyricsByKeyword(keyword: String) = client.get("/search") {
        parameter("ver", 1)
        parameter("man", "yes")
        parameter("client", "mobi")
        url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false)) // Thêm tham số keyword vào URL, sau đó encode đúng định dạng URL.
    }.body<SearchLyricsResponse>().candidates

    private suspend fun searchSong(keyword: String) =
        client.get("https://mobileservice.kugou.com/api/v3/search/song") { // Tìm kiếm bài hát bằng từ khóa
            parameter("version", 9108)
            parameter("plat", 0)
            parameter("pagesize", 8)
            parameter("showtype", 0)
            url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false))
        }.body<SearchSongResponse>().data.info // Trả về danh sách bài hát tìm được từ API của KuGou.

    private fun keyword(artist: String, title: String): String { // Tạo từ khóa tìm kiếm từ tên nghệ sĩ và tiêu đề bài hát
        val (newTitle, featuring) = title.extract(" (feat. ", ')') // Tách tiêu đề bài hát và tên nghệ sĩ hợp tác

        val newArtist = (if (featuring.isEmpty()) artist else "$artist, $featuring") // Nếu không có nghệ sĩ hợp tác thì chỉ cần tên nghệ sĩ chính
            .replace(", ", "、")
            .replace(" & ", "、")
            .replace(".", "")

        return "$newArtist - $newTitle" // Trả về từ khóa tìm kiếm với định dạng "Tên nghệ sĩ - Tiêu đề bài hát"
    }

    // Tách chuỗi thành hai phần dựa trên dấu phân cách
    private fun String.extract(startDelimiter: String, endDelimiter: Char): Pair<String, String> {
        val startIndex = indexOf(startDelimiter).takeIf { it != -1 } ?: return this to ""
        val endIndex = indexOf(endDelimiter, startIndex).takeIf { it != -1 } ?: return this to ""

        return removeRange(startIndex, endIndex + 1) to substring(startIndex + startDelimiter.length, endIndex)
    }

    // Đối tượng này đại diện cho lời bài hát
    @JvmInline // Tạo một lớp Lyrics với thuộc tính value là một chuỗi
    value class Lyrics(val value: String) {
        @Suppress("CyclomaticComplexMethod") // Bỏ qua cảnh báo CyclomaticComplexity
        // Hàm này chuẩn hóa lời bài hát bằng cách loại bỏ các dòng không cần thiết và mã hóa HTML
        fun normalize(): Lyrics {
            var toDrop = 0 // Biến để lưu trữ số lượng ký tự cần loại bỏ
            var maybeToDrop = 0 // Biến để lưu trữ số lượng ký tự có thể cần loại bỏ

            val text = value.replace("\r\n", "\n").trim() // Thay thế ký tự xuống dòng và loại bỏ khoảng trắng ở đầu và cuối chuỗi

            for (line in text.lineSequence()) when { // Duyệt qua từng dòng trong lời bài hát
                line.startsWith("[ti:") ||
                        line.startsWith("[ar:") ||
                        line.startsWith("[al:") ||
                        line.startsWith("[by:") ||
                        line.startsWith("[hash:") ||
                        line.startsWith("[sign:") ||
                        line.startsWith("[qq:") ||
                        line.startsWith("[total:") ||
                        line.startsWith("[offset:") ||
                        line.startsWith("[id:") ||
                        line.containsAt("]Written by：", 9) ||
                        line.containsAt("]Lyrics by：", 9) ||
                        line.containsAt("]Composed by：", 9) ||
                        line.containsAt("]Producer：", 9) ||
                        line.containsAt("]作曲 : ", 9) ||
                        line.containsAt("]作词 : ", 9) -> {
                    toDrop += line.length + 1 + maybeToDrop // Cộng dồn số ký tự cần loại bỏ
                    maybeToDrop = 0
                }

                maybeToDrop == 0 -> maybeToDrop = line.length + 1 // Nếu không có ký tự nào cần loại bỏ thì gán giá trị cho maybeToDrop

                else -> {
                    maybeToDrop = 0
                    break
                }
            }

            return Lyrics(text.drop(toDrop + maybeToDrop).removeHtmlEntities()) // Trả về lời bài hát đã chuẩn hóa
        }

        // Hàm này kiểm tra xem chuỗi có chứa một chuỗi con tại một chỉ số bắt đầu nhất định hay không
        private fun String.containsAt(charSequence: CharSequence, startIndex: Int) =
            regionMatches(startIndex, charSequence, 0, charSequence.length)

        // Hàm này loại bỏ các thực thể HTML trong chuỗi
        private fun String.removeHtmlEntities() = replace("&apos;", "'")
    }
}
