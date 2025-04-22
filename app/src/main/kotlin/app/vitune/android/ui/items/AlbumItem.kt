package app.vitune.android.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vitune.android.models.Album
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
fun AlbumItem(
    album: Album,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = AlbumItem(
    thumbnailUrl = album.thumbnailUrl,
    title = album.title,
    authors = album.authorsText,
    year = album.year,
    thumbnailSize = thumbnailSize,
    alternative = alternative,
    modifier = modifier
)

@Composable
fun AlbumItem(
    album: Innertube.AlbumItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = AlbumItem(
    thumbnailUrl = album.thumbnail?.url,
    title = album.info?.name,
    authors = album.authors?.joinToString("") { it.name.orEmpty() },
    year = album.year,
    thumbnailSize = thumbnailSize,
    alternative = alternative,
    modifier = modifier
)

@Composable
fun AlbumItem(
    thumbnailUrl: String?,        // Đường dẫn ảnh thumbnail của album (có thể null)
    title: String?,               // Tiêu đề album (có thể null)
    authors: String?,             // Tác giả (có thể null)
    year: String?,                // Năm phát hành (có thể null)
    thumbnailSize: Dp,            // Kích thước của thumbnail (ảnh vuông)
    modifier: Modifier = Modifier,
    alternative: Boolean = false  // Chế độ hiển thị: dọc (Column) hay ngang (Row)
) = ItemContainer( // Dùng layout linh hoạt: Row hoặc Column tuỳ thuộc vào `alternative`
    alternative = alternative,
    thumbnailSize = thumbnailSize,
    modifier = Modifier
        .clip(LocalAppearance.current.thumbnailShape) // Bo góc theo style toàn cục
            then modifier // Kết hợp thêm modifier bên ngoài truyền vào
    //Modifier.then(other: Modifier) là một hàm để kết hợp (combine/chaining) hai Modifier lại với nhau.
    //Nó đơn giản là gắn thêm modifier mới vào cuối chuỗi modifier hiện có.
) {
    // Lấy style & hình dạng hiện tại từ theme
    val typography = LocalAppearance.current.typography
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    // Hiển thị ảnh thumbnail bằng thư viện AsyncImage (coil)
    AsyncImage(
        model = thumbnailUrl?.thumbnail(thumbnailSize.px), // Resize ảnh theo kích thước
        contentDescription = null, // Không cần mô tả nội dung ảnh
        contentScale = ContentScale.Crop, // Cắt ảnh sao cho vừa khít khung
        modifier = Modifier
            .clip(thumbnailShape) // Bo góc
            .size(thumbnailSize)  // Kích thước vuông
    )

    // Phần hiển thị thông tin (tiêu đề, tác giả, năm)
    ItemInfoContainer {
        // Nếu có tiêu đề, hiển thị nó
        title?.let {
            BasicText(
                text = title,
                style = typography.xs.semiBold, // Style chữ nhỏ đậm
                maxLines = if (alternative) 1 else 2, // Dạng ngang thì nhiều dòng hơn
                overflow = TextOverflow.Ellipsis // Hiển thị "..." nếu quá dài
            )
        }

        // Nếu không ở chế độ alternative, thì hiển thị thêm tác giả
        if (!alternative) authors?.let {
            BasicText(
                text = authors,
                style = typography.xs.semiBold.secondary, // Style phụ nhẹ hơn
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Nếu có năm phát hành, hiển thị phía dưới
        year?.let {
            BasicText(
                text = year,
                style = typography.xxs.semiBold.secondary, // Cỡ nhỏ hơn
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp) // Cách dòng phía trên
            )
        }
    }
}


@Composable
// Hàm này tạo một AlbumItem với ảnh thumbnail giả lập (placeholder) cho các trường hợp không có dữ liệu thực tế.
fun AlbumItemPlaceholder(
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ItemContainer(
    alternative = alternative,
    thumbnailSize = thumbnailSize,
    modifier = modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val thumbnailShape = LocalAppearance.current.thumbnailShape

    // Hiển thị khung giả lập ảnh thumbnail (dạng shimmer/placeholder)
    Spacer(
        modifier = Modifier
            .background(color = colorPalette.shimmer, shape = thumbnailShape)
            .size(thumbnailSize)
    )

    // Placeholder cho nội dung text
    ItemInfoContainer {
        TextPlaceholder() // Dòng đầu tiên (title)
        if (!alternative) TextPlaceholder() // Dòng tác giả (nếu không phải dạng alternative)
        TextPlaceholder(modifier = Modifier.padding(top = 4.dp)) // Năm phát hành
    }
}

