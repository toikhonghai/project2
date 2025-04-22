package app.vitune.providers.innertube.models.bodies

import app.vitune.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
/// Dùng để gửi yêu cầu lấy thêm dữ liệu (ví dụ: tải thêm video, danh sách bài hát).
data class ContinuationBody(
    val context: Context = Context.DefaultWeb,
    val continuation: String
)
