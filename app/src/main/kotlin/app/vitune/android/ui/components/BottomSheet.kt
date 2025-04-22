package app.vitune.android.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import app.vitune.compose.routing.CallbackPredictiveBackHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BottomSheet(
    state: BottomSheetState,
    collapsedContent: @Composable BoxScope.(Modifier) -> Unit, // Nội dung sẽ hiển thị khi BottomSheet ở trạng thái collapsed (thu gọn).
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    indication: Indication? = LocalIndication.current, // Là một kiểu UI indication (hiệu ứng phản hồi khi người dùng tương tác), được lấy từ LocalIndication.current mặc định.
    backHandlerEnabled: Boolean = true, // Chỉ định xem có cho phép xử lý sự kiện quay lại (back press) của hệ điều hành hay không.
    content: @Composable BoxScope.() -> Unit // Nội dung sẽ hiển thị khi BottomSheet ở trạng thái expanded (mở rộng).
) = Box(
    modifier = modifier
        .offset(y = (state.expandedBound - state.value).coerceAtLeast(0.dp)) // Đặt vị trí của BottomSheet theo chiều dọc, với chiều cao được tính từ vị trí expandedBound tới value hiện tại của state.
        // Nếu vị trí âm, nó sẽ được giới hạn về 0 (không bị văng ra ngoài màn hình).
        .pointerInput(state) { // Được sử dụng để theo dõi các cử chỉ kéo (drag) của người dùng.
            val velocityTracker = VelocityTracker() // Tạo một đối tượng VelocityTracker để theo dõi tốc độ kéo của người dùng.

            detectVerticalDragGestures( //  để theo dõi sự thay đổi khi người dùng kéo BottomSheet.
                onVerticalDrag = { change, dragAmount -> // Gọi mỗi khi người dùng đang kéo.
                    // change: thông tin về sự thay đổi (như vị trí, kích thước, v.v.).
                    // dragAmount: khoảng người dùng đã kéo (dương nếu kéo xuống, âm nếu kéo lên).
                    velocityTracker.addPointerInputChange(change) // Cập nhật tốc độ kéo vào VelocityTracker.
                    state.dispatchRawDelta(dragAmount) // Cập nhật vị trí của BottomSheet theo khoảng kéo.
                },
                onDragCancel = { // Gọi khi thao tác kéo bị hủy giữa chừng (ví dụ: mất focus, rút tay ra quá nhanh…).
                    velocityTracker.resetTracking() // Đặt lại VelocityTracker về trạng thái ban đầu.
                    state.snapTo(state.collapsedBound) // Đặt vị trí BottomSheet về trạng thái collapsed.
                },
                onDragEnd = { // Gọi khi người dùng thả tay ra sau khi kéo.
                    val velocity = -velocityTracker.calculateVelocity().y // Tính toán tốc độ kéo theo chiều dọc (y-axis) và đổi dấu.
                    velocityTracker.resetTracking() // Đặt lại VelocityTracker về trạng thái ban đầu.
                    state.fling(velocity, onDismiss) // Gọi hàm fling để quyết định BottomSheet sẽ đi về đâu dựa trên tốc độ kéo và vị trí hiện tại.
                }
            )
        }
) {
    if (state.value > state.collapsedBound) CallbackPredictiveBackHandler( // Đây là composable tùy biến xử lý predictive back gesture (cử chỉ vuốt ngược để thoát – Android 14+).
        enabled = !state.collapsing && backHandlerEnabled, // Chỉ định xem có cho phép xử lý cử chỉ quay lại hay không.
        onStart = { }, // Lúc bắt đầu gesture → thường dùng để set flag
        onProgress = { state.collapse(progress = it) }, // Gọi hàm collapse để thu gọn BottomSheet theo tỷ lệ phần trăm.
        onFinish = { state.collapseSoft() }, // Gọi hàm collapseSoft để thu gọn BottomSheet với hiệu ứng mượt mà.
        onCancel = { state.expandSoft() } // Gọi hàm expandSoft để mở rộng BottomSheet với hiệu ứng mượt mà khi gesture bị hủy.
    )
    if (!state.dismissed && !state.collapsed) content() // Nếu BottomSheet không bị ẩn và không ở trạng thái thu gọn, thì hiển thị nội dung bên trong.

    if (!state.expanded && (onDismiss == null || !state.dismissed)) Box( // Nếu BottomSheet không ở trạng thái mở rộng và không bị ẩn hoàn toàn, thì hiển thị một lớp phủ (overlay) để che đi nội dung bên dưới.
        modifier = Modifier
            .graphicsLayer { // Dùng để thay đổi alpha (độ mờ) của collapsed content theo state.progress
                alpha = 1f - (state.progress * 16).coerceAtMost(1f) // coerceAtMost() trong Kotlin là một hàm dùng để giới hạn giá trị tối đa.
                // Nếu giá trị lớn hơn 1f thì sẽ trả về 1f, nếu nhỏ hơn thì trả về giá trị đó.
            }
            .fillMaxWidth()
            .height(state.collapsedBound)
    ) {
        collapsedContent( // Gọi hàm collapsedContent để hiển thị nội dung khi BottomSheet ở trạng thái thu gọn.
            Modifier.clickable(
                onClick = state::expandSoft, // Khi người dùng nhấn vào lớp phủ, BottomSheet sẽ mở rộng với hiệu ứng mượt mà.
                indication = indication, // Sử dụng indication để hiển thị hiệu ứng phản hồi khi người dùng nhấn vào lớp phủ.
                interactionSource = remember { MutableInteractionSource() } // Tạo một đối tượng MutableInteractionSource để theo dõi các tương tác của người dùng.
            )
        )
    }
}
/*
BottomSheetState là một lớp dùng để quản lý vị trí và hoạt ảnh (animation) của một bottom sheet trong giao diện người dùng.
Lớp này được đánh dấu bằng @Stable, có nghĩa là các thể hiện của lớp này có thể được quan sát thay đổi một cách an toàn trong Jetpack Compose.
Lớp này triển khai giao diện DraggableState, cho phép nó phản hồi với các cử chỉ kéo (drag).
 */
@Suppress("TooManyFunctions") // TooManyFunctions là một cảnh báo trong Android Studio, cho biết rằng lớp này có quá nhiều hàm (functions) và có thể cần được chia nhỏ thành các lớp khác nhau để dễ quản lý hơn.
@Stable
class BottomSheetState
@Suppress("LongParameterList")
internal constructor( // internal constructor có nghĩa là lớp này chỉ có thể được khởi tạo từ bên trong module này.
    density: Density, // Cung cấp thông tin về độ phân giải của màn hình, được sử dụng để chuyển đổi đơn vị pixel thành đơn vị độc lập với mật độ (Dp).
    initialValue: Dp, // Giá trị ban đầu của vị trí bottom sheet, được cung cấp dưới dạng Dp (Density-independent Pixel).
    private val coroutineScope: CoroutineScope,
    private val onAnchorChanged: (Anchor) -> Unit, // Hàm này sẽ được gọi khi vị trí bottom sheet thay đổi, với tham số là Anchor (một kiểu dữ liệu đại diện cho các trạng thái khác nhau của bottom sheet).
    val dismissedBound: Dp, // Giới hạn dưới của bottom sheet, tức là vị trí mà bottom sheet sẽ bị ẩn đi hoàn toàn.
    val collapsedBound: Dp, // Giới hạn giữa của bottom sheet, tức là vị trí mà bottom sheet sẽ được thu gọn lại.
    val expandedBound: Dp // Giới hạn trên của bottom sheet, tức là vị trí mà bottom sheet sẽ được mở rộng hoàn toàn.
) : DraggableState { // DraggableState là một giao diện trong Jetpack Compose, cho phép các thành phần giao diện người dùng phản hồi với các cử chỉ kéo (drag).
    private val animatable = Animatable( // Animatable là một lớp trong Jetpack Compose, cho phép tạo ra các hoạt ảnh (animations) mượt mà.
        initialValue = initialValue,
        typeConverter = Dp.VectorConverter // VectorConverter là một lớp chuyển đổi giữa các kiểu dữ liệu khác nhau, trong trường hợp này là giữa Dp và Vector.
    )
    val value by animatable.asState() // asState() là một hàm mở rộng trong Jetpack Compose, cho phép chuyển đổi một Animatable thành một State, để có thể quan sát các thay đổi của nó.

    private val draggableState = DraggableState { delta ->
        coroutineScope.launch {
            animatable.snapTo(animatable.value - with(density) { delta.toDp() }) // Cập nhật ngay lập tức vị trí mới (không animation), để phản hồi theo tay người dùng kéo.
            //vị trí mới = vị trí cũ - khoảng kéo
        }
    }

    // Hàm này sẽ được gọi khi người dùng kéo bottom sheet, và nó sẽ cập nhật vị trí của bottom sheet theo khoảng cách kéo.
    // dragPriority: MutatePriority Xác định độ ưu tiên của thao tác kéo này.
    //Dùng để kiểm soát nếu có nhiều thao tác animation khác đang chiếm quyền
//    block: suspend DragScope.() -> Unit
//    Đây là hàm chứa logic kéo thực sự — ví dụ như cập nhật vị trí, hoặc dispatch delta.
    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) =
        draggableState.drag(dragPriority, block)

    override fun dispatchRawDelta(delta: Float) = draggableState.dispatchRawDelta(delta) // Cập nhật vị trí của bottom sheet theo khoảng cách kéo.

    /*
    derivedStateOf { ... } là một API trong Compose dùng để tạo state phụ thuộc (derived state) từ các state khác.
    Khi các giá trị phụ thuộc thay đổi (value ở đây là Dp của sheet), thì giá trị bên trong derivedStateOf cũng được recompute.
     */
    val dismissed by derivedStateOf { value == dismissedBound } // Trả về true nếu vị trí hiện tại bằng vị trí "dismissed" (ẩn hẳn sheet).
    val collapsed by derivedStateOf { value == collapsedBound } // Trả về true nếu vị trí hiện tại bằng vị trí "collapsed" (thu gọn sheet).
    val expanded by derivedStateOf { value == expandedBound } // Trả về true nếu vị trí hiện tại bằng vị trí "expanded" (mở rộng sheet).
    val collapsing by derivedStateOf {
        animatable.targetValue == collapsedBound || animatable.targetValue == dismissedBound
    } // Trả về true nếu vị trí hiện tại đang trong quá trình thu gọn hoặc ẩn hẳn sheet.
    val progress by derivedStateOf { 1f - (expandedBound - value) / (expandedBound - collapsedBound) } // Tính toán tỷ lệ phần trăm vị trí hiện tại so với khoảng cách giữa vị trí "expanded" và "collapsed".

    // Hàm này sẽ được gọi khi vị trí bottom sheet thay đổi, với tham số là Anchor (một kiểu dữ liệu đại diện cho các trạng thái khác nhau của bottom sheet).
    private fun deferAnimateTo( // Tạo một animation chuyển tiếp mượt mà từ vị trí hiện tại (animatable.value) đến vị trí mới
        newValue: Dp,
        spec: AnimationSpec<Dp> = spring() // Mặc định sử dụng animation kiểu spring() (bật bật như lò xo).
    ) = coroutineScope.launch {
        animatable.animateTo(newValue, spec) // Chạy animation từ vị trí hiện tại đến vị trí mới
    }

    private fun collapse(spec: AnimationSpec<Dp> = spring()) { // gọi khi bạn muốn "thu gọn" sheet (collapse).
        onAnchorChanged(Anchor.Collapsed) // Gửi callback onAnchorChanged để thông báo cho bên ngoài biết sheet đang chuyển trạng thái.
        deferAnimateTo(collapsedBound, spec) // Chạy animation từ vị trí hiện tại đến vị trí "collapsed".
    }

    private fun expand(spec: AnimationSpec<Dp> = spring()) {
        onAnchorChanged(Anchor.Expanded)
        deferAnimateTo(expandedBound, spec)
    }

    private fun dismiss(spec: AnimationSpec<Dp> = spring()) {
        onAnchorChanged(Anchor.Dismissed)
        deferAnimateTo(dismissedBound, spec)
    }

    fun collapse(progress: Float) { // Dùng khi bạn muốn điều khiển animation theo tay kéo (drag) thủ công
        snapTo(expandedBound - progress * (expandedBound - collapsedBound)) // Tính toán vị trí mới dựa trên tỷ lệ phần trăm.
    }
    //ví dụ:
    //progress = 0f → không gập gì cả → vẫn ở expandedBound
    //progress = 1f → gập hết → đến collapsedBound

    fun collapseSoft() = collapse(tween(300)) // hiệu ứng tween(300) = animation mượt kéo dài 300ms.
    fun expandSoft() = expand(tween(300))
    fun dismissSoft() = dismiss(tween(300))

    fun snapTo(value: Dp) = coroutineScope.launch { // Gọi hàm này khi bạn muốn đặt vị trí bottom sheet đến một giá trị cụ thể mà không có animation.
        animatable.snapTo(value)
    }

    // Đây là logic quyết định sheet sẽ đi về đâu sau khi bạn "thả tay" (fling), dựa vào tốc độ kéo (velocity) và vị trí hiện tại (value).
    fun fling(velocity: Float, onDismiss: (() -> Unit)?) = when {
        velocity > 250 -> expand() // Nếu tốc độ kéo lớn hơn 250, sheet sẽ mở rộng hoàn toàn (expanded).
        velocity < -250 -> {
            if (value < collapsedBound && onDismiss != null) { // Nếu vị trí hiện tại nhỏ hơn collapsedBound và có callback onDismiss, thì gọi dismiss() và onDismiss().
                dismiss()
                onDismiss()
            } else collapse()
        }

        else -> { // velocity nhỏ (thả nhẹ nhàng) Tính khoảng cách để chọn điểm gần nhất
            val l1 = (collapsedBound - dismissedBound) / 2 // Tính khoảng cách giữa dismissed và collapsed
            val l2 = (expandedBound - collapsedBound) / 2

            when (value) {
                in dismissedBound..l1 -> {
                    if (onDismiss != null) {
                        dismiss()
                        onDismiss()
                    } else collapse()
                }

                in l1..l2 -> collapse()
                in l2..expandedBound -> expand()

                else -> Unit
            }
        }
    }

    /*
    xử lý nested scroll (cuộn lồng nhau) trong BottomSheet,
    giúp sheet phản ứng mượt mà khi cuộn một nội dung bên trong, ví dụ: cuộn LazyColumn mà đồng thời sheet cũng bị kéo.
     */
    val preUpPostDownNestedScrollConnection
        get() = object : NestedScrollConnection { // NestedScrollConnection  giao diện để chặn và can thiệp vào các thao tác scroll/phím fling trước và sau khi nó truyền vào nội dung bên trong.
            var isTopReached = false // kiểm tra xem người dùng đã cuộn tới đầu nội dung bên trong chưa (ví dụ: cuộn LazyColumn tới vị trí đầu tiên).

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset { // Hàm này sẽ được gọi trước khi cuộn nội dung bên trong.
                if (expanded && available.y < 0) isTopReached = false // Nếu sheet đang mở rộng và user cuộn lên trên (available.y < 0) → reset cờ isTopReached.

                return if (isTopReached && available.y < 0 && source == NestedScrollSource.UserInput) { // Nếu đang ở đầu và người dùng kéo lên → sheet sẽ được kéo thay vì nội dung bên trong.
                    dispatchRawDelta(available.y) // Gọi hàm dispatchRawDelta để cập nhật vị trí sheet theo khoảng cách kéo.
                    available
                } else Offset.Zero
            }

            override fun onPostScroll( // Được gọi sau khi nội dung con xử lý cuộn.
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!isTopReached) isTopReached = consumed.y == 0f && available.y > 0 // Nếu nội dung bên trong không cuộn được nữa
                // (consumed.y == 0f) mà người dùng còn kéo xuống (available.y > 0) → tức là đã tới đỉnh rồi ⇒ set isTopReached = true.

                return if (isTopReached && source == NestedScrollSource.UserInput) {
                    dispatchRawDelta(available.y)
                    available
                } else Offset.Zero
            }

            /*
            Khi người dùng fling (vuốt nhanh), nếu đang ở đỉnh (isTopReached) thì fling sẽ được chuyển qua sheet, thay vì fling nội dung bên trong.
             */
            override suspend fun onPreFling(available: Velocity) = if (isTopReached) {
                val velocity = -available.y
                fling(velocity, null)

                available
            } else Velocity.Zero

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity { // Reset cờ isTopReached = false sau khi fling xong.
                isTopReached = false
                return Velocity.Zero
            }
        }

    @JvmInline
    value class Anchor private constructor(internal val value: Int) { // Đây là kiểu enum "nhẹ" để biểu diễn 3 trạng thái: Dismissed, Collapsed, Expanded.
        companion object {
            val Dismissed = Anchor(value = 0)
            val Collapsed = Anchor(value = 1)
            val Expanded = Anchor(value = 2)
        }

        object Saver : androidx.compose.runtime.saveable.Saver<Anchor, Int> { // Saver giúp Compose lưu/khôi phục trạng thái Anchor khi recomposition hoặc thay đổi cấu hình (như xoay màn hình).
            override fun restore(value: Int) = when (value) {
                0 -> Dismissed
                1 -> Collapsed
                2 -> Expanded
                else -> error("Anchor $value does not exist!")
            }

            override fun SaverScope.save(value: Anchor) = value.value
        }
    }
}

@Composable
fun rememberBottomSheetState( // Hàm này dùng để tạo và nhớ một thể hiện của BottomSheetState trong Compose.
    dismissedBound: Dp,
    expandedBound: Dp,
    key: Any? = Unit,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: BottomSheetState.Anchor = BottomSheetState.Anchor.Dismissed
): BottomSheetState {
    val density = LocalDensity.current // đây là đối tượng Density hiện tại, dùng để chuyển đổi giữa các đơn vị đo lường khác nhau (như Dp và pixel).
    val coroutineScope = rememberCoroutineScope()

    var previousAnchor by rememberSaveable(stateSaver = BottomSheetState.Anchor.Saver) { // ưu trữ giá trị previousAnchor (trạng thái trước đó của BottomSheet).
        mutableStateOf(initialAnchor) // Đây là giá trị sẽ được dùng để xác định trạng thái ban đầu của BottomSheet.
    }

    return remember(key, dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        BottomSheetState(
            density = density,
            onAnchorChanged = { previousAnchor = it }, //  Là callback giúp cập nhật giá trị previousAnchor khi trạng thái của BottomSheet thay đổi.
            coroutineScope = coroutineScope,
            dismissedBound = dismissedBound.coerceAtMost(expandedBound), // đảm bảo dismissedBound không lớn hơn expandedBound
            collapsedBound = collapsedBound,
            expandedBound = expandedBound,
            initialValue = when (previousAnchor) {
                BottomSheetState.Anchor.Dismissed -> dismissedBound
                BottomSheetState.Anchor.Collapsed -> collapsedBound
                BottomSheetState.Anchor.Expanded -> expandedBound
                else -> error("Unknown BottomSheet anchor")
            }
        )
    }
}
