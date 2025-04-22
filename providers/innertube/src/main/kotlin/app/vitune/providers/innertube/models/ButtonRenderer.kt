package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ButtonRenderer( //  đại diện cho một nút bấm trên giao diện.
    val navigationEndpoint: NavigationEndpoint? //  chứa thông tin về hành động khi bấm vào nút (ví dụ: điều hướng đến một trang khác, phát nhạc, mở danh sách phát,...).
)

@Serializable
data class SubscribeButtonRenderer( // đại diện cho nút "Đăng ký kênh" trên YouTube Music.
    val subscriberCountText: Runs?
)
