package app.vitune.providers.innertube.models.bodies

import app.vitune.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class QueueBody( // Lớp này được sử dụng để tạo một yêu cầu đến API của YouTube Music để thêm video vào hàng đợi phát nhạc.
    val context: Context = Context.DefaultWeb,
    val videoIds: List<String>? = null,
    val playlistId: String? = null
)
