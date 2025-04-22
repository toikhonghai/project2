package app.vitune.core.ui.utils

import android.content.res.Configuration
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration

val isLandscape //Biến isLandscape kiểm tra xem màn hình có đang ở chế độ ngang (Landscape) không.
    @Composable // Chỉ có thể gọi bên trong Composable function
    @ReadOnlyComposable // Không gây ra bất kỳ thay đổi trạng thái nào
    get() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE //LocalConfiguration.current.orientation: Lấy thông tin về cấu hình của thiết bị hiện tại.

//Kiểm tra xem thiết bị có chạy Android 6 (Marshmallow) trở lên không.
//Build.VERSION_CODES.M tương ứng với Android 6 (API 23).
@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M) //giúp trình kiểm tra mã biết rằng biến này đảm bảo API >= 23.
inline val isAtLeastAndroid6
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
inline val isAtLeastAndroid7
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
inline val isAtLeastAndroid8
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
inline val isAtLeastAndroid9
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
inline val isAtLeastAndroid10
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
inline val isAtLeastAndroid11
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
inline val isAtLeastAndroid12
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
inline val isAtLeastAndroid13
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

//Kiểm tra Composable đã được chạy hay chưa
@Composable
fun isCompositionLaunched(): Boolean {
    var isLaunched by remember { mutableStateOf(false) } // Lưu trạng thái trong Composable
    LaunchedEffect(Unit) {
        isLaunched = true // Đánh dấu là đã khởi chạy khi hiệu ứng được kích hoạt
    }
    return isLaunched
}

