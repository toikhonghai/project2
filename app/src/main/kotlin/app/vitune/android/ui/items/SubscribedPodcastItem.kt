package app.vitune.android.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.android.models.PodcastEntity
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.LocalAppearance
import coil3.compose.AsyncImage

@Composable
fun SubscribedPodcastItem(
    podcastEntity: PodcastEntity, // Đổi sang PodcastEntity
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick).background(colorPalette.background0)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = podcastEntity.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = modifier.weight(1f)) {
                BasicText(
                    text = podcastEntity.title,
                    style = typography.xs.semiBold.copy(Color.White),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                BasicText(
                    text = podcastEntity.authorName ?: "",
                    style = typography.xs.semiBold.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                podcastEntity.episodeCount?.let {
                    BasicText(
                        text = "$it episodes",
                        style = typography.xxs.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onMenuClick,
                icon = R.drawable.ellipsis_horizontal,
                color = colorPalette.text,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}