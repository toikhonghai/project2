package app.vitune.android.ui.items

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.models.PlaylistPreview
import app.vitune.android.models.PodcastPlaylistPreview
import app.vitune.android.ui.components.themed.TextPlaceholder
import app.vitune.android.utils.center
import app.vitune.android.utils.color
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.thumbnail
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.onOverlay
import app.vitune.core.ui.overlay
import app.vitune.core.ui.shimmer
import app.vitune.core.ui.utils.px
import app.vitune.core.ui.utils.roundedShape
import app.vitune.providers.innertube.Innertube
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Composable
fun PlaylistItem(
    @DrawableRes icon: Int,
    colorTint: Color,
    name: String?,
    songCount: Int?,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = PlaylistItem(
    thumbnailContent = {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorTint),
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
        )
    },
    songCount = songCount,
    name = name,
    channelName = null,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
// Hàm này tạo ra một PlaylistItem với ảnh động hoặc nhiều ảnh thumbnail
fun PlaylistItem(
    playlist: PlaylistPreview,     // Dữ liệu của playlist (bao gồm tên, ID, ảnh, số bài hát,...)
    thumbnailSize: Dp,             // Kích thước thumbnail
    modifier: Modifier = Modifier, // Modifier mở rộng
    alternative: Boolean = false   // Layout dọc hay ngang
) {
    val thumbnailSizePx = thumbnailSize.px // Kích thước pixel của thumbnail

    // Lấy danh sách ảnh thumbnail (có thể là 1 hoặc nhiều)
    val thumbnails by remember {
        // Nếu playlist đã có thumbnail sẵn (thumbnail đơn)
        playlist.thumbnail?.let {
            flowOf(listOf(it)) // Tạo flow từ 1 item
        }
        // Nếu không, lấy danh sách thumbnail từ database theo playlist ID
            ?: Database
                .playlistThumbnailUrls(playlist.playlist.id)
                .distinctUntilChanged() // Chỉ emit khi có thay đổi
                .map { urls ->
                    urls.map { it.thumbnail(thumbnailSizePx / 2) } // Resize ảnh cho từng phần
                }
    }.collectAsState( // Chuyển Flow -> State để dùng trong Compose
        initial = emptyList(),     // Giá trị ban đầu rỗng
        context = Dispatchers.IO   // Thực hiện trong IO dispatcher
    )

    // Gọi lại PlaylistItem với thumbnailContent là ảnh động / 4 ảnh
    PlaylistItem(
        thumbnailContent = {
            // Nếu chỉ có 1 thumbnail, hiển thị ảnh full
            if (thumbnails.toSet().size == 1)
                AsyncImage(
                    model = thumbnails.first().thumbnail(thumbnailSizePx),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = it
                )
            else Box(modifier = it.fillMaxSize()) {
                // Nếu có nhiều thumbnail (playlist từ nhiều bài), hiển thị 4 ảnh chia góc
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).forEachIndexed { index, alignment ->
                    AsyncImage(
                        model = thumbnails.getOrNull(index), // Có thể ít hơn 4 ảnh
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(alignment)      // Căn vào góc tương ứng
                            .fillMaxSize(.5f)      // Mỗi ảnh chiếm 1/4 diện tích
                    )
                }
            }
        },
        songCount = playlist.songCount,          // Số bài hát trong playlist
        name = playlist.playlist.name,           // Tên playlist
        channelName = null,                      // Không có tên kênh (có thể sửa nếu cần)
        thumbnailSize = thumbnailSize,
        modifier = modifier,
        alternative = alternative
    )
}


@Composable
fun PlaylistItem(
    playlist: Innertube.PlaylistItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = PlaylistItem(
    thumbnailUrl = playlist.thumbnail?.url,
    songCount = playlist.songCount,
    name = playlist.info?.name,
    channelName = playlist.channel?.name,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)

@Composable
fun PlaylistItem(
    thumbnailUrl: String?,
    songCount: Int?,
    name: String?,
    channelName: String?,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = PlaylistItem(
    thumbnailContent = {
        AsyncImage(
            model = thumbnailUrl?.thumbnail(thumbnailSize.px),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = it
        )
    },
    songCount = songCount,
    name = name,
    channelName = channelName,
    thumbnailSize = thumbnailSize,
    modifier = modifier,
    alternative = alternative
)
@Composable
fun PlaylistItem(
    playlist: PodcastPlaylistPreview,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) {
    val thumbnailSizePx = thumbnailSize.px

    val thumbnails by remember {
        playlist.thumbnail?.let {
            flowOf(listOf(it))
        } ?: Database
            .podcastPlaylistThumbnailUrls(playlist.id)
            .distinctUntilChanged()
            .map { urls ->
                urls.map { it.thumbnail(thumbnailSizePx / 2) }
            }
    }.collectAsState(
        initial = emptyList(),
        context = Dispatchers.IO
    )

    PlaylistItem(
        thumbnailContent = {
            if (thumbnails.toSet().size == 1)
                AsyncImage(
                    model = thumbnails.first().thumbnail(thumbnailSizePx),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = it
                )
            else Box(modifier = it.fillMaxSize()) {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).forEachIndexed { index, alignment ->
                    AsyncImage(
                        model = thumbnails.getOrNull(index),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(alignment)
                            .fillMaxSize(.5f)
                    )
                }
            }
        },
        songCount = playlist.episodeCount,
        name = playlist.name,
        channelName = null,
        thumbnailSize = thumbnailSize,
        modifier = modifier,
        alternative = alternative
    )
}
@Composable
fun PlaylistItem(
    thumbnailContent: @Composable BoxScope.(modifier: Modifier) -> Unit, // Composable hiển thị thumbnail (ảnh bìa playlist)
    songCount: Int?,                    // Số lượng bài hát trong playlist
    name: String?,                      // Tên playlist
    channelName: String?,              // Tên kênh hoặc người tạo
    thumbnailSize: Dp,                  // Kích thước ảnh thumbnail
    modifier: Modifier = Modifier,      // Modifier mở rộng từ ngoài truyền vào
    alternative: Boolean = false        // True nếu hiển thị theo layout dọc
) = ItemContainer(
    alternative = alternative,
    thumbnailSize = thumbnailSize,
    modifier = Modifier.clip(
        ( // Bo góc thumbnail theo theme + padding
                LocalAppearance.current.thumbnailShapeCorners +
                        if (alternative) Dimensions.items.alternativePadding else Dimensions.items.horizontalPadding
                ).roundedShape
    ) then modifier // Kết hợp modifier mặc định + modifier từ ngoài
) { centeredModifier ->
    val (colorPalette, typography, thumbnailShapeCorners) = LocalAppearance.current

    // Khối chứa thumbnail
    Box(
        modifier = centeredModifier
            .clip(thumbnailShapeCorners.roundedShape)
            .background(color = colorPalette.background1) // Màu nền phía sau ảnh
            .let {
                if (alternative) it // Nếu layout dọc thì fill width + hình vuông
                    .sizeIn(
                        minWidth = thumbnailSize,
                        minHeight = thumbnailSize
                    )
                    .fillMaxWidth()
                    .aspectRatio(1f)
                else it.requiredSize(thumbnailSize) // Ngược lại, kích thước cố định
            }
    ) {
        thumbnailContent(Modifier.fillMaxSize()) // Hiển thị nội dung thumbnail truyền vào

        // Hiển thị số bài hát ở góc phải dưới thumbnail (nếu có)
        songCount?.let {
            BasicText(
                text = "$songCount",
                style = typography.xxs.medium.color(colorPalette.onOverlay),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(all = Dimensions.items.gap)
                    .background(
                        color = colorPalette.overlay,
                        shape = (thumbnailShapeCorners - Dimensions.items.gap).coerceAtLeast(0.dp).roundedShape
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.BottomEnd) // Căn về góc dưới bên phải
            )
        }
    }

    // Hiển thị tên playlist và tên kênh
    ItemInfoContainer(modifier = if (alternative && channelName.isNullOrBlank()) centeredModifier else Modifier) {
        BasicText(
            text = name.orEmpty(),
            style = typography.xs.semiBold.let {
                if (alternative && channelName.isNullOrBlank()) it.center else it
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Nếu có tên kênh thì hiển thị
        if (channelName?.isNotBlank() == true) BasicText(
            text = channelName,
            style = typography.xs.semiBold.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
fun PlaylistItemPlaceholder(
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    alternative: Boolean = false
) = ItemContainer(
    alternative = alternative,
    thumbnailSize = thumbnailSize,
    modifier = modifier
) {
    val (colorPalette, _, _, thumbnailShape) = LocalAppearance.current

    // Khối giả lập thumbnail (shimmer loading)
    Spacer(
        modifier = Modifier
            .background(color = colorPalette.shimmer, shape = thumbnailShape)
            .size(thumbnailSize)
    )

    // Phần placeholder cho text
    ItemInfoContainer(
        horizontalAlignment = if (alternative) Alignment.CenterHorizontally else Alignment.Start
    ) {
        TextPlaceholder() // Placeholder cho tên playlist
        TextPlaceholder() // Placeholder cho tên kênh
    }
}

