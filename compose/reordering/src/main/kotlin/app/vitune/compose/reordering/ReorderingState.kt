package app.vitune.compose.reordering

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/*
Lớp này giữ trạng thái của quá trình kéo và thả các item trong một danh sách,
cung cấp thông tin về các item trong danh sách,
đồng thời quản lý các animation liên quan đến việc thay đổi vị trí của các item.
 */
@Stable
class ReorderingState(
    val lazyListState: LazyListState, // LazyListState chứa thông tin về trạng thái của danh sách
    val coroutineScope: CoroutineScope, // CoroutineScope để thực hiện các tác vụ bất đồng bộ
    private val lastIndex: Int, // Chỉ số cuối cùng của danh sách
    internal val onDragStart: () -> Unit, // Hàm callback được gọi khi bắt đầu kéo một item.
    internal val onDragEnd: (Int, Int) -> Unit, // Hàm được gọi khi kết thúc kéo
    private val extraItemCount: Int // Số lượng item bổ sung (có thể là 0)
) {
    internal val offset = Animatable(0, Int.VectorConverter) // Biến Animatable dùng để quản lý vị trí của item trong quá trình kéo và thả.
    // Việc thay đổi giá trị của offset sẽ làm thay đổi vị trí của item trên màn hình.
    // VectorConverter là một lớp giúp chuyển đổi giữa các giá trị số và vector animation
    // vector animation là một loại animation trong Android, cho phép bạn tạo ra các hiệu ứng chuyển động mượt mà và tự nhiên hơn bằng cách sử dụng vector graphics.

    internal var draggingIndex by mutableIntStateOf(-1) // Chỉ số của item đang được kéo
    internal var reachedIndex by mutableIntStateOf(-1) // Chỉ số của item mà item đang được kéo đã đến gần nhất
    internal var draggingItemSize by mutableIntStateOf(0) // Kích thước của item đang được kéo

    private lateinit var itemInfo: LazyListItemInfo // Thông tin về item đang được kéo

    private var previousItemSize = 0 // Kích thước của item trước đó
    private var nextItemSize = 0 // Kích thước của item tiếp theo

    private var overscrolled = 0 // Biến này dùng để theo dõi việc cuộn quá mức (overscroll) của danh sách

    internal var indexesToAnimate = mutableStateMapOf<Int, Animatable<Int, AnimationVector1D>>() // Danh sách các chỉ số của các item cần được animate trong quá trình kéo và thả
    private var animatablesPool: AnimatablesPool<Int, AnimationVector1D>? = null // Pool chứa các Animatable để tái sử dụng, giúp tiết kiệm bộ nhớ và tăng hiệu suất

    val isDragging: Boolean // Trạng thái kéo hiện tại
        get() = draggingIndex != -1 // Nếu draggingIndex khác -1, có nghĩa là đang có một item được kéo

    /*
    Phương thức này được gọi khi người dùng bắt đầu kéo một item. Nó sẽ:
    Cập nhật các giá trị như draggingIndex, reachedIndex, và kích thước item.
    Cập nhật phạm vi của offset để giới hạn sự di chuyển của item.
    Khởi tạo một pool các Animatable để tái sử dụng trong quá trình kéo.
     */
    fun onDragStart(index: Int) {
        overscrolled = 0
        itemInfo = lazyListState.layoutInfo.visibleItemsInfo
            .find { it.index == index + extraItemCount } ?: return // Tìm kiếm thông tin của item đang được kéo trong danh sách

        onDragStart() // Gọi hàm callback khi bắt đầu kéo
        draggingIndex = index // Cập nhật chỉ số của item đang được kéo
        reachedIndex = index // Cập nhật chỉ số của item mà item đang được kéo đã đến gần nhất
        draggingItemSize = itemInfo.size // Cập nhật kích thước của item đang được kéo

        nextItemSize = draggingItemSize // Cập nhật kích thước của item tiếp theo
        previousItemSize = -draggingItemSize // Cập nhật kích thước của item trước đó

        offset.updateBounds( // Cập nhật phạm vi của offset
            lowerBound = -index * draggingItemSize, // Giới hạn dưới của phạm vi
            upperBound = (lastIndex - index) * draggingItemSize // Giới hạn trên của phạm vi
        )

        animatablesPool = AnimatablesPool( // Tạo một pool các Animatable để tái sử dụng
            initialValue = 0, // Giá trị khởi tạo của Animatable
            typeConverter = Int.VectorConverter // Chuyển đổi giữa các giá trị số và vector animation
        )
    }

    /*
    Phương thức này được gọi khi người dùng kéo item. Nó sẽ:
    Tính toán và cập nhật delta (sự thay đổi trong vị trí) từ thao tác kéo.
    Thực hiện animation để di chuyển item (thông qua việc thay đổi giá trị offset).
    Nếu người dùng kéo qua giới hạn của các item khác (vượt qua vị trí kế tiếp hoặc trước đó), nó sẽ thay đổi vị trí của các item này và áp dụng các animation.
     */
    @Suppress("CyclomaticComplexMethod") // Cảnh báo này cho biết rằng phương thức có độ phức tạp cao và có thể được tối ưu hóa.
    fun onDrag(change: PointerInputChange, dragAmount: Offset) { // PointerInputChange là một lớp đại diện cho một sự kiện đầu vào từ người dùng (như chạm hoặc kéo)
        if (!isDragging) return

        change.consume() // Tiêu thụ sự kiện đầu vào để không cho các thành phần khác xử lý nó

        val delta = when (lazyListState.layoutInfo.orientation) {
            Orientation.Vertical -> dragAmount.y
            Orientation.Horizontal -> dragAmount.x
        }.roundToInt() // Làm tròn giá trị delta về số nguyên

        val targetOffset = offset.value + delta

        coroutineScope.launch { offset.snapTo(targetOffset) } // Cập nhật giá trị offset với giá trị mới

        when {
            targetOffset > nextItemSize -> {
                if (reachedIndex < lastIndex) {
                    reachedIndex += 1
                    nextItemSize += draggingItemSize
                    previousItemSize += draggingItemSize

                    val indexToAnimate = reachedIndex - if (draggingIndex < reachedIndex) 0 else 1

                    coroutineScope.launch { // Khởi chạy một coroutine để thực hiện animation
                        val animatable = indexesToAnimate.getOrPut(indexToAnimate) {
                            animatablesPool?.acquire() ?: return@launch
                        }

                        if (draggingIndex < reachedIndex) {
                            animatable.snapTo(0)
                            animatable.animateTo(-draggingItemSize)
                        } else {
                            animatable.snapTo(draggingItemSize)
                            animatable.animateTo(0)
                        }

                        indexesToAnimate.remove(indexToAnimate)
                        animatablesPool?.release(animatable)
                    }
                }
            }

            targetOffset < previousItemSize -> { // Nếu item đang được kéo đã vượt qua item trước đó
                if (reachedIndex > 0) {
                    reachedIndex -= 1
                    previousItemSize -= draggingItemSize
                    nextItemSize -= draggingItemSize

                    val indexToAnimate = reachedIndex + if (draggingIndex > reachedIndex) 0 else 1

                    coroutineScope.launch {
                        val animatable = indexesToAnimate.getOrPut(indexToAnimate) {
                            animatablesPool?.acquire() ?: return@launch
                        }

                        if (draggingIndex > reachedIndex) {
                            animatable.snapTo(0)
                            animatable.animateTo(draggingItemSize)
                        } else {
                            animatable.snapTo(-draggingItemSize)
                            animatable.animateTo(0)
                        }
                        indexesToAnimate.remove(indexToAnimate)
                        animatablesPool?.release(animatable)
                    }
                }
            }

            else -> { // Nếu item đang được kéo không vượt qua item trước đó hoặc tiếp theo
                val offsetInViewPort = targetOffset + itemInfo.offset - overscrolled

                val topOverscroll = lazyListState.layoutInfo.viewportStartOffset +
                        lazyListState.layoutInfo.beforeContentPadding - offsetInViewPort // Tính toán độ cuộn quá mức ở trên cùng
                val bottomOverscroll = lazyListState.layoutInfo.viewportEndOffset -
                        lazyListState.layoutInfo.afterContentPadding - offsetInViewPort - itemInfo.size // Tính toán độ cuộn quá mức ở dưới cùng

                if (topOverscroll > 0) overscroll(topOverscroll) else if (bottomOverscroll < 0)
                    overscroll(bottomOverscroll)
            }
        }
    }

    /*
    Phương thức này được gọi khi người dùng kết thúc thao tác kéo và thả. Nó sẽ:
    Thực hiện animation để đưa item về vị trí trung gian giữa các vị trí trước và sau.
    Cập nhật các trạng thái như draggingIndex, reachedIndex và giải phóng pool các Animatable.
     */
    fun onDragEnd() {
        if (!isDragging) return //

        coroutineScope.launch {
            offset.animateTo((previousItemSize + nextItemSize) / 2) // Thực hiện animation để đưa item về vị trí trung gian giữa các vị trí trước và sau

            withContext(Dispatchers.Main) { onDragEnd(draggingIndex, reachedIndex) } // Gọi hàm callback khi kết thúc kéo
            //withContext l à một hàm trong Kotlin Coroutines, cho phép bạn chuyển đổi giữa các context khác nhau (ví dụ: từ IO sang Main) trong một coroutine.

            if (areEquals()) { // Kiểm tra xem item đang được kéo có bằng với item đã đến gần nhất không
                draggingIndex = -1
                reachedIndex = -1
                draggingItemSize = 0
                offset.snapTo(0)
            }

            animatablesPool = null // Giải phóng pool các Animatable
        }
    }

    /*
    Phương thức này được gọi khi người dùng kéo vượt qua giới hạn của danh sách (overscroll). Nó sẽ:
    Cập nhật vị trí của danh sách bằng cách cuộn thêm vào hoặc ra khỏi viewport.
    viewport là một khung nhìn (view) trong giao diện người dùng, nơi mà nội dung được hiển thị.
     */
    private fun overscroll(overscroll: Int) {
        val newHeight = itemInfo.offset - overscroll // Tính toán chiều cao mới của item sau khi cuộn
        @Suppress("ComplexCondition") // Cảnh báo này cho biết rằng điều kiện trong if có độ phức tạp cao và có thể được tối ưu hóa.
        if (
            !(overscroll > 0 && newHeight <= lazyListState.layoutInfo.viewportEndOffset) &&
            !(overscroll < 0 && newHeight >= lazyListState.layoutInfo.viewportStartOffset) // Kiểm tra xem chiều cao mới có nằm trong phạm vi của viewport không
        ) return

        coroutineScope.launch { // Khởi chạy một coroutine để thực hiện cuộn
            lazyListState.scrollBy(-overscroll.toFloat())
            offset.snapTo(offset.value - overscroll)
        }
        overscrolled -= overscroll
    }

    //Phương thức này kiểm tra xem item đang kéo có thực sự đổi chỗ với item mà nó được thả vào hay không.
    // Nếu không có thay đổi, quá trình kéo sẽ kết thúc mà không có sự thay đổi vị trí thực sự.
    private fun areEquals() = lazyListState.layoutInfo.visibleItemsInfo.find { // Tìm kiếm item trong danh sách
        it.index + extraItemCount == draggingIndex // Kiểm tra xem item có chỉ số bằng với draggingIndex không
    }?.key == lazyListState.layoutInfo.visibleItemsInfo.find {
        it.index + extraItemCount == reachedIndex
    }?.key
}
/*
tạo và nhớ một đối tượng ReorderingState trong phạm vi Composable.
Hàm này giúp dễ dàng theo dõi và quản lý trạng thái của các item khi người dùng
thực hiện thao tác kéo và thả trong một LazyList (chẳng hạn như LazyColumn hoặc LazyRow).
 */
@Composable
fun rememberReorderingState(
    lazyListState: LazyListState,
    key: Any,
    onDragEnd: (Int, Int) -> Unit,
    onDragStart: () -> Unit = {},
    extraItemCount: Int = 0
): ReorderingState { // Hàm này tạo ra một đối tượng ReorderingState và lưu trữ nó trong bộ nhớ của Composable.
    val coroutineScope = rememberCoroutineScope() // Lấy CoroutineScope hiện tại để thực hiện các tác vụ bất đồng bộ

    return remember(key) {
        ReorderingState(
            lazyListState = lazyListState,
            coroutineScope = coroutineScope,
            lastIndex = if (key is List<*>) key.lastIndex else lazyListState.layoutInfo.totalItemsCount,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            extraItemCount = extraItemCount
        )
    }
}
