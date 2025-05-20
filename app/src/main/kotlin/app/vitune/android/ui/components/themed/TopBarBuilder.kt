package app.vitune.android.ui.components.themed

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clip
import androidx.media3.common.util.Log
import app.vitune.android.R
import app.vitune.android.utils.medium
import app.vitune.core.ui.LocalAppearance
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

class TopBarBuilder @PublishedApi internal constructor() {
    companion object {
        @Composable
        inline fun rememberTopBar(
            selectedMainTab: MainTab?,
            crossinline content: TopBarBuilder.() -> Unit
        ) = rememberSaveable(
            selectedMainTab,
            saver = listSaver<ImmutableList<TopBarItem>, Parcelable>(
                save = { items -> items.toList() }, // Lưu danh sách TopBarItem dưới dạng List<Parcelable>
                restore = { parcelables ->
                    parcelables.mapNotNull { parcelable ->
                        when (parcelable) {
                            is TopBarItem.Title -> parcelable
                            is TopBarItem.Button -> parcelable
                            is TopBarItem.IconButton -> parcelable
                            else -> null // Bỏ qua các đối tượng không hợp lệ
                        }
                    }.toImmutableList()
                }
            )
        ) {
            TopBarBuilder().apply(content).items.values.toImmutableList()
        }
    }

    @PublishedApi
    internal val items = mutableMapOf<String, TopBarItem>()

    fun titleRes(
        @StringRes titleRes: Int
    ): TopBarItem.Title {
        val ret = TopBarItem.Title(
            key = "title",
            titleRes = titleRes
        )
        items["title"] = ret
        return ret
    }

    fun titleText(
        titleText: String
    ): TopBarItem.Title {
        val ret = TopBarItem.Title(
            key = "title",
            titleText = titleText
        )
        items["title"] = ret
        return ret
    }

    fun buttonRes(
        @StringRes textRes: Int,
        onClick: () -> Unit,
        enabled: () -> Boolean = { true }
    ): TopBarItem.Button {
        val key = "button_${items.size}"
        val ret = TopBarItem.Button(
            key = key,
            textRes = textRes,
            onClick = onClick,
            enabled = enabled
        )
        items[key] = ret
        return ret
    }

    fun buttonText(
        text: String,
        onClick: () -> Unit,
        enabled: () -> Boolean = { true }
    ): TopBarItem.Button {
        val key = "button_${items.size}"
        val ret = TopBarItem.Button(
            key = key,
            textValue = text,
            onClick = onClick,
            enabled = enabled
        )
        items[key] = ret
        return ret
    }

    fun iconButton(
        @DrawableRes icon: Int,
        onClick: () -> Unit
    ): TopBarItem.IconButton {
        val key = "icon_button_${items.size}"
        val ret = TopBarItem.IconButton(
            key = key,
            icon = icon,
            onClick = onClick
        )
        items[key] = ret
        return ret
    }
}

@Parcelize
sealed class TopBarItem : Parcelable {
    abstract val key: String

    @Parcelize
    data class Title(
        override val key: String,
        @StringRes private val titleRes: Int? = null,
        private val titleText: String? = null
    ) : TopBarItem() {
        @IgnoredOnParcel
        val title: @Composable () -> String = {
            if (titleRes != null) stringResource(titleRes) else titleText ?: ""
        }
    }

    @Parcelize
    data class Button(
        override val key: String,
        @StringRes private val textRes: Int? = null,
        private val textValue: String? = null,
        @IgnoredOnParcel
        private val onClick: () -> Unit = {},
        @IgnoredOnParcel
        private val enabled: () -> Boolean = { true }
    ) : TopBarItem() {
        @IgnoredOnParcel
        val text: @Composable () -> String = {
            if (textRes != null) stringResource(textRes) else textValue ?: ""
        }

        @IgnoredOnParcel
        val onClickHandler: () -> Unit = onClick

        @IgnoredOnParcel
        val isEnabled: () -> Boolean = enabled
    }

    data class IconButton(
        override val key: String,
        @DrawableRes val icon: Int,
        @IgnoredOnParcel
        private val onClick: () -> Unit = {} // Đảm bảo giá trị mặc định
    ) : TopBarItem() {
        @IgnoredOnParcel
        val onClickHandler: () -> Unit = onClick
    }
}

@Composable
fun TopBar(
    items: List<TopBarItem>,
    modifier: Modifier = Modifier,
    showMainTabs: Boolean = false,
    selectedMainTab: MainTab? = null
) {
    val (colorPalette, typography) = LocalAppearance.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Buttons and IconButtons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                when (item) {
                    is TopBarItem.Button -> {
                        val onClick = item.onClickHandler
                        val enabled = item.isEnabled
                        SecondaryTextButton(
                            text = item.text(),
                            onClick = onClick,
                            enabled = enabled()
                        )
                    }
                    is TopBarItem.IconButton -> {
                        val onClick = item.onClickHandler
                        val isMusicButton = item.icon == R.drawable.musical_notes
                        val isPodcastButton = item.icon == R.drawable.podcast
                        val isSelected = when {
                            isMusicButton -> selectedMainTab == MainTab.Music
                            isPodcastButton -> selectedMainTab == MainTab.Podcast
                            else -> false
                        }

                        Image(
                            painter = painterResource(item.icon),
                            contentDescription = when {
                                isMusicButton -> stringResource(R.string.music)
                                isPodcastButton -> stringResource(R.string.podcast)
                                else -> null
                            },
                            colorFilter = ColorFilter.tint(
                                if (isSelected) colorPalette.text else colorPalette.textDisabled
                            ),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onClick()
                                }
                                .padding(all = 12.dp)
                                .size(22.dp)
                        )
                    }
                    is TopBarItem.Title -> {} // Title is handled separately
                }
            }
        }

        // Right side: Title
        items.find { it is TopBarItem.Title }?.let { titleItem ->
            (titleItem as TopBarItem.Title).let { title ->
                BasicText(
                    text = title.title(),
                    style = typography.xxl.medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}