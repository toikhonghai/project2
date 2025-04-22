package app.vitune.android.utils

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.util.Log
import android.util.Rational
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.OnPictureInPictureModeChangedProvider
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.graphics.toRect
import app.vitune.android.R
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.compose.persist.findActivityNullable
import app.vitune.core.ui.utils.isAtLeastAndroid12
import app.vitune.core.ui.utils.isAtLeastAndroid7
import app.vitune.core.ui.utils.isAtLeastAndroid8

//PIP (viết tắt của Picture-in-Picture) là chế độ hiển thị cửa sổ nổi nhỏ,
// cho phép một ứng dụng hiển thị video hoặc nội dung nào đó ở dạng thu nhỏ, nổi lên trên các ứng dụng khác — thường dùng để xem video trong khi làm việc khác.
private fun logError(throwable: Throwable) = Log.e("PipHandler", "An error occurred", throwable) // Ghi log lỗi với tag "PipHandler" khi một exception xảy ra, dùng Log.e.

@Suppress("DEPRECATION")
fun Activity.maybeEnterPip() = when {
    !isAtLeastAndroid7 -> false // API < 24 không hỗ trợ PiP
    !packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) -> false // thiết bị không hỗ trợ tính năng PiP
    else -> runCatching {
        if (isAtLeastAndroid8)
            enterPictureInPictureMode(PictureInPictureParams.Builder().build()) // Android 8+ dùng API mới
        else
            enterPictureInPictureMode() // Android 7 dùng API cũ
    }.onFailure(::logError).isSuccess // log lỗi nếu thất bại
}

fun Activity.setAutoEnterPip(autoEnterIfPossible: Boolean) = if (isAtLeastAndroid12)
    setPictureInPictureParams(
        PictureInPictureParams.Builder()
            .setAutoEnterEnabled(autoEnterIfPossible) // Android 12+ cho phép PiP tự động khi điều kiện phù hợp
            .build()
    )
else Unit


fun Activity.setPipParams(
    rect: Rect, // Hình chữ nhật đại diện vùng focus
    targetNumerator: Int, // tỉ lệ width
    targetDenominator: Int, // tỉ lệ height
    autoEnterIfPossible: Boolean = AppearancePreferences.autoPip, // lấy giá trị mặc định từ preferences
    block: PictureInPictureParams.Builder.() -> PictureInPictureParams.Builder = { this } // cho phép cấu hình thêm nếu cần
) {
    if (isAtLeastAndroid8)
        setPictureInPictureParams(
            PictureInPictureParams.Builder()
                .block() // cho phép cấu hình tùy biến
                .setSourceRectHint(rect) // hint cho phần focus trong PiP
                .setAspectRatio(Rational(targetNumerator, targetDenominator)) // thiết lập tỷ lệ khung hình
                .let {
                    if (isAtLeastAndroid12) it
                        .setAutoEnterEnabled(autoEnterIfPossible)
                        .setSeamlessResizeEnabled(false) // disable auto resize (mặc định true Android 12)
                    else it
                }
                .build()
        )
}

fun Activity.maybeExitPip() = when {
    !isAtLeastAndroid7 -> false // không hỗ trợ
    !isInPictureInPictureMode -> false // không đang ở trong chế độ PiP
    else -> runCatching {
        moveTaskToBack(false) // đưa app về nền
        application.startActivity(
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) // gọi lại Activity để thoát PiP
        )
    }.onFailure(::logError).isSuccess
}

@Composable
fun rememberPipHandler(key: Any = Unit): PipHandler {
    // Lấy context hiện tại (thường là Activity)
    val context = LocalContext.current

    // Dùng remember để lấy Activity từ context và giữ nó ổn định trong recomposition
    val activity = remember(context) { context.findActivityNullable() }

    // Tạo và nhớ đối tượng PipHandler, tự động giữ nguyên nếu activity và key không đổi
    return remember(activity, key) {
        PipHandler(
            // Hàm gọi để vào chế độ Picture-in-Picture (PIP)
            enterPip = { activity?.maybeEnterPip() },

            // Hàm gọi để thoát khỏi chế độ PIP
            exitPip = { activity?.maybeExitPip() }
        )
    }
}


@Immutable
// PipHandler chứa logic để vào/thoát chế độ Picture-in-Picture (PiP)
data class PipHandler internal constructor(
    private val enterPip: () -> Boolean?, // Hàm lambda để gọi vào PiP mode
    private val exitPip: () -> Boolean?   // Hàm lambda để gọi thoát khỏi PiP mode
) {
    fun enterPictureInPictureMode() = enterPip() == true // Trả về true nếu vào PiP thành công
    fun exitPictureInPictureMode() = exitPip() == true   // Trả về true nếu thoát PiP thành công
}

// Extension property để kiểm tra nếu activity đang ở chế độ PiP (Android >= 7)
private val Activity?.pip get() = if (isAtLeastAndroid7) this?.isInPictureInPictureMode == true else false

@Composable
// Composable này theo dõi trạng thái PiP và gọi callback khi có thay đổi
fun isInPip(
    onChange: (Boolean) -> Unit = { } // Hàm callback khi trạng thái PiP thay đổi
): Boolean {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivityNullable() } // Tìm Activity từ Context
    val currentOnChange by rememberUpdatedState(onChange) // Giữ callback luôn mới
    var pip by rememberSaveable { mutableStateOf(activity.pip) } // Trạng thái PiP hiện tại

    DisposableEffect(activity, currentOnChange) { // Đăng ký listener để theo dõi trạng thái PiP
        // Nếu activity không hỗ trợ PiP mode listener thì không làm gì cả
        if (activity !is OnPictureInPictureModeChangedProvider) return@DisposableEffect onDispose { }

        // Tạo listener để cập nhật trạng thái khi vào/thoát PiP
        val listener: (PictureInPictureModeChangedInfo) -> Unit = {
            pip = it.isInPictureInPictureMode
            currentOnChange(pip)
        }

        activity.addOnPictureInPictureModeChangedListener(listener) // Đăng ký listener

        onDispose {
            activity.removeOnPictureInPictureModeChangedListener(listener) // Huỷ đăng ký khi composable bị huỷ
        }
    }

    return pip // Trả về trạng thái hiện tại của PiP
}

// Modifier giúp cập nhật tham số PiP và hành động (RemoteAction) khi view được bố trí
fun Modifier.pip(
    activity: Activity,
    targetNumerator: Int, // Tỉ lệ khung hình: tử số (VD: 16)
    targetDenominator: Int, // Tỉ lệ khung hình: mẫu số (VD: 9)
    actions: ActionReceiver? = null // Tập hợp các hành động sẽ hiển thị trong chế độ PiP
) = this.onGloballyPositioned { layoutCoordinates ->
    // Khi layout được định vị, cập nhật thông số PiP
    activity.setPipParams(
        rect = layoutCoordinates.boundsInWindow().toAndroidRectF().toRect(),
        targetNumerator = targetNumerator,
        targetDenominator = targetDenominator
    ) {
        // Nếu có actions thì set chúng vào trong PiP params
        if (actions != null) setActions(
            actions.all.values.map {
                RemoteAction(
                    it.icon ?: Icon.createWithResource(activity, R.drawable.ic_launcher_foreground),
                    it.title.orEmpty(),
                    it.contentDescription.orEmpty(),
                    with(activity) { it.pendingIntent }
                )
            }
        ) else this
    }
}

@Composable
// Composable bao bọc nội dung và áp dụng chế độ PiP tự động
fun Pip(
    numerator: Int, // Ví dụ: 16 (tỉ lệ PiP)
    denominator: Int, // Ví dụ: 9 (tỉ lệ PiP)
    modifier: Modifier = Modifier,
    actions: ActionReceiver? = null, // Hành động trong PiP (như Play/Pause)
    content: @Composable BoxScope.() -> Unit // Nội dung hiển thị trong PiP
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(context, actions) {
        val currentActions = actions ?: return@DisposableEffect onDispose { }
        currentActions.register(context) // Đăng ký broadcast receiver để nhận các action
        onDispose {
            context.unregisterReceiver(currentActions) // Bỏ đăng ký khi không dùng nữa
            activity.setAutoEnterPip(false) // Tắt chế độ tự động vào PiP
        }
    }

    // Dùng Box để bọc nội dung, áp dụng modifier.pip
    Box(
        modifier = modifier.pip(
            activity = activity,
            targetNumerator = numerator,
            targetDenominator = denominator,
            actions = actions
        ),
        content = content
    )
}

@Composable
// Hỗ trợ hiệu ứng chuyển cảnh giữa các state có key khác nhau và giữ lại state tương ứng
fun <T : Any> KeyedCrossfade(
    state: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "",
        modifier = modifier
    ) { currentState ->
        saveableStateHolder.SaveableStateProvider(key = currentState) {
            content(currentState) // Render nội dung tương ứng với state hiện tại
        }
    }
}
