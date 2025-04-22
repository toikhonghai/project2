package app.vitune.android.models

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import app.vitune.core.ui.ColorParceler
import app.vitune.providers.innertube.Innertube
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith

/*
@Parcelize Đây là một annotation của Kotlin để tự động tạo mã liên quan đến Parcelable,
giúp lớp Mood có thể được truyền giữa các component trong ứng dụng Android (như Activity, Fragment)
thông qua Intent mà không cần viết thủ công mã để chuyển đổi đối tượng thành dạng Parcelable.
 */
@Parcelize
data class Mood(
    val name: String,
    val color: @WriteWith<ColorParceler> Color, // Sử dụng ColorParceler để chuyển đổi giữa Color và Parcelable
    val browseId: String?,
    val params: String? // Tham số bổ sung cho endpoint
) : Parcelable

fun Innertube.Mood.Item.toUiMood() = Mood(
    name = title,
    color = Color(stripeColor),
    browseId = endpoint.browseId, // Lấy browseId từ endpoint
    params = endpoint.params // Lấy các tham số từ endpoint (nếu có
)
