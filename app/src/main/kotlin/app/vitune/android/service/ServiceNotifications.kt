package app.vitune.android.service

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.media3.common.util.NotificationUtil.Importance
import androidx.media3.common.util.UnstableApi
import app.vitune.android.R
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.absoluteValue
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

// Lớp trừu tượng để quản lý Notification Channels
abstract class NotificationChannels {
    // Handler để thực hiện các thao tác trong Main Thread
    private val handler = Handler(Looper.getMainLooper())

    // Đại diện cho một Notification Channel cụ thể
    @OptIn(UnstableApi::class)
    inner class Channel internal constructor(
        val id: String, // ID của channel
        @StringRes val description: Int, // Mô tả dạng tài nguyên chuỗi
        val notificationId: Int? = null, // ID cố định cho notification (nếu là singleNotification)
        val importance: @Importance Int, // Mức độ ưu tiên
        val options: NotificationChannelCompat.Builder.() -> NotificationChannelCompat.Builder // DSL để tùy chỉnh
    ) {
        // Extension để lấy NotificationManager từ context
        private val Context.notificationManager
            get() = getSystemService<NotificationManager>()
                ?: error("No NotificationManager available")

        // Tạo ra Notification với notificationId và builder truyền vào
        private fun createNotification(
            context: Context,
            notification: NotificationCompat.Builder.() -> NotificationCompat.Builder
        ): Pair<Int, Notification> =
            (notificationId ?: randomNotificationId()) to NotificationCompat.Builder(context, id)
                .let {
                    if (notificationId == null) it else it.setOnlyAlertOnce(false) // Chỉ cảnh báo 1 lần nếu có ID
                }
                .run(notification)
                .build()

        // Tạo hoặc cập nhật channel trong hệ thống
        fun upsertChannel(context: Context) = NotificationManagerCompat
            .from(context)
            .createNotificationChannel(
                NotificationChannelCompat.Builder(id, importance)
                    .setName(context.getString(description)) // Đặt tên kênh
                    .run(options) // Tuỳ chỉnh thêm nếu có
                    .build()
            )

        // Gửi notification (chạy trên Main Thread)
        fun sendNotification(
            context: Context,
            notification: NotificationCompat.Builder.() -> NotificationCompat.Builder
        ) = runCatching {
            handler.post {
                val manager = context.notificationManager
                upsertChannel(context) // Đảm bảo channel tồn tại
                val (id, notif) = createNotification(context, notification)
                manager.notify(id, notif)
            }
        }

        // Dùng cho service - hiển thị notification foreground
        context(Service)
        fun startForeground(
            context: Context,
            notification: NotificationCompat.Builder.() -> NotificationCompat.Builder
        ) = runCatching {
            handler.post {
                upsertChannel(context)
                val (id, notif) = createNotification(context, notification)
                startForeground(id, notif)
            }
        }

        // Huỷ thông báo đã gửi
        fun cancel(
            context: Context,
            notificationId: Int? = null
        ) = runCatching {
            handler.post {
                context.notificationManager.cancel((this.notificationId ?: notificationId)!!)
            }
        }
    }

    // Danh sách lưu các channel đã khởi tạo
    private val mutableChannels = mutableListOf<Channel>()

    // Biến để tạo ra các notificationId duy nhất (trong khoảng 1001..2001)
    private val index = AtomicInteger(1001)

    // Sinh notificationId ngẫu nhiên ngoài khoảng [1001..2001] để tránh đụng
    private fun randomNotificationId(): Int {
        var random = Random.nextInt().absoluteValue
        while (random in 1001..2001) {
            random = Random.nextInt().absoluteValue
        }
        return random
    }

    // Hàm gọi trong Application để tạo toàn bộ các channel đã đăng ký
    context(Application)
    fun createAll() = handler.post {
        mutableChannels.forEach { it.upsertChannel(this@Application) }
    }

    // DSL để khai báo channel dưới dạng delegate `val x by channel(...)`
    @OptIn(UnstableApi::class)
    fun channel(
        name: String? = null,
        @StringRes description: Int,
        importance: @Importance Int,
        singleNotification: Boolean, // Nếu true thì gắn 1 ID duy nhất
        options: NotificationChannelCompat.Builder.() -> NotificationChannelCompat.Builder = { this }
    ) = readOnlyProvider<NotificationChannels, Channel> { _, property ->
        val channel = Channel(
            id = "${name?.lowercase() ?: property.name.lowercase()}_channel_id", // Tạo id dựa theo tên biến
            description = description,
            notificationId = if (singleNotification) index.getAndIncrement().also {
                if (it > 2001) error("More than 1000 unique notifications created!")
            } else null,
            importance = importance,
            options = options
        )
        mutableChannels += channel
        { _, _ -> channel } // Trả về provider cho property delegate
    }
}

// Provider để tạo delegate cho các thuộc tính không thay đổi (readonly)
inline fun <ThisRef, Return> readOnlyProvider( // ThisRef là kiểu của đối tượng chứa thuộc tính, Return là kiểu trả về của thuộc tính
    crossinline provide: (
        thisRef: ThisRef,
        property: KProperty<*>
    ) -> (thisRef: ThisRef, property: KProperty<*>) -> Return
) = PropertyDelegateProvider<ThisRef, ReadOnlyProperty<ThisRef, Return>> { thisRef, property ->
    val provider = provide(thisRef, property)
    ReadOnlyProperty { innerThisRef, innerProperty -> provider(innerThisRef, innerProperty) }
}

// Đối tượng cụ thể chứa các channel của Service, sử dụng NotificationChannels
object ServiceNotifications : NotificationChannels() {

    // Channel thông báo mặc định (ví dụ đang phát nhạc)
    val default by channel(
        description = R.string.now_playing,
        importance = NotificationManagerCompat.IMPORTANCE_LOW,
        singleNotification = true
    )

    // Thông báo sleep timer
    val sleepTimer by channel(
        name = "sleep_timer",
        description = R.string.sleep_timer,
        importance = NotificationManagerCompat.IMPORTANCE_LOW,
        singleNotification = true
    )

    // Kênh thông báo tải dữ liệu
    val download by channel(
        description = R.string.pre_cache,
        importance = NotificationManagerCompat.IMPORTANCE_LOW,
        singleNotification = true
    )

    val podcastDownload by channel(
        name = "podcast_download",
        description = R.string.new_podcast_episode,
        importance = NotificationManagerCompat.IMPORTANCE_LOW,
        singleNotification = true
    )
    val podcastNewEpisodes by channel(
        name = "podcast_new_episodes",
        description = R.string.new_podcast_episodes,
        importance = NotificationManagerCompat.IMPORTANCE_DEFAULT,
        singleNotification = true
    ) {
        setLightsEnabled(true).setVibrationEnabled(true)
    }

    // Thông báo có phiên bản mới
    val version by channel(
        description = R.string.version_check,
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
        singleNotification = true
    ) {
        setLightsEnabled(true).setVibrationEnabled(true) // Tuỳ chỉnh kênh có rung/đèn
    }

    // Thông báo khi auto skip do lỗi
    val autoSkip by channel(
        name = "autoskip",
        description = R.string.skip_on_error,
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
        singleNotification = true
    ) {
        setLightsEnabled(true).setVibrationEnabled(true)
    }
}
