package app.vitune.providers.piped

import app.vitune.providers.piped.models.CreatedPlaylist
import app.vitune.providers.piped.models.Instance
import app.vitune.providers.piped.models.Playlist
import app.vitune.providers.piped.models.PlaylistPreview
import app.vitune.providers.piped.models.Session
import app.vitune.providers.piped.models.authenticatedWith
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

operator fun Url.div(path: String) = URLBuilder(this).apply { path(path) }.build() // Cho phép sử dụng / giữa một URL và một chuỗi để tạo URL mới.
operator fun JsonElement.div(key: String) = jsonObject[key]!! // Truy cập phần tử JSON bằng cách sử dụng toán tử "/". Nó sẽ trả về JsonElement tương ứng với khóa đã cho.

// đây là một extension function cho JsonElement, cho phép truy cập các phần tử JSON bằng cách sử dụng toán tử "/"
object Piped {
    private val client by lazy {
        HttpClient(CIO) { // Tạo một HttpClient với engine CIO
            install(ContentNegotiation) { // Cài đặt plugin ContentNegotiation, CIO (Coroutine-based I/O) là một HTTP engine trong Ktor Client,
                // được thiết kế để hoạt động tối ưu với coroutine. Nó sử dụng sockets không đồng bộ và native coroutines để xử lý HTTP request.
                json(
                    Json {
                        isLenient = true // Cho phép phân tích cú pháp JSON không nghiêm ngặt
                        ignoreUnknownKeys = true // Bỏ qua các khóa không xác định trong JSON
                    }
                )
            }

            install(HttpRequestRetry) { // Cài đặt plugin HttpRequestRetry để tự động thử lại các yêu cầu HTTP khi gặp lỗi
                exponentialDelay() // Sử dụng độ trễ tăng dần giữa các lần thử lại
                maxRetries = 2 // Số lần thử lại tối đa là 2
            }

            install(HttpTimeout) { // Cài đặt plugin HttpTimeout để thiết lập thời gian chờ cho các yêu cầu HTTP
                connectTimeoutMillis = 1000L // Thời gian chờ kết nối là 1 giây
                requestTimeoutMillis = 5000L // Thời gian chờ yêu cầu là 5 giây
            }

            expectSuccess = true // Khi cài đặt expectSuccess là true, Ktor sẽ ném ra ngoại lệ nếu mã trạng thái HTTP không phải là 2xx

            defaultRequest {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
    }

    private val mutex = Mutex() //mutex (loại Mutex()): Giúp đảm bảo rằng chỉ một request có thể chạy tại một thời điểm.

    private suspend fun request( // Hàm này thực hiện một yêu cầu HTTP đến API của Piped
        session: Session,
        endpoint: String,
        block: HttpRequestBuilder.() -> Unit = { }
    ) = mutex.withLock { // Dùng withLock {} để tránh các vấn đề liên quan đến cạnh tranh dữ liệu (race condition).
        client.request(url = session.apiBaseUrl / endpoint) { // Ghép Base URL với endpoint để tạo URL đầy đủ.
            block() //  Gọi lambda để tùy chỉnh request (nếu có).
            header("Authorization", session.token)
        }
    }

    // Hàm này thực hiện một yêu cầu HTTP đến API của Piped
    // Đọc nội dung phản hồi HTTP và convert sang kiểu JSON (JsonElement).
    // Vì body<T>() là một hàm suspend, nó chỉ chạy được trong coroutine.
    // Dùng toán tử / để truy xuất một khóa (key) trong JSON.
    private suspend fun HttpResponse.isOk() =
        (body<JsonElement>() / "message").jsonPrimitive.content == "ok" // Kiểm tra xem phản hồi có mã trạng thái 200 hay không.
        // Nghĩa là jsonElement / "message" tương đương với jsonElement.jsonObject["message"]!!

    suspend fun getInstances() = runCatchingCancellable { // Hàm này lấy danh sách các instance Piped
        client.get("https://piped-instances.kavin.rocks/").body<List<Instance>>() // Gửi yêu cầu GET đến URL và phân tích cú pháp phản hồi thành danh sách các instance.
    }

    suspend fun login(apiBaseUrl: Url, username: String, password: String) = // Hàm này thực hiện đăng nhập vào Piped
        runCatchingCancellable {
            apiBaseUrl authenticatedWith ( // Sử dụng extension function authenticatedWith để thực hiện đăng nhập
                    client.post(apiBaseUrl / "login") {
                        setBody(
                            mapOf(
                                "username" to username,
                                "password" to password
                            )
                        )
                    }.body<JsonElement>() / "token" // Lấy mã thông báo từ phản hồi JSON
                    ).jsonPrimitive.content
        }

    val playlist = Playlists()

    class Playlists internal constructor() { // Nội bộ để không thể khởi tạo bên ngoài
        suspend fun list(session: Session) = runCatchingCancellable {
            request(session, "user/playlists").body<List<PlaylistPreview>>() // Lấy danh sách các playlist của người dùng
        }

        suspend fun create(session: Session, name: String) = runCatchingCancellable { // Hàm này tạo một playlist mới
            request(session, "user/playlists/create") {
                method = HttpMethod.Post
                setBody(mapOf("name" to name))
            }.body<CreatedPlaylist>() // Gửi yêu cầu POST đến API để tạo playlist mới và phân tích cú pháp phản hồi thành CreatedPlaylist
        }

        suspend fun rename(session: Session, id: UUID, name: String) = runCatchingCancellable { // Hàm này đổi tên một playlist
            request(session, "user/playlists/rename") {
                method = HttpMethod.Post
                setBody(
                    mapOf(
                        "playlistId" to id.toString(),
                        "newName" to name
                    )
                )
            }.isOk()
        }

        suspend fun delete(session: Session, id: UUID) = runCatchingCancellable { // Hàm này xóa một playlist
            request(session, "user/playlists/delete") {
                method = HttpMethod.Post
                setBody(mapOf("playlistId" to id.toString()))
            }.isOk()
        }

        suspend fun add(session: Session, id: UUID, videos: List<String>) = runCatchingCancellable { // Hàm này thêm video vào một playlist
            request(session, "user/playlists/add") {
                method = HttpMethod.Post
                setBody(
                    mapOf(
                        "playlistId" to id.toString(),
                        "videoIds" to videos
                    )
                )
            }.isOk()
        }

        suspend fun remove(session: Session, id: UUID, idx: Int) = runCatchingCancellable { // Hàm này xóa video khỏi một playlist
            request(session, "user/playlists/remove") {
                method = HttpMethod.Post
                setBody(
                    mapOf(
                        "playlistId" to id.toString(),
                        "index" to idx
                    )
                )
            }.isOk()
        }

        suspend fun songs(session: Session, id: UUID) = runCatchingCancellable { // Hàm này lấy danh sách video trong một playlist
            request(session, "playlists/$id").body<Playlist>()
        }
    }
}
