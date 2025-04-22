package app.vitune.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.valentinilk.shimmer.shimmer

/*
Hàm ShimmerHost trong Jetpack Compose là một @Composable dùng để bọc nội dung cần
hiệu ứng shimmer – một kiểu hiệu ứng thường dùng để hiển thị placeholder (nội dung giả lập) trong lúc đang tải dữ liệu.
 */
@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) = Column(
    horizontalAlignment = horizontalAlignment,
    verticalArrangement = verticalArrangement,
    modifier = modifier
        .shimmer()
        .graphicsLayer(alpha = 0.99f) // Đặt alpha gần 1 để tránh bị mờ
        .drawWithContent {
            drawContent() // Vẽ nội dung bên trong Column như bình thường.
            drawRect( // Sau khi vẽ nội dung, nó vẽ một hình chữ nhật có hiệu ứng gradient từ đen đến trong suốt.
                brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent)),
                blendMode = BlendMode.DstIn // Sử dụng chế độ hòa trộn DstIn để chỉ hiển thị phần nội dung bên trong hình chữ nhật.
            )
        },
    content = content
)
