package app.vitune.providers.kugou.models

import kotlinx.serialization.Serializable

@Serializable
// đây là một class dữ liệu trong Kotlin, được sử dụng để ánh xạ dữ liệu từ JSON
internal data class SearchSongResponse(
    val data: Data
) {
    @Serializable
    internal data class Data(
        val info: List<Info>
    ) {
        @Serializable
        internal data class Info(
            val duration: Long,
            val hash: String // Mã băm của bài hát
        )
    }
}
