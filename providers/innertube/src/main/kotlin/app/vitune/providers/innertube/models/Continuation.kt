package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class) // để sử dụng JsonNames
@Serializable
data class Continuation( // dùng trong API để lấy thêm dữ liệu (ví dụ: tải thêm video, danh sách bài hát).
    @JsonNames("nextContinuationData", "nextRadioContinuationData") // tên trường trong JSON
    val nextContinuationData: Data? // dữ liệu tiếp theo để tải thêm
) {
    @Serializable
    data class Data(
        val continuation: String?
    )
}
