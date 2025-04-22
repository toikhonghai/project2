package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.vitune.providers.piped.models.authenticatedWith
import io.ktor.http.Url

@Immutable
@Entity(
    indices = [ // Tạo một chỉ mục (index) trên hai cột apiBaseUrl và username.
        Index(
            value = ["apiBaseUrl", "username"], // các cột được chỉ mục hóa
            unique = true // unique = true: đảm bảo rằng không có bản ghi nào có cùng apiBaseUrl và username trùng nhau.
        )
    ]
)
data class PipedSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val apiBaseUrl: Url, // URL của API mà người dùng đang kết nối đến (có thể là instance của piped.video chẳng hạn).
    val token: String, // token xác thực của người dùng (có thể là JWT token hoặc một loại token khác).
    val username: String // the username should never change on piped  Ghi chú bên cạnh nói rằng username không bao giờ thay đổi trong hệ thống piped.
) {
    fun toApiSession() = apiBaseUrl authenticatedWith token // Hàm này sẽ trả về một đối tượng ApiSession, trong đó apiBaseUrl được xác thực bằng token.
    // Điều này có thể được sử dụng để tạo một phiên làm việc với API của piped.video.
}
