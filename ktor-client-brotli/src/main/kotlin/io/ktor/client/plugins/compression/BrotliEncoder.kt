package io.ktor.client.plugins.compression

import io.ktor.util.ContentEncoder
import io.ktor.util.Encoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.brotli.dec.BrotliInputStream
import kotlin.coroutines.CoroutineContext

//Brotli là một thuật toán nén dữ liệu hiệu quả cao, được phát triển bởi Google,
// nhằm giảm kích thước dữ liệu truyền tải trên mạng mà vẫn giữ tốc độ giải nén nhanh.
// Đối tượng singleton `BrotliEncoder` để mã hóa nội dung HTTP bằng Brotli.
internal object BrotliEncoder : ContentEncoder, Encoder by Brotli {
    override val name: String = "br" // Định danh "br" (Brotli) theo tiêu chuẩn HTTP
}

// Đối tượng `Brotli` thực hiện giải mã dữ liệu, nhưng không hỗ trợ mã hóa.
private object Brotli : Encoder {

    // Phương thức encode() bị vô hiệu hóa do BrotliOutputStream chưa khả dụng
    private fun encode(): Nothing =
        error("BrotliOutputStream not available (https://github.com/google/brotli/issues/715)")

    // Giải mã dữ liệu nén Brotli từ một ByteReadChannel
    override fun decode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext
    ) = BrotliInputStream(source.toInputStream()).toByteReadChannel() // Chuyển stream thành ByteReadChannel

    // Ném lỗi nếu cố gắng sử dụng Brotli để mã hóa dữ liệu
    override fun encode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext
    ) = encode()

    // Tương tự, ném lỗi khi cố gắng nén dữ liệu vào ByteWriteChannel
    override fun encode(
        source: ByteWriteChannel,
        coroutineContext: CoroutineContext
    ) = encode()
}
