package app.vitune.providers.innertube.models.bodies

import app.vitune.providers.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
// Chứa thông tin về yêu cầu tiếp theo trong quá trình phát nhạc trên YouTube Music.
data class NextBody(
    val context: Context = Context.DefaultWeb,
    val videoId: String?,
    val isAudioOnly: Boolean = true, // chỉ định rằng yêu cầu này là để phát nhạc
    val playlistId: String? = null,
    val tunerSettingValue: String = "AUTOMIX_SETTING_NORMAL",
    val index: Int? = null,
    val params: String? = null,
    val playlistSetVideoId: String? = null,
    val watchEndpointMusicSupportedConfigs: WatchEndpointMusicSupportedConfigs = WatchEndpointMusicSupportedConfigs(
        musicVideoType = "MUSIC_VIDEO_TYPE_ATV"
    )
) {
    @Serializable
    data class WatchEndpointMusicSupportedConfigs( // Lớp này chứa các cấu hình hỗ trợ cho video nhạc
        val musicVideoType: String
    )
}
