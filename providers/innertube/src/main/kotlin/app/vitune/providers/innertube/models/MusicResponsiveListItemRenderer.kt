package app.vitune.providers.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MusicResponsiveListItemRenderer( //Lưu trữ thông tin của một mục trong danh sách nhạc như tiêu đề, hình thu nhỏ, điều hướng, huy hiệu, v.v.
    val fixedColumns: List<FlexColumn>?,
    val flexColumns: List<FlexColumn>,
    val thumbnail: ThumbnailRenderer?,
    val navigationEndpoint: NavigationEndpoint?,
    val badges: List<Badge>?
) {
    @Serializable
    data class FlexColumn( //Lưu trữ thông tin của một cột trong danh sách nhạc, có thể là cột cố định hoặc cột linh hoạt.
        @JsonNames("musicResponsiveListItemFixedColumnRenderer")
        val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer?
    ) {
        @Serializable
        data class MusicResponsiveListItemFlexColumnRenderer( //Lưu trữ thông tin của một cột trong danh sách nhạc, có thể là cột cố định hoặc cột linh hoạt.
            val text: Runs?
        )
    }
}
