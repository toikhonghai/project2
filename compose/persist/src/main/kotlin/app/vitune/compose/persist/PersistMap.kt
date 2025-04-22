package app.vitune.compose.persist

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

@JvmInline
// Khai báo một lớp dữ liệu có tên PersistMap, sử dụng để lưu trữ trạng thái lâu dài trong Composition.
value class PersistMap(val map: MutableMap<String, MutableState<*>> = hashMapOf()) {
    /*
    Key (String): Một chuỗi định danh (có thể là tag của trạng thái được lưu).
    Value (MutableState<*>): Trạng thái có thể thay đổi (giữ trạng thái lâu dài trong Composition).
     */
    fun clean(prefix: String) = map.keys.removeAll { it.startsWith(prefix) } // Xóa tất cả trạng thái nào có key bắt đầu bằng prefix.
}
// Tạo một CompositionLocal giúp lưu PersistMap, dùng để truyền trạng thái giữa các Composable mà không cần truyền trực tiếp qua tham số.
val LocalPersistMap = compositionLocalOf<PersistMap?> { // Giá trị mặc định là null, điều này có nghĩa nếu LocalPersistMap chưa được cung cấp (CompositionLocalProvider), nó sẽ báo lỗi.
    Log.e("PersistMap", "Tried to reference uninitialized PersistMap, stacktrace:")
    runCatching { error("Stack:") }.exceptionOrNull()?.printStackTrace() // tạo ra một ngoại lệ và in ra stack trace của nó
    null
}
