package app.vitune.providers.lrclib

import app.vitune.providers.lrclib.models.Track
import app.vitune.providers.lrclib.models.bestMatchingFor
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val AGENT = "ViTune (https://github.com/25huizengek1/ViTune)"

object LrcLib {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }

            defaultRequest {
                url("https://lrclib.net")
                header("Lrclib-Client", AGENT)
            }

            install(UserAgent) {
                agent = AGENT
            }

            expectSuccess = true
        }
    }

    private suspend fun queryLyrics(
        artist: String,
        title: String,
        album: String? = null
    ) = client.get("/api/search") {
        parameter("track_name", title)
        parameter("artist_name", artist)
        if (album != null) parameter("album_name", album)
    }.body<List<Track>>()

    private suspend fun queryLyrics(query: String) = client.get("/api/search") {
        parameter("q", query)
    }.body<List<Track>>()

    suspend fun lyrics(
        artist: String,
        title: String,
        album: String? = null,
        synced: Boolean = true
    ) = runCatchingCancellable {
        queryLyrics(
            artist = artist,
            title = title,
            album = album
        ).let { list ->
            list.filter { if (synced) it.syncedLyrics != null else it.plainLyrics != null }
        }
    }

    suspend fun lyrics(
        query: String,
        synced: Boolean = true
    ) = runCatchingCancellable {
        queryLyrics(query = query).let { list ->
            list.filter { if (synced) it.syncedLyrics != null else it.plainLyrics != null }
        }
    }

    suspend fun bestLyrics(
        artist: String,
        title: String,
        duration: Duration,
        album: String? = null,
        synced: Boolean = true
    ) = lyrics(
        artist = artist,
        title = title,
        album = album,
        synced = synced
    )?.mapCatching { tracks ->
        tracks.bestMatchingFor(title, duration)
            ?.let { if (synced) it.syncedLyrics else it.plainLyrics }
            ?.let {
                Lyrics(
                    text = it,
                    synced = synced
                )
            }
    }

    data class Lyrics(
        val text: String,
        val synced: Boolean
    ) {
        fun asLrc() = LrcParser.parse(text)?.toLrcFile()
    }
}

/*
Đây là một trình phân tích cú pháp (parser) cho tệp LRC (lời bài hát có timestamp).
Mục tiêu của nó là đọc nội dung tệp, trích xuất thông tin meta (tiêu đề, nghệ sĩ, album, v.v.)
và phân tích từng dòng lời bài hát kèm thời gian.
 */
object LrcParser {
    /*
    (\\d{2,}): Phút (ví dụ 01)
    (\\d{2}): Giây (ví dụ 23)
    (\\d{2,3}): Mili-giây (ví dụ 45 hoặc 450)
    (.*): Nội dung lời bài hát (ví dụ: "This is a lyric")
    (.+?): Tên metadata (ví dụ: ar)
    (.*?): Giá trị metadata (ví dụ: Artist Name)
     */
    private val lyricRegex = "^\\[(\\d{2,}):(\\d{2}).(\\d{2,3})](.*)$".toRegex() // dùng để bắt các dòng thời gian + lời bài hát
    private val metadataRegex = "^\\[(.+?):(.*?)]$".toRegex() // dùng để bắt các dòng metadata

    // Định nghĩa một interface Line với các loại dòng khác nhau trong tệp LRC
    sealed interface Line {
        val raw: String?

        data object Invalid : Line {
            override val raw: String? = null
        }

        data class Metadata(
            val key: String,
            val value: String,
            override val raw: String
        ) : Line

        data class Lyric(
            val timestamp: Long,
            val line: String,
            override val raw: String
        ) : Line
    }

    // Hàm mở rộng để xử lý lỗi cho các kết quả
    private fun <T> Result<T>.handleError(logging: Boolean) = onFailure {
        when {
            it is CancellationException -> throw it
            logging -> it.printStackTrace()
        }
    }

    // Hàm mở rộng để chuyển đổi một chuỗi thành một dòng LRC
    fun parse(
        raw: String, // Nội dung tệp LRC
        logging: Boolean = false // Có in ra lỗi hay không
    ) = raw.lines().mapNotNull { line -> // Tách từng dòng
        line.substringBefore('#').trim().takeIf { it.isNotBlank() } // Bỏ qua các dòng trống hoặc chỉ có dấu #
    }.map { line ->
        runCatching {
            val results = lyricRegex.find(line)?.groups ?: error("Invalid lyric") // Tìm kiếm các nhóm trong dòng lời bài hát
            val (minutes, seconds, millis, lyric) = results.drop(1).take(4).mapNotNull { it?.value } // Lấy các nhóm đã tìm thấy
            val duration = minutes.toInt().minutes +
                    seconds.toInt().seconds +
                    millis.padEnd(length = 3, padChar = '0').toInt().milliseconds // Tính toán thời gian

            Line.Lyric( // Tạo một đối tượng Lyric với thời gian và lời bài hát
                timestamp = duration.inWholeMilliseconds,
                line = lyric.trim(),
                raw = line
            )
        }.handleError(logging).recoverCatching { // Xử lý lỗi nếu không tìm thấy nhóm
            val results = metadataRegex.find(line)?.groups ?: error("Invalid metadata")
            val (key, value) = results.drop(1).take(2).mapNotNull { it?.value }

            Line.Metadata(
                key = key.trim(),
                value = value.trim(),
                raw = line
            )
        }.handleError(logging).getOrDefault(Line.Invalid)
    }.takeIf { lrc -> lrc.isNotEmpty() && !lrc.all { it == Line.Invalid } }

    // Hàm mở rộng để chuyển đổi danh sách các dòng thành một tệp LRC
    data class LrcFile(
        val metadata: Map<String, String>,
        val lines: Map<Long, String>,
        val errors: Int
    ) {
        val title get() = metadata["ti"]
        val artist get() = metadata["ar"]
        val album get() = metadata["al"]
        val author get() = metadata["au"]
        val duration
            get() = metadata["length"]?.runCatching {
                val (minutes, seconds) = split(":", limit = 2)
                minutes.toInt().minutes + seconds.toInt().seconds
            }?.getOrNull()
        val fileAuthor get() = metadata["by"]
        val offset get() = metadata["offset"]?.removePrefix("+")?.toIntOrNull()?.milliseconds
        val tool get() = metadata["re"] ?: metadata["tool"]
        val version get() = metadata["ve"]
    }
}

// Chuyển đổi danh sách các dòng LRC thành một tệp LRC
fun List<LrcParser.Line>.toLrcFile(): LrcParser.LrcFile {
    val metadata = mutableMapOf<String, String>()
    val lines = mutableMapOf(0L to "")
    var errors = 0

    forEach {
        when (it) {
            LrcParser.Line.Invalid -> errors++
            is LrcParser.Line.Lyric -> lines += it.timestamp to it.line
            is LrcParser.Line.Metadata -> metadata += it.key to it.value
        }
    }

    return LrcParser.LrcFile(
        metadata = metadata,
        lines = lines,
        errors = errors
    )
}
