package app.vitune.android.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.surface

@Composable
// Hàm này tạo ra một nút hình tròn với biểu tượng bên trong, thường được sử dụng để thực hiện các hành động phụ hoặc bổ sung trong giao diện người dùng.
fun SecondaryButton(
    onClick: () -> Unit,
    @DrawableRes iconId: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val (colorPalette) = LocalAppearance.current

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .background(colorPalette.surface)
            .size(48.dp)
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colorPalette.text), // nhuộm màu toàn bộ ảnh (icon) theo màu chỉ định.
            modifier = Modifier
                .align(Alignment.Center)
                .size(18.dp)
        )
    }
}
