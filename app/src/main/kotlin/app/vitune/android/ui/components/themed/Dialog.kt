package app.vitune.android.ui.components.themed

import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.vitune.android.R
import app.vitune.android.utils.center
import app.vitune.android.utils.drawCircle
import app.vitune.android.utils.medium
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.Appearance
import app.vitune.core.ui.BuiltInFontFamily
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.defaultLightPalette
import app.vitune.core.ui.typographyOf
import app.vitune.core.ui.utils.roundedShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay

@Composable
fun TextFieldDialog(
    hintText: String, // Gợi ý hiển thị trong TextField
    onDismiss: () -> Unit, // Hàm gọi khi dialog bị đóng
    onAccept: (String) -> Unit, // Hàm gọi khi nhấn nút xác nhận (Done)
    modifier: Modifier = Modifier, // Modifier tuỳ chỉnh giao diện
    cancelText: String = stringResource(R.string.cancel), // Văn bản nút Cancel (mặc định lấy từ resources)
    doneText: String = stringResource(R.string.done), // Văn bản nút Done (mặc định lấy từ resources)
    initialTextInput: String = "", // Giá trị mặc định ban đầu trong TextField
    singleLine: Boolean = true, // Cho phép nhập 1 dòng hay nhiều dòng
    maxLines: Int = 1, // Số dòng tối đa của TextField
    onCancel: () -> Unit = onDismiss, // Hàm gọi khi nhấn Cancel (mặc định là đóng dialog)
    isTextInputValid: (String) -> Boolean = { it.isNotEmpty() }, // Kiểm tra giá trị nhập có hợp lệ không
    keyboardOptions: KeyboardOptions = KeyboardOptions() // Cấu hình bàn phím ảo (IME)
) = DefaultDialog(
    onDismiss = onDismiss, // Đóng dialog
    modifier = modifier
) {
    val focusRequester = remember { FocusRequester() } // Dùng để yêu cầu focus tự động vào TextField
    val (_, typography) = LocalAppearance.current // Lấy font chữ từ theme hiện tại

    var value by rememberSaveable(initialTextInput) { mutableStateOf(initialTextInput) }
    // Biến lưu giá trị nhập vào, được lưu qua recomposition

    LaunchedEffect(Unit) {
        delay(300) // Delay nhẹ để tránh xung đột khi mở bàn phím
        focusRequester.requestFocus() // Tự động focus vào TextField
    }

    TextField(
        value = value, // Giá trị hiện tại
        onValueChange = { value = it }, // Khi thay đổi giá trị nhập
        textStyle = typography.xs.semiBold.center, // Kiểu chữ: nhỏ, đậm, căn giữa
        singleLine = singleLine,
        maxLines = maxLines,
        hintText = hintText,
        keyboardActions = KeyboardActions(
            onDone = {
                if (isTextInputValid(value)) {
                    onDismiss() // Đóng dialog nếu hợp lệ
                    onAccept(value) // Gửi giá trị lên callback
                }
            }
        ),
        keyboardOptions = keyboardOptions, // đây
        modifier = Modifier
            .padding(all = 16.dp)
            .weight(weight = 1f, fill = false)
            .focusRequester(focusRequester) // Gắn focus requester vào TextField
    )

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        DialogTextButton(
            text = cancelText,
            onClick = onCancel // Nhấn Cancel thì gọi onCancel
        )

        DialogTextButton(
            primary = true,
            text = doneText,
            onClick = {
                if (isTextInputValid(value)) {
                    onAccept(value)
                    onDismiss()
                }
            }
        )
    }
}

@Composable
fun <T> NumberFieldDialog( // Dialog cho phép nhập số kiểu T (T là Number và Comparable)
    onDismiss: () -> Unit, // Hàm được gọi khi đóng dialog
    onAccept: (T) -> Unit, // Hàm được gọi khi nhấn xác nhận, trả về giá trị kiểu T
    initialValue: T, // Giá trị ban đầu được hiển thị trong TextField
    defaultValue: T, // Giá trị mặc định nếu người dùng nhập sai
    convert: (String) -> T?, // Hàm chuyển đổi chuỗi nhập vào thành kiểu T
    range: ClosedRange<T>, // Phạm vi giá trị cho phép
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    doneText: String = stringResource(R.string.done),
    onCancel: () -> Unit = onDismiss
) where T : Number, T : Comparable<T> = TextFieldDialog( // Dùng lại TextFieldDialog ở trên
    hintText = "",
    onDismiss = onDismiss,
    onAccept = {
        // Chuyển đổi chuỗi nhập => ép về default nếu null => ép nằm trong khoảng range
        onAccept((convert(it) ?: defaultValue).coerceIn(range))
    },
    modifier = modifier,
    cancelText = cancelText,
    doneText = doneText,
    initialTextInput = initialValue.toString(), // Hiển thị initial value trong TextField
    onCancel = onCancel,
    isTextInputValid = { true }, // Luôn cho là hợp lệ, xử lý hợp lệ ở phần convert + coerceIn
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // Chỉ cho nhập số
)


@Composable
fun ConfirmationDialog(
    text: String, // Nội dung hiển thị trong dialog
    onDismiss: () -> Unit, // Hàm gọi khi đóng dialog
    onConfirm: () -> Unit, // Hàm gọi khi người dùng xác nhận
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    confirmText: String = stringResource(R.string.confirm),
    onCancel: () -> Unit = onDismiss
) = DefaultDialog(
    onDismiss = onDismiss,
    modifier = modifier
) {
    // Gọi phần thân của dialog
    ConfirmationDialogBody(
        text = text,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        cancelText = cancelText,
        confirmText = confirmText,
        onCancel = onCancel
    )
}


@Suppress("ModifierMissing", "UnusedReceiverParameter") //
@Composable
fun ColumnScope.ConfirmationDialogBody( // Phần thân của dialog xác nhận
    text: String, // Nội dung hiển thị
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    cancelText: String = stringResource(R.string.cancel),
    confirmText: String = stringResource(R.string.confirm),
    onCancel: () -> Unit = onDismiss
) {
    val (_, typography) = LocalAppearance.current // Lấy font chữ từ theme

    BasicText(
        text = text,
        style = typography.xs.medium.center, // Font cỡ nhỏ, medium, căn giữa
        modifier = Modifier.padding(all = 16.dp)
    )

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly, // Các nút giãn đều
        modifier = Modifier.fillMaxWidth()
    ) {
        DialogTextButton(
            text = cancelText,
            onClick = {
                onCancel() // Gọi hàm hủy (tuỳ chỉnh)
                onDismiss() // Đóng dialog
            }
        )

        DialogTextButton(
            text = confirmText,
            primary = true, // Nút chính
            onClick = {
                onConfirm() // Gọi hành động xác nhận
                onDismiss() // Đóng dialog
            }
        )
    }
}


@Composable
fun DefaultDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    horizontalPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) = Dialog(onDismissRequest = onDismiss) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
            .padding(all = 48.dp)
            .background(
                color = LocalAppearance.current.colorPalette.background1,
                shape = 8.dp.roundedShape
            )
            .padding(
                horizontal = horizontalPadding,
                vertical = 16.dp
            ),
        content = content
    )
}

@Composable
fun <T> ValueSelectorDialog(
    onDismiss: () -> Unit,
    title: String,
    selectedValue: T,
    values: ImmutableList<T>,
    onValueSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    valueText: @Composable (T) -> String = { it.toString() }
) = Dialog(onDismissRequest = onDismiss) {
    ValueSelectorDialogBody(
        onDismiss = onDismiss,
        title = title,
        selectedValue = selectedValue,
        values = values,
        onValueSelect = onValueSelect,
        modifier = modifier
            .padding(all = 48.dp)
            .background(
                color = LocalAppearance.current.colorPalette.background1,
                shape = 8.dp.roundedShape
            )
            .padding(vertical = 16.dp),
        valueText = valueText
    )
}

@Composable
fun <T> ValueSelectorDialogBody(
    onDismiss: () -> Unit,
    title: String,
    selectedValue: T?,
    values: ImmutableList<T>,
    onValueSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    valueText: @Composable (T) -> String = { it.toString() }
) = Column(modifier = modifier) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = title,
        style = typography.s.semiBold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
    )

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        values.forEach { value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .clickable(
                        onClick = {
                            onDismiss()
                            onValueSelect(value)
                        }
                    )
                    .padding(vertical = 12.dp, horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                if (selectedValue == value) Canvas(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            color = colorPalette.accent,
                            shape = CircleShape
                        )
                ) {
                    drawCircle(
                        color = colorPalette.onAccent,
                        radius = 4.dp.toPx(),
                        center = size.center,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.4f),
                            blurRadius = 4.dp.toPx(),
                            offset = Offset(x = 0f, y = 1.dp.toPx())
                        )
                    )
                } else Spacer(
                    modifier = Modifier
                        .size(18.dp)
                        .border(
                            width = 1.dp,
                            color = colorPalette.textDisabled,
                            shape = CircleShape
                        )
                )

                BasicText(
                    text = valueText(value),
                    style = typography.xs.medium
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.End)
            .padding(end = 24.dp)
    ) {
        DialogTextButton(
            text = stringResource(R.string.cancel),
            onClick = onDismiss
        )
    }
}

@Suppress("ModifierMissing") // intentional, I guess
@Composable
fun ColumnScope.SliderDialogBody(
    provideState: @Composable () -> MutableState<Float>,
    onSlideComplete: (newState: Float) -> Unit,
    min: Float,
    max: Float,
    toDisplay: @Composable (Float) -> String = { it.toString() },
    @IntRange(from = 0) steps: Int = 0,
    label: String? = null
) {
    val (_, typography) = LocalAppearance.current
    var state by provideState()

    if (label != null) BasicText(
        text = label,
        style = typography.xs.semiBold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
    )

    Slider(
        state = state,
        setState = { state = it },
        onSlideComplete = { onSlideComplete(state) },
        range = min..max,
        steps = steps,
        modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )

    BasicText(
        text = toDisplay(state),
        style = typography.s.semiBold,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun SliderDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = { }
) = Dialog(onDismissRequest = onDismiss) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(
        modifier = modifier
            .padding(all = 48.dp)
            .background(color = colorPalette.background1, shape = 8.dp.roundedShape)
            .padding(vertical = 16.dp)
    ) {
        BasicText(
            text = title,
            style = typography.s.semiBold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
        )

        content()

        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 24.dp)
        ) {
            DialogTextButton(
                text = stringResource(R.string.confirm),
                onClick = onDismiss,
                modifier = Modifier
            )
        }
    }
}

