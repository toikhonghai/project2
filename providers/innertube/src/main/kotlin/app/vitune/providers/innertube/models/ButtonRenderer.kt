package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ButtonRenderer( //  đại diện cho một nút bấm trên giao diện.
    val navigationEndpoint: NavigationEndpoint? //  chứa thông tin về hành động khi bấm vào nút (ví dụ: điều hướng đến một trang khác, phát nhạc, mở danh sách phát,...).
)

@Serializable
data class SubscribeButtonRenderer(
    val subscribed: Boolean? = null,
    val subscribedButtonText: Runs? = null,
    val unsubscribedButtonText: Runs? = null,
    val channelId: String? = null,
    val subscriberCountText: Runs? = null,
    val subscribedText: Runs? = null,
    val unsubscribedText: Runs? = null
)