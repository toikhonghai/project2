package app.vitune.providers.github

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val API_VERSION = "2022-11-28"
private const val CONTENT_TYPE = "application"
private const val CONTENT_SUBTYPE = "vnd.github+json" //Subtype GitHub dùng cho JSON

// Định nghĩa các hằng số cho các loại nội dung và phiên bản API của GitHub
object GitHub {
    /*
    Dùng CIO Engine của Ktor để xử lý HTTP request
    - CIO là một engine nhẹ và hiệu quả cho việc xử lý các yêu cầu HTTP.
    - Ktor là một framework cho việc xây dựng ứng dụng web và client HTTP trong Kotlin.
    CIO (Coroutine-based I/O) giúp tối ưu hiệu suất, phù hợp cho ứng dụng không cần TLS cấp hệ thống.
     */
    internal val httpClient by lazy {
        HttpClient(CIO) { // Xử lý yêu cầu HTTP không đồng bộ
            val contentType = ContentType(
                CONTENT_TYPE,
                CONTENT_SUBTYPE
            ) //Tạo ContentType dựa trên application/vnd.github+json.

            // Cấu hình serialization để xử lý JSON
            install(ContentNegotiation) {
                val json = Json {
                    ignoreUnknownKeys = true // Bỏ qua các khóa không xác định trong JSON
                }

                // Đăng ký serializer JSON
                json(json)

                // Đăng ký serializer JSON với contentType tùy chỉnh
                json(
                    json = json,
                    contentType = contentType
                )
            }

            // Cấu hình các giá trị mặc định cho request
            defaultRequest {
                url("https://api.github.com") // Đặt URL gốc của GitHub API
                headers["X-GitHub-Api-Version"] =
                    API_VERSION // Gửi header để chỉ định phiên bản API

                // Xác định kiểu nội dung chấp nhận và kiểu nội dung gửi đi
                accept(contentType)
                contentType(ContentType.Application.Json)
            }

            // Nếu HTTP status code >= 400 thì tự động ném exception
            expectSuccess = true
        }
    }

    /**
     * Hàm mở rộng cho HttpRequestBuilder để thêm phân trang vào request.
     * @param size Số lượng phần tử trên mỗi trang.
     * @param page Số trang (bắt đầu từ 1).
     */
    fun HttpRequestBuilder.withPagination(size: Int, page: Int) {
        require(page > 0) { "GitHub error: invalid page ($page), pagination starts at page 1" }
        require(size > 0) { "GitHub error: invalid page size ($size), a page has to have at least a single item" }

        // Thêm các tham số truy vấn vào URL của request
        parameter("per_page", size)
        parameter("page", page)
    }
}