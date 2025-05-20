package app.vitune.android.ui.items

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.LocalAppearance
import app.vitune.providers.innertube.Innertube
import coil3.compose.AsyncImage

@Composable
fun PodcastEpisodeItem(
    episode: Innertube.PodcastEpisodeItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography, _, thumbnailShape) = LocalAppearance.current
    val thumbnailSize = 60.dp // Kích thước cố định, tương tự SongItem
    val backgroundColor by animateColorAsState(
        targetValue = Color.Transparent, // Không hỗ trợ isPlaying trong chữ ký gốc
        label = ""
    )

    ItemContainer(
        alternative = false,
        thumbnailSize = thumbnailSize,
        modifier = modifier
            .background(backgroundColor)
            .clip(thumbnailShape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(thumbnailSize)
        ) {
            AsyncImage(
                model = episode.thumbnail?.url,
                error = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(thumbnailShape)
                    .background(colorPalette.background1)
                    .fillMaxSize()
            )
        }

        ItemInfoContainer {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BasicText(
                    text = episode.info?.name.orEmpty(),
                    style = typography.xs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onMenuClick,
                    icon = R.drawable.ellipsis_horizontal,
                    color = colorPalette.text,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                episode.podcast?.name?.let { podcastName ->
                    BasicText(
                        text = podcastName,
                        style = typography.xs.semiBold.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                episode.durationText?.let { duration ->
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
}