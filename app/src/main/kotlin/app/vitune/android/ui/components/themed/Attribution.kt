package app.vitune.android.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.utils.align
import app.vitune.android.utils.disabled
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.LocalAppearance

@Composable
// Hàm này hiển thị một đoạn văn bản với một phần trích dẫn từ Wikipedia.
fun Attribution(
    text: String,
    modifier: Modifier = Modifier
) = Column {
    val (_, typography) = LocalAppearance.current
    val windowInsets = LocalPlayerAwareWindowInsets.current // Lấy WindowInsets từ LocalPlayerAwareWindowInsets

    val endPaddingValues = windowInsets
        .only(WindowInsetsSides.End)
        .asPaddingValues() // Lấy padding ở cạnh phải màn hình (để tránh overlap với thanh điều khiển).

    val attributionsIndex = text.lastIndexOf("\n\n${stringResource(R.string.from_wikipedia)}") // Tìm vị trí của phần trích dẫn từ Wikipedia trong văn bản.

    var expanded by rememberSaveable { mutableStateOf(false) } // Trạng thái mở rộng của văn bản.
    var overflow by rememberSaveable { mutableStateOf(false) } // Kiểm tra xem văn bản có bị tràn không.

    AnimatedContent( // Sử dụng AnimatedContent để tạo hiệu ứng chuyển tiếp khi mở rộng hoặc thu gọn văn bản.
        targetState = expanded,
        label = ""
    ) { isExpanded ->
        Row(
            modifier = modifier
                .padding(endPaddingValues)
                .let {
                    if (overflow) it.clickable {
                        expanded = !expanded
                    } else it
                }
        ) {
            BasicText(
                text = stringResource(R.string.quote_open),
                style = typography.xxl.semiBold,
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .align(Alignment.Top)
            )
            BasicText(
                text = if (attributionsIndex == -1) text else text.substring(0, attributionsIndex), // Lấy phần văn bản trước phần trích dẫn.
                style = typography.xxs.secondary,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis, // Nếu văn bản bị tràn, hiển thị dấu ba chấm.
                onTextLayout = {
                    if (!expanded) overflow = it.hasVisualOverflow // Kiểm tra xem văn bản có bị tràn hay không.
                }
            )

            BasicText(
                text = stringResource(R.string.quote_close),
                style = typography.xxl.semiBold,
                modifier = Modifier
                    .offset(y = 4.dp)
                    .align(Alignment.Bottom)
            )
        }
    }

    if (attributionsIndex != -1) BasicText( // Nếu có phần trích dẫn từ Wikipedia, hiển thị nó.
        text = stringResource(R.string.wikipedia_cc_by_sa),
        style = typography.xxs.disabled.align(TextAlign.End),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .padding(endPaddingValues)
    )
}
