package app.vitune.core.ui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
//Gui 1 thong bao (intent) khi am luong thay doi
fun Context.streamVolumeFlow(
    stream: Int = AudioManager.STREAM_MUSIC, // Mặc định theo dõi âm lượng của STREAM_MUSIC
    @ContextCompat.RegisterReceiverFlags
    flags: Int = ContextCompat.RECEIVER_NOT_EXPORTED // Đảm bảo receiver không bị xuất ra ngoài app
) = callbackFlow { // Sử dụng callbackFlow để lắng nghe sự kiện thay đổi âm lượng và gửi dữ liệu vào Flow
    val receiver = object : BroadcastReceiver() { // Định nghĩa BroadcastReceiver ẩn danh
        override fun onReceive(context: Context, intent: Intent) { // Khi nhận được Intent
            val extras = intent.extras?.volumeChangedIntentBundle ?: return // Lấy thông tin từ Intent, nếu null thì return
            if (stream == extras.streamType) trySend(extras.value) // Nếu stream đúng với loại đang theo dõi, gửi giá trị âm lượng mới vào Flow
        }
    }

    ContextCompat.registerReceiver(
        /* context = */ this@Context, // Đăng ký receiver với context hiện tại
        /* receiver = */ receiver, // Đối tượng receiver lắng nghe sự kiện
        /* filter = */ IntentFilter(VolumeChangedIntentBundleAccessor.ACTION), // Lắng nghe sự kiện thay đổi âm lượng
        /* flags = */ flags // Cờ xác định cách đăng ký receiver
    )
    awaitClose { unregisterReceiver(receiver) } // Khi Flow bị đóng, hủy đăng ký receiver để tránh rò rỉ bộ nhớ
}
//Lay thong tin tu intent
class VolumeChangedIntentBundleAccessor(val bundle: Bundle = Bundle()) : BundleAccessor { // Lớp hỗ trợ truy cập dữ liệu từ Bundle
    companion object {
        const val ACTION = "android.media.VOLUME_CHANGED_ACTION" // Hằng số đại diện cho Intent thay đổi âm lượng

        fun bundle(block: VolumeChangedIntentBundleAccessor.() -> Unit) =
            VolumeChangedIntentBundleAccessor().apply(block).bundle // Hỗ trợ tạo Bundle dễ dàng
    }

    var streamType by bundle.int("android.media.EXTRA_VOLUME_STREAM_TYPE") // Loại stream bị thay đổi
    var value by bundle.int("android.media.EXTRA_VOLUME_STREAM_VALUE") // Giá trị âm lượng mới
}

inline val Bundle.volumeChangedIntentBundle get() = VolumeChangedIntentBundleAccessor(this)
