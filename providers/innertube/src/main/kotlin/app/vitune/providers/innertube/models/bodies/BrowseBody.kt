package app.vitune.providers.innertube.models.bodies

import app.vitune.providers.innertube.models.Context
import kotlinx.serialization.Serializable
//ửi yêu cầu đến API của YouTube Music, nhằm lấy thông tin về một playlist, album hoặc danh sách nội dung dựa trên browseId
@Serializable
data class BrowseBody(
    val context: Context = Context.DefaultWeb,
    val browseId: String,
    val params: String? = null
)
