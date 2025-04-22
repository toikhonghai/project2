package app.vitune.android.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vitune.android.models.Artist
import app.vitune.android.ui.components.themed.TextPlaceholder
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.thumbnail
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.shimmer
import app.vitune.core.ui.utils.px
import app.vitune.providers.innertube.Innertube
import coil3.compose.AsyncImage

@Composable
fun ArtistItem(
    artist: Artist,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ArtistItem(
    thumbnailUrl = artist.thumbnailUrl,
    name = artist.name,
    subscribersCount = null,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
fun ArtistItem(
    artist: Innertube.ArtistItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ArtistItem(
    thumbnailUrl = artist.thumbnail?.url,
    name = artist.info?.name,
    subscribersCount = artist.subscribersCountText,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
fun ArtistItem(
    thumbnailUrl: String?,               // Ảnh đại diện nghệ sĩ
    name: String?,                       // Tên nghệ sĩ
    subscribersCount: String?,          // Số lượng người theo dõi (subscribers)
    thumbnailSize: Dp,                   // Kích thước ảnh đại diện (vuông)
    modifier: Modifier = Modifier,       // Modifier truyền từ ngoài
    alternative: Boolean = false         // Dạng hiển thị: dọc (Column) hoặc ngang (Row)
) = ItemContainer(
    alternative = alternative,
    thumbnailSize = thumbnailSize,
    horizontalAlignment = Alignment.CenterHorizontally, // Căn giữa các item khi dùng Column
    modifier = Modifier
        .clip(LocalAppearance.current.thumbnailShape) // Bo góc mặc định từ theme
            then modifier // Kết hợp thêm modifier từ ngoài
) {
    val (_, typography) = LocalAppearance.current // Lấy typography từ theme (bỏ qua colorPalette)

    // Hiển thị ảnh đại diện (thumbnail) dạng hình tròn
    AsyncImage(
        model = thumbnailUrl?.thumbnail(thumbnailSize.px), // Resize theo kích thước
        contentDescription = null,
        modifier = Modifier
            .clip(CircleShape) // Bo tròn
            .requiredSize(thumbnailSize) // Đặt kích thước ảnh
    )

    // Hiển thị phần thông tin: tên nghệ sĩ và số lượng người theo dõi
    ItemInfoContainer(
        horizontalAlignment = if (alternative) Alignment.CenterHorizontally else Alignment.Start
    ) {
        BasicText(
            text = name.orEmpty(), // Nếu name == null thì dùng chuỗi rỗng
            style = typography.xs.semiBold,
            maxLines = if (alternative) 1 else 2,
            overflow = TextOverflow.Ellipsis
        )

        // Nếu có thông tin số lượng người theo dõi, hiển thị
        subscribersCount?.let {
            BasicText(
                text = subscribersCount,
                style = typography.xxs.semiBold.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ArtistItemPlaceholder(
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) {
    val (colorPalette) = LocalAppearance.current // Lấy màu nền shimmer từ theme

    ItemContainer(
        alternative = alternative,
        thumbnailSize = thumbnailSize,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Hiển thị hình tròn shimmer (ảnh giả lập đang loading)
        Spacer(
            modifier = Modifier
                .background(color = colorPalette.shimmer, shape = CircleShape)
                .size(thumbnailSize)
        )

        // Hiển thị placeholder cho text (dòng tên + dòng phụ)
        ItemInfoContainer(
            horizontalAlignment = if (alternative) Alignment.CenterHorizontally else Alignment.Start
        ) {
            TextPlaceholder()
            TextPlaceholder(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

