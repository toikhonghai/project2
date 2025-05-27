package app.vitune.providers.innertube.models

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.json
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.parameters
import io.ktor.http.userAgent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.Locale
// file này được sử dụng để định nghĩa các lớp và đối tượng liên quan đến ngữ cảnh (context) của ứng dụng,
// bao gồm thông tin về client, người dùng và các dịch vụ bên thứ ba.
@Serializable
data class Context( // lớp Context này dùng để lưu thông tin về client và các thông tin khác liên quan đến request.
    val client: Client,
    val thirdParty: ThirdParty? = null,
    val user: User? = User()
) {
    @Serializable
    data class Client( // lớp này dùng để lưu thông tin về client (khách hàng) và các thông tin khác liên quan đến request.
        @Transient //dùng để đánh dấu một thuộc tính không được serialize (không lưu vào JSON, không ghi vào file...).
        val clientId: Int = 0,
        val clientName: String,
        val clientVersion: String,
        val platform: String? = null,
        val hl: String = "en",
        val gl: String = "US",
        @SerialName("visitorData")
        val defaultVisitorData: String = DEFAULT_VISITOR_DATA,
        val androidSdkVersion: Int? = null,
        val userAgent: String? = null,
        val referer: String? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val osName: String? = null,
        val osVersion: String? = null,
        val acceptHeader: String? = null,
        val timeZone: String? = "UTC",
        val utcOffsetMinutes: Int? = 0,
        @Transient
        val apiKey: String? = null,
        @Transient
        val music: Boolean = false
    ) {
        @Serializable
        data class Configuration( // lớp này dùng để lưu trữ cấu hình của client.
            @SerialName("PLAYER_JS_URL")
            val playerUrl: String? = null,
            @SerialName("WEB_PLAYER_CONTEXT_CONFIGS")
            val contextConfigs: Map<String, ContextConfig>? = null,
            @SerialName("VISITOR_DATA")
            val visitorData: String? = null,
            @SerialName("INNERTUBE_CONTEXT")
            val innertubeContext: Context
        ) {
            @Serializable
            data class ContextConfig(
                val jsUrl: String? = null
            )
        }

        @Transient
        private val mutex = Mutex() // Mutex được sử dụng để đồng bộ hóa truy cập đến biến ytcfg.

        @Transient
        private var ytcfg: Configuration? = null // ytcfg là một biến tạm thời để lưu trữ cấu hình của YouTube.

        private val baseUrl // là URL cơ sở của YouTube, được xác định dựa trên platform và music.
            get() = when {
                platform == "TV" -> "https://www.youtube.com/tv"
                music -> "https://music.youtube.com/"
                else -> "https://www.youtube.com/"
            }
        val root get() = if (music) "https://music.youtube.com/" else "https://www.youtube.com/"

        internal val jsUrl
            get() = ytcfg?.playerUrl
                ?: ytcfg?.contextConfigs?.firstNotNullOfOrNull { it.value.jsUrl }

        val visitorData // là một thuộc tính không thể thay đổi, nó sẽ lấy giá trị từ ytcfg hoặc từ defaultVisitorData.
            get() = ytcfg?.visitorData
                ?: ytcfg?.innertubeContext?.client?.defaultVisitorData
                ?: defaultVisitorData

        companion object { // Đối tượng này chứa các hằng số và regex cho lớp Client.
            private val YTCFG_REGEX = "ytcfg\\.set\\s*\\(\\s*(\\{[\\s\\S]+?\\})\\s*\\)".toRegex()
        }

        context(HttpMessageBuilder)
        fun apply() { // được gọi bên trong HttpMessageBuilder, giúp thiết lập header và tham số cho request.
            userAgent?.let { userAgent(it) } // Thiết lập User-Agent (trình giả lập trình duyệt)
            //userAgent giúp API nhận diện loại trình duyệt gửi request.

            headers {
                referer?.let { set("Referer", it) } // Chỉ định trang web trước đó (giúp giả lập request hợp lệ).
                set("X-Youtube-Bootstrap-Logged-In", "false")
                set("X-YouTube-Client-Name", clientId.toString())
                set("X-YouTube-Client-Version", clientVersion)
                apiKey?.let { set("X-Goog-Api-Key", it) }
                set("X-Goog-Visitor-Id", visitorData)
            }

            parameters {
                apiKey?.let { set("key", it) }
            }
        }

        suspend fun getConfiguration(): Configuration? = mutex.withLock { //mutex.withLock đảm bảo rằng không có 2 coroutine nào chạy hàm này cùng lúc.
            ytcfg ?: runCatching { // Nếu ytcfg chưa được khởi tạo, thì thực hiện khối lệnh bên trong runCatching.
                val playerPage = Innertube.client.get(baseUrl) { // Dùng Innertube.client.get(baseUrl) để gửi HTTP GET request đến YouTube.
                    userAgent?.let { header("User-Agent", it) } // Thiết lập User-Agent cho request.
                }.bodyAsText() // Lấy nội dung HTML của trang web.

                val objStr = YTCFG_REGEX // Sử dụng regex để tìm kiếm chuỗi JSON trong nội dung HTML.
                    .find(playerPage) // Tìm kiếm chuỗi JSON trong nội dung HTML.
                    ?.groups // Lấy các nhóm trong regex match.
                    ?.get(1) // Lấy nhóm đầu tiên (chuỗi JSON).
                    ?.value // Lấy giá trị của nhóm đầu tiên.
                    ?.trim() // Xóa khoảng trắng ở đầu và cuối chuỗi JSON.
                    ?.takeIf { it.isNotBlank() } ?: return@runCatching null // Nếu chuỗi JSON rỗng, trả về null.

                json.decodeFromString<Configuration>(objStr).also { ytcfg = it } // Giải mã chuỗi JSON thành đối tượng Configuration và lưu vào biến ytcfg.
            }.getOrElse { // Nếu có lỗi xảy ra trong quá trình lấy cấu hình, in ra lỗi và trả về null.
                it.printStackTrace() // In ra lỗi để kiểm tra.
                null // Trả về null nếu không thể lấy cấu hình.
            }
        }
    }

    @Serializable
    data class ThirdParty( // Lớp này dùng để lưu thông tin về các dịch vụ bên thứ ba.
        val embedUrl: String
    )

    @Serializable
    data class User( // Lớp này dùng để lưu thông tin về người dùng.
        val lockedSafetyMode: Boolean = false
    )

    context(HttpMessageBuilder)
    fun apply() = client.apply()

    companion object { // Đối tượng này chứa các hằng số và phương thức tiện ích cho lớp Context.
        private val Context.withLang: Context // là một thuộc tính mở rộng cho lớp Context, giúp thiết lập ngôn ngữ và quốc gia dựa trên ngôn ngữ mặc định của thiết bị.
            get() {
                val locale = Locale.getDefault() // Lấy ngôn ngữ mặc định của thiết bị.

                return copy(
                    client = client.copy(
                        hl = locale // là mã ngôn ngữ (language code) của thiết bị.
                            .toLanguageTag() // Chuyển đổi ngôn ngữ sang định dạng mã ngôn ngữ (language tag).
                            .replace("-Hant", "")
                            .takeIf { it in validLanguageCodes } ?: "en",
                        gl = locale
                            .country
                            .takeIf { it in validCountryCodes } ?: "US"
                    )
                )
            }
        const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D" // Mã visitor data mặc định được sử dụng trong các request đến YouTube.

        val DefaultWeb get() = DefaultWebNoLang.withLang // là một thuộc tính mở rộng cho lớp Context, giúp thiết lập ngôn ngữ và quốc gia dựa trên ngôn ngữ mặc định của thiết bị.

        val DefaultWebNoLang = Context( // lớp Context này dùng để lưu thông tin về client và các thông tin khác liên quan đến request.
            client = Client(
                clientId = 67,
                clientName = "WEB_REMIX",
                clientVersion = "1.20220606.03.00",
                platform = "DESKTOP",
                userAgent = UserAgents.DESKTOP,
                referer = "https://music.youtube.com/",
                music = true
            )
        )

        val DefaultIOS = Context( // lớp Context này dùng để lưu thông tin về client và các thông tin khác liên quan đến request.
            client = Client(
                clientId = 5,
                clientName = "IOS",
                clientVersion = "20.03.02",
                deviceMake = "Apple",
                deviceModel = "iPhone16,2",
                osName = "iPhone",
                osVersion = "18.2.1.22C161",
                acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                userAgent = UserAgents.IOS,
                apiKey = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
                music = false
            )
        )

        val DefaultAndroid = Context( // lớp Context này dùng để lưu thông tin về client và các thông tin khác liên quan đến request.
            client = Client(
                clientId = 3,
                clientName = "ANDROID",
                clientVersion = "19.44.38",
                osName = "Android",
                osVersion = "11",
                platform = "MOBILE",
                androidSdkVersion = 30,
                userAgent = UserAgents.ANDROID,
                apiKey = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
                music = false
            )
        )

        val DefaultAndroidMusic = Context( // lớp Context này dùng để lưu thông tin về client và các thông tin khác liên quan đến request.
            client = Client(
                clientId = 21,
                clientName = "ANDROID_MUSIC",
                clientVersion = "7.27.52",
                platform = "MOBILE",
                osVersion = "11",
                androidSdkVersion = 30,
                userAgent = UserAgents.ANDROID_MUSIC,
                apiKey = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
                music = true
            )
        )

        val DefaultTV = Context( // lớp Context này dùng để lưu thông tin về client và các thông tin khác liên quan đến request.
            client = Client(
                clientId = 7,
                clientName = "TVHTML5",
                clientVersion = "7.20241201.18.00",
                platform = "TV",
                userAgent = UserAgents.TV,
                referer = "https://www.youtube.com/",
                music = false
            )
        )

        val DefaultVietnam = Context(
            client = Client(
                clientId = 21,
                clientName = "ANDROID_MUSIC",
                clientVersion = "7.27.52",
                platform = "MOBILE",
                osVersion = "11",
                androidSdkVersion = 30,
                userAgent = UserAgents.ANDROID_MUSIC,
                apiKey = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
                music = true,
                hl = "vi",
                gl = "VN"
            )
        )
    }
}

// @formatter:off
@Suppress("MaximumLineLength")
val validLanguageCodes = // là danh sách các mã ngôn ngữ hợp lệ mà ứng dụng hỗ trợ.
    listOf("af", "az", "id", "ms", "ca", "cs", "da", "de", "et", "en-GB", "en", "es", "es-419", "eu", "fil", "fr", "fr-CA", "gl", "hr", "zu", "is", "it", "sw", "lt", "hu", "nl", "nl-NL", "no", "or", "uz", "pl", "pt-PT", "pt", "ro", "sq", "sk", "sl", "fi", "sv", "bo", "vi", "tr", "bg", "ky", "kk", "mk", "mn", "ru", "sr", "uk", "el", "hy", "iw", "ur", "ar", "fa", "ne", "mr", "hi", "bn", "pa", "gu", "ta", "te", "kn", "ml", "si", "th", "lo", "my", "ka", "am", "km", "zh-CN", "zh-TW", "zh-HK", "ja", "ko")

@Suppress("MaximumLineLength")
val validCountryCodes = // là danh sách các mã quốc gia hợp lệ mà ứng dụng hỗ trợ.
    listOf("DZ", "AR", "AU", "AT", "AZ", "BH", "BD", "BY", "BE", "BO", "BA", "BR", "BG", "KH", "CA", "CL", "HK", "CO", "CR", "HR", "CY", "CZ", "DK", "DO", "EC", "EG", "SV", "EE", "FI", "FR", "GE", "DE", "GH", "GR", "GT", "HN", "HU", "IS", "IN", "ID", "IQ", "IE", "IL", "IT", "JM", "JP", "JO", "KZ", "KE", "KR", "KW", "LA", "LV", "LB", "LY", "LI", "LT", "LU", "MK", "MY", "MT", "MX", "ME", "MA", "NP", "NL", "NZ", "NI", "NG", "NO", "OM", "PK", "PA", "PG", "PY", "PE", "PH", "PL", "PT", "PR", "QA", "RO", "RU", "SA", "SN", "RS", "SG", "SK", "SI", "ZA", "ES", "LK", "SE", "CH", "TW", "TZ", "TH", "TN", "TR", "UG", "UA", "AE", "GB", "US", "UY", "VE", "VN", "YE", "ZW")
// @formatter:on

//User-Agent (UA) là một chuỗi ký tự mà trình duyệt, ứng dụng hoặc thiết bị gửi kèm khi thực hiện yêu cầu HTTP (HTTP request).
// Chuỗi này giúp server nhận diện được loại thiết bị, hệ điều hành và phần mềm đang truy cập trang web hoặc API.
@Suppress("MaximumLineLength")
object UserAgents { // lớp này dùng để định nghĩa các User-Agent khác nhau cho các nền tảng khác nhau.
    const val DESKTOP = // là User-Agent cho trình duyệt trên máy tính để bàn.
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"
    const val ANDROID = "com.google.android.youtube/19.44.38 (Linux; U; Android 11) gzip"
    const val ANDROID_MUSIC = // là User-Agent cho ứng dụng YouTube Music trên Android.
        "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 11) gzip"
    const val PLAYSTATION = "Mozilla/5.0 (PlayStation 4 5.55) AppleWebKit/601.2 (KHTML, like Gecko)"
    const val IOS = "com.google.ios.youtube/20.03.02 (iPhone16,2; U; CPU iOS 18_2_1 like Mac OS X;)"
    const val TV = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Version"
}
