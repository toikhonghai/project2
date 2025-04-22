package app.vitune.providers.sponsorblock.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
// Định nghĩa lớp giá trị `Segment` để lưu trữ thông tin về đoạn video
data class Segment(
    // Danh sách thời gian bắt đầu và kết thúc của đoạn video (đo bằng giây)
    internal val segment: List<Double>,

    // UUID của đoạn video, dùng `@SerialName` để ánh xạ tên trường trong JSON
    @SerialName("UUID")
    val uuid: String? = null,

    // Danh mục của đoạn video (ví dụ: Sponsor, SelfPromo, ...)
    val category: Category,

    // Loại hành động mà đoạn video thực hiện (ví dụ: Play, Skip, ...)
    @SerialName("actionType")
    val action: Action,

    // Mô tả chi tiết về đoạn video (ví dụ: "Đoạn video giới thiệu quảng cáo")
    val description: String
) {
    // Tính toán thời gian bắt đầu của đoạn video từ phần tử đầu tiên trong `segment`
    val start get() = segment.first().seconds

    // Tính toán thời gian kết thúc của đoạn video từ phần tử thứ hai trong `segment`
    val end get() = segment[1].seconds
}

