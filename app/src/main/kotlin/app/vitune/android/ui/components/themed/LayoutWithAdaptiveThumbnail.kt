package app.vitune.android.ui.components.themed

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import app.vitune.android.utils.thumbnail
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.shimmer
import app.vitune.core.ui.utils.isLandscape
import app.vitune.core.ui.utils.px
import coil3.compose.AsyncImage
import com.valentinilk.shimmer.shimmer

@Composable
// Tạo một layout có thể tự động thay đổi cách hiển thị thumbnail tùy theo orientation.
// nếu là chế độ ngang, nó sẽ hiển thị hình thu nhỏ và nội dung bên cạnh nhau
// nếu không, nó sẽ chỉ hiển thị nội dung
inline fun LayoutWithAdaptiveThumbnail(
    thumbnailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) = if (isLandscape) Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
) {
    thumbnailContent()
    content()
} else Box(modifier = modifier) { content() }

// Trả về một Composable hiển thị hình thumbnail (hoặc hiệu ứng loading shimmer nếu đang tải).
@SuppressLint("UnusedBoxWithConstraintsScope")
fun adaptiveThumbnailContent(
    isLoading: Boolean,
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape? = null
): @Composable () -> Unit = {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 16.dp)
    ) {
        val (colorPalette, _, _, thumbnailShape) = LocalAppearance.current
        val thumbnailSize =
            if (isLandscape) (maxHeight - 96.dp - Dimensions.items.collapsedPlayerHeight)
            else maxWidth

        val innerModifier = Modifier
            .clip(shape ?: thumbnailShape)
            .size(thumbnailSize)

        if (isLoading) Spacer(
            modifier = innerModifier
                .shimmer()
                .background(colorPalette.shimmer)
        ) else AsyncImage(
            model = url?.thumbnail(thumbnailSize.px),
            contentDescription = null,
            modifier = innerModifier.background(colorPalette.background1)
        )
    }
}
