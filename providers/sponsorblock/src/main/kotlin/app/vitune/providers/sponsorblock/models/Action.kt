package app.vitune.providers.sponsorblock.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
// đây là một enum class đại diện cho các loại hành động trong SponsorBlock
enum class Action(val serialName: String) {
    @SerialName("skip")
    Skip("skip"), // Phần nội dung có thể bỏ qua trong video.

    @SerialName("mute")
    Mute("mute"), // Phần nội dung bị tắt tiếng trong video.

    @SerialName("full")
    Full("full"), // Phần nội dung đầy đủ trong video.

    @SerialName("poi")
    POI("poi"), // Phần nội dung điểm quan tâm trong video.

    @SerialName("chapter")
    Chapter("chapter") // Phần nội dung chương trong video.
}
