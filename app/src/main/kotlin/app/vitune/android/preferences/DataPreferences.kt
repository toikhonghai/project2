package app.vitune.android.preferences

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.vitune.android.GlobalPreferencesHolder
import app.vitune.android.R
import app.vitune.core.data.enums.CoilDiskCacheSize
import app.vitune.core.data.enums.ExoPlayerDiskCacheSize
import app.vitune.providers.innertube.Innertube
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

// DataPreferences là một singleton object kế thừa từ GlobalPreferencesHolder,
// dùng để lưu các thiết lập toàn cục của ứng dụng.
object DataPreferences : GlobalPreferencesHolder() {

    // Dung lượng tối đa cho cache đĩa của thư viện Coil (dùng cho ảnh)
    var coilDiskCacheMaxSize by enum(CoilDiskCacheSize.`128MB`)

    // Dung lượng tối đa cho cache đĩa của ExoPlayer (dùng cho video/audio)
    var exoPlayerDiskCacheMaxSize by enum(ExoPlayerDiskCacheSize.`2GB`)

    // Tạm dừng lưu lại lịch sử phát nhạc
    var pauseHistory by boolean(false)

    // Tạm dừng lưu thời lượng đã nghe bài hát
    var pausePlaytime by boolean(false)

    // Tạm dừng lưu lại lịch sử tìm kiếm
    var pauseSearchHistory by boolean(false)

    // Thiết lập số lượng tối đa cho danh sách top (mặc định 50)
    val topListLengthProperty = int(50)
    var topListLength by topListLengthProperty

    // Thiết lập khoảng thời gian cho danh sách top (mặc định là AllTime - mọi thời gian)
    val topListPeriodProperty = enum(TopListPeriod.AllTime)
    var topListPeriod by topListPeriodProperty

    // Thiết lập nguồn dữ liệu cho mục "Quick Picks" (Gợi ý nhanh)
    var quickPicksSource by enum(QuickPicksSource.Trending)

    // Thiết lập khoảng thời gian để kiểm tra phiên bản mới (mặc định là tắt)
    var versionCheckPeriod by enum(VersionCheckPeriod.Off)

    // Có nên cache dữ liệu của Quick Picks hay không (mặc định là true)
    var shouldCacheQuickPicks by boolean(true)

    // Lưu trữ dữ liệu đã cache của Quick Picks (theo định dạng JSON)
    var cachedQuickPicks by json(Innertube.RelatedPage())

    // Tự động đồng bộ danh sách phát với server
    var autoSyncPlaylists by boolean(true)

    // Enum đại diện cho khoảng thời gian lọc trong danh sách top
    enum class TopListPeriod(
        val displayName: @Composable () -> String, // Hiển thị tên theo resource string
        val duration: Duration? = null             // Khoảng thời gian tương ứng
    ) {
        PastDay(displayName = { stringResource(R.string.past_24_hours) }, duration = 1.days),
        PastWeek(displayName = { stringResource(R.string.past_week) }, duration = 7.days),
        PastMonth(displayName = { stringResource(R.string.past_month) }, duration = 30.days),
        PastYear(displayName = { stringResource(R.string.past_year) }, duration = 365.days),
        AllTime(displayName = { stringResource(R.string.all_time) }) // Không giới hạn thời gian
    }

    // Enum cho biết nguồn dữ liệu cho phần Quick Picks
    enum class QuickPicksSource(val displayName: @Composable () -> String) {
        Trending(displayName = { stringResource(R.string.trending) }),              // Theo xu hướng
        LastInteraction(displayName = { stringResource(R.string.last_interaction) }) // Theo tương tác gần nhất
    }

    // Enum cho tần suất kiểm tra phiên bản ứng dụng
    enum class VersionCheckPeriod(
        val displayName: @Composable () -> String, // Tên hiển thị
        val period: Duration?                      // Khoảng thời gian tương ứng
    ) {
        Off(displayName = { stringResource(R.string.off_text) }, period = null),     // Tắt kiểm tra
        Hourly(displayName = { stringResource(R.string.hourly) }, period = 1.hours), // Kiểm tra mỗi giờ
        Daily(displayName = { stringResource(R.string.daily) }, period = 1.days),    // Kiểm tra hàng ngày
        Weekly(displayName = { stringResource(R.string.weekly) }, period = 7.days)   // Kiểm tra hàng tuần
    }
}
