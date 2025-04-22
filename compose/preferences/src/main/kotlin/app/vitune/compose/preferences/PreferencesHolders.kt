package app.vitune.compose.preferences

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// file này định nghĩa một lớp PreferencesHolder để quản lý SharedPreferences trong ứng dụng Android.
// SharedPreferences là một cách lưu trữ dữ liệu dạng key-value trong Android.

private val coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("PreferencesHolders")) // đây là một coroutine scope để thực hiện các tác vụ bất đồng bộ liên quan đến SharedPreferences

private val canWriteState get() = !Snapshot.current.readOnly && !Snapshot.current.root.readOnly // kiểm tra xem trạng thái hiện tại có thể ghi hay không.
// Nếu có bất kỳ trạng thái nào đang ở chế độ chỉ đọc, thì không thể ghi vào SharedPreferences. Điều này giúp đảm bảo rằng việc ghi vào SharedPreferences chỉ xảy ra khi trạng thái không bị khóa trong một giao dịch khác.

@Stable // đánh dấu rằng lớp này có thể được sử dụng trong Compose để theo dõi sự thay đổi trạng thái
// đây là một lớp đại diện cho một thuộc tính SharedPreferences, cho phép đọc và ghi giá trị từ SharedPreferences một cách an toàn và hiệu quả trong môi trường Compose.
data class SharedPreferencesProperty<T : Any>(
    private val name: String? = null,
    private val get: SharedPreferences.(key: String) -> T,
    private val set: SharedPreferences.Editor.(key: String, value: T) -> Unit, // hàm này được sử dụng để ghi giá trị vào SharedPreferences
    private val default: T
) : ReadWriteProperty<PreferencesHolder, T> { // ReadWriteProperty là một giao thức trong Kotlin cho phép bạn định nghĩa cách đọc và ghi giá trị của thuộc tính
    // Tạo một thuộc tính trạng thái (state) để lưu trữ giá trị hiện tại của thuộc tính SharedPreferences
    private val state = mutableStateOf(default)
    val stateFlow = MutableStateFlow(default) // Tạo một StateFlow để theo dõi sự thay đổi của giá trị trong SharedPreferences
    private var listener: OnSharedPreferenceChangeListener? = null // Lưu trữ listener để lắng nghe sự thay đổi trong SharedPreferences

    // Hàm này được gọi khi thuộc tính SharedPreferences được khởi tạo lần đầu tiên
    // nó sẽ lấy giá trị từ SharedPreferences và thiết lập listener để theo dõi sự thay đổi nếu có
    // Cập nhật giá trị đồng thời cho cả mutableStateOf và StateFlow.
    private fun setState(newValue: T) {
        state.value = newValue
        stateFlow.update { newValue }
    }

    private inline val KProperty<*>.key get() = this@SharedPreferencesProperty.name ?: name // lấy tên của thuộc tính từ KProperty
    // Nếu không có tên, sử dụng tên của thuộc tính trong SharedPreferencesProperty

/*
Khi lần đầu đọc giá trị từ property:
Gọi thisRef.get(property.key) để lấy từ SharedPreferences.
Đăng ký listener (OnSharedPreferenceChangeListener) để tự động cập nhật lại khi SharedPreferences thay đổi ở nơi khác.
Kiểm tra key có khớp không và nếu khác với state hiện tại thì gọi setState.
 */
    override fun getValue(thisRef: PreferencesHolder, property: KProperty<*>): T {
        if (listener == null && canWriteState) {
            setState(thisRef.get(property.key))

            listener = OnSharedPreferenceChangeListener { preferences, key -> // đăng ký một listener để lắng nghe sự thay đổi trong SharedPreferences
                if (key != property.key || !canWriteState) return@OnSharedPreferenceChangeListener // kiểm tra xem key có khớp với thuộc tính hiện tại không

                preferences.get(property.key).let { // lấy giá trị từ SharedPreferences
                    if (it != state.value) setState(it) // nếu giá trị khác với giá trị hiện tại thì cập nhật lại
                }
            }

            thisRef.registerOnSharedPreferenceChangeListener(listener) // đăng ký listener để lắng nghe sự thay đổi trong SharedPreferences
        }
        return state.value
    }

    // Hàm này được gọi khi thuộc tính SharedPreferences được gán giá trị mới
    // KProperty<*> là một tham số đại diện cho thuộc tính mà chúng ta đang làm việc
    override fun setValue(thisRef: PreferencesHolder, property: KProperty<*>, value: T) =
        coroutineScope.launch { // khởi chạy một coroutine để thực hiện tác vụ bất đồng bộ
            thisRef.edit(commit = true) { // sử dụng edit() để bắt đầu một giao dịch ghi vào SharedPreferences
                set(property.key, value)
            }
        }.let { }
}

/**
 * A snapshottable, thread-safe, compose-first, extensible SharedPreferences wrapper that supports
 * virtually all types, and if it doesn't, one could simply type
 * `fun myNewType(...) = SharedPreferencesProperty(...)` and start implementing. Starts off as given
 * defaultValue until we are allowed to subscribe to SharedPreferences. Caution: the type of the
 * preference has to be [Stable], otherwise UB will occur.
 */
// SharedPreferencesHolder là một lớp đại diện cho một đối tượng SharedPreferences
// nó cho phép bạn lưu trữ và truy xuất các giá trị từ SharedPreferences một cách an toàn và hiệu quả trong môi trường Compose
// SharedPreferences là một lớp trong Android được sử dụng để lưu trữ dữ liệu dạng key-value
// open class là một từ khóa trong Kotlin để định nghĩa một lớp có thể được kế thừa
// nó cho phép bạn tạo một lớp cơ sở mà các lớp khác có thể kế thừa từ đó
//  Ủy quyền (by) toàn bộ API của SharedPreferences, nên PreferencesHolder có thể gọi trực tiếp các hàm như getString, edit,...
open class PreferencesHolder(
    application: Application,
    name: String,
    mode: Int = Context.MODE_PRIVATE
) : SharedPreferences by application.getSharedPreferences(name, mode) { // ủy quyền toàn bộ API của SharedPreferences
    // ủy quyền cho SharedPreferences, tức là tất cả các phương thức của SharedPreferences đều có thể được gọi trực tiếp từ PreferencesHolder
    // Ứng dụng sẽ sử dụng SharedPreferences để lưu trữ dữ liệu

    // Đây là các factory function để tạo ra các SharedPreferencesProperty tương ứng với từng kiểu dữ liệu:
    fun boolean(
        defaultValue: Boolean,
        name: String? = null
    ) = SharedPreferencesProperty( // tạo một đối tượng SharedPreferencesProperty
        get = { getBoolean(it, defaultValue) }, // lấy giá trị boolean từ SharedPreferences
        set = { k, v -> putBoolean(k, v) }, // ghi giá trị boolean vào SharedPreferences
        default = defaultValue,
        name = name
    )

    fun string(
        defaultValue: String,
        name: String? = null
    ) = SharedPreferencesProperty(
        get = { getString(it, null) ?: defaultValue },
        set = { k, v -> putString(k, v) },
        default = defaultValue,
        name = name
    )

    fun int(
        defaultValue: Int,
        name: String? = null
    ) = SharedPreferencesProperty(
        get = { getInt(it, defaultValue) },
        set = { k, v -> putInt(k, v) },
        default = defaultValue,
        name = name
    )

    fun float(
        defaultValue: Float,
        name: String? = null
    ) = SharedPreferencesProperty(
        get = { getFloat(it, defaultValue) },
        set = { k, v -> putFloat(k, v) },
        default = defaultValue,
        name = name
    )

    fun long(
        defaultValue: Long,
        name: String? = null
    ) = SharedPreferencesProperty(
        get = { getLong(it, defaultValue) },
        set = { k, v -> putLong(k, v) },
        default = defaultValue,
        name = name
    )

    /*
    Từ khóa reified chỉ dùng được trong hàm inline.
    Nó cho phép bạn truy cập kiểu T tại thời gian chạy, trong khi bình thường các generic type bị type-erased (xóa kiểu) khi biên dịch
     */
    inline fun <reified T : Enum<T>> enum( // T là một kiểu dữ liệu Enum
        // T::class.java.name là tên của lớp Enum
        defaultValue: T,
        name: String? = null
    ) = SharedPreferencesProperty(
        get = {
            getString(it, null) // lấy ra tên của Enum dạng String, ví dụ "DARK"
                ?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
                ?: defaultValue
            /*
            enumValueOf<T>(it) → chuyển chuỗi "DARK" về Enum Theme.DARK
            runCatching { ... }.getOrNull() → tránh crash nếu string không hợp lệ
            ?: defaultValue → fallback khi lỗi
             */
        },
        set = { k, v -> putString(k, v.name) },
        default = defaultValue,
        name = name
    )

    fun stringSet(
        defaultValue: Set<String>,
        name: String? = null
    ) = SharedPreferencesProperty(
        get = { getStringSet(it, null) ?: defaultValue },
        set = { k, v -> putStringSet(k, v) },
        default = defaultValue,
        name = name
    )

    @PublishedApi // đánh dấu rằng thuộc tính này có thể được sử dụng trong các hàm nội bộ
    internal val defaultJson = Json { // tạo một đối tượng Json với các cấu hình mặc định
        isLenient = true // cho phép phân tích cú pháp các chuỗi JSON không chính xác
        prettyPrint = false // không in ra định dạng đẹp
        ignoreUnknownKeys = true // bỏ qua các khóa không xác định trong JSON
        encodeDefaults = true // mã hóa các giá trị mặc định
    }

    inline fun <reified Serializable : Any> json( // T là một kiểu dữ liệu Serializable
        defaultValue: Serializable,
        name: String? = null,
        json: Json = defaultJson // sử dụng Json mặc định nếu không có json nào được truyền vào
    ): SharedPreferencesProperty<Serializable> = SharedPreferencesProperty( // tạo một đối tượng SharedPreferencesProperty
        /*
        Lấy chuỗi từ SharedPreferences (dạng JSON)
        Dùng json.decodeFromString<T>(...) để chuyển về object
        Nếu lỗi → trả về defaultValue
         */
        get = { k ->
            getString(k, json.encodeToString(defaultValue))?.let { json.decodeFromString(it) } // reified giúp gọi json.decodeFromString<Serializable>(...) mà không cần truyền class thủ công
                ?: defaultValue
        },
        set = { k, v -> putString(k, json.encodeToString(v)) },
        default = defaultValue,
        name = name
    )
}
