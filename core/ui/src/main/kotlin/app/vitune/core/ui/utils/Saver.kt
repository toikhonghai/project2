package app.vitune.core.ui.utils

import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.flow.MutableStateFlow

/*
định nghĩa các Saver trong Jetpack Compose để lưu trạng thái của các đối tượng vào Bundle,
giúp khôi phục dữ liệu sau khi cấu trúc giao diện thay đổi (chẳng hạn khi xoay màn hình).
 */
fun <Type : Any> stateFlowSaver() = stateFlowSaverOf<Type, Type>(
    from = { it },
    to = { it }
    //Sử dụng stateFlowSaverOf() với from = { it } và to = { it }, nghĩa là không thay đổi dữ liệu khi lưu/trả về.
)

//Generic Saver cho MutableStateFlow<Type>.
inline fun <Type, Saveable : Any> stateFlowSaverOf(
    crossinline from: (Saveable) -> Type,
    crossinline to: (Type) -> Saveable
) = object : Saver<MutableStateFlow<Type>, Saveable> {
    override fun restore(value: Saveable) = MutableStateFlow(from(value)) //restore(value: Saveable): Chuyển giá trị được lưu vào MutableStateFlow.
    override fun SaverScope.save(value: MutableStateFlow<Type>) = to(value.value)// save(value: MutableStateFlow<Type>): Lưu giá trị hiện tại của MutableStateFlow.
}

inline fun <reified T : Parcelable> stateListSaver() = listSaver<SnapshotStateList<T>, T>(//listSaver giúp lưu danh sách kiểu T : Parcelable
    save = { it.toList() }, //Chuyển danh sách SnapshotStateList<T> thành List<T> để lưu vào Bundle.
    restore = { it.toMutableStateList() } //Khôi phục từ List<T> về SnapshotStateList<T>.
)
/*
Saver<T, Saveable> là một interface trong Jetpack Compose giúp lưu trạng thái của đối tượng tùy chỉnh vào Bundle,
giúp khôi phục dữ liệu khi xoay màn hình hoặc khi Activity bị hủy.
 */
inline fun <reified E : Enum<E>> enumSaver() = object : Saver<E, String> {//Saver cho Enum, giúp lưu enum dưới dạng chuỗi (String).
    override fun restore(value: String) = enumValues<E>().first { it.name == value }
    override fun SaverScope.save(value: E) = value.name
}
