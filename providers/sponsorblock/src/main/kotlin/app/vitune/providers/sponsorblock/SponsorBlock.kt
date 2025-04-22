package app.vitune.providers.sponsorblock

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// sử dụng để quản lý API của SponsorBlock, đảm bảo chỉ một HttpClient tồn tại.
object SponsorBlock {
    internal val httpClient by lazy {
        HttpClient(CIO) { // Tạo một HttpClient với engine CIO
            install(ContentNegotiation) { // Cài đặt plugin ContentNegotiation giúp tự động chuyển đổi dữ liệu JSON thành đối tượng Kotlin.
                json(
                    Json {
                        ignoreUnknownKeys = true
                    }
                )
            }

            defaultRequest {
                url("https://sponsor.ajay.app")

                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }

            expectSuccess = true
        }
    }
}
