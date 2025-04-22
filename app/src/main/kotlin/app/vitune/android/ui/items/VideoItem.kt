package app.vitune.android.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vitune.android.ui.components.themed.TextPlaceholder
import app.vitune.android.utils.color
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.onOverlay
import app.vitune.core.ui.overlay
import app.vitune.core.ui.shimmer
import app.vitune.core.ui.utils.roundedShape
import app.vitune.providers.innertube.Innertube
import coil3.compose.AsyncImage

@Composable
fun VideoItem(
    video: Innertube.VideoItem,
    thumbnailWidth: Dp,
    thumbnailHeight: Dp,
    modifier: Modifier = Modifier
) = VideoItem(
    thumbnailUrl = video.thumbnail?.url,
    duration = video.durationText,
    title = video.info?.name,
    uploader = video.authors?.joinToString("") { it.name.orEmpty() },
    views = video.viewsText,
    thumbnailWidth = thumbnailWidth,
    thumbnailHeight = thumbnailHeight,
    modifier = modifier
)

@Composable
fun VideoItem(
    thumbnailUrl: String?,      // URL ảnh đại diện (thumbnail) của video
    duration: String?,          // Thời lượng video (vd: "5:32")
    title: String?,             // Tiêu đề video
    uploader: String?,          // Tên người đăng video
    views: String?,             // Số lượt xem
    thumbnailWidth: Dp,         // Chiều rộng thumbnail
    thumbnailHeight: Dp,        // Chiều cao thumbnail
    modifier: Modifier = Modifier
) = ItemContainer(
    alternative = false,
    thumbnailSize = 0.dp, // Không dùng thumbnailSize ở đây, vì thumbnail có kích thước riêng
    modifier = Modifier
        .clip(LocalAppearance.current.thumbnailShape) // Bo góc theo giao diện
        .then(modifier) // Kết hợp với modifier bên ngoài nếu có
) {
    // Lấy style từ theme hiện tại
    val (colorPalette, typography, thumbnailShapeCorners) = LocalAppearance.current

    // Vùng hiển thị thumbnail + thời lượng
    Box {
        // Hiển thị ảnh thumbnail video
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .clip(thumbnailShapeCorners.roundedShape) // Bo góc thumbnail
                .size(width = thumbnailWidth, height = thumbnailHeight)
        )

        // Nếu có thời lượng thì hiển thị ở góc dưới bên phải của thumbnail
        duration?.let {
            BasicText(
                text = duration,
                style = typography.xxs.medium.color(colorPalette.onOverlay), // Màu chữ nổi trên nền tối
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(all = Dimensions.items.gap) // Padding ngoài
                    .background(
                        color = colorPalette.overlay, // Nền mờ phía sau thời lượng
                        shape = (thumbnailShapeCorners - Dimensions.items.gap)
                            .coerceAtLeast(0.dp)
                            .roundedShape
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp) // Padding trong
                    .align(Alignment.BottomEnd) // Canh góc phải dưới
            )
        }
    }

    // Hiển thị phần thông tin bên dưới thumbnail
    ItemInfoContainer {
        // Tiêu đề video
        BasicText(
            text = title.orEmpty(),
            style = typography.xs.semiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Người đăng video
        BasicText(
            text = uploader.orEmpty(),
            style = typography.xs.semiBold.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Số lượt xem (nếu có)
        views?.let {
            BasicText(
                text = views,
                style = typography.xxs.medium.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun VideoItemPlaceholder(
    thumbnailWidth: Dp,         // Chiều rộng ảnh đại diện (thumbnail) giả
    thumbnailHeight: Dp,        // Chiều cao ảnh đại diện (thumbnail) giả
    modifier: Modifier = Modifier
) = ItemContainer(
    alternative = false,
    thumbnailSize = 0.dp,       // Không sử dụng thumbnailSize trong layout này
    modifier = modifier         // Modifier được truyền từ bên ngoài
) {
    // Lấy màu shimmer và hình dạng bo góc từ giao diện hiện tại
    val colorPalette = LocalAppearance.current.colorPalette
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    // Placeholder cho thumbnail video
    Spacer(
        modifier = Modifier
            .background(
                color = colorPalette.shimmer,  // Màu shimmer giả hiệu ứng loading
                shape = thumbnailShape         // Bo góc cho khớp với UI thật
            )
            .size(width = thumbnailWidth, height = thumbnailHeight)
    )

    // Phần nội dung thông tin giả phía dưới thumbnail
    ItemInfoContainer {
        TextPlaceholder() // Placeholder dòng tiêu đề
        TextPlaceholder() // Placeholder người đăng
        TextPlaceholder(  // Placeholder lượt xem (thêm padding phía trên cho cách dòng)
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

