package app.vitune.providers.innertube

import app.vitune.providers.innertube.models.Context
import app.vitune.providers.innertube.models.MusicNavigationButtonRenderer
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.innertube.models.Runs
import app.vitune.providers.innertube.models.Thumbnail
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.brotli
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.host
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.http.parseQueryString
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
/*
internal là một modifier (phạm vi truy cập - visibility modifier) trong Kotlin.
Nó được sử dụng để giới hạn phạm vi truy cập của một thành phần (biến, hàm, class, object, v.v.) chỉ trong cùng một module.
 plugin là một tập hợp các chức năng mở rộng giúp tùy chỉnh và xử lý HTTP request/response.
    - Ktor là một framework cho việc xây dựng ứng dụng web và client HTTP trong Kotlin.
    - OkHttp là một thư viện HTTP client phổ biến trong Android và Java.
     Origin là nguồn gốc (domain + protocol + port) của một trang web hoặc API request.
 */
//Lớp này dùng để chứa thông tin về các mã lỗi xảy ra trong quá trình xử lý HTTP request/response.
internal val json = Json {
    ignoreUnknownKeys = true //ỏ qua các khóa (keys) không xác định trong JSON.
    explicitNulls = false // Không encode các giá trị null khi chuyển đổi sang JSON.
    encodeDefaults = true // Luôn encode giá trị mặc định của các thuộc tính vào JSON.,  Nếu một thuộc tính có giá trị mặc định, nó vẫn sẽ xuất hiện trong JSON thay vì bị bỏ qua.
}

//Lớp này dùng để chứa thông tin về mã JavaScript challenge từ YouTube.
object Innertube {
    private var javascriptChallenge: JavaScriptChallenge? = null //dùng để giải mã signature hoặc xử lý bảo vệ JavaScript của YouTube.

    private val OriginInterceptor = createClientPlugin("OriginInterceptor") { //Tạo một plugin mới có tên là "OriginInterceptor".
        client.sendPipeline.intercept(HttpSendPipeline.State) { //Chặn (intercept) pipeline của request trước khi nó được gửi đi.
            context.headers {
                val host =
                    if (context.host == "youtubei.googleapis.com") "www.youtube.com" else context.host
                val origin = "${context.url.protocol.name}://$host" //Lấy giao thức (http hoặc https) từ context.url.protocol.name.
                set("host", host)
                set("x-origin", origin) //Header tùy chỉnh, thường dùng để giả lập request đến từ trình duyệt web.
                set("origin", origin) //Header chuẩn dùng để kiểm soát CORS (Cross-Origin Resource Sharing).
                //YouTube API có thể yêu cầu header "x-origin" để xác thực request hợp lệ.
            }
        }
    }
    //Dùng thư viện LoggerFactory để tạo logger, giúp ghi log các hoạt động của Innertube.
    val logger: Logger = LoggerFactory.getLogger(Innertube::class.java)
    val baseClient = HttpClient(OkHttp) {
        expectSuccess = true // Ném lỗi nếu HTTP response không thành công.

        //Nếu HTTP response có mã lỗi không hợp lệ (code không nằm trong 100-599) thì ném
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                val ex = cause as? ResponseException ?: return@handleResponseExceptionWithRequest
                val code = ex.response.status.value
                if (code !in (100..<600)) throw InvalidHttpCodeException(code)
            }
        }

        install(ContentNegotiation) {//Cài đặt ContentNegotiation để dùng JSON serialization.
            json(json)
        }

        install(ContentEncoding) {//Cài đặt ContentEncoding để hỗ trợ nén dữ liệu (Brotli, Gzip, Deflate).
            brotli(1.0f)
            gzip(0.9f)
            deflate(0.8f)
        }

        install(Logging) { //Cài đặt Logging để ghi log request/response.
            level = LogLevel.INFO
        }

        install(OriginInterceptor)//Cài đặt OriginInterceptor để chỉnh sửa header request.
    }
    val client = baseClient.config { // Cấu hình client với các plugin và cài đặt mặc định.
        defaultRequest {
            url(scheme = "https", host = "music.youtube.com") { // Đặt giao thức là HTTPS và host là music.youtube.com.
                contentType(ContentType.Application.Json) // Đặt kiểu nội dung là JSON.
                headers {
                    set("X-Goog-Api-Key", API_KEY) //Header này thường được yêu cầu bởi YouTube API để xác thực request.
                }
                parameters { // Thêm các tham số vào URL.
                    set("prettyPrint", "false")
                    set("key", API_KEY)
                }
            }
        }
    }

    // Danh sách các biểu thức chính quy (Regex) có thể được dùng để phân tích cú pháp JavaScript của YouTube.
    //Giải mã signature của YouTube video khi bị mã hóa.
    //Phân tích JavaScript của YouTube để trích xuất các thông tin quan trọng.
    @Suppress("all")
    private val regexes = listOf(
        "\\bm=([a-zA-Z0-9$]{2,})\\(decodeURIComponent\\(h\\.s\\)\\)".toRegex(),
        "\\bc&&\\(c=([a-zA-Z0-9$]{2,})\\(decodeURIComponent\\(c\\)\\)".toRegex(),
        "(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2,})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)".toRegex(),
        "([\\w$]+)\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)\\s*;".toRegex()
    )

    private suspend fun getJavaScriptChallenge(context: Context): JavaScriptChallenge? { //Lấy mã JavaScript challenge từ YouTube.
        if (javascriptChallenge != null) return javascriptChallenge

        context.client.getConfiguration() // Lấy cấu hình của client.
        val jsUrl = context.client.jsUrl ?: return null // Lấy URL của JavaScript challenge từ cấu hình client.

        //Gửi yêu cầu HTTP để tải file JavaScript và chuyển nó thành chuỗi văn bản (bodyAsText()).
        val sourceFile = baseClient // Tạo một client mới để gửi yêu cầu.
            .get("${context.client.root}$jsUrl") {
                context.apply()
            }
            .bodyAsText() // Lấy nội dung của response dưới dạng chuỗi văn bản.

        //Dùng regex để tìm signature timestamp trong nội dung JavaScript. Nếu không tìm thấy, trả về null.
        val timestamp = "(?:signatureTimestamp|sts):(\\d{5})".toRegex() // Biểu thức chính quy để tìm timestamp trong mã JavaScript.
            .find(sourceFile)
            ?.groups
            ?.get(1)
            ?.value
            ?.trim()
            ?.takeIf { it.isNotBlank() } ?: return null
        //Dùng regex để tìm tên hàm giải mã trong mã JavaScript. Nếu không tìm thấy, trả về null.
        val functionName = regexes.firstNotNullOfOrNull { regex -> // Tìm kiếm trong mã JavaScript để lấy tên hàm giải mã.
            regex
                .find(sourceFile)
                ?.groups
                ?.get(1)
                ?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        } ?: return null

        return JavaScriptChallenge( // Tạo một đối tượng JavaScriptChallenge với các thông tin đã tìm được.
            source = sourceFile
                .replace("document.location.hostname", "\"youtube.com\"")
                .replace("window.location.hostname", "\"youtube.com\"")
                .replace("XMLHttpRequest.prototype.fetch", "\"aaa\""), // Thay thế các đoạn mã không cần thiết trong mã JavaScript.
            timestamp = timestamp, // Thời gian giải mã.
            functionName = functionName // Tên hàm giải mã.
        ).also { javascriptChallenge = it } // Lưu lại đối tượng JavaScriptChallenge để sử dụng sau này.
    }

    //Hàm này giải mã cipher signature để lấy URL thực của video.
    suspend fun decodeSignatureCipher(context: Context, cipher: String): String? = runCatchingCancellable { // Giải mã cipher.
        val params = parseQueryString(cipher) // Phân tích cú pháp chuỗi cipher thành các tham số.
        val signature = params["s"] ?: return@runCatchingCancellable null // Lấy chữ ký từ chuỗi cipher.
        val signatureParam = params["sp"] ?: return@runCatchingCancellable null // Lấy tham số chữ ký từ chuỗi cipher.
        val url = params["url"] ?: return@runCatchingCancellable null // Lấy URL từ chuỗi cipher.

        val actualSignature = getJavaScriptChallenge(context)?.decode(signature)// Giải mã chữ ký bằng hàm decode trong mã JavaScript.
            ?: return@runCatchingCancellable null
        "$url&$signatureParam=$actualSignature"
    }?.onFailure { it.printStackTrace() }?.getOrNull()

    suspend fun getSignatureTimestamp(context: Context): String? = runCatchingCancellable { // Lấy dấu thời gian giải mã từ mã JavaScript.
        getJavaScriptChallenge(context)?.timestamp
    }?.onFailure { it.printStackTrace() }?.getOrNull()

    private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"

    ////Các endpoint API dùng để lấy thông tin về video, tìm kiếm, lấy danh sách phát,...
    private const val BASE = "/youtubei/v1"
    internal const val BROWSE = "$BASE/browse"
    internal const val NEXT = "$BASE/next"
    internal const val PLAYER = "https://youtubei.googleapis.com/youtubei/v1/player"
    internal const val PLAYER_MUSIC = "$BASE/player"
    internal const val QUEUE = "$BASE/music/get_queue"
    internal const val SEARCH = "$BASE/search"
    internal const val SEARCH_SUGGESTIONS = "$BASE/music/get_search_suggestions"
    internal const val MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK =
        "musicResponsiveListItemRenderer(flexColumns,fixedColumns,thumbnail,navigationEndpoint,badges)"
    internal const val MUSIC_TWO_ROW_ITEM_RENDERER_MASK =
        "musicTwoRowItemRenderer(thumbnailRenderer,title,subtitle,navigationEndpoint)"

    @Suppress("MaximumLineLength")
    internal const val PLAYLIST_PANEL_VIDEO_RENDERER_MASK =
        "playlistPanelVideoRenderer(title,navigationEndpoint,longBylineText,shortBylineText,thumbnail,lengthText,badges)" //Lớp này dùng để chứa thông tin về video trong danh sách phát.

    internal const val PODCAST_RENDERER_MASK =
        "musicResponsiveListItemRenderer(flexColumns,thumbnail,navigationEndpoint,badges,menu)"

    internal const val PODCAST_EPISODE_RENDERER_MASK =
        "musicResponsiveListItemRenderer(flexColumns,fixedColumns,thumbnail,navigationEndpoint,badges,menu,playlistItemData)"

    //Dùng để thêm header "X-Goog-FieldMask" vào HTTP request, giúp ẩn dữ liệu trả về chỉ lấy thông tin cần thiết.
    internal fun HttpRequestBuilder.mask(value: String = "*") =
        header("X-Goog-FieldMask", value)

    //Lớp này dùng để chứa thông tin về endpoint của video, danh sách phát,...
    @Serializable
    data class Info<T : NavigationEndpoint.Endpoint>(
        val name: String?,
        val endpoint: T?
    ) {
        @Suppress("UNCHECKED_CAST")
        constructor(run: Runs.Run) : this(
            name = run.text,
            endpoint = run.navigationEndpoint?.endpoint as T?
        )
    }

    //Lớp này dùng để chứa thông tin về các tham số tìm kiếm trong API.
    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val Song = SearchFilter("EgWKAQIIAWoOEAMQBBAJEAoQBRAQEBU%3D")
            val Video = SearchFilter("EgWKAQIQAWoOEAMQBBAJEAoQBRAQEBU%3D")
            val Album = SearchFilter("EgWKAQIYAWoOEAMQBBAJEAoQBRAQEBU%3D")
            val Artist = SearchFilter("EgWKAQIgAWoOEAMQBBAJEAoQBRAQEBU%3D")
            val CommunityPlaylist = SearchFilter("EgeKAQQoAEABag4QAxAEEAkQChAFEBAQFQ%3D%3D")
            val Podcast = SearchFilter("EgWKAQJQAWoOEAMQBBAJEAoQBRAQEBU%3D") // Added podcast filter
        }
    }

    //sealed trong Kotlin được sử dụng để hạn chế kế thừa
    //Lớp này dùng để chứa thông tin về các hình ảnh thumbnail của video, danh sách phát,...
    sealed class Item {
        abstract val thumbnail: Thumbnail?
        abstract val key: String
    }

    //Lớp này dùng để chứa thông tin về các bài hát trong danh sách phát, album,...
    @Serializable
    data class SongItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val album: Info<NavigationEndpoint.Endpoint.Browse>?,
        val durationText: String?,
        val explicit: Boolean,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.videoId!!

        companion object
    }

    //Lớp này dùng để chứa thông tin về các video trong danh sách phát, album,...
    data class VideoItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val viewsText: String?,
        val durationText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.videoId!!

        val isOfficialMusicVideo: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_OMV"

        companion object
    }

    //Lớp này dùng để chứa thông tin về các album trong danh sách phát, album,...
    @Serializable
    data class AlbumItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    //Lớp này dùng để chứa thông tin về các nghệ sĩ trong danh sách phát, album,...
    @Serializable
    data class ArtistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val subscribersCountText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    //Lớp này dùng để chứa thông tin về các danh sách phát trong danh sách phát, album,...
    @Serializable
    data class PlaylistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val channel: Info<NavigationEndpoint.Endpoint.Browse>?,
        val songCount: Int?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.browseId!!

        companion object
    }

    @Serializable
    data class PodcastItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val description: String?,
        val episodeCount: Int?,
        override val thumbnail: Thumbnail?
    ) : Item(){
        override val key get() = info!!.endpoint!!.browseId!!

        companion object // companion object lay ra một đối tượng tĩnh trong lớp,
    // cho phép truy cập vào các thuộc tính và phương thức tĩnh mà không cần tạo một thể hiện của lớp.
    }

    @Serializable
    data class PodcastEpisodeItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val podcast: Info<NavigationEndpoint.Endpoint.Browse>?,
        val durationText: String?, // Thời gian phát của tập podcast
        val publishedTimeText: String?, // Thời gian phát hành của tập podcast
        val description: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info!!.endpoint!!.videoId!!
        companion object
    }

    //Lớp này dùng để chứa thông tin về các trang trong ứng dụng YouTube Music.
    data class ArtistPage(
        val name: String?,
        val description: String?,
        val thumbnail: Thumbnail?,
        val shuffleEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val radioEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val songs: List<SongItem>?,
        val songsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val albums: List<AlbumItem>?,
        val albumsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val singles: List<AlbumItem>?,
        val singlesEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val subscribersCountText: String?
    )

    //Lớp này dùng để chứa thông tin về các trang danh sách phát hoặc album trong ứng dụng YouTube Music.
    data class PlaylistOrAlbumPage(
        val title: String?,
        val description: String?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        val thumbnail: Thumbnail?,
        val url: String?,
        val songsPage: ItemsPage<SongItem>?,
        val otherVersions: List<AlbumItem>?,
        val otherInfo: String?
    )

    //Lớp này dùng để chứa thông tin về các trang video trong ứng dụng YouTube Music.
    data class NextPage(
        val itemsPage: ItemsPage<SongItem>?,
        val playlistId: String?,
        val params: String? = null,
        val playlistSetVideoId: String? = null
    )

    // Trang chi tiết của một podcast: gồm tiêu đề, mô tả, ảnh, nút đăng ký, và danh sách tập
    data class PodcastPage(
        val title: String?, // Tiêu đề podcast
        val description: String?, // Mô tả ngắn gọn
        val author: Info<NavigationEndpoint.Endpoint.Browse>?, // Tác giả chính (channel)
        val thumbnail: Thumbnail?, // Ảnh đại diện podcast
        val subscriptionButton: SubscriptionButton?, // Nút đăng ký / huỷ đăng ký
        val episodes: ItemsPage<PodcastEpisodeItem>? // Danh sách các tập của podcast
    ) {
        // ✅ Lớp con đại diện cho nút đăng ký kênh podcast
        @Serializable
        data class SubscriptionButton(
            val subscribed: Boolean, // Đã đăng ký hay chưa
            val subscribedButtonText: String?, // Văn bản khi đã đăng ký (vd: "Đã đăng ký")
            val unsubscribedButtonText: String?, // Văn bản khi chưa đăng ký (vd: "Đăng ký")
            val channelId: String? // ID của kênh
        )
    }

    //Lớp này dùng để chứa thông tin về các trang tìm kiếm trong ứng dụng YouTube Music.
    @Serializable
    data class RelatedPage(
        val songs: List<SongItem>? = null,
        val playlists: List<PlaylistItem>? = null,
        val albums: List<AlbumItem>? = null,
        val artists: List<ArtistItem>? = null,
        val podcasts: List<PodcastItem>? = null
    )

    //Lớp này dùng để chứa thông tin về các trang khám phá trong ứng dụng YouTube Music.
    data class DiscoverPage(
        val newReleaseAlbums: List<AlbumItem>,
        val moods: List<Mood.Item>,
        val trending: Trending
    ) {
        data class Trending(
            val songs: List<SongItem>,
            val endpoint: NavigationEndpoint.Endpoint.Browse?
        )
    }

    //Lớp này dùng để chứa thông tin về các trang khám phá trong ứng dụng YouTube Music.
    data class Mood(
        val title: String,
        val items: List<Item>
    ) {
        data class Item( // Lớp này dùng để chứa thông tin về các trang tìm kiếm trong ứng dụng YouTube Music.
            val title: String,
            val stripeColor: Long, // Màu sắc của thanh bên trái
            val endpoint: NavigationEndpoint.Endpoint.Browse // Endpoint để điều hướng đến trang tương ứng
        ) : Innertube.Item() {
            override val thumbnail get() = null
            override val key
                get() = "${endpoint.browseId.orEmpty()}${endpoint.params?.let { "/$it" }.orEmpty()}"

            companion object
        }
    }

    //Lớp này dùng để chứa thông tin về các trang tìm kiếm trong ứng dụng YouTube Music.
    fun MusicNavigationButtonRenderer.toMood(): Mood.Item? {
        return Mood.Item(
            title = buttonText.runs.firstOrNull()?.text ?: return null,
            stripeColor = solid?.leftStripeColor ?: return null,
            endpoint = clickCommand.browseEndpoint ?: return null
        )
    }

    //Lớp này dùng để chứa thông tin về các trang tìm kiếm trong ứng dụng YouTube Music.
    data class ItemsPage<T : Item>(
        val items: List<T>?,
        val continuation: String?
    )
    
}

//Lớp này dùng để chứa thông tin về các lỗi xảy ra trong quá trình xử lý HTTP request/response.
data class InvalidHttpCodeException(val code: Int) :
    IllegalStateException("Invalid http code received: $code")

