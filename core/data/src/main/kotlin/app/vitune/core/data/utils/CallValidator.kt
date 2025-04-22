package app.vitune.core.data.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.os.Process
import androidx.annotation.XmlRes
import java.security.MessageDigest

/**
 * Stateful caller validator for Android intents, based on XML caller data
 */
class CallValidator(
    context: Context,
    @XmlRes callerList: Int // Danh sách các ứng dụng được phép gọi, lấy từ XML
) {
    private val packageManager = context.packageManager // Quản lý gói ứng dụng

    // Danh sách trắng (whitelist) các ứng dụng được phép gọi
    private val whitelist = runCatching {
        context.resources.getXml(callerList)
    }.getOrNull()?.let(Whitelist::parse) ?: Whitelist()

    // Lấy chữ ký của hệ thống Android (dùng để xác thực các ứng dụng hệ thống)
    private val systemSignature = getPackageInfo("android")?.signature

    // Bộ nhớ đệm cache giúp giảm số lần truy vấn PackageManager (tránh hiệu suất kém)
    private val cache = mutableMapOf<Pair<String, Int>, Boolean>()

    // Hàm kiểm tra xem một ứng dụng (theo package name và UID) có quyền gọi hay không
    fun canCall(pak: String, uid: Int) = cache.getOrPut(pak to uid) cache@{
        // Lấy thông tin gói ứng dụng
        val info = getPackageInfo(pak) ?: return@cache false

        // Kiểm tra UID của ứng dụng có khớp với UID được truyền vào không
        if (info.applicationInfo?.uid != uid) return@cache false

        // Lấy chữ ký của ứng dụng
        val signature = info.signature ?: return@cache false

        // Danh sách các quyền mà ứng dụng đã yêu cầu và được cấp phép
        val permissions = info.requestedPermissions?.filterIndexed { index, _ ->
            info
                .requestedPermissionsFlags
                ?.getOrNull(index)
                ?.let { it and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 } == true
        }

        // Xác thực theo nhiều tiêu chí khác nhau
        when {
            uid == Process.myUid() -> true // Nếu là chính ứng dụng đang chạy, cho phép
            uid == Process.SYSTEM_UID -> true // Nếu là UID của hệ thống, cho phép
            signature == systemSignature -> true // Nếu ứng dụng có chữ ký hệ thống, cho phép
            whitelist.isWhitelisted(pak, signature) -> true // Nếu có trong danh sách trắng, cho phép
            permissions != null && Manifest.permission.MEDIA_CONTENT_CONTROL in permissions -> true // Nếu có quyền điều khiển phương tiện, cho phép
            permissions != null && Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE in permissions -> true // Nếu có quyền nghe thông báo, cho phép
            else -> false // Nếu không thỏa bất kỳ điều kiện nào, từ chối
        }
    }

    // Hàm lấy thông tin gói ứng dụng từ PackageManager (hỗ trợ các phiên bản Android cũ)
    @Suppress("DEPRECATION") // backwards compat
    private fun getPackageInfo(
        pak: String,
        flags: Int = PackageManager.GET_SIGNATURES or PackageManager.GET_PERMISSIONS
    ) = runCatching {
        packageManager.getPackageInfo(
            /* packageName = */ pak,
            /* flags = */ flags
        )
    }.getOrNull()

    // Hàm lấy chữ ký SHA-256 của ứng dụng từ PackageInfo
    @Suppress("DEPRECATION") // backwards compat
    private val PackageInfo.signature
        get() = signatures?.let { signatures ->
            if (signatures.size != 1) null // Nếu có nhiều chữ ký, trả về null (không hợp lệ)
            else signatures.firstOrNull()?.toByteArray()?.sha256
        }

    // Hàm chuyển đổi chữ ký của ứng dụng thành chuỗi SHA-256
    @Suppress("ImplicitDefaultLocale") // not relevant
    private val ByteArray.sha256: String?
        get() = runCatching {
            val md = MessageDigest.getInstance("SHA256") // Tạo bộ băm SHA-256
            md.update(this) // Băm dữ liệu
            md.digest() // Lấy kết quả băm
        }.getOrNull()?.joinToString(":") { String.format("%02x", it) } // Định dạng thành chuỗi hex
}

// Định nghĩa một lớp giá trị (value class) để biểu diễn danh sách trắng (whitelist)
// @JvmInline tối ưu hóa bộ nhớ bằng cách không tạo instance wrapper trừ khi cần thiết
@JvmInline
value class Whitelist(private val map: WhitelistMap = mapOf()) {

    companion object {
        // Hàm phân tích file XML chứa danh sách trắng và trả về đối tượng Whitelist
        fun parse(parser: XmlResourceParser) = Whitelist(
            buildMap {
                runCatching {
                    var event = parser.next() // Đọc sự kiện đầu tiên trong XML

                    // Lặp qua toàn bộ tài liệu XML
                    while (event != XmlResourceParser.END_DOCUMENT) {
                        // Nếu gặp thẻ <signature>, gọi hàm putV2Tag để xử lý
                        if (event == XmlResourceParser.START_TAG && parser.name == "signature")
                            putV2Tag(parser)

                        event = parser.next() // Đọc tiếp sự kiện XML
                    }
                }
            }
        )

        // Hàm này xử lý thẻ <signature> trong XML và thêm vào danh sách trắng
        private fun MutableMap<String, Set<Key>>.putV2Tag(parser: XmlResourceParser) =
            runCatching {
                // Lấy giá trị thuộc tính "package" từ thẻ <signature>
                val pak = parser.getAttributeValue(
                    /* namespace = */ null,
                    /* name = */ "package"
                )

                // Tạo tập hợp các khóa (Key) chứa thông tin chữ ký
                val keys = buildSet {
                    var event = parser.next()
                    while (event != XmlResourceParser.END_TAG) {
                        add(
                            Key(
                                release = parser.getAttributeBooleanValue(
                                    /* namespace = */ null,
                                    /* attribute = */ "release",
                                    /* defaultValue = */ false
                                ),
                                signature = parser
                                    .nextText() // Lấy nội dung chữ ký
                                    .replace(WHITESPACE_REGEX, "") // Xóa khoảng trắng và xuống dòng
                                    .lowercase() // Chuyển thành chữ thường để tránh lỗi so sánh
                            )
                        )

                        event = parser.next() // Đọc sự kiện tiếp theo
                    }
                }

                // Lưu vào danh sách trắng
                put(
                    key = pak,
                    value = keys
                )
            }
    }

    // Hàm kiểm tra xem một ứng dụng có trong danh sách trắng hay không
    fun isWhitelisted(pak: String, signature: String) =
        map[pak]?.first { it.signature == signature } != null

    // Lớp dữ liệu để lưu thông tin chữ ký ứng dụng
    data class Key(
        val signature: String, // Chữ ký số SHA-256 của ứng dụng
        val release: Boolean // Xác định đây là bản phát hành hay bản debug
    )
}

// Định nghĩa kiểu alias cho WhitelistMap, giúp mã ngắn gọn hơn
typealias WhitelistMap = Map<String, Set<Whitelist.Key>>

// Biểu thức chính quy để xóa khoảng trắng và ký tự xuống dòng
private val WHITESPACE_REGEX = "\\s|\\n".toRegex()
