package app.vitune.android.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vitune.core.ui.Dimensions

// Hàm Composable có tên ItemContainer, dùng để tạo container linh hoạt kiểu Column hoặc Row dựa trên biến 'alternative'.
@Composable
inline fun ItemContainer(
    alternative: Boolean, // Nếu true thì hiển thị theo dạng cột (Column), ngược lại là hàng (Row).
    thumbnailSize: Dp, // Kích thước chiều rộng của ảnh thumbnail khi ở dạng Column.
    modifier: Modifier = Modifier, // Modifier để tuỳ chỉnh thêm nếu cần.
    horizontalAlignment: Alignment.Horizontal = Alignment.Start, // Căn chỉnh ngang cho Column.
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically, // Căn chỉnh dọc cho Row.
    content: @Composable (centeredModifier: Modifier) -> Unit // Nội dung con, truyền vào một Modifier để căn giữa phù hợp.
) =
    // Nếu alternative = true thì dùng Column layout
    if (alternative) Column(
        horizontalAlignment = horizontalAlignment, // Căn chỉnh ngang của Column
        verticalArrangement = Arrangement.spacedBy(12.dp), // Khoảng cách giữa các phần tử trong Column
        modifier = modifier
            .padding(Dimensions.items.alternativePadding) // Padding cho layout dạng Column (12.dp)
            .width(thumbnailSize) // Chiều rộng cố định (dựa theo kích thước thumbnail)
    ) {
        // Gọi content và truyền vào Modifier căn giữa theo chiều ngang
        content(Modifier.align(Alignment.CenterHorizontally))
    } else Row(
        verticalAlignment = verticalAlignment, // Căn chỉnh dọc của Row
        horizontalArrangement = Arrangement.spacedBy(12.dp), // Khoảng cách giữa các phần tử trong Row
        modifier = modifier
            .padding(
                vertical = Dimensions.items.verticalPadding,
                horizontal = Dimensions.items.horizontalPadding
            ) // Padding cho layout dạng Row (custom trong Dimensions)
            .fillMaxWidth() // Chiều rộng tự động mở rộng hết màn hình
    ) {
        // Gọi content và truyền vào Modifier căn giữa theo chiều dọc
        content(Modifier.align(Alignment.CenterVertically))
    }

// Hàm Composable có tên ItemInfoContainer, dùng để hiển thị nội dung theo dạng cột có khoảng cách giữa các dòng.
@Composable
inline fun ItemInfoContainer( // Inline function là một hàm được biên dịch vào nơi gọi nó, giúp giảm overhead khi gọi hàm.
    modifier: Modifier = Modifier, // Modifier tuỳ chỉnh thêm
    horizontalAlignment: Alignment.Horizontal = Alignment.Start, // Căn chỉnh ngang trong Column
    content: @Composable ColumnScope.() -> Unit // Nội dung con, chạy trong ColumnScope
) = Column(
    horizontalAlignment = horizontalAlignment, // Căn chỉnh ngang trong Column
    verticalArrangement = Arrangement.spacedBy(4.dp), // Khoảng cách giữa các phần tử là 4.dp
    modifier = modifier, // Gắn modifier bên ngoài vào
    content = content // Render nội dung con
)
