package app.vitune.compose.routing

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

typealias TransitionScope<T> = AnimatedContentTransitionScope<T> // AnimatedContentTransitionScope là một lớp trong Jetpack Compose,
// dùng để quản lý các hiệu ứng chuyển động (animation) giữa các trạng thái khác nhau của một composable.
typealias TransitionSpec<T> = TransitionScope<T>.() -> ContentTransform // TransitionSpec là một kiểu dữ liệu đại diện cho các thông số chuyển động
// (animation spec) cho một hiệu ứng chuyển động trong Jetpack Compose.

private val defaultTransitionSpec: TransitionSpec<Route?> = { // đây là một hàm lambda, định nghĩa các thông số chuyển động mặc định cho hiệu ứng chuyển động.
    when {
        isStacking -> defaultStacking // đây là một hàm mở rộng (extension function) kiểm tra xem có đang trong quá trình "stacking" hay không.
        isStill -> defaultStill // trạng thái không thay đổi
        else -> defaultUnstacking // trạng thái "unstacking"
    }
}

@Composable
// đây là một hàm composable, cho phép bạn định nghĩa một RouteHandler với các thông số tùy chọn
// như modifier, listenToGlobalEmitter, transitionSpec và nội dung bên trong.
fun RouteHandler(
    modifier: Modifier = Modifier,
    listenToGlobalEmitter: Boolean = true,
    transitionSpec: TransitionSpec<Route?> = defaultTransitionSpec,
    content: @Composable RouteHandlerScope.() -> Unit // nội dung bên trong của RouteHandler, được định nghĩa bởi RouteHandlerScope.
) {
    var child: Route? by rememberSaveable { mutableStateOf(null) } // child là một biến trạng thái (state) dùng để lưu trữ route hiện tại,

    RouteHandler(
        child = child,
        setChild = { child = it },
        listenToGlobalEmitter = listenToGlobalEmitter, // nếu là true, sẽ lắng nghe các sự kiện route toàn cục
        transitionSpec = transitionSpec, // các thông số chuyển động cho hiệu ứng chuyển động
        modifier = modifier,
        content = content
    )
}

interface Router { // Router là một giao diện (interface) đại diện cho các chức năng điều hướng trong ứng dụng.
    operator fun Route0.invoke() // gọi hàm invoke trên Route0, cho phép bạn sử dụng Route0 như một hàm
    operator fun <P0> Route1<P0>.invoke(p0: P0) // gọi hàm invoke trên Route1, cho phép bạn sử dụng Route1 như một hàm với tham số P0
    operator fun <P0, P1> Route2<P0, P1>.invoke(p0: P0, p1: P1) // gọi hàm invoke trên Route2, cho phép bạn sử dụng Route2 như một hàm với tham số P0 và P1
    operator fun <P0, P1, P2> Route3<P0, P1, P2>.invoke(p0: P0, p1: P1, p2: P2) // gọi hàm invoke trên Route3, cho phép bạn sử dụng Route3 như một hàm với tham số P0, P1 và P2
    operator fun <P0, P1, P2, P3> Route4<P0, P1, P2, P3>.invoke(p0: P0, p1: P1, p2: P2, p3: P3) // gọi hàm invoke trên Route4, cho phép bạn sử dụng Route4 như một hàm với tham số P0, P1, P2 và P3

    val pop: () -> Unit
    val push: (Route?) -> Unit
}

@Stable
class RootRouter : Router { // RootRouter là một lớp trong Jetpack Compose, dùng để quản lý các route (đường dẫn) trong ứng dụng.
    private inline fun route(block: RouteHandlerScope.() -> Unit?) = current?.block() ?: Unit // gọi hàm block trên RouteHandlerScope hiện tại, nếu không có thì trả về Unit

    var current: RouteHandlerScope? by mutableStateOf(null) // current là một biến trạng thái (state) dùng để lưu trữ route hiện tại, được khởi tạo bằng null

    override val pop = {
        route {
            pop()
        }
    }

    override val push = { route: Route? ->
        route {
            replace(route)
        }
    }

    override operator fun Route0.invoke() = push(this) // gọi hàm invoke trên Route0, cho phép bạn sử dụng Route0 như một hàm

    override operator fun <P0> Route1<P0>.invoke(p0: P0) = route { // gọi hàm invoke trên Route1, cho phép bạn sử dụng Route1 như một hàm với tham số P0
        args[0] = p0 // lưu tham số P0 vào mảng args
        push(this@invoke) // gọi hàm push với route hiện tại
    }

    // Gọi Route2 như một hàm: ví dụ route2(p0, p1)
    override operator fun <P0, P1> Route2<P0, P1>.invoke(p0: P0, p1: P1) = route {
        args[0] = p0
        args[1] = p1
        push(this@invoke)
    }

    // Gọi Route3 như một hàm: ví dụ route3(p0, p1, p2)
    override operator fun <P0, P1, P2> Route3<P0, P1, P2>.invoke(p0: P0, p1: P1, p2: P2) = route {
        args[0] = p0
        args[1] = p1
        args[2] = p2
        push(this@invoke)
    }

    // Gọi Route4 như một hàm: ví dụ route4(p0, p1, p2, p3)
    override operator fun <P0, P1, P2, P3> Route4<P0, P1, P2, P3>.invoke(
        p0: P0,
        p1: P1,
        p2: P2,
        p3: P3
    ) = route {
        args[0] = p0
        args[1] = p1
        args[2] = p2
        args[3] = p3
        push(this@invoke)
    }
}

@JvmInline
value class RootRouterOwner internal constructor(val router: RootRouter) // RootRouterOwner là một lớp đại diện cho một đối tượng Router gốc trong ứng dụng.
// Nó sử dụng value class để đóng gói một RootRouter lại thành một RootRouterOwner.

val LocalRouteHandler = compositionLocalOf<RootRouter?> { null } // LocalRouteHandler là một CompositionLocal, cho phép bạn chia sẻ một đối tượng RootRouter trong cây composable.

@Composable
// hàm này cho phép bạn định nghĩa một RouteHandler với các thông số tùy chọn như child, setChild, modifier, listenToGlobalEmitter, transitionSpec và nội dung bên trong.
fun ProvideRootRouter(content: @Composable RootRouterOwner.() -> Unit) =
    LocalRouteHandler.current.let { current -> // lấy giá trị của LocalRouteHandler hiện tại
        if (current == null) {
            val newHandler = RootRouter()
            CompositionLocalProvider(LocalRouteHandler provides newHandler) {
                content(RootRouterOwner(newHandler)) // nếu current là null, tạo một RootRouter mới và cung cấp nó cho các composable bên trong
            }
        } else content(RootRouterOwner(current))
    }

@Composable
// hàm này cho phép bạn định nghĩa một RouteHandler với các thông số tùy chọn như child, setChild, modifier, listenToGlobalEmitter, transitionSpec và nội dung bên trong.
// Nó sử dụng ProvideRootRouter để cung cấp một RootRouter cho các composable bên trong.
// Nó cũng xử lý các sự kiện vuốt back (Predictive Back Gesture) và chuyển đổi giữa các trạng thái route khác nhau.
private fun RouteHandler(
    child: Route?,
    setChild: (Route?) -> Unit,
    modifier: Modifier = Modifier,
    listenToGlobalEmitter: Boolean = true,
    transitionSpec: TransitionSpec<Route?> = defaultTransitionSpec,
    content: @Composable RouteHandlerScope.() -> Unit
) = ProvideRootRouter { // cung cấp một RootRouter cho các composable bên trong
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher // lấy dispatcher để xử lý sự kiện back
    val parameters = rememberSaveable { arrayOfNulls<Any?>(4) } // tạo một mảng để lưu trữ các tham số truyền vào route

    if (listenToGlobalEmitter && child == null) OnGlobalRoute { (route, args) -> // nếu không có child và listenToGlobalEmitter là true, lắng nghe các sự kiện route toàn cục
        args.forEachIndexed(parameters::set) // lưu các tham số vào mảng parameters
        setChild(route)// gọi hàm setChild để thay đổi route hiện tại
    }

    var predictiveBackProgress: Float? by remember { mutableStateOf(null) } // biến này dùng để lưu trữ tiến trình của thao tác vuốt back
    CallbackPredictiveBackHandler( // xử lý thao tác vuốt back
        enabled = child != null,
        onStart = { predictiveBackProgress = 0f }, // bắt đầu vuốt
        onProgress = { predictiveBackProgress = it }, // cập nhật tiến trình vuốt
        onFinish = {
            predictiveBackProgress = null
            setChild(null)
        },
        onCancel = {
            predictiveBackProgress = null
        }
    )

    fun Route?.scope() = RouteHandlerScope(
        child = this,
        args = parameters,
        replace = setChild,
        pop = { backDispatcher?.onBackPressed() }, // quay lại route trước đó
        root = router
    )

    val transitionState = remember { SeekableTransitionState(child) } // tạo một trạng thái chuyển tiếp (transition state) để quản lý các hiệu ứng chuyển động giữa các trạng thái khác nhau

    if (predictiveBackProgress == null) LaunchedEffect(child) { // nếu không có thao tác vuốt back, gọi hàm LaunchedEffect để cập nhật trạng thái chuyển tiếp
        // LaunchedEffect là một hàm trong Jetpack Compose, cho phép bạn chạy một coroutine khi một giá trị thay đổi.
        if (transitionState.currentState != child) transitionState.animateTo(child)
    } else LaunchedEffect(predictiveBackProgress) { // nếu có thao tác vuốt back, gọi hàm LaunchedEffect để cập nhật trạng thái chuyển tiếp
        transitionState.seekTo( // cập nhật trạng thái chuyển tiếp
            fraction = predictiveBackProgress ?: 0f,
            targetState = null
        )
    }

    rememberTransition( // tạo một trạng thái chuyển tiếp để quản lý các hiệu ứng chuyển động giữa các trạng thái khác nhau
        transitionState = transitionState,
        label = null
    ).AnimatedContent( // AnimatedContent là một composable trong Jetpack Compose, cho phép bạn tạo các hiệu ứng chuyển động giữa các trạng thái khác nhau.
        transitionSpec = transitionSpec,
        modifier = modifier
    ) {
        val scope = remember(it) { it.scope() }

        LaunchedEffect(predictiveBackProgress, scope) {
            if (predictiveBackProgress == null && scope.child == null) router.current = scope
        }

        scope.content()
    }
}
