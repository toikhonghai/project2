package app.vitune.core.data.utils

import android.net.Uri
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// Lớp RingBuffer (Bộ đệm vòng) có kích thước cố định, khi thêm phần tử mới sẽ ghi đè phần tử cũ.
open class RingBuffer<T>(val size: Int, private val init: (index: Int) -> T) : Iterable<T> {
    // Danh sách chứa các phần tử của bộ đệm
    private val list = MutableList(size, init)

    // Biến index để xác định vị trí phần tử tiếp theo sẽ được ghi vào, đồng bộ hóa để tránh lỗi đa luồng
    @get:Synchronized
    @set:Synchronized
    private var index = 0

    // Toán tử get để truy xuất phần tử theo index (trả về null nếu index không hợp lệ)
    operator fun get(index: Int) = list.getOrNull(index)

    // Toán tử += để thêm phần tử mới vào buffer, nếu đầy thì ghi đè lên phần tử cũ (theo vòng lặp)
    operator fun plusAssign(element: T) {
        list[index++ % size] = element
    }

    // Triển khai iterator để có thể lặp qua các phần tử trong RingBuffer
    override fun iterator() = list.iterator()

    // Hàm clear để đặt lại toàn bộ buffer về trạng thái ban đầu
    fun clear() = list.indices.forEach {
        list[it] = init(it)
    }
}

// Lớp UriCache lưu trữ một bộ đệm vòng của các URI với khóa và siêu dữ liệu kèm theo
class UriCache<Key : Any, Meta>(size: Int = 16) {
    // Bộ đệm vòng để lưu danh sách các URI đã cache
    private val buffer = RingBuffer<CachedUri<Key, Meta>?>(size) { null }

    // Data class lưu thông tin của một URI được cache
    data class CachedUri<Key, Meta> internal constructor(
        val key: Key,           // Khóa dùng để tìm kiếm URI
        val meta: Meta,         // Siêu dữ liệu đi kèm
        val uri: Uri,           // Địa chỉ URI
        val validUntil: Instant? // Thời gian hết hạn của URI (nếu có)
    )

    // Toán tử get: tìm một URI trong buffer dựa vào key, chỉ trả về nếu URI còn hợp lệ (chưa hết hạn)
    operator fun get(key: Key) = buffer.find {
        it != null &&  // Kiểm tra phần tử không null
                it.key == key && // Kiểm tra khóa trùng khớp
                (it.validUntil == null || it.validUntil > Clock.System.now()) // Kiểm tra thời gian hết hạn
    }

    // Hàm push: thêm một URI mới vào cache
    fun push(
        key: Key,
        meta: Meta,
        uri: Uri,
        validUntil: Instant?
    ) {
        // Nếu URI đã hết hạn ngay lúc thêm, bỏ qua
        if (validUntil != null && validUntil <= Clock.System.now()) return

        // Thêm URI vào buffer
        buffer += CachedUri(key, meta, uri, validUntil)
    }

    // Hàm clear: xóa toàn bộ cache
    fun clear() = buffer.clear()
}
