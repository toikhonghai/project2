package app.vitune.core.ui.utils

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.IntDef
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Marker interface that marks a class as Bundle accessor
 */
// Marker interface để đánh dấu lớp có thể sử dụng property delegate với Bundle
interface BundleAccessor

// Hàm tạo delegate để đọc/ghi dữ liệu vào Bundle một cách tự động
private inline fun <T> Bundle.bundleDelegate(
    name: String? = null, // Tên key trong Bundle (mặc định lấy tên biến nếu không truyền)
    crossinline get: Bundle.(String) -> T, // Hàm lấy giá trị từ Bundle
    crossinline set: Bundle.(k: String, v: T) -> Unit // Hàm đặt giá trị vào Bundle
) = PropertyDelegateProvider<BundleAccessor, ReadWriteProperty<BundleAccessor, T>> { _, property ->
    val actualName = name ?: property.name // Nếu không truyền key, dùng tên biến làm key

    object : ReadWriteProperty<BundleAccessor, T> {
        override fun getValue(thisRef: BundleAccessor, property: KProperty<*>) =
            get(this@Bundle, actualName) // Lấy giá trị từ Bundle

        override fun setValue(thisRef: BundleAccessor, property: KProperty<*>, value: T) =
            set(this@Bundle, actualName, value) // Ghi giá trị vào Bundle
    }
}

// Delegate giúp truy cập boolean trong Bundle
context(BundleAccessor)
val Bundle.boolean get() = boolean()

// Tạo property delegate cho Boolean
context(BundleAccessor)
fun Bundle.boolean(name: String? = null) = bundleDelegate(
    name = name,
    get = { getBoolean(it) }, // Dùng getBoolean() để lấy giá trị
    set = { k, v -> putBoolean(k, v) } // Dùng putBoolean() để lưu giá trị
)

context(BundleAccessor)
val Bundle.byte get() = byte()

context(BundleAccessor)
fun Bundle.byte(name: String? = null) = bundleDelegate(
    name = name,
    get = { getByte(it) },
    set = { k, v -> putByte(k, v) }
)

context(BundleAccessor)
val Bundle.char get() = char()

context(BundleAccessor)
fun Bundle.char(name: String? = null) = bundleDelegate(
    name = name,
    get = { getChar(it) },
    set = { k, v -> putChar(k, v) }
)

context(BundleAccessor)
val Bundle.short get() = short()

context(BundleAccessor)
fun Bundle.short(name: String? = null) = bundleDelegate(
    name = name,
    get = { getShort(it) },
    set = { k, v -> putShort(k, v) }
)

context(BundleAccessor)
val Bundle.int get() = int()

context(BundleAccessor)
fun Bundle.
        int(name: String? = null) = bundleDelegate(
    name = name,
    get = { getInt(it) },
    set = { k, v -> putInt(k, v) }
)

context(BundleAccessor)
val Bundle.long get() = long()

context(BundleAccessor)
fun Bundle.long(name: String? = null) = bundleDelegate(
    name = name,
    get = { getLong(it) },
    set = { k, v -> putLong(k, v) }
)

context(BundleAccessor)
val Bundle.float get() = float()

context(BundleAccessor)
fun Bundle.float(name: String? = null) = bundleDelegate(
    name = name,
    get = { getFloat(it) },
    set = { k, v -> putFloat(k, v) }
)

context(BundleAccessor)
val Bundle.double get() = double()

context(BundleAccessor)
fun Bundle.double(name: String? = null) = bundleDelegate(
    name = name,
    get = { getDouble(it) },
    set = { k, v -> putDouble(k, v) }
)

context(BundleAccessor)
val Bundle.string get() = string()

context(BundleAccessor)
fun Bundle.string(name: String? = null) = bundleDelegate(
    name = name,
    get = { getString(it) },
    set = { k, v -> putString(k, v) }
)

context(BundleAccessor)
val Bundle.intList get() = intList()

context(BundleAccessor)
fun Bundle.intList(name: String? = null) = bundleDelegate(
    name = name,
    get = { getIntegerArrayList(it) },
    set = { k, v -> putIntegerArrayList(k, v) }
)

context(BundleAccessor)
val Bundle.stringList get() = stringList()

context(BundleAccessor)
fun Bundle.stringList(name: String? = null) = bundleDelegate<List<String>?>(
    name = name,
    get = { getStringArrayList(it) },
    set = { k, v -> putStringArrayList(k, v?.let { ArrayList(it) }) }
)

context(BundleAccessor)
val Bundle.booleanArray get() = booleanArray()

context(BundleAccessor)
fun Bundle.booleanArray(name: String? = null) = bundleDelegate(
    name = name,
    get = { getBooleanArray(it) },
    set = { k, v -> putBooleanArray(k, v) }
)

context(BundleAccessor)
val Bundle.byteArray get() = byteArray()

context(BundleAccessor)
fun Bundle.byteArray(name: String? = null) = bundleDelegate(
    name = name,
    get = { getByteArray(it) },
    set = { k, v -> putByteArray(k, v) }
)

context(BundleAccessor)
val Bundle.shortArray get() = shortArray()

context(BundleAccessor)
fun Bundle.shortArray(name: String? = null) = bundleDelegate(
    name = name,
    get = { getShortArray(it) },
    set = { k, v -> putShortArray(k, v) }
)

context(BundleAccessor)
val Bundle.charArray get() = charArray()

context(BundleAccessor)
fun Bundle.charArray(name: String? = null) = bundleDelegate(
    name = name,
    get = { getCharArray(it) },
    set = { k, v -> putCharArray(k, v) }
)

context(BundleAccessor)
val Bundle.intArray get() = intArray()

context(BundleAccessor)
fun Bundle.intArray(name: String? = null) = bundleDelegate(
    name = name,
    get = { getIntArray(it) },
    set = { k, v -> putIntArray(k, v) }
)

context(BundleAccessor)
val Bundle.floatArray get() = floatArray()

context(BundleAccessor)
fun Bundle.floatArray(name: String? = null) = bundleDelegate(
    name = name,
    get = { getFloatArray(it) },
    set = { k, v -> putFloatArray(k, v) }
)

context(BundleAccessor)
val Bundle.doubleArray get() = doubleArray()

context(BundleAccessor)
fun Bundle.doubleArray(name: String? = null) = bundleDelegate(
    name = name,
    get = { getDoubleArray(it) },
    set = { k, v -> putDoubleArray(k, v) }
)

context(BundleAccessor)
val Bundle.stringArray get() = stringArray()

context(BundleAccessor)
fun Bundle.stringArray(name: String? = null) = bundleDelegate(
    name = name,
    get = { getStringArray(it) },
    set = { k, v -> putStringArray(k, v) }
)

class SongBundleAccessor(val extras: Bundle = Bundle()) : BundleAccessor {
    companion object {
        // Tạo một Bundle với dữ liệu từ block lambda
        fun bundle(block: SongBundleAccessor.() -> Unit) = SongBundleAccessor().apply(block).extras
    }

    // Các thuộc tính được ủy quyền cho Bundle
    var albumId by extras.string
    var durationText by extras.string
    var artistNames by extras.stringList
    var artistIds by extras.stringList
    var explicit by extras.boolean
    var isFromPersistentQueue by extras.boolean
}

// Mở rộng Bundle để có thể truy cập SongBundleAccessor
inline val Bundle.songBundle get() = SongBundleAccessor(this)

class ActivityIntentBundleAccessor(val extras: Bundle = Bundle()) : BundleAccessor {
    companion object {
        // Tạo một Bundle với dữ liệu từ block lambda
        fun bundle(block: ActivityIntentBundleAccessor.() -> Unit) = ActivityIntentBundleAccessor().apply(block).extras
    }

    // Các thuộc tính được ủy quyền cho Bundle
    var query by extras.string(SearchManager.QUERY)
    var text by extras.string(Intent.EXTRA_TEXT)
    var mediaFocus by extras.string(MediaStore.EXTRA_MEDIA_FOCUS)

    var album by extras.string(MediaStore.EXTRA_MEDIA_ALBUM)
    var artist by extras.string(MediaStore.EXTRA_MEDIA_ARTIST)
    var genre by extras.string("android.intent.extra.genre")
    var playlist by extras.string("android.intent.extra.playlist")
    var title by extras.string(MediaStore.EXTRA_MEDIA_TITLE)
}

// Mở rộng Bundle để có thể truy cập ActivityIntentBundleAccessor
inline val Bundle.activityIntentBundle get() = ActivityIntentBundleAccessor(this)


// Annotation này chỉ tồn tại trong mã nguồn, không ảnh hưởng đến runtime hoặc bytecode.
@Retention(AnnotationRetention.SOURCE)
// Xác định annotation có thể áp dụng vào những nơi nào: biến, hàm, tham số, getter, setter, v.v.
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.TYPE,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY
)
// Định nghĩa một tập hợp giá trị hợp lệ (giống như enum nhưng tiết kiệm bộ nhớ hơn).
@IntDef(
    AudioEffect.CONTENT_TYPE_MUSIC,
    AudioEffect.CONTENT_TYPE_MOVIE,
    AudioEffect.CONTENT_TYPE_GAME,
    AudioEffect.CONTENT_TYPE_VOICE
)
annotation class ContentType // Annotation này sẽ dùng để kiểm tra giá trị hợp lệ cho biến kiểu Int.


// Lớp này giúp truy cập và thao tác với dữ liệu trong Bundle.
class EqualizerIntentBundleAccessor(val extras: Bundle = Bundle()) : BundleAccessor {

    companion object {
        // Hàm tạo nhanh một Bundle với logic tùy chỉnh thông qua lambda.
        fun bundle(block: EqualizerIntentBundleAccessor.() -> Unit) =
            EqualizerIntentBundleAccessor().apply(block).extras

        // Hàm mở bộ cân bằng âm thanh (Equalizer) với sessionId và kiểu nội dung.
        context(Context)
        fun sendOpenEqualizer(
            sessionId: Int,
            @ContentType // Đảm bảo giá trị type phải thuộc nhóm đã định nghĩa trong @IntDef.
            type: Int = AudioEffect.CONTENT_TYPE_MUSIC
        ) = sendBroadcast( // Gửi broadcast để mở Equalizer.
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                replaceExtras( // Thêm dữ liệu vào intent.
                    bundle {
                        audioSession = sessionId
                        packageName = this@Context.packageName
                        contentType = type
                    }
                )
            }
        )

        // Hàm gửi broadcast để đóng Equalizer với sessionId.
        context(Context)
        fun sendCloseEqualizer(sessionId: Int) = sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                replaceExtras(
                    EqualizerIntentBundleAccessor.bundle {
                        audioSession = sessionId
                    }
                )
            }
        )
    }

    // Tạo các thuộc tính giúp truy xuất dữ liệu từ Bundle một cách thuận tiện.
    var audioSession by extras.int(AudioEffect.EXTRA_AUDIO_SESSION)
    var packageName by extras.string(AudioEffect.EXTRA_PACKAGE_NAME)

    // Kiểu dữ liệu phải là một trong các giá trị hợp lệ được định nghĩa trong @IntDef.
    var contentType by extras.int(AudioEffect.EXTRA_CONTENT_TYPE)
        @ContentType // Đánh dấu getter với annotation để kiểm tra giá trị hợp lệ.
        get

        @SuppressLint("SupportAnnotationUsage")
        @ContentType
        set
}

// Mở rộng Bundle để có thể dễ dàng truy cập bằng `equalizerIntentBundle`.
inline val Bundle.equalizerIntentBundle get() = EqualizerIntentBundleAccessor(this)

