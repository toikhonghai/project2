@file:Suppress("UNCHECKED_CAST")

package app.vitune.compose.routing

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.parcelize.Parcelize

// Đánh dấu class có thể truyền qua Bundle (Intent) trong Android
@Parcelize
@Immutable // Đảm bảo không thay đổi sau khi khởi tạo
sealed class Route : Parcelable {// Route là một lớp trừu tượng (abstract class) đại diện cho các đường dẫn trong ứng dụng.
    abstract val tag: String // Định danh duy nhất cho mỗi Route

    // So sánh 2 Route dựa trên tag
    override fun equals(other: Any?) = when {
        this === other -> true
        other is Route -> tag == other.tag
        else -> false
    }

    override fun hashCode() = tag.hashCode() // Tạo mã băm (hash code) cho Route dựa trên tag

    // Gửi yêu cầu điều hướng (route request) thông qua globalRouteFlow (không suspend)
    protected fun global(args: Array<Any?>) = globalRouteFlow.tryEmit(
        RouteRequest(
            route = this,
            args = args
        )
    )

    // Gửi route và chờ đến khi có người lắng nghe (suspend version)
    protected suspend fun ensureGlobal(args: Array<Any?>) {
        globalRouteFlow.subscriptionCount.filter { it > 0 }.first() // chờ có subscriber
        globalRouteFlow.emit(
            RouteRequest(
                route = this,
                args = args
            )
        )
    }
}

// Route không có tham số
@Immutable
class Route0(override val tag: String) : Route() {
    // Khi được gọi trong RouteHandlerScope, chỉ render content nếu route này là route con hiện tại
    context(RouteHandlerScope) // context này cho phép sử dụng RouteHandlerScope nghĩa là có thể gọi các hàm trong RouteHandlerScope
    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        if (this == child) content()
    }

    // Kích hoạt route này thông qua globalRouteFlow (không có tham số)
    fun global() = global(emptyArray())

    // Kích hoạt route này một cách an toàn (chờ có subscriber)
    suspend fun ensureGlobal() = ensureGlobal(emptyArray())
}

// Route có 1 tham số
@Immutable
class Route1<P0>(override val tag: String) : Route() {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable (P0) -> Unit) {
        if (this == child) content(args[0] as P0) // ép kiểu tham số từ args
    }

    fun global(p0: P0) = global(arrayOf(p0))
    suspend fun ensureGlobal(p0: P0) = ensureGlobal(arrayOf(p0))
}

// Route có 2 tham số
@Immutable
class Route2<P0, P1>(override val tag: String) : Route() {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable (P0, P1) -> Unit) {
        if (this == child) content(
            args[0] as P0,
            args[1] as P1
        )
    }

    fun global(p0: P0, p1: P1) = global(arrayOf(p0, p1))
    suspend fun ensureGlobal(p0: P0, p1: P1) = ensureGlobal(arrayOf(p0, p1))
}

// Route có 3 tham số
@Immutable
class Route3<P0, P1, P2>(override val tag: String) : Route() {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable (P0, P1, P2) -> Unit) {
        if (this == child) content(
            args[0] as P0,
            args[1] as P1,
            args[2] as P2
        )
    }

    fun global(p0: P0, p1: P1, p2: P2) = global(arrayOf(p0, p1, p2))
    suspend fun ensureGlobal(p0: P0, p1: P1, p2: P2) = ensureGlobal(arrayOf(p0, p1, p2))
}

// Route có 4 tham số
@Immutable
class Route4<P0, P1, P2, P3>(override val tag: String) : Route() {
    context(RouteHandlerScope)
    @Composable
    operator fun invoke(content: @Composable (P0, P1, P2, P3) -> Unit) {
        if (this == child) content(
            args[0] as P0,
            args[1] as P1,
            args[2] as P2,
            args[3] as P3
        )
    }

    fun global(p0: P0, p1: P1, p2: P2, p3: P3) = global(arrayOf(p0, p1, p2, p3))
    suspend fun ensureGlobal(p0: P0, p1: P1, p2: P2, p3: P3) = ensureGlobal(arrayOf(p0, p1, p2, p3))
}

