package app.vitune.android.preferences

import android.media.audiofx.PresetReverb
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.vitune.android.GlobalPreferencesHolder
import app.vitune.android.R

// Đối tượng Singleton để quản lý các thiết lập liên quan đến trình phát nhạc
object PlayerPreferences : GlobalPreferencesHolder() {

    // ------------------- Các tùy chọn phát nhạc ----------------------

    val isInvincibilityEnabledProperty = boolean(false) // Tùy chọn "bất tử" (ẩn ý, có thể liên quan đến debug/test)
    var isInvincibilityEnabled by isInvincibilityEnabledProperty

    val trackLoopEnabledProperty = boolean(false) // Lặp bài hát hiện tại
    var trackLoopEnabled by trackLoopEnabledProperty

    val queueLoopEnabledProperty = boolean(true) // Lặp toàn bộ hàng đợi
    var queueLoopEnabled by queueLoopEnabledProperty

    val skipSilenceProperty = boolean(false) // Bỏ qua khoảng lặng trong bài hát
    var skipSilence by skipSilenceProperty

    val volumeNormalizationProperty = boolean(false) // Chuẩn hóa âm lượng giữa các bài hát
    var volumeNormalization by volumeNormalizationProperty

    val volumeNormalizationBaseGainProperty = float(5.00f) // Mức gain cơ bản cho chuẩn hóa âm lượng
    var volumeNormalizationBaseGain by volumeNormalizationBaseGainProperty

    val bassBoostProperty = boolean(false) // Bật tăng cường bass
    var bassBoost by bassBoostProperty

    val bassBoostLevelProperty = int(5) // Mức độ tăng cường bass (0–10)
    var bassBoostLevel by bassBoostLevelProperty

    val reverbProperty = enum(Reverb.None) // Hiệu ứng vang (reverb)
    var reverb by reverbProperty

    val resumePlaybackWhenDeviceConnectedProperty = boolean(false) // Tự động tiếp tục phát khi kết nối tai nghe/Bluetooth
    var resumePlaybackWhenDeviceConnected by resumePlaybackWhenDeviceConnectedProperty

    val speedProperty = float(1f) // Tốc độ phát (1.0 = bình thường)
    var speed by speedProperty

    val pitchProperty = float(1f) // Cao độ (pitch), mặc định 1.0
    var pitch by pitchProperty

    // ------------------- Các tùy chọn liên quan đến giao diện ----------------------

    var minimumSilence by long(2_000_000L) // Độ dài khoảng lặng tối thiểu để bỏ qua (nano giây)
    var persistentQueue by boolean(true) // Giữ nguyên hàng đợi phát khi app khởi động lại
    var stopWhenClosed by boolean(false) // Dừng nhạc khi app bị đóng
    var stopOnMinimumVolume by boolean(true) // Dừng nhạc khi âm lượng giảm xuống mức tối thiểu

    var isShowingLyrics by boolean(false) // Đang hiển thị lời bài hát
    var isShowingSynchronizedLyrics by boolean(false) // Lời bài hát có đồng bộ theo thời gian không

    var isShowingPrevButtonCollapsed by boolean(false) // Thu gọn nút quay lại
    var horizontalSwipeToClose by boolean(false) // Vuốt ngang để đóng trình phát
    var horizontalSwipeToRemoveItem by boolean(false) // Vuốt để xóa item trong hàng đợi

    var playerLayout by enum(PlayerLayout.New) // Giao diện trình phát (cổ điển / mới)
    var seekBarStyle by enum(SeekBarStyle.Wavy) // Kiểu thanh tiến trình (seek bar)
    var wavySeekBarQuality by enum(WavySeekBarQuality.Great) // Chất lượng hiển thị của thanh tiến trình wavy

    var showLike by boolean(false) // Hiển thị nút like
    var showRemaining by boolean(false) // Hiển thị thời gian còn lại

    var lyricsKeepScreenAwake by boolean(false) // Giữ sáng màn hình khi hiển thị lời bài hát
    var lyricsShowSystemBars by boolean(true) // Hiển thị thanh trạng thái khi hiển thị lời bài hát

    var skipOnError by boolean(false) // Tự động bỏ qua khi xảy ra lỗi phát
    var handleAudioFocus by boolean(true) // Xử lý audio focus (ngắt khi có cuộc gọi...)

    var pauseCache by boolean(false) // Tạm dừng cache?

    val sponsorBlockEnabledProperty = boolean(false) // Bỏ qua quảng cáo tài trợ (SponsorBlock)
    var sponsorBlockEnabled by sponsorBlockEnabledProperty

    // ------------------- Các enum hỗ trợ hiển thị UI ----------------------

    enum class PlayerLayout(val displayName: @Composable () -> String) {
        Classic(displayName = { stringResource(R.string.classic_player_layout_name) }), // Giao diện cũ
        New(displayName = { stringResource(R.string.new_player_layout_name) }) // Giao diện mới
    }

    enum class SeekBarStyle(val displayName: @Composable () -> String) {
        Static(displayName = { stringResource(R.string.static_seek_bar_name) }), // Seek bar tĩnh
        Wavy(displayName = { stringResource(R.string.wavy_seek_bar_name) }) // Seek bar dạng sóng
    }

    enum class WavySeekBarQuality(
        val quality: Float,
        val displayName: @Composable () -> String
    ) {
        Poor(quality = 50f, displayName = { stringResource(R.string.seek_bar_quality_poor) }),
        Low(quality = 25f, displayName = { stringResource(R.string.seek_bar_quality_low) }),
        Medium(quality = 15f, displayName = { stringResource(R.string.seek_bar_quality_medium) }),
        High(quality = 5f, displayName = { stringResource(R.string.seek_bar_quality_high) }),
        Great(quality = 1f, displayName = { stringResource(R.string.seek_bar_quality_great) }),
        Subpixel(quality = 0.5f, displayName = { stringResource(R.string.seek_bar_quality_subpixel) })
    }

    enum class Reverb(
        val preset: Short,
        val displayName: @Composable () -> String
    ) {
        None(preset = PresetReverb.PRESET_NONE, displayName = { stringResource(R.string.none) }),
        SmallRoom(preset = PresetReverb.PRESET_SMALLROOM, displayName = { stringResource(R.string.reverb_small_room) }),
        MediumRoom(preset = PresetReverb.PRESET_MEDIUMROOM, displayName = { stringResource(R.string.reverb_medium_room) }),
        LargeRoom(preset = PresetReverb.PRESET_LARGEROOM, displayName = { stringResource(R.string.reverb_large_room) }),
        MediumHall(preset = PresetReverb.PRESET_MEDIUMHALL, displayName = { stringResource(R.string.reverb_medium_hall) }),
        LargeHall(preset = PresetReverb.PRESET_LARGEHALL, displayName = { stringResource(R.string.reverb_large_hall) }),
        Plate(preset = PresetReverb.PRESET_PLATE, displayName = { stringResource(R.string.reverb_plate) })
    }
}
