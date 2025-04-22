package app.vitune.compose.persist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Suppress("UNCHECKED_CAST") // đây là một cảnh báo từ Kotlin, cho biết rằng kiểu dữ liệu không khớp với kiểu mong đợi
@Composable
// Hàm này được sử dụng để lưu trữ một giá trị có thể thay đổi trong trạng thái của ứng dụng Compose
// SnapshotMutationPolicy<T> là giao diện (interface) trong Compose runtime, dùng để xác định khi nào Compose nên coi một giá trị đã thay đổi — tức là khi nào cần recompose lại giao diện.
fun <T> persist(
    tag: String, // Khóa để lưu trạng thái. Dùng để phân biệt từng biến trạng thái riêng biệt.
    initialValue: T, // Giá trị mặc định khởi tạo nếu chưa có gì được lưu
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy() // Chính sách kiểm tra sự thay đổi
// mặc định là structuralEqualityPolicy() (so sánh theo giá trị).
): MutableState<T> {
    val persistMap = LocalPersistMap.current // Lấy giá trị của LocalPersistMap hiện tại

    return remember(persistMap) { //  đảm bảo đoạn logic chỉ được thực thi lại nếu persistMap thay đổi.
        /*
        Nếu persistMap không null:
        Sử dụng getOrPut(tag) để lấy MutableState tương ứng với tag, hoặc tạo mới nếu chưa có.
        Cần ép kiểu về MutableState<T> (dùng as?) vì map chứa Any?.
        Nếu persistMap là null hoặc kiểu không khớp → tạo mới MutableState với initialValue.
         */
        persistMap?.map?.getOrPut(tag) { mutableStateOf(initialValue, policy) } as? MutableState<T>
            ?: mutableStateOf(initialValue, policy)
    }
}

@Composable
fun <T> persistList(tag: String): MutableState<ImmutableList<T>> =  // Tạo một danh sách bất biến (ImmutableList) được lưu trữ và có thể thay đổi.
    // Không giống mutableListOf(), danh sách này không thể bị thay đổi trực tiếp mà chỉ có thể tạo một bản sao mới với thay đổi áp dụng.
    persist(tag = tag, initialValue = persistentListOf()) //  persistentListOf()Đây là Immutable List (danh sách bất biến) từ thư viện

@Composable
// Dùng cho trường hợp không có giá trị khởi tạo cụ thể, cho phép giá trị có thể là null.
fun <T : Any?> persist(tag: String): MutableState<T?> = // T : Any? nghĩa là T có thể nullable
    // ersist<T>(key) là một delegate dùng để lưu trạng thái lâu dài (thường là trong Preferences hoặc local storage).
    // Khác với remember hay rememberSaveable chỉ giữ state trong session hoặc rotation,
    // persist giữ lại state ngay cả khi bạn rời Composable hoặc thoát app (giống SharedPreferences nhưng reactive hơn).
    persist(tag = tag, initialValue = null)
