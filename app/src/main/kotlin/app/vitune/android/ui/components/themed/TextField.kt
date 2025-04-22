package app.vitune.android.ui.components.themed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.Appearance
import app.vitune.core.ui.LocalAppearance

@Composable
fun ColumnScope.TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    appearance: Appearance = LocalAppearance.current,
    textStyle: TextStyle = appearance.typography.xs.semiBold,
    singleLine: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default, // Hành động bàn phím mặc định
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        imeAction = if (singleLine) ImeAction.Done else ImeAction.None // Hành động bàn phím IME
        // Nếu singleLine là true, thì hành động IME sẽ là Done, ngược lại sẽ là None
    ),
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None, // Cho hiệu ứng hiển thị text (ví dụ: ẩn mật khẩu)
    onTextLayout: (TextLayoutResult) -> Unit = { },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }, // Theo dõi các tương tác (focus, pressed, hovered, etc.)
    hintText: String? = null
) = BasicTextField(
    value = value, // Giá trị hiện tại của TextField
    onValueChange = onValueChange, // Callback khi người dùng thay đổi nội dung
    modifier = modifier, // Modifier để tùy chỉnh layout, padding, size,...
    enabled = enabled, // Cho phép người dùng nhập hay không
    readOnly = readOnly, // Text chỉ đọc, không thể chỉnh sửa
    keyboardOptions = keyboardOptions, // Cấu hình bàn phím (IME action,...)
    keyboardActions = keyboardActions, // Xử lý hành vi khi nhấn phím (như Done, Search,...)
    textStyle = textStyle, // Style văn bản bên trong TextField
    singleLine = singleLine, // Nếu true: chỉ cho nhập 1 dòng
    maxLines = maxLines, // Số dòng tối đa có thể hiển thị
    minLines = minLines, // Số dòng tối thiểu (dùng cho TextField nhiều dòng)
    visualTransformation = visualTransformation, // Dùng để ẩn nội dung (ví dụ mật khẩu)
    onTextLayout = onTextLayout, // Callback nhận thông tin layout của văn bản (vd: chiều cao, dòng,...)
    interactionSource = interactionSource, // Theo dõi trạng thái người dùng tương tác (focus, pressed,...)

    // Tùy chỉnh màu con trỏ (cursor)
    cursorBrush = SolidColor(appearance.colorPalette.text),

    // `decorationBox` cho phép thêm thành phần UI phụ xung quanh TextField
    decorationBox = { innerTextField ->

        // Nếu `hintText` khác null thì render phần gợi ý (placeholder)
        hintText?.let { text ->

            // Sử dụng AnimatedVisibility để hiện/ẩn hintText với hiệu ứng mượt
            this@TextField.AnimatedVisibility(
                visible = value.isEmpty(), // Chỉ hiện nếu không có nội dung
                enter = fadeIn(tween(100)), // Hiệu ứng hiện vào
                exit = fadeOut(tween(100)), // Hiệu ứng biến mất
                modifier = Modifier.weight(1f) // Trọng số dùng khi nằm trong Column/Row
            ) {
                BasicText(
                    text = text, // Nội dung gợi ý
                    maxLines = 1, // Chỉ hiển thị một dòng
                    overflow = TextOverflow.Ellipsis, // Nếu quá dài sẽ hiện "..."
                    style = textStyle.secondary // Dùng style phụ cho hint (nhẹ hơn text chính)
                )
            }
        }

        // Thành phần văn bản nhập thực tế sẽ được hiển thị ở đây
        innerTextField
    }
)

@Composable
fun RowScope.TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    appearance: Appearance = LocalAppearance.current,
    textStyle: TextStyle = appearance.typography.xs.semiBold,
    singleLine: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        imeAction = if (singleLine) ImeAction.Done else ImeAction.None
    ),
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = { },
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    hintText: String? = null
) = BasicTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    enabled = enabled,
    readOnly = readOnly,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    textStyle = textStyle,
    singleLine = singleLine,
    maxLines = maxLines,
    minLines = minLines,
    visualTransformation = visualTransformation,
    onTextLayout = onTextLayout,
    interactionSource = interactionSource,
    cursorBrush = SolidColor(appearance.colorPalette.text),
    decorationBox = { innerTextField ->
        hintText?.let { text ->
            this@TextField.AnimatedVisibility(
                visible = value.isEmpty(),
                enter = fadeIn(tween(100)),
                exit = fadeOut(tween(100)),
                modifier = Modifier.weight(1f)
            ) {
                BasicText(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = textStyle.secondary
                )
            }
        }

        innerTextField()
    }
)
