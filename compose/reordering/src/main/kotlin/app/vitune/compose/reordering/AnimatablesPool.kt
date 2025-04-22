package app.vitune.compose.reordering

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/*
Lớp AnimatablesPool<T, V : AnimationVector> trong đoạn mã Kotlin này định nghĩa một pool (bể chứa) các đối tượng Animatable<T, V>,
dùng để tái sử dụng thay vì tạo mới nhiều lần — điều này giúp giảm cấp phát bộ nhớ và tăng hiệu suất,
đặc biệt trong các animation UI liên tục (Compose chẳng hạn).
T: kiểu dữ liệu thực (ví dụ: Float, Dp, Offset, v.v.)
V: kiểu animation vector đại diện cho T (ví dụ: AnimationVector1D, AnimationVector2D, v.v.)
 */
class AnimatablesPool<T, V : AnimationVector>(
    private val initialValue: T, // giá trị khởi tạo cho Animatable
    private val typeConverter: TwoWayConverter<T, V>, // chuyển đổi giữa kiểu dữ liệu thực và animation vector
    private val visibilityThreshold: T? = null
) {
    private val animatables = mutableListOf<Animatable<T, V>>() // danh sách chứa các Animatable đã được tạo ra
    private val mutex = Mutex() // mutex để đồng bộ hóa truy cập vào danh sách animatables

    // có nhiệm vụ lấy một đối tượng Animatable từ pool
    suspend fun acquire() = mutex.withLock {
        animatables.removeFirstOrNull() ?: Animatable(
            initialValue = initialValue,
            typeConverter = typeConverter,
            visibilityThreshold = visibilityThreshold, //  ngưỡng tối thiểu để coi là có chuyển động. Thường dùng để xác định xem animation có nên chạy không nếu sự thay đổi quá nhỏ.
            label = "AnimatablesPool: Animatable" // nhãn cho animation, giúp dễ dàng theo dõi trong logcat
        )
    }

    /*
    Mutex (viết tắt của Mutual Exclusion) là một cơ chế đồng bộ (synchronization) trong lập trình,
    giúp đảm bảo rằng chỉ một thực thể (thread hoặc coroutine) có thể truy cập vào một tài nguyên hoặc khối mã (code block) nào đó tại một thời điểm.
     */
    // trả lại một đối tượng Animatable vào pool để có thể tái sử dụng sau này
    suspend fun release(animatable: Animatable<T, V>) = mutex.withLock {
        animatable.snapTo(initialValue) // đặt lại giá trị của Animatable về giá trị khởi tạo
        animatables += animatable
    }
}
