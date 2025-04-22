package io.ktor.client.plugins.compression

// Mở rộng cấu hình `ContentEncodingConfig` để hỗ trợ nén Brotli
fun ContentEncodingConfig.brotli(quality: Float? = null) {
    // Đăng ký `BrotliEncoder` làm bộ mã hóa tùy chỉnh với chất lượng nén `quality`
    customEncoder(BrotliEncoder, quality)
}

