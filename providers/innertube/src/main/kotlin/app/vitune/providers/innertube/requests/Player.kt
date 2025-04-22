package app.vitune.providers.innertube.requests

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.Context
import app.vitune.providers.innertube.models.PlayerResponse
import app.vitune.providers.innertube.models.bodies.PlayerBody
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.util.generateNonce
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

// Gửi một yêu cầu HTTP để lấy thông tin phát lại từ YouTube Music API và
private suspend fun Innertube.tryContexts(
    body: PlayerBody,
    vararg contexts: Context // Danh sách các ngữ cảnh để thử nghiệm
): PlayerResponse? { // Trả về PlayerResponse nếu thành công, null nếu không thành công
    contexts.forEach { context ->
        if (!currentCoroutineContext().isActive) return null

        logger.info("Trying ${context.client.clientName} ${context.client.clientVersion} ${context.client.platform}")
        val cpn = generateNonce(16).decodeToString() // Tạo một chuỗi ngẫu nhiên 16 ký tự
        runCatchingCancellable {
            client.post(if (context.client.music) PLAYER_MUSIC else PLAYER) {
                setBody( //  được sử dụng để thiết lập nội dung (payload) của HTTP request trước khi gửi đến server.
                    body.copy(
                        context = context,
                        cpn = cpn,
                        playbackContext = PlayerBody.PlaybackContext(
                            contentPlaybackContext = PlayerBody.PlaybackContext.ContentPlaybackContext( //  Tạo một ngữ cảnh phát lại nội dung
                                signatureTimestamp = getSignatureTimestamp(context) // Lấy timestamp chữ ký
                            )
                        )
                    )
                )

                context.apply() // Áp dụng ngữ cảnh vào yêu cầu

                parameter("t", generateNonce(12))
                header("X-Goog-Api-Format-Version", "2")
                parameter("id", body.videoId)
            }.body<PlayerResponse>().also { logger.info("Got $it") } // Gửi yêu cầu HTTP POST đến server và nhận phản hồi dưới dạng PlayerResponse
        }
            ?.getOrNull()
            ?.takeIf { it.isValid } // Kiểm tra xem phản hồi có hợp lệ hay không
            ?.let {
                return it.copy(
                    cpn = cpn,
                    context = context
                )
            }
    }

    return null
}

// Kiểm tra xem phản hồi có hợp lệ hay không
private val PlayerResponse.isValid
    get() = playabilityStatus?.status == "OK" &&
            streamingData?.adaptiveFormats?.any { it.url != null || it.signatureCipher != null } == true

// Lấy timestamp chữ ký từ ngữ cảnh
suspend fun Innertube.player(body: PlayerBody): Result<PlayerResponse?>? = runCatchingCancellable {
    tryContexts( // Gọi hàm tryContexts để thử nghiệm với các ngữ cảnh khác nhau
        body = body,
        Context.DefaultIOS,
        Context.DefaultWeb,
        Context.DefaultTV,
        Context.DefaultAndroidMusic
    )
}
