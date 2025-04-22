package app.vitune.providers.translate.requests

import app.vitune.providers.translate.Translate
import app.vitune.providers.translate.models.Language
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.charsets.name
import java.nio.charset.Charset

// Hàm mở rộng `translate` cho đối tượng `Translate` để dịch văn bản từ ngôn ngữ này sang ngôn ngữ khác
suspend fun Translate.translate(
    text: String,                              // Văn bản cần dịch
    from: Language = Language.Auto,            // Ngôn ngữ nguồn (mặc định là Auto)
    to: Language,                              // Ngôn ngữ đích (bắt buộc phải chỉ định)
    host: String = "translate.googleapis.com", // Máy chủ API (mặc định là Google Dịch)
    charset: Charset = Charsets.UTF_8          // Bộ mã ký tự (mặc định là UTF-8)
): String {
    require(to != Language.Auto) { "The target language cannot be Auto" } // Đảm bảo ngôn ngữ đích không phải là Auto

    val encoding = charset.name.replace("_", "-") // Chuẩn hóa tên bộ mã ký tự

    return client.get("https://$host/translate_a/single") {
        dt.forEach { parameter("dt", it) } // Thêm các tham số cho API request
        parameter("client", "gtx")         // Chỉ định client (Google API)
        parameter("ie", encoding)          // Bộ mã ký tự đầu vào
        parameter("oe", encoding)          // Bộ mã ký tự đầu ra
        parameter("otf", 1)                // Bật dịch trực tuyến
        parameter("ssel", 0)               // Chỉ số lựa chọn ngôn ngữ nguồn
        parameter("tsel", 0)               // Chỉ số lựa chọn ngôn ngữ đích
        parameter("sl", from.code)         // Mã ngôn ngữ nguồn
        parameter("tl", to.code)           // Mã ngôn ngữ đích
        parameter("hl", to.code)           // Ngôn ngữ giao diện người dùng
        parameter("q", text)               // Chuỗi văn bản cần dịch
    }.bodyAsText(charset) // Trả về kết quả dịch dưới dạng chuỗi văn bản
}

