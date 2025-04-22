package app.vitune.compose.persist

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun PersistMapCleanup(prefix: String) { // Dọn dẹp trạng thái trong PersistMap khi Composable bị hủy (unmounted).
    val context = LocalContext.current
    val persistMap = LocalPersistMap.current

    DisposableEffect(persistMap) { // giúp thực hiện tác vụ cleanup khi Composable bị hủy.
        onDispose { // chạy khi Composable không còn tồn tại trong Composition.
            if (context.findActivityNullable()?.isChangingConfigurations == false) // đảm bảo không xóa dữ liệu khi xoay màn hình, tránh mất trạng thái không mong muốn.
                persistMap?.clean(prefix) // để xóa tất cả trạng thái có key bắt đầu bằng prefix.
        }
    }
}

/*
Tìm Activity chứa Context hiện tại, nếu có.
Hữu ích khi ta có Context từ LocalContext.current, nhưng cần lấy Activity để kiểm tra isChangingConfigurations.
 */
fun Context.findActivityNullable(): Activity? {
    var current = this
    while (current is ContextWrapper) { // Một Context có thể được bọc bởi nhiều ContextWrapper (ví dụ: ContextThemeWrapper, ApplicationContext).
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
