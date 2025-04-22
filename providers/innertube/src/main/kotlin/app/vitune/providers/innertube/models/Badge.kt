package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

//xác định xem một bài hát có nội dung Explicit (Nội dung nhạy cảm, không phù hợp với trẻ em) hay không.
@Serializable
data class Badge(
    val musicInlineBadgeRenderer: MusicInlineBadgeRenderer?
) {
    @Serializable
    data class MusicInlineBadgeRenderer(
        val icon: MusicNavigationButtonRenderer.Icon
    )
}
// Kiểm tra xem trong danh sách Badge có tồn tại biểu tượng badge Explicit hay không.
val List<Badge>?.isExplicit
    get() = this?.find {
        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
    } != null
