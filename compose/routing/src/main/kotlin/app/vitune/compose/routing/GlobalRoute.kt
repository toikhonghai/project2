package app.vitune.compose.routing

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow

// Một typealias cho cặp Route + mảng tham số truyền vào (arguments)
typealias RouteRequestDefinition = Pair<Route, Array<Any?>>

// Sử dụng value class để đóng gói một Route + args lại thành một RouteRequest
@JvmInline
value class RouteRequest private constructor(private val def: RouteRequestDefinition) {

    // Constructor công khai dùng để tạo RouteRequest từ route + args
    constructor(route: Route, args: Array<Any?>) : this(route to args)

    // Truy cập route từ cặp Pair
    val route get() = def.first

    // Truy cập args từ cặp Pair
    val args get() = def.second

    // Cho phép sử dụng destructuring declaration: val (r, a) = RouteRequest(...)
    operator fun component1() = route
    operator fun component2() = args
}

// Một SharedFlow dùng để truyền các yêu cầu chuyển Route một cách toàn cục (global)
// extraBufferCapacity = 1 → để tránh mất sự kiện nếu không có subscriber ngay lập tức
internal val globalRouteFlow = MutableSharedFlow<RouteRequest>(extraBufferCapacity = 1)


// Composable xử lý thao tác vuốt back (Predictive Back Gesture) theo nhiều giai đoạn
@Composable
fun CallbackPredictiveBackHandler(
    enabled: Boolean,                     // Kích hoạt hay không
    onStart: () -> Unit,                  // Gọi khi bắt đầu vuốt
    onProgress: (Float) -> Unit,          // Gọi liên tục với progress (0f → 1f)
    onFinish: () -> Unit,                 // Gọi khi vuốt hoàn tất
    onCancel: () -> Unit                  // Gọi khi hủy thao tác vuốt
) = PredictiveBackHandler(enabled = enabled) { progress ->

    onStart() // Bắt đầu vuốt

    // Lưu ý: Bình thường CancellationException nên được ném ra lại,
    // nhưng ở đây nó được nuốt để xác định trạng thái cancel
    @Suppress("SwallowedException")
    try {
        progress.collect {
            onProgress(it.progress) // Gửi progress liên tục
        }
        onFinish() // Vuốt hoàn tất
    } catch (e: CancellationException) {
        onCancel() // Vuốt bị hủy
    }
}


// Hàm xử lý khi có route global mới được gửi lên (emit vào globalRouteFlow)
@Composable
fun OnGlobalRoute(block: suspend (RouteRequest) -> Unit) {
    // rememberUpdatedState giúp đảm bảo khối block mới nhất luôn được gọi (tránh stale reference)
    val currentBlock by rememberUpdatedState(block)

    // Khởi chạy một coroutine để lắng nghe globalRouteFlow
    LaunchedEffect(Unit) {
        globalRouteFlow.collect {
            currentBlock(it) // Gọi block xử lý với RouteRequest nhận được
        }
    }
}

