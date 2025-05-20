package app.vitune.providers.innertube.models.bodies

import app.vitune.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody( // Lớp này được sử dụng để tạo ra một yêu cầu tìm kiếm đến API của YouTube
    val context: Context = Context.DefaultWeb,
    val query: String,
    val params: String? = null
)
