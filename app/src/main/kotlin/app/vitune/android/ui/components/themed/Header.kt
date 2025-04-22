package app.vitune.android.ui.components.themed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vitune.android.ui.components.FadingRow
import app.vitune.android.utils.medium
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.shimmer
import kotlin.random.Random

/**
 * Composable tạo Header với tiêu đề và các nút hành động (nếu có).
 * @param title Tiêu đề của Header.
 * @param modifier Modifier để tuỳ chỉnh giao diện.
 * @param actionsContent Nội dung của các hành động ở góc phải.
 */
@Composable
// ham này tạo ra một Header với tiêu đề và các nút hành động (nếu có).
fun Header(
    title: String,
    modifier: Modifier = Modifier,
    actionsContent: @Composable RowScope.() -> Unit = {} // Lambda chứa các thành phần ở bên phải Header.
) = Header(
    modifier = modifier,
    titleContent = {
        FadingRow { // Hiệu ứng làm mờ khi tiêu đề quá dài.
            BasicText(
                text = title,
                style = LocalAppearance.current.typography.xxl.medium, // Áp dụng kiểu chữ của ứng dụng.
                maxLines = 1 // Giới hạn tiêu đề chỉ hiển thị một dòng.
            )
        }
    },
    actionsContent = actionsContent
)

/**
 * Header có thể nhận nội dung tuỳ chỉnh cho tiêu đề và các nút hành động.
 * @param titleContent Nội dung của tiêu đề (có thể là Text hoặc Icon kết hợp).
 * @param actionsContent Các nút hoặc biểu tượng ở góc phải của Header.
 * @param modifier Modifier để tuỳ chỉnh giao diện.
 */
@Composable
fun Header(
    titleContent: @Composable () -> Unit,
    actionsContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier
) = Box(
    contentAlignment = Alignment.CenterEnd, // Canh chỉnh nội dung về bên phải.
    modifier = modifier
        .padding(horizontal = 16.dp) // Khoảng cách lề hai bên.
        .height(Dimensions.items.headerHeight) // Chiều cao của Header.
        .fillMaxWidth() // Chiếm toàn bộ chiều ngang.
) {
    titleContent() // Hiển thị nội dung tiêu đề.

    Row(
        verticalAlignment = Alignment.CenterVertically, // Canh chỉnh các nút hành động theo chiều dọc.
        horizontalArrangement = Arrangement.spacedBy(8.dp), // Khoảng cách giữa các thành phần.
        modifier = Modifier
            .align(Alignment.BottomEnd) // Đặt nội dung ở góc phải dưới.
            .heightIn(min = 48.dp), // Chiều cao tối thiểu.
        content = actionsContent // Hiển thị các nút hành động.
    )
}

/**
 * Placeholder (khung chờ) của Header khi dữ liệu chưa sẵn sàng.
 * Được sử dụng để tạo hiệu ứng shimmer (nền nhấp nháy).
 */
@Composable
fun HeaderPlaceholder(modifier: Modifier = Modifier) = Box(
    contentAlignment = Alignment.CenterEnd, // Canh chỉnh nội dung về bên phải.
    modifier = modifier
        .padding(horizontal = 16.dp) // Lề hai bên.
        .height(Dimensions.items.headerHeight) // Chiều cao của Header.
        .fillMaxWidth() // Chiếm toàn bộ chiều ngang.
) {
    val (colorPalette, typography) = LocalAppearance.current

    // Sinh chuỗi giả để hiển thị hiệu ứng loading.
    val text = remember { List(Random.nextInt(4, 16)) { " " }.joinToString(separator = "") }

    Box(
        modifier = Modifier
            .background(colorPalette.shimmer) // Áp dụng hiệu ứng shimmer (nền nhấp nháy).
            .fillMaxWidth(remember { 0.25f + Random.nextFloat() * 0.5f }) // Ngẫu nhiên độ rộng của tiêu đề.
    ) {
        BasicText(
            text = text,
            style = typography.xxl.medium, // Áp dụng kiểu chữ lớn.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis // Hiển thị dấu "..." nếu tiêu đề quá dài.
        )
    }
}
