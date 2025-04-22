package app.vitune.android.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.R
import app.vitune.android.models.Song
import app.vitune.android.preferences.OrderPreferences
import app.vitune.android.service.LOCAL_KEY_PREFIX
import app.vitune.android.transaction
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.AudioMediaCursor
import app.vitune.android.utils.hasPermission
import app.vitune.android.utils.medium
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.isAtLeastAndroid13
import app.vitune.core.ui.utils.isCompositionLaunched
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Xác định quyền truy cập phù hợp tùy theo phiên bản Android
private val permission = if (isAtLeastAndroid13)
    Manifest.permission.READ_MEDIA_AUDIO // Android 13+ cần quyền này để đọc audio
else
    Manifest.permission.READ_EXTERNAL_STORAGE // Android <13 dùng quyền cũ

@Route
@Composable
fun HomeLocalSongs(onSearchClick: () -> Unit) = with(OrderPreferences) {
    val context = LocalContext.current // Lấy Context hiện tại
    val (_, typography) = LocalAppearance.current

    // Trạng thái kiểm tra quyền truy cập
    var hasPermission by remember(isCompositionLaunched()) {
        mutableStateOf(context.applicationContext.hasPermission(permission)) // Kiểm tra permission ban đầu
    }

    // Bộ xử lý yêu cầu quyền truy cập
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { hasPermission = it } // Cập nhật trạng thái nếu người dùng cấp quyền
    )

    // Nếu đã có quyền thì bắt đầu thu thập nhạc nội bộ (sử dụng flow)
    LaunchedEffect(hasPermission) {
        if (hasPermission) context.musicFilesAsFlow().collect()
    }

    if (hasPermission) {
        // Giao diện hiển thị bài hát cục bộ nếu đã cấp quyền
        HomeSongs(
            onSearchClick = onSearchClick, // Hành động tìm kiếm
            songProvider = {
                Database.songs(
                    sortBy = localSongSortBy, // Tiêu chí sắp xếp
                    sortOrder = localSongSortOrder, // Thứ tự sắp xếp
                    isLocal = true // Chỉ lấy nhạc nội bộ
                ).map { songs ->
                    songs.filter { it.durationText != "0:00" } // Bỏ bài hát không có thời lượng
                }
            },
            sortBy = localSongSortBy,
            setSortBy = { localSongSortBy = it },
            sortOrder = localSongSortOrder,
            setSortOrder = { localSongSortOrder = it },
            title = stringResource(R.string.local) // Tiêu đề màn hình
        )
    } else {
        // Nếu chưa có quyền → yêu cầu cấp quyền
        LaunchedEffect(Unit) { launcher.launch(permission) }

        // Giao diện hiển thị khi bị từ chối quyền truy cập
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                text = stringResource(R.string.media_permission_declined), // Thông báo không có quyền
                modifier = Modifier.fillMaxWidth(0.75f),
                style = typography.m.medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Nút mở cài đặt ứng dụng để cấp quyền thủ công
            SecondaryTextButton(
                text = stringResource(R.string.open_settings),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            setData(Uri.fromParts("package", context.packageName, null)) // Mở trang chi tiết ứng dụng
                        }
                    )
                }
            )
        }
    }
}

// Tạo một CoroutineScope dành riêng cho việc xử lý MediaStore, chạy trên luồng IO.
private val mediaScope = CoroutineScope(Dispatchers.IO + CoroutineName("MediaStore worker"))

/**
 * Trả về một StateFlow danh sách các bài hát (Song) lấy từ MediaStore (thư viện nhạc nội bộ).
 */
fun Context.musicFilesAsFlow(): StateFlow<List<Song>> = flow {
    var version: String? = null // Biến dùng để theo dõi version của MediaStore, để phát hiện thay đổi.

    while (currentCoroutineContext().isActive) { // Lặp liên tục khi coroutine còn hoạt động
        val newVersion = MediaStore.getVersion(applicationContext) // Lấy version mới nhất của MediaStore

        if (version != newVersion) { // Nếu version thay đổi → dữ liệu đã thay đổi
            version = newVersion

            // Truy vấn danh sách nhạc từ MediaStore
            AudioMediaCursor.query(contentResolver) {
                buildList {
                    while (next()) { // Duyệt qua từng dòng trong Cursor
                        if (!isMusic || duration == 0) continue // Bỏ qua nếu không phải bài hát hoặc độ dài bằng 0

                        add(
                            Song(
                                id = "$LOCAL_KEY_PREFIX$id", // Tạo ID duy nhất cho bài hát
                                title = name, // Tên bài hát
                                artistsText = artist, // Tên nghệ sĩ
                                durationText = duration.milliseconds.toComponents { minutes, seconds, _ ->
                                    "$minutes:${seconds.toString().padStart(2, '0')}" // Chuyển duration thành chuỗi "mm:ss"
                                },
                                thumbnailUrl = albumUri.toString() // Ảnh đại diện album
                            )
                        )
                    }
                }
            }?.let { emit(it) } // Nếu có kết quả thì emit (phát) danh sách ra flow
        }

        delay(5.seconds) // Đợi 5 giây trước khi kiểm tra lại để tránh spam CPU
    }
}
    .distinctUntilChanged() // Chỉ emit khi danh sách bài hát thực sự thay đổi
    .onEach { songs -> // Mỗi lần danh sách thay đổi thì lưu vào cơ sở dữ liệu
        transaction {
            songs.forEach(Database::insert) // Lưu từng bài hát
        }
    }
    .stateIn( // Biến flow thành StateFlow để giữ trạng thái và chia sẻ
        mediaScope, // CoroutineScope đã tạo ở trên
        SharingStarted.Eagerly, // Bắt đầu chia sẻ ngay khi có subscriber
        listOf() // Giá trị khởi tạo là danh sách rỗng
    )

