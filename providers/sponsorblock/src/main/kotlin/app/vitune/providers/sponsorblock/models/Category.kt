package app.vitune.providers.sponsorblock.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
// đây là một enum class đại diện cho các loại danh mục trong SponsorBlock
enum class Category(val serialName: String) {
    @SerialName("sponsor") // Phần nội dung tài trợ trong video.
    Sponsor("sponsor"), // Ví dụ, nếu API trả về "category": "sponsor", nó sẽ được ánh xạ thành Category.Sponsor.

    @SerialName("selfpromo") // Phần nội dung tự quảng cáo trong video.
    SelfPromotion("selfpromo"),

    @SerialName("interaction") // Phần nội dung tương tác trong video.
    Interaction("interaction"),

    @SerialName("intro") // Phần nội dung giới thiệu trong video.
    Intro("intro"),

    @SerialName("outro")
    Outro("outro"),

    @SerialName("preview")
    Preview("preview"),

    @SerialName("music_offtopic") // Phần nội dung âm nhạc không liên quan trong video.
    OfftopicMusic("music_offtopic"),

    @SerialName("filler")
    Filler("filler"),

    @SerialName("poi_highlight") // Phần nội dung nổi bật trong video.
    PoiHighlight("poi_highlight")
}
