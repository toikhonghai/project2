package app.vitune.android.ui.items

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import androidx.media3.common.MediaItem
import app.vitune.android.R
import app.vitune.android.models.Song
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.ui.components.themed.NonQueuedMediaItemMenu
import app.vitune.android.ui.components.themed.TextPlaceholder
import app.vitune.android.utils.asMediaItem
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.thumbnail
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.shimmer
import app.vitune.core.ui.utils.px
import app.vitune.core.ui.utils.songBundle
import app.vitune.providers.innertube.Innertube
import coil3.compose.AsyncImage

@Composable
fun SongItem(
    song: Innertube.SongItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    showDuration: Boolean = true,
    clip: Boolean = true,
    isPlaying: Boolean = false,
    hideExplicit: Boolean = AppearancePreferences.hideExplicit
) = SongItem(
    id = song.key,
    modifier = modifier,
    thumbnailUrl = song.thumbnail?.size(thumbnailSize.px),
    title = song.info?.name,
    authors = song.authors?.joinToString("") { it.name.orEmpty() },
    duration = song.durationText,
    explicit = song.explicit,
    thumbnailSize = thumbnailSize,
    showDuration = showDuration,
    clip = clip,
    isPlaying = isPlaying,
    hideExplicit = hideExplicit,
    mediaItem = song.asMediaItem
)

@Composable
fun SongItem(
    song: MediaItem,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    onThumbnailContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDuration: Boolean = true,
    clip: Boolean = true,
    isPlaying: Boolean = false,
    hideExplicit: Boolean = AppearancePreferences.hideExplicit
) {
    val extras = remember(song) { song.mediaMetadata.extras?.songBundle }

    SongItem(
        id = song.mediaId,
        modifier = modifier,
        thumbnailUrl = song.mediaMetadata.artworkUri.thumbnail(thumbnailSize.px)?.toString(),
        title = song.mediaMetadata.title?.toString(),
        authors = song.mediaMetadata.artist?.toString(),
        duration = extras?.durationText,
        explicit = extras?.explicit == true,
        thumbnailSize = thumbnailSize,
        onThumbnailContent = onThumbnailContent,
        trailingContent = trailingContent,
        showDuration = showDuration,
        clip = clip,
        isPlaying = isPlaying,
        hideExplicit = hideExplicit,
        mediaItem = song
    )
}

@Composable
fun SongItem(
    song: Song,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    index: Int? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    showDuration: Boolean = true,
    clip: Boolean = true,
    isPlaying: Boolean = false,
    hideExplicit: Boolean = AppearancePreferences.hideExplicit
) = SongItem(
    id = song.id,
    modifier = modifier,
    index = index,
    thumbnailUrl = song.thumbnailUrl?.thumbnail(thumbnailSize.px),
    title = song.title,
    authors = song.artistsText,
    duration = song.durationText,
    explicit = song.explicit,
    thumbnailSize = thumbnailSize,
    onThumbnailContent = onThumbnailContent,
    trailingContent = trailingContent,
    showDuration = showDuration,
    clip = clip,
    isPlaying = isPlaying,
    hideExplicit = hideExplicit,
    mediaItem = song.asMediaItem
)

@Composable
private fun SongItem(
    id: String,
    thumbnailUrl: String?,                  // URL ảnh thumbnail của bài hát
    title: String?,                        // Tiêu đề bài hát
    authors: String?,                      // Tên nghệ sĩ
    duration: String?,                     // Thời lượng bài hát (vd: "3:45")
    explicit: Boolean,                     // Có nhãn "explicit" hay không (nội dung nhạy cảm)
    thumbnailSize: Dp,                     // Kích thước của ảnh thumbnail
    modifier: Modifier = Modifier,         // Modifier bên ngoài cho item
    index: Int? = null,                    // Số thứ tự trong danh sách phát (nếu có)
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null, // Nội dung thêm trong thumbnail
    trailingContent: @Composable (() -> Unit)? = null,             // Composable tùy chỉnh phía cuối (vd: nút thêm)
    showDuration: Boolean = true,          // Hiện thời lượng hay không
    clip: Boolean = true,                  // Có bo viền góc hay không
    isPlaying: Boolean = false,            // Bài hát có đang phát không
    hideExplicit: Boolean = AppearancePreferences.hideExplicit, // Ẩn nhãn explicit nếu người dùng chọn
    mediaItem: MediaItem? = null        // Bài hát hiện tại (nếu có)
) {
    // Lấy giao diện hiện tại (màu sắc, kiểu chữ, hình dạng thumbnail)
    val (colorPalette, typography, _, thumbnailShape) = LocalAppearance.current

    // Gọi composable SongItem bên dưới (tách phần giao diện để tái sử dụng)
    SongItem(
        title = title,
        authors = authors,
        duration = duration,
        explicit = explicit,
        thumbnailSize = thumbnailSize,
        thumbnailContent = {
            // Box hiển thị ảnh thumbnail
            Box(
                modifier = Modifier
                    .clip(thumbnailShape)                         // Bo góc theo cấu hình
                    .background(colorPalette.background1)        // Nền cho ảnh khi chưa tải xong
                    .fillMaxSize()
            ) {
                // Ảnh bài hát
                AsyncImage(
                    model = thumbnailUrl,                        // Load ảnh từ URL
                    error = painterResource(id = R.drawable.ic_launcher_foreground), // Ảnh lỗi
                    contentDescription = null,
                    contentScale = ContentScale.Crop,            // Cắt ảnh để vừa khung
                    modifier = Modifier.fillMaxSize()
                )

                // Nếu có chỉ số (index), hiển thị số thứ tự
                if (index != null) {
                    Box(
                        modifier = Modifier
                            .background(color = Color.Black.copy(alpha = 0.75f)) // Nền đen bán trong suốt
                            .fillMaxSize()
                    )
                    BasicText(
                        text = "${index + 1}",                   // Hiển thị chỉ số (1-based)
                        style = typography.xs.semiBold.copy(color = Color.White), // Chữ trắng, in đậm
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Cho phép hiển thị nội dung bổ sung trong thumbnail nếu được cung cấp
            onThumbnailContent?.invoke(this)
        },
        modifier = modifier,
        trailingContent = trailingContent,
        showDuration = showDuration,
        clip = clip,
        isPlaying = isPlaying,
        hideExplicit = hideExplicit,
        mediaItem = mediaItem
    )
}


@Composable
private fun SongItem(
    title: String?,                                  // Tiêu đề bài hát
    authors: String?,                                // Tác giả / nghệ sĩ
    duration: String?,                               // Thời lượng bài hát
    explicit: Boolean,                               // Có gắn nhãn "explicit" không
    thumbnailSize: Dp,                               // Kích thước của ảnh thumbnail
    thumbnailContent: @Composable BoxScope.() -> Unit, // Nội dung thumbnail (hình ảnh, số thứ tự...)
    modifier: Modifier = Modifier,                   // Modifier bên ngoài
    trailingContent: @Composable (() -> Unit)? = null, // Nội dung phía sau (trailing), vd: nút more
    showDuration: Boolean = true,                    // Có hiển thị thời lượng không
    clip: Boolean = true,                            // Có bo góc không
    isPlaying: Boolean = false,                      // Bài hát đang phát hay không
    hideExplicit: Boolean = AppearancePreferences.hideExplicit, // Ẩn nhãn "explicit" nếu người dùng thiết lập,
    mediaItem: MediaItem? = null,                  // Bài hát hiện tại (nếu có)
) {
    // Lấy theme giao diện: màu sắc và kiểu chữ
    val (colorPalette, typography) = LocalAppearance.current
    val menuState = LocalMenuState.current

    // Animate background: nếu đang phát thì đổi nền
    val backgroundColor by animateColorAsState(
        targetValue = if (isPlaying) colorPalette.background2 else Color.Transparent,
        label = ""
    )

    // Nếu không phải explicit hoặc không bị ẩn thì tiếp tục vẽ item
    if (!(hideExplicit && explicit)) ItemContainer(
        alternative = false,
        thumbnailSize = thumbnailSize,
        modifier = modifier
            .background(backgroundColor) // Nền thay đổi khi đang phát
            .let {
                if (clip) Modifier.clip(LocalAppearance.current.thumbnailShape) then it
                else it
            }
    ) {
        // Vùng hiển thị ảnh thumbnail
        Box(
            modifier = Modifier.size(thumbnailSize),
            content = thumbnailContent
        )

        // Vùng chứa thông tin bài hát
        ItemInfoContainer {
            // Nếu có trailingContent (vd: nút...), hiển thị tiêu đề và trailing
            trailingContent?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BasicText(
                        text = title.orEmpty(),                     // Tiêu đề bài hát
                        style = typography.xs.semiBold,            // Kiểu chữ nhỏ, in đậm
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,          // Cắt nếu quá dài
                        modifier = Modifier.weight(1f)
                    )

                    it() // Gọi trailing content
                }
            } ?: BasicText(
                text = title.orEmpty(),                             // Chỉ hiện tiêu đề nếu không có trailing
                style = typography.xs.semiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Hiển thị hàng phụ: tác giả + explicit + thời lượng
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cột bên trái: tác giả + icon explicit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    authors?.let {
                        BasicText(
                            text = authors,                         // Tên nghệ sĩ
                            style = typography.xs.semiBold.secondary, // Kiểu chữ phụ
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(
                                weight = 1f,
                                fill = false
                            )
                        )
                    }
                    // Hiển thị biểu tượng "explicit" nếu có
                    if (explicit) Image(
                        painter = painterResource(R.drawable.explicit),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorPalette.text),
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally){
                    mediaItem?.let { item ->
                        IconButton(
                            onClick = {
                                menuState.display {
                                    NonQueuedMediaItemMenu(
                                        onDismiss = menuState::hide,
                                        mediaItem = mediaItem
                                    )
                                }
                            },
                            icon = R.drawable.ellipsis_horizontal,
                            color = colorPalette.text, // nếu muốn đổi màu icon
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Thời lượng bài hát
                    if (showDuration) duration?.let {
                        BasicText(
                            text = duration,
                            style = typography.xxs.secondary.medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    } else Unit // Nếu bị ẩn vì là explicit và bị cấu hình ẩn thì không render gì
}


@Composable
fun SongItemPlaceholder(
    thumbnailSize: Dp,                // Kích thước của thumbnail (hình vuông đại diện bài hát)
    modifier: Modifier = Modifier     // Modifier để tùy chỉnh layout bên ngoài
) = ItemContainer(
    alternative = false,              // Dùng kiểu hiển thị chính (không phải kiểu alternative)t
    thumbnailSize = thumbnailSize,
    modifier = modifier
) {
    // Lấy theme hiện tại: màu sắc, hình dạng bo góc
    val (colorPalette, _, _, thumbnailShape) = LocalAppearance.current

    // Placeholder cho ảnh thumbnail bài hát (hiệu ứng shimmer)
    Spacer(
        modifier = Modifier
            .background(
                color = colorPalette.shimmer,    // Màu nền shimmer placeholder
                shape = thumbnailShape           // Bo góc như ảnh thật
            )
            .size(thumbnailSize)                 // Kích thước ảnh giả
    )

    // Placeholder cho phần thông tin bài hát (tiêu đề và tác giả)
    ItemInfoContainer {
        TextPlaceholder() // Placeholder dòng đầu (vd: tiêu đề bài hát)
        TextPlaceholder() // Placeholder dòng thứ hai (vd: tên tác giả)
    }
}

