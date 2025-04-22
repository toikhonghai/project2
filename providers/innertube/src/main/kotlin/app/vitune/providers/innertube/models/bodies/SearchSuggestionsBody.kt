package app.vitune.providers.innertube.models.bodies

import app.vitune.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsBody( // Lớp này được sử dụng để tạo ra một yêu cầu tìm kiếm gợi ý từ API của YouTube.
    val context: Context = Context.DefaultWeb,
    val input: String
)
