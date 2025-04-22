package app.vitune.android.ui.components.themed

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.compose.reordering.ReorderingState
import app.vitune.compose.reordering.reorder
import app.vitune.core.ui.LocalAppearance

@Composable
// hàm này tạo ra một nút có biểu tượng để thay đổi thứ tự của các mục trong danh sách
fun ReorderHandle(
    reorderingState: ReorderingState,
    index: Int,
    modifier: Modifier = Modifier
) = IconButton(
    icon = R.drawable.reorder,
    color = LocalAppearance.current.colorPalette.textDisabled,
    indication = null,
    onClick = {},
    modifier = modifier
        .reorder( // hàm này cho phép người dùng thay đổi thứ tự của các mục trong danh sách
            reorderingState = reorderingState,
            index = index
        )
        .size(18.dp)
)
