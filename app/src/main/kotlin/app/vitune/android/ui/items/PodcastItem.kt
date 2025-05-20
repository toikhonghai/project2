package app.vitune.android.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.utils.color
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.onOverlay
import app.vitune.core.ui.overlay
import app.vitune.core.ui.utils.roundedShape
import app.vitune.providers.innertube.Innertube
import coil3.compose.AsyncImage

@Composable
fun PodcastItem(
    podcast: Innertube.PodcastItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography, thumbnailShapeCorners) = LocalAppearance.current
    val thumbnailSize = 60.dp // Kích thước cố định, tương tự PlaylistItem mặc định

    ItemContainer(
        alternative = false,
        thumbnailSize = thumbnailSize,
        modifier = modifier
            .clip(
                (thumbnailShapeCorners + Dimensions.items.horizontalPadding).roundedShape
            )
            .clickable(onClick = onClick)
    ) { centeredModifier ->
        Box(
            modifier = centeredModifier
                .clip(thumbnailShapeCorners.roundedShape)
                .background(colorPalette.background1)
                .size(thumbnailSize)
        ) {
            AsyncImage(
                model = podcast.thumbnail?.url,
                error = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            podcast.episodeCount?.let {
                BasicText(
                    text = "$it",
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
                        .align(Alignment.BottomEnd)
                )
            }
        }

        ItemInfoContainer {
            BasicText(
                text = podcast.info?.name.orEmpty(),
                style = typography.xs.semiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            podcast.authors?.joinToString { it.name ?: "" }?.let { authors ->
                if (authors.isNotBlank()) {
                    BasicText(
                        text = authors,
                        style = typography.xs.semiBold.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

        }
    }
}