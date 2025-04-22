package app.vitune.android.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.vitune.core.ui.LocalAppearance

@Composable
// HeaderIconButton là một hàm composable để tạo nút icon ở đầu trang
fun HeaderIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false) // hiệu ứng gợn son khi người dùng nhấn vào nút, bounded = false có nghĩa là hiệu ứng này sẽ không bị giới hạn trong một vùng nhất định
) {
    val (colorPalette) = LocalAppearance.current

    HeaderIconButton( // HeaderIconButton là một hàm composable để tạo nút icon ở đầu trang
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        indication = indication,
        enabled = true,
        color = if (enabled) colorPalette.text else colorPalette.textDisabled
    )
}

@Composable
fun HeaderIconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) = IconButton(
    icon = icon,
    color = color,
    onClick = onClick,
    enabled = enabled,
    indication = indication,
    modifier = modifier
        .padding(all = 4.dp)
        .size(18.dp)
)

@Composable
fun IconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) {
    val (colorPalette) = LocalAppearance.current

    IconButton(
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        indication = indication,
        enabled = true,
        color = if (enabled) colorPalette.text else colorPalette.textDisabled
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    indication: Indication? = ripple(bounded = false)
) = Image(
    painter = painterResource(icon),
    contentDescription = null,
    colorFilter = ColorFilter.tint(color),
    modifier = Modifier
        .clickable(
            indication = indication,
            interactionSource = remember { MutableInteractionSource() },
            enabled = enabled,
            onClick = onClick
        )
        .then(modifier)
)
