package app.vitune.android.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.offline.DownloadService.sendAddDownload
import app.vitune.android.BuildConfig
import app.vitune.core.ui.utils.isAtLeastAndroid11
import app.vitune.core.ui.utils.isAtLeastAndroid6

// Tạo Intent chung từ Context đến class T
inline fun <reified T> Context.intent(): Intent = Intent(this@Context, T::class.java)

// Tạo PendingIntent để phát broadcast
inline fun <reified T : BroadcastReceiver> Context.broadcastPendingIntent(
    requestCode: Int = 0,
    flags: Int = if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0
): PendingIntent = PendingIntent.getBroadcast(this, requestCode, intent<T>(), flags)

// Tạo PendingIntent để mở Activity
inline fun <reified T : Activity> Context.activityPendingIntent(
    requestCode: Int = 0,
    @PendingIntentCompat.Flags flags: Int = 0,
    block: Intent.() -> Unit = { }
) = pendingIntent(
    intent = intent<T>().apply(block),
    requestCode = requestCode,
    flags = flags
)

// Hàm hỗ trợ tạo PendingIntent cho Activity, có xử lý `FLAG_IMMUTABLE` nếu Android >= 6
fun Context.pendingIntent(
    intent: Intent,
    requestCode: Int = 0,
    @PendingIntentCompat.Flags flags: Int = 0
): PendingIntent = PendingIntent.getActivity(
    /* context = */ this,
    /* requestCode = */ requestCode,
    /* intent = */ intent,
    /* flags = */ (if (isAtLeastAndroid6) PendingIntent.FLAG_IMMUTABLE else 0) or flags
)

// Kiểm tra xem app có bị hạn chế bởi battery optimization không
val Context.isIgnoringBatteryOptimizations
    get() = !isAtLeastAndroid6 ||
            getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(packageName) ?: true

// Hiển thị toast với message và thời lượng tuỳ chọn
fun Context.toast(
    message: String,
    duration: ToastDuration = ToastDuration.Short
) = Toast.makeText(this, message, duration.length).show()

// Wrapper để gom Toast.LENGTH_SHORT và Toast.LENGTH_LONG
@JvmInline
value class ToastDuration private constructor(internal val length: Int) {
    companion object {
        val Short = ToastDuration(length = Toast.LENGTH_SHORT)
        val Long = ToastDuration(length = Toast.LENGTH_LONG)
    }
}

// Mở link YouTube Music bằng ứng dụng thay vì trình duyệt (nếu có)
fun launchYouTubeMusic(
    context: Context,
    endpoint: String,
    tryWithoutBrowser: Boolean = true
): Boolean {
    return try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://music.youtube.com/${endpoint.dropWhile { it == '/' }}")
        ).apply {
            if (tryWithoutBrowser && isAtLeastAndroid11) {
                flags = Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
            }
        }

        // Chỉ định app nào sẽ xử lý, tránh mở chính app hiện tại
        intent.`package` =
            context.applicationContext.packageManager.queryIntentActivities(intent, 0)
                .firstOrNull {
                    it?.activityInfo?.packageName != null &&
                            BuildConfig.APPLICATION_ID !in it.activityInfo.packageName
                }?.activityInfo?.packageName
                ?: return false

        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        // Thử lại không giới hạn browser nếu lần đầu thất bại
        if (tryWithoutBrowser) launchYouTubeMusic(
            context = context,
            endpoint = endpoint,
            tryWithoutBrowser = false
        ) else false
    }
}

// Truy tìm Activity từ Context nếu có lồng ContextWrapper
fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("Should be called in the context of an Activity")
}

// Kiểm tra xem app có quyền cụ thể không
fun Context.hasPermission(permission: String) = ContextCompat.checkSelfPermission(
    applicationContext,
    permission
) == PackageManager.PERMISSION_GRANTED

// Gửi yêu cầu tải xuống bằng ExoPlayer DownloadService
@OptIn(UnstableApi::class)
inline fun <reified T : DownloadService> Context.download(request: DownloadRequest) = runCatching {
    sendAddDownload(
        /* context         = */ this,
        /* clazz           = */ T::class.java,
        /* downloadRequest = */ request,
        /* foreground      = */ true // true: tải xuống trong foreground
    )
}.recoverCatching {
    sendAddDownload(
        /* context         = */ this,
        /* clazz           = */ T::class.java,
        /* downloadRequest = */ request,
        /* foreground      = */ false // false: tải xuống trong background
    )
}
