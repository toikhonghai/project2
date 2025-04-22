package app.vitune.android.ui.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vitune.core.ui.utils.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

@Stable
@JvmInline
// Hàm này dùng để tạo một SwipeState, đại diện cho trạng thái của một phần tử có thể vuốt (swipeable).
// SwipeState này sẽ được sử dụng để theo dõi vị trí vuốt và trạng thái của nó.
// @PublishedApi: Annotation này cho biết rằng hàm này có thể được sử dụng bên ngoài gói hiện tại, nhưng không nên được sử dụng trực tiếp.
// internal constructor: Constructor này chỉ có thể được gọi từ trong gói hiện tại.
value class SwipeState @PublishedApi internal constructor(
    // offsetLazy sẽ chỉ được khởi tạo khi cần (lazy init)
    private val offsetLazy: Lazy<Animatable<Float, AnimationVector1D>> = lazy { acquire() } // offsetLazy là một Animatable dùng để theo dõi vị trí vuốt
) {
    // offset thực tế – là một Animatable có thể animate giá trị Float
    internal val offset get() = offsetLazy.value

    // Object chứa danh sách pool Animatable để tái sử dụng
    private companion object {
        private val animatables = mutableListOf<Animatable<Float, AnimationVector1D>>() // pool tái sử dụng
        private val coroutineScope = CoroutineScope(Dispatchers.IO)

        // Lấy từ pool hoặc tạo mới
        fun acquire() = animatables.removeFirstOrNull() ?: Animatable(0f)

        // Trả lại pool sau khi dùng
        fun recycle(animatable: Animatable<Float, AnimationVector1D>) {
            coroutineScope.launch {
                animatable.snapTo(0f) // reset offset về 0
                animatables += animatable // đưa lại vào danh sách pool
            }
        }
    }

    // Trả về offset hiện tại (theo dp), giới hạn trong `bounds` nếu có
    @Composable
    fun calculateOffset(bounds: ClosedRange<Dp>? = null) =
        offset.value.px.dp.let { if (bounds == null) it else it.coerceIn(bounds) }

    // Hàm gọi để trả lại animatable vào pool
    @PublishedApi
    internal fun recycle() = recycle(offset)
}

@Suppress("NOTHING_TO_INLINE")
@Composable
// Hàm này dùng để nhớ SwipeState theo key, nếu không có key thì sẽ tạo mới SwipeState.
inline fun rememberSwipeState(key: Any?): SwipeState {
    val state = remember(key) { SwipeState() } // nhớ SwipeState theo key

    // Khi không còn dùng nữa (Composable bị dispose), recycle lại offset
    DisposableEffect(key) {
        onDispose {
            state.recycle()
        }
    }

    return state
}

// Hàm này dùng để vuốt một phần tử (như Dialog, BottomSheet, v.v.) với callback cho cả hai chiều trái và phải.
fun Modifier.onSwipe(
    state: SwipeState? = null,
    key: Any = Unit,
    animateOffset: Boolean = false,
    orientation: Orientation = Orientation.Horizontal,
    delay: Duration = Duration.ZERO,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) },
    animationSpec: AnimationSpec<Float> = spring(),
    bounds: ClosedRange<Dp>? = null,
    requireUnconsumed: Boolean = false,
    onSwipeOut: suspend (animationJob: Job) -> Unit // callback gọi khi vuốt trái hoặc phải đủ xa
) = onSwipe(
    state = state,
    key = key,
    animateOffset = animateOffset,
    onSwipeLeft = onSwipeOut, // gán cùng callback cho cả trái và phải
    onSwipeRight = onSwipeOut,
    orientation = orientation,
    delay = delay,
    decay = decay,
    animationSpec = animationSpec,
    requireUnconsumed = requireUnconsumed,
    bounds = bounds
)

@Suppress("CyclomaticComplexMethod") // Cảnh báo này là do hàm có quá nhiều nhánh điều kiện
// Hàm này dùng để vuốt một phần tử (như Dialog, BottomSheet, v.v.) với callback cho cả hai chiều trái và phải.
fun Modifier.onSwipe(
    state: SwipeState? = null, // Trạng thái vuốt, nếu null sẽ tạo mới
    key: Any = Unit, // Dùng để nhớ đúng SwipeState theo từng phần tử
    animateOffset: Boolean = false, // Có animate offset hay không
    onSwipeLeft: suspend (animationJob: Job) -> Unit = { }, // Callback khi vuốt sang trái
    onSwipeRight: suspend (animationJob: Job) -> Unit = { }, // Callback khi vuốt sang phải
    orientation: Orientation = Orientation.Horizontal, // Chiều vuốt: ngang/dọc
    delay: Duration = Duration.ZERO, // Độ trễ trước khi gọi callback
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) }, // animation decay
    animationSpec: AnimationSpec<Float> = spring(), // animation kiểu lò xo
    bounds: ClosedRange<Dp>? = null, // Giới hạn offset cho phép
    requireUnconsumed: Boolean = false // Chỉ nhận event nếu chưa bị consume
) = this.composed {
    val swipeState = state ?: rememberSwipeState(key) // Lấy hoặc tạo SwipeState

    // Lắng nghe gesture
    pointerInput(key) {
        coroutineScope {
            val velocityTracker = VelocityTracker() // Theo dõi tốc độ vuốt

            while (isActive) {
                velocityTracker.resetTracking()

                awaitPointerEventScope { // Chờ sự kiện vuốt
                    val pointer = awaitFirstDown(requireUnconsumed = requireUnconsumed).id // Lấy ID của pointer
                    launch { swipeState.offset.snapTo(0f) } // reset offset

                    val onDrag: (PointerInputChange) -> Unit = { // Xử lý sự kiện vuốt
                        val change = if (orientation == Orientation.Horizontal)
                            it.positionChange().x else it.positionChange().y

                        launch { swipeState.offset.snapTo(swipeState.offset.value + change) }
                        velocityTracker.addPosition(it.uptimeMillis, it.position)

                        if (change != 0f) it.consume() // consume gesture
                    }

                    // Chờ vuốt và xử lý
                    if (orientation == Orientation.Horizontal) {
                        awaitHorizontalTouchSlopOrCancellation(pointer) { change, _ -> onDrag(change) } // xuất hiện vuốt ngang
                            ?: return@awaitPointerEventScope
                        horizontalDrag(pointer, onDrag)
                    } else {
                        awaitVerticalTouchSlopOrCancellation(pointer) { change, _ -> onDrag(change) } // xuất hiện vuốt dọc
                            ?: return@awaitPointerEventScope
                        verticalDrag(pointer, onDrag)
                    }
                }

                // Sau khi vuốt, tính toán tốc độ và hướng
                val targetOffset = decay().calculateTargetValue( // tính toán tốc độ và hướng
                    initialValue = swipeState.offset.value, // giá trị ban đầu
                    initialVelocity = velocityTracker.calculateVelocity().let {
                        if (orientation == Orientation.Horizontal) it.x else it.y
                    }
                )
                val size = if (orientation == Orientation.Horizontal) size.width else size.height

                // Animate tới điểm kết thúc hoặc reset về 0
                launch animationEnd@{
                    when {
                        targetOffset >= size / 2 -> { // vuốt phải
                            val animationJob = launch {
                                swipeState.offset.animateTo(size.toFloat(), animationSpec) // animate tới điểm kết thúc
                            }
                            delay(delay)
                            onSwipeRight(animationJob) // gọi callback vuốt phải
                        }

                        targetOffset <= -size / 2 -> { // vuốt trái
                            val animationJob = launch {
                                swipeState.offset.animateTo(-size.toFloat(), animationSpec) // animate tới điểm kết thúc
                            }
                            delay(delay)
                            onSwipeLeft(animationJob)
                        }
                    }

                    // Trở về vị trí ban đầu sau khi xử lý
                    swipeState.offset.animateTo(0f, animationSpec)
                }
            }
        }
    }.let { modifier ->
        // Nếu animate offset thì apply offset vào Modifier
        when {
            animateOffset && orientation == Orientation.Horizontal ->
                modifier.offset(x = swipeState.calculateOffset(bounds = bounds))

            animateOffset && orientation == Orientation.Vertical ->
                modifier.offset(y = swipeState.calculateOffset(bounds = bounds))

            else -> modifier
        }
    }
}

// Hàm này dùng để vuốt để đóng một phần tử (như Dialog, BottomSheet, v.v.)
fun Modifier.swipeToClose(
    key: Any = Unit,
    state: SwipeState? = null,
    delay: Duration = Duration.ZERO, // Thời gian delay trước khi gọi callback
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) }, // Đây là hàm để tính toán độ giảm tốc độ của vuốt
    requireUnconsumed: Boolean = false, // Nếu true, chỉ nhận sự kiện vuốt nếu chưa bị consume
    onClose: suspend (animationJob: Job) -> Unit // Callback khi vuốt thành công
) = this.composed {
    val swipeState = state ?: rememberSwipeState(key)
    val density = LocalDensity.current

    var currentWidth by remember { mutableIntStateOf(0) } // Chiều rộng pixel thực tế
    val currentWidthDp by remember { derivedStateOf { currentWidth.px.dp(density) } }

    val bounds by remember { derivedStateOf { -currentWidthDp..0.dp } } // Giới hạn vuốt trái

    this
        .onSizeChanged { currentWidth = it.width } // Ghi lại chiều rộng hiện tại
        .alpha((currentWidthDp + swipeState.calculateOffset(bounds = bounds)) / currentWidthDp)
        .onSwipe(
            state = swipeState,
            key = key,
            animateOffset = true,
            onSwipeLeft = onClose, // Khi vuốt sang trái: gọi onClose()
            orientation = Orientation.Horizontal, // Vuốt ngang
            delay = delay,
            decay = decay, // Hàm tính toán độ giảm tốc độ
            requireUnconsumed = requireUnconsumed,
            bounds = bounds
        )
}

