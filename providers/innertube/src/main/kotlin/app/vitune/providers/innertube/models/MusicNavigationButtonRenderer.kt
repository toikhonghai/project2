package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable
//đại diện cho một nút điều hướng trong giao diện ứng dụng nghe nhạc.
// Nó chứa thông tin về văn bản, biểu tượng và hành động khi nhấn vào.
@Serializable
data class MusicNavigationButtonRenderer(
    val buttonText: Runs,
    val solid: Solid?,
    val iconStyle: IconStyle?,
    val clickCommand: NavigationEndpoint
) {
    val isMood: Boolean
        get() = clickCommand.browseEndpoint?.browseId == "FEmusic_moods_and_genres_category" // Kiểm tra xem nút có phải là nút Mood hay không(chủ đề hoặc thể loại nhạc).

    @Serializable
    data class Solid(
        val leftStripeColor: Long // Màu sắc của dải bên trái của nút điều hướng.
    )

    @Serializable
    data class IconStyle(
        val icon: Icon // Biểu tượng của nút điều hướng.
    )

    @Serializable
    data class Icon(
        val iconType: String // Loại biểu tượng, ví dụ: "MUSIC_EXPLICIT_BADGE" cho biểu tượng nội dung nhạy cảm.
    )
}
