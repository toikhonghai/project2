package app.vitune.android.ui.components.themed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.core.ui.Appearance
import app.vitune.core.ui.BuiltInFontFamily
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.defaultLightPalette
import app.vitune.core.ui.typographyOf
import app.vitune.core.ui.utils.roundedShape

@Composable
// `Menu` là một thành phần giao diện dạng cột, dùng để hiển thị danh sách các mục trong menu.
inline fun Menu(
    modifier: Modifier = Modifier, // Modifier để tùy chỉnh giao diện.
    shape: Shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp), // Định hình góc trên của menu.
    content: @Composable ColumnScope.() -> Unit // Nội dung của menu.
) = Column(
    modifier = modifier
        .fillMaxWidth() // Menu sẽ chiếm toàn bộ chiều rộng.
        .clip(shape) // Cắt bo tròn góc trên của menu.
        .verticalScroll(rememberScrollState()) // Cho phép cuộn menu khi nội dung dài.
        .background(LocalAppearance.current.colorPalette.background1) // Thiết lập màu nền.
        .padding(top = 2.dp) // Khoảng cách nhỏ phía trên.
        .padding(vertical = 8.dp) // Khoảng cách trên dưới của menu.
        .navigationBarsPadding(), // Tự động thêm padding để tránh bị che bởi thanh điều hướng.
    content = content
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
// `MenuEntry` là một mục trong menu, có biểu tượng, văn bản và có thể nhấn.
fun MenuEntry(
    @DrawableRes icon: Int, // ID của icon hiển thị bên trái.
    text: String, // Văn bản chính của mục menu.
    onClick: () -> Unit, // Hàm xử lý khi người dùng nhấn vào mục menu.
    modifier: Modifier = Modifier, // Modifier để tùy chỉnh giao diện.
    secondaryText: String? = null, // Văn bản phụ (tùy chọn).
    enabled: Boolean = true, // Xác định mục menu có thể nhấn hay không.
    onLongClick: (() -> Unit)? = null, // Hàm xử lý khi người dùng nhấn giữ (tùy chọn).
    trailingContent: (@Composable () -> Unit)? = null // Thành phần tùy chỉnh hiển thị ở bên phải (tùy chọn).
) {
    val (colorPalette, typography) = LocalAppearance.current // Lấy màu sắc và kiểu chữ từ theme hiện tại.

    Row(
        verticalAlignment = Alignment.CenterVertically, // Căn chỉnh các thành phần theo chiều dọc.
        horizontalArrangement = Arrangement.spacedBy(24.dp), // Khoảng cách giữa các thành phần.
        modifier = modifier
            .combinedClickable( // Cho phép nhấn hoặc nhấn giữ.
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .fillMaxWidth() // Mục menu sẽ chiếm toàn bộ chiều rộng.
            .alpha(if (enabled) 1f else 0.4f) // Nếu mục bị vô hiệu hóa, nó sẽ mờ đi.
            .padding(horizontal = 24.dp) // Thêm khoảng cách bên trái và bên phải.
    ) {
        // Hiển thị icon của mục menu.
        Image(
            painter = painterResource(icon),
            contentDescription = null, // Không cần mô tả truy cập.
            colorFilter = ColorFilter.tint(colorPalette.text), // Áp dụng màu từ theme.
            modifier = Modifier.size(15.dp) // Kích thước cố định của icon.
        )

        // Cột chứa văn bản chính và văn bản phụ.
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp) // Thêm khoảng cách trên dưới.
                .weight(1f) // Chiếm toàn bộ không gian còn lại.
        ) {
            // Hiển thị văn bản chính.
            BasicText(
                text = text,
                style = typography.xs.medium
            )

            // Hiển thị văn bản phụ nếu có.
            secondaryText?.let { secondaryText ->
                BasicText(
                    text = secondaryText,
                    style = typography.xxs.medium.secondary
                )
            }
        }

        // Hiển thị nội dung tùy chỉnh ở bên phải nếu có.
        trailingContent?.invoke()
    }
}

@Preview(
    name = "Dialog",
    showSystemUi = true,
    device = Devices.PIXEL_4_XL
)
@Composable
fun MenuPreview() {
    val colorPalette = defaultLightPalette
    val typography = typographyOf(
        color = colorPalette.text,
        fontFamily = BuiltInFontFamily.System,
        applyFontPadding = false
    )
    val appearance = Appearance(
        colorPalette = colorPalette,
        typography = typography,
        thumbnailShapeCorners = 8.dp
    )
    CompositionLocalProvider(LocalAppearance provides appearance) {
        Surface(
            color = colorPalette.background1,
            shape = 8.dp.roundedShape
        ) {
            MenuEntry(icon = app.vitune.android.R.drawable.search, text = "Test", onClick = { }, modifier = Modifier.padding(16.dp))
        }
    }
}
