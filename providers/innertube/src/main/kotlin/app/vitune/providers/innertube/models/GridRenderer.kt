package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

//sử dụng để biểu diễn một lưới chứa danh sách các mục trong ứng dụng YouTube Music.
// Nó có thể chứa các nút điều hướng, mục hai hàng (hiển thị thông tin bài hát, nghệ sĩ, album...), và tiêu đề lưới.
@Serializable
data class GridRenderer(
    val items: List<Item>?,
    val header: Header?
) {
    @Serializable
    data class Item(
        val musicNavigationButtonRenderer: MusicNavigationButtonRenderer?, // đại diện cho một nút điều hướng trong ứng dụng YouTube Music.
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? // đại diện cho một mục hai hàng trong ứng dụng YouTube Music.
    )

    @Serializable
    data class Header(
        val gridHeaderRenderer: GridHeaderRenderer? // đại diện cho tiêu đề của lưới.
    )

    @Serializable
    data class GridHeaderRenderer(
        val title: Runs? // đại diện cho tiêu đề của lưới, có thể chứa nhiều đoạn văn bản (runs).
    )
}
