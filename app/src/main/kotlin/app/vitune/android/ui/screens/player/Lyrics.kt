package app.vitune.android.ui.screens.player

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.Lyrics
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.query
import app.vitune.android.service.LOCAL_KEY_PREFIX
import app.vitune.android.transaction
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.themed.CircularProgressIndicator
import app.vitune.android.ui.components.themed.DefaultDialog
import app.vitune.android.ui.components.themed.Menu
import app.vitune.android.ui.components.themed.MenuEntry
import app.vitune.android.ui.components.themed.TextField
import app.vitune.android.ui.components.themed.TextFieldDialog
import app.vitune.android.ui.components.themed.TextPlaceholder
import app.vitune.android.ui.components.themed.ValueSelectorDialogBody
import app.vitune.android.ui.modifiers.verticalFadingEdge
import app.vitune.android.utils.SynchronizedLyrics
import app.vitune.android.utils.SynchronizedLyricsState
import app.vitune.android.utils.center
import app.vitune.android.utils.color
import app.vitune.android.utils.isInPip
import app.vitune.android.utils.medium
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.toast
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.onOverlay
import app.vitune.core.ui.onOverlayShimmer
import app.vitune.core.ui.overlay
import app.vitune.core.ui.utils.dp
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.NextBody
import app.vitune.providers.innertube.requests.lyrics
import app.vitune.providers.kugou.KuGou
import app.vitune.providers.lrclib.LrcLib
import app.vitune.providers.lrclib.LrcParser
import app.vitune.providers.lrclib.models.Track
import app.vitune.providers.lrclib.toLrcFile
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val UPDATE_DELAY = 50L // Khoảng thời gian delay giữa các lần cập nhật lời bài hát

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean, // Quyết định có hiển thị lyrics không
    onDismiss: () -> Unit,
    mediaMetadataProvider: () -> MediaMetadata, // Cung cấp thông tin metadata của bài hát, metadata là chứa thông tin về tên nghệ sĩ, bài hát,...
    durationProvider: () -> Long, // Cung cấp thời lượng bài hát
    ensureSongInserted: () -> Unit, // Đảm bảo bài hát đã được chèn vào cơ sở dữ liệu
    modifier: Modifier = Modifier,
    onMenuLaunch: () -> Unit = { }, // Mở menu khi nhấn vào icon
    onOpenDialog: (() -> Unit)? = null,
    shouldShowSynchronizedLyrics: Boolean = PlayerPreferences.isShowingSynchronizedLyrics, // Cho biết có hiển thị lời đồng bộ không
    setShouldShowSynchronizedLyrics: (Boolean) -> Unit = {
        PlayerPreferences.isShowingSynchronizedLyrics =
            it // Lời bài hát có đồng bộ theo thời gian không
    },
    shouldKeepScreenAwake: Boolean = PlayerPreferences.lyricsKeepScreenAwake, // Ngăn không cho màn hình tắt khi đang xem lyrics
    shouldUpdateLyrics: Boolean = true,
    showControls: Boolean = true
) = AnimatedVisibility(
    visible = isDisplayed, // Hiển thị nếu isDisplayed = true
    enter = fadeIn(), // Hiệu ứng fade in khi xuất hiện
    exit = fadeOut() // Hiệu ứng fade out khi biến mất
) {
    // Các state cập nhật theo hàm truyền vào, đảm bảo luôn dùng giá trị mới nhất khi recomposition
    val currentEnsureSongInserted by rememberUpdatedState(ensureSongInserted)
    val currentMediaMetadataProvider by rememberUpdatedState(mediaMetadataProvider)
    val currentDurationProvider by rememberUpdatedState(durationProvider)

    // Một số giá trị từ context và theme
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current
    val density =
        LocalDensity.current // .current trả về một instance Density, chứa các phương thức chuyển đổi như dp -> px,
    val view = LocalView.current

    // Kiểm tra đang ở chế độ Picture-in-Picture
    val pip = isInPip()

    // Lưu state lời bài hát
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }

    // Có hiển thị lời đồng bộ hay không
    val showSynchronizedLyrics = remember(shouldShowSynchronizedLyrics, lyrics) {
        shouldShowSynchronizedLyrics && lyrics?.synced?.isBlank() != true // isBlank() == true: kiểm tra xem lyrics có rỗng hay không
    }

    // Các biến trạng thái liên quan đến chỉnh sửa lời
    var editing by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var picking by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }
    var error by remember(mediaId, shouldShowSynchronizedLyrics) { mutableStateOf(false) }

    // Nội dung lời bài hát đang được hiển thị
    val text = remember(lyrics, showSynchronizedLyrics) {
        if (showSynchronizedLyrics) lyrics?.synced else lyrics?.fixed
    }

    // Kiểm tra xem file lời đồng bộ có hợp lệ không
    var invalidLrc by remember(text) { mutableStateOf(false) }

    // Giữ màn hình không tắt khi đang xem lyrics nếu được bật
    DisposableEffect(shouldKeepScreenAwake) {
        view.keepScreenOn = shouldKeepScreenAwake
        onDispose {
            view.keepScreenOn = false // Tắt giữ màn hình khi composable bị huỷ
        }
    }

    LaunchedEffect(mediaId, shouldShowSynchronizedLyrics) {
        runCatching { // runCatching giúp bắt lỗi mà không làm crash ứng dụng
            withContext(Dispatchers.IO) { // Chuyển sang IO thread để thực hiện các thao tác truy cập database/mạng

                Database
                    .lyrics(mediaId) // Lấy lyrics từ database theo mediaId
                    .distinctUntilChanged() // Chỉ emit khi dữ liệu thực sự thay đổi
                    .cancellable() // Cho phép cancel flow nếu LaunchedEffect bị hủy
                    .collect { currentLyrics -> // Thu thập dữ liệu lyrics từ flow

                        if (
                            !shouldUpdateLyrics || // Nếu không cần cập nhật lyrics
                            (currentLyrics?.fixed != null && currentLyrics.synced != null) // hoặc cả lyrics tĩnh và lyrics đồng bộ đều đã có
                        ) {
                            lyrics = currentLyrics // Gán lyrics hiện tại
                        } else {
                            // Nếu cần cập nhật lyrics, bắt đầu xử lý lấy thông tin bài hát
                            val mediaMetadata = currentMediaMetadataProvider()
                            var duration = withContext(Dispatchers.Main) {
                                currentDurationProvider()
                            }

                            // Đợi cho đến khi lấy được duration hợp lệ (khác C.TIME_UNSET)
                            while (duration == C.TIME_UNSET) {
                                delay(100)
                                duration = withContext(Dispatchers.Main) {
                                    currentDurationProvider()
                                }
                            }

                            // Lấy thông tin metadata: album, nghệ sĩ, tiêu đề
                            val album = mediaMetadata.albumTitle?.toString()
                            val artist = mediaMetadata.artist?.toString().orEmpty()
                            val title = mediaMetadata.title?.toString().orEmpty().let {
                                if (mediaId.startsWith(LOCAL_KEY_PREFIX)) {
                                    // Nếu là bài hát local thì loại bỏ đuôi file (.mp3, .flac, ...)
                                    it.substringBeforeLast('.').trim()
                                } else it
                            }

                            // Reset lyrics và error trước khi tìm mới
                            lyrics = null
                            error = false

                            // Thử lấy lyrics tĩnh (fixed) từ API hoặc thư viện LrcLib
                            val fixed = currentLyrics?.fixed ?: Innertube
                                .lyrics(NextBody(videoId = mediaId))
                                ?.getOrNull()
                            ?: LrcLib.bestLyrics(
                                artist = artist,
                                title = title,
                                duration = duration.milliseconds,
                                album = album,
                                synced = false
                            )?.map { it?.text }?.getOrNull()

                            // Thử lấy lyrics đồng bộ (synced) từ LrcLib hoặc KuGou
                            val synced = currentLyrics?.synced ?: LrcLib.bestLyrics(
                                artist = artist,
                                title = title,
                                duration = duration.milliseconds,
                                album = album
                            )?.map { it?.text }?.getOrNull()
                            ?: LrcLib.bestLyrics(
                                artist = artist,
                                title = title.split("(")[0].trim(), // Thử bỏ phần (Live), (Remix) nếu có
                                duration = duration.milliseconds,
                                album = album
                            )?.map { it?.text }?.getOrNull()
                            ?: KuGou.lyrics(
                                artist = artist,
                                title = title,
                                duration = duration / 1000 // KuGou dùng giây
                            )?.map { it?.value }?.getOrNull()

                            // Tạo Lyrics object và lưu vào database nếu có dữ liệu
                            Lyrics(
                                songId = mediaId,
                                fixed = fixed.orEmpty(),
                                synced = synced.orEmpty()
                            ).also { // also là một hàm mở rộng cho phép thực hiện một hành động trên đối tượng mà không thay đổi nó
                                ensureActive() // Đảm bảo coroutine vẫn đang hoạt động

                                transaction {
                                    runCatching {
                                        currentEnsureSongInserted() // Đảm bảo bài hát đã được thêm vào DB
                                        Database.upsert(it) // Cập nhật lyrics vào DB
                                    }
                                }
                            }
                        }

                        // Kiểm tra lỗi nếu lyrics bị trống
                        error =
                            (shouldShowSynchronizedLyrics && lyrics?.synced?.isBlank() == true) ||
                                    (!shouldShowSynchronizedLyrics && lyrics?.fixed?.isBlank() == true)
                    }
            }
        }.exceptionOrNull()?.let {
            // Nếu có lỗi xảy ra (ngoại trừ bị hủy coroutine), in lỗi
            if (it is CancellationException) throw it
            else it.printStackTrace()
        }
    }

    // Nếu người dùng đang chỉnh sửa lyrics (bằng tay)
    if (editing) TextFieldDialog(
        hintText = stringResource(R.string.enter_lyrics), // Gợi ý trong ô nhập
        initialTextInput = (
                if (shouldShowSynchronizedLyrics) lyrics?.synced else lyrics?.fixed
                ).orEmpty(), // Dữ liệu lyrics ban đầu (hiển thị trong TextField)
        singleLine = false,
        maxLines = 10, // Tối đa 10 dòng
        isTextInputValid = { true }, // Không kiểm tra nội dung đầu vào
        onDismiss = { editing = false }, // Đóng dialog khi hủy
        onAccept = { // Khi người dùng xác nhận
            transaction {
                runCatching {
                    currentEnsureSongInserted() // Đảm bảo bài hát đã tồn tại trong DB

                    // Cập nhật lyrics tùy vào loại lyrics đang chỉnh
                    Database.upsert(
                        if (shouldShowSynchronizedLyrics) Lyrics(
                            songId = mediaId,
                            fixed = lyrics?.fixed, // Giữ nguyên fixed
                            synced = it // Cập nhật synced mới
                        ) else Lyrics(
                            songId = mediaId,
                            fixed = it, // Cập nhật fixed mới
                            synced = lyrics?.synced // Giữ nguyên synced
                        )
                    )
                }
            }
        }
    )


// Nếu đang ở trạng thái 'picking' lyrics từ thư viện LrcLib
// Và người dùng đang xem lyrics đồng bộ (synchronized lyrics)
    if (picking && shouldShowSynchronizedLyrics) {
        // Trạng thái chứa truy vấn tìm kiếm ban đầu (dựa theo tiêu đề bài hát)
        var query by rememberSaveable {
            mutableStateOf(
                currentMediaMetadataProvider().title?.toString().orEmpty().let {
                    if (mediaId.startsWith(LOCAL_KEY_PREFIX)) {
                        // Nếu bài hát là local thì cắt đuôi file (.mp3, .flac, ...)
                        it.substringBeforeLast('.').trim()
                    } else it
                }
            )
        }

        // Hiển thị dialog tìm kiếm lyrics bằng LrcLib
        LrcLibSearchDialog(
            query = query, // Truy vấn tìm kiếm hiện tại
            setQuery = { query = it }, // Cập nhật truy vấn tìm kiếm khi người dùng gõ
            onDismiss = { picking = false }, // Đóng dialog khi hủy
            onPick = { result -> // Khi người dùng chọn một lyrics từ kết quả tìm kiếm
                runCatching {
                    transaction {
                        // Cập nhật lyrics trong database với kết quả chọn từ LrcLib
                        Database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = lyrics?.fixed, // Giữ nguyên phần fixed nếu có
                                synced = result.syncedLyrics // Gán lyrics đồng bộ mới
                            )
                        )
                    }
                }
            }
        )
    }

    // Một BoxWithConstraints cho phép truy cập kích thước của bố cục cha
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            // Khi người dùng chạm vào overlay, hàm onDismiss sẽ được gọi
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .fillMaxSize()
            .background(colorPalette.overlay) // Overlay có màu bán trong suốt
    ) {
        // Tạo một hiệu ứng hoạt hình thay đổi chiều cao theo maxHeight
        val animatedHeight by animateDpAsState(
            targetValue = maxHeight,
            label = ""
        )

        // Hiển thị thông báo lỗi nếu không có lời bài hát
        AnimatedVisibility(
            visible = error, // Hiển thị nếu có lỗi
            enter = slideInVertically { -it }, // hiệu ứng trượt vào từ trên xuống
            exit = slideOutVertically { -it }, // hiệu ứng trượt ra lên trên
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BasicText(
                text = stringResource(
                    // Hiển thị chuỗi phù hợp tùy theo trạng thái lời đồng bộ
                    if (shouldShowSynchronizedLyrics) R.string.synchronized_lyrics_not_available
                    else R.string.lyrics_not_available
                ),
                style = typography.xs.center.medium.color(colorPalette.onOverlay), // Kiểu chữ nhỏ, căn giữa
                modifier = Modifier
                    .background(Color.Black.copy(0.4f)) // Nền đen trong suốt 40%
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                maxLines = if (pip) 1 else Int.MAX_VALUE, // Nếu đang ở chế độ picture-in-picture thì chỉ hiện 1 dòng
                overflow = TextOverflow.Ellipsis // Dòng dài sẽ bị cắt "..."
            )
        }

        // Hiển thị thông báo nếu lời bài hát đồng bộ bị lỗi (sai định dạng)
        AnimatedVisibility(
            visible = !text.isNullOrBlank() && !error && invalidLrc && shouldShowSynchronizedLyrics,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            BasicText(
                text = stringResource(R.string.invalid_synchronized_lyrics),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .background(Color.Black.copy(0.4f))
                    .padding(all = 8.dp)
                    .fillMaxWidth(),
                maxLines = if (pip) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Khởi tạo trạng thái lời bài hát được đồng bộ hóa từ dữ liệu hiện tại
        val lyricsState = rememberSaveable(text) {
            val file = lyrics?.synced?.takeIf { it.isNotBlank() }?.let {
                // Phân tích dữ liệu LRC (lyrics có thời gian)
                LrcParser.parse(it)?.toLrcFile()
            }

            // Tạo state từ kết quả phân tích
            SynchronizedLyricsState(
                sentences = file?.lines, // Các dòng lời bài hát
                offset = file?.offset?.inWholeMilliseconds ?: 0L // Offset thời gian nếu có
            )
        }

        // Dựa trên lyricsState để hiển thị lời bài hát đồng bộ
        val synchronizedLyrics = remember(lyricsState) {
            // Đánh dấu là invalid nếu không có câu nào trong lời
            invalidLrc = lyricsState.sentences == null

            // Tạo SynchronizedLyrics nếu có dữ liệu hợp lệ
            lyricsState.sentences?.let {
                SynchronizedLyrics(it.toImmutableMap()) {
                    // Cập nhật vị trí hiện tại của player để đồng bộ
                    binder?.player?.let { player ->
                        player.currentPosition + UPDATE_DELAY + lyricsState.offset -
                                (lyrics?.startTime ?: 0L)
                    } ?: 0L // Trả về 0 nếu player null
                }
            }
        }

        AnimatedContent( // Hiệu ứng chuyển đổi giữa các trạng thái
            targetState = showSynchronizedLyrics, // Theo dõi trạng thái có nên hiển thị lời đồng bộ hay không
            transitionSpec = { fadeIn() togetherWith fadeOut() }, // Hiệu ứng chuyển đổi fadeIn + fadeOut
            label = ""
        ) { synchronized -> // synchronized = true nếu đang hiển thị lời đồng bộ
            val lazyListState = rememberLazyListState() // Quản lý trạng thái scroll cho LazyColumn

            if (synchronized) {
                // Khối chạy song song để đồng bộ lời bài hát theo thời gian
                LaunchedEffect(synchronizedLyrics, density, animatedHeight) {
                    val currentSynchronizedLyrics = synchronizedLyrics ?: return@LaunchedEffect // nếu không có lời đồng bộ thì thoát
                    val centerOffset =
                        with(density) { (-animatedHeight / 3).roundToPx() } // Đặt dòng lời cần hiển thị gần giữa màn hình

                    // Scroll đến dòng hiện tại đầu tiên khi bắt đầu
                    lazyListState.animateScrollToItem(
                        index = currentSynchronizedLyrics.index + 1,
                        scrollOffset = centerOffset
                    )

                    // Cập nhật dòng lời bài hát theo thời gian thực
                    while (true) {
                        delay(UPDATE_DELAY)
                        if (!currentSynchronizedLyrics.update()) continue // Nếu chưa đến dòng tiếp theo thì bỏ qua

                        lazyListState.animateScrollToItem(
                            index = currentSynchronizedLyrics.index + 1,
                            scrollOffset = centerOffset
                        )
                    }
                }

                // Nếu synchronizedLyrics != null thì hiển thị LazyColumn để vẽ từng dòng lời
                if (synchronizedLyrics != null) LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = false, // Không cho người dùng cuộn thủ công
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .verticalFadingEdge() // Hiệu ứng làm mờ ở phần trên/dưới
                        .fillMaxWidth()
                ) {
                    // Spacer phía trên để căn giữa nội dung
                    item(key = "header", contentType = 0) {
                        Spacer(modifier = Modifier.height(maxHeight))
                    }

                    // Hiển thị từng dòng lời bài hát
                    // itemsIndexed là một hàm tiện ích của LazyColumn (hoặc LazyRow)
                    itemsIndexed(
                        items = synchronizedLyrics.sentences.values.toImmutableList()
                    ) { index, sentence ->
                        // Màu chữ được chuyển động (trắng nếu đang phát, mờ nếu chưa tới)
                        val color by animateColorAsState(
                            if (index == synchronizedLyrics.index) Color.White
                            else colorPalette.textDisabled
                        )

                        // Nếu câu lời là trống thì hiển thị biểu tượng nốt nhạc
                        if (sentence.isBlank()) Image(
                            painter = painterResource(R.drawable.musical_notes),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(color),
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 32.dp)
                                .size(typography.xs.fontSize.dp)
                        ) else BasicText(
                            text = sentence,
                            style = typography.xs.center.medium.color(color),
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 32.dp)
                        )
                    }

                    // Spacer phía dưới để căn giữa nội dung
                    item(key = "footer", contentType = 0) {
                        Spacer(modifier = Modifier.height(maxHeight))
                    }
                }
            } else BasicText(
                // Nếu không dùng synchronized lyrics thì hiển thị toàn bộ lời dạng text bình thường
                text = lyrics?.fixed.orEmpty(),
                style = typography.xs.center.medium.color(colorPalette.onOverlay),
                modifier = Modifier
                    .verticalFadingEdge()
                    .verticalScroll(rememberScrollState()) // Cho phép cuộn thủ công
                    .fillMaxWidth()
                    .padding(vertical = maxHeight / 4, horizontal = 32.dp)
            )
        }

        // Nếu chưa có lời bài hát (text == null) và không có lỗi
        if (text == null && !error) Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.shimmer() // Hiệu ứng shimmer khi đang loading
        ) {
            repeat(4) { // Hiển thị 4 dòng placeholder
                TextPlaceholder(
                    color = colorPalette.onOverlayShimmer, // Màu shimmer dựa trên theme
                    modifier = Modifier.alpha(1f - it * 0.2f) // Giảm độ đậm dần theo từng dòng
                )
            }
        }

        // Nếu cần hiển thị điều khiển (ví dụ: nút mở rộng giao diện)
        if (showControls) {
            if (onOpenDialog != null) Image(
                painter = painterResource(R.drawable.expand), // Icon mở rộng
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.onOverlay), // Màu icon phù hợp với nền overlay
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = ripple(bounded = false), // Hiệu ứng ripple khi nhấn
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            onOpenDialog() // Gọi callback để mở dialog hoặc giao diện mở rộng
                        }
                    )
                    .padding(all = 8.dp) // Padding nội bộ
                    .size(20.dp) // Kích thước icon
                    .align(Alignment.BottomStart) // Căn dưới bên trái trong Box cha
            )

            Image(
                painter = painterResource(R.drawable.ellipsis_horizontal), // Icon ba chấm ngang
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.onOverlay), // Đổi màu icon theo theme overlay
                modifier = Modifier
                    .padding(all = 4.dp)
                    .clickable(
                        indication = ripple(bounded = false), // Hiệu ứng ripple không bị giới hạn theo kích thước view
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            // Khi người dùng nhấn vào biểu tượng
                            onMenuLaunch() // Gọi callback nếu có

                            menuState.display {
                                Menu {
                                    // Entry 1: Bật/tắt chế độ lời đồng bộ
                                    MenuEntry(
                                        icon = R.drawable.time,
                                        text = stringResource(
                                            if (shouldShowSynchronizedLyrics) R.string.show_unsynchronized_lyrics
                                            else R.string.show_synchronized_lyrics
                                        ),
                                        secondaryText = if (shouldShowSynchronizedLyrics) null
                                        else stringResource(R.string.provided_lyrics_by),
                                        onClick = {
                                            menuState.hide()
                                            setShouldShowSynchronizedLyrics(!shouldShowSynchronizedLyrics)
                                        }
                                    )

                                    // Entry 2: Chỉnh sửa lời bài hát
                                    MenuEntry(
                                        icon = R.drawable.pencil,
                                        text = stringResource(R.string.edit_lyrics),
                                        onClick = {
                                            menuState.hide()
                                            editing =
                                                true // Đặt biến `editing` để chuyển sang chế độ chỉnh sửa
                                        }
                                    )

                                    // Entry 3: Tìm lời bài hát trên Google
                                    MenuEntry(
                                        icon = R.drawable.search,
                                        text = stringResource(R.string.search_lyrics_online),
                                        onClick = {
                                            menuState.hide()
                                            val mediaMetadata = currentMediaMetadataProvider()

                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                        putExtra(
                                                            SearchManager.QUERY,
                                                            "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                        )
                                                    }
                                                )
                                            } catch (e: ActivityNotFoundException) {
                                                context.toast(context.getString(R.string.no_browser_installed))
                                            }
                                        }
                                    )

                                    // Entry 4: Làm mới lời bài hát (xoá lời đồng bộ hoặc lời thường hiện tại)
                                    MenuEntry(
                                        icon = R.drawable.sync,
                                        text = stringResource(R.string.refetch_lyrics),
                                        enabled = lyrics != null, // Chỉ cho phép bấm nếu có lyrics
                                        onClick = {
                                            menuState.hide()

                                            transaction {
                                                runCatching {
                                                    currentEnsureSongInserted()

                                                    Database.upsert(
                                                        if (shouldShowSynchronizedLyrics)
                                                            Lyrics(
                                                                songId = mediaId,
                                                                fixed = lyrics?.fixed,
                                                                synced = null // Xoá lời đồng bộ
                                                            )
                                                        else Lyrics(
                                                            songId = mediaId,
                                                            fixed = null, // Xoá lời thường
                                                            synced = lyrics?.synced
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    // Nếu đang ở chế độ lời đồng bộ thì hiển thị thêm 2 entry
                                    if (shouldShowSynchronizedLyrics) {
                                        // Entry 5: Chọn lời đồng bộ từ LRC lib
                                        MenuEntry(
                                            icon = R.drawable.download,
                                            text = stringResource(R.string.pick_from_lrclib),
                                            onClick = {
                                                menuState.hide()
                                                picking = true // Hiển thị UI chọn lời từ thư viện
                                            }
                                        )

                                        // Entry 6: Cài đặt thời gian bắt đầu của lời bài hát
                                        MenuEntry(
                                            icon = R.drawable.play_skip_forward,
                                            text = stringResource(R.string.set_lyrics_start_offset),
                                            secondaryText = stringResource(
                                                R.string.set_lyrics_start_offset_description
                                            ),
                                            onClick = {
                                                menuState.hide()
                                                lyrics?.let {
                                                    val startTime = binder?.player?.currentPosition
                                                    query {
                                                        Database.upsert(it.copy(startTime = startTime))
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    .padding(all = 8.dp)
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun LrcLibSearchDialog(
    query: String, // Truy vấn tìm kiếm lyrics
    setQuery: (String) -> Unit, // Hàm cập nhật truy vấn
    onDismiss: () -> Unit, // Hàm gọi khi đóng dialog
    onPick: (Track) -> Unit, // Hàm callback khi người dùng chọn một track
    modifier: Modifier = Modifier
) = DefaultDialog(
    onDismiss = onDismiss,
    horizontalPadding = 0.dp,
    modifier = modifier
) {
    val (_, typography) = LocalAppearance.current // Truy xuất style chữ

    val tracks = remember { mutableStateListOf<Track>() } // Danh sách track tìm được
    var loading by remember { mutableStateOf(true) } // Trạng thái đang tải
    var error by remember { mutableStateOf(false) } // Trạng thái lỗi

    // Khi query thay đổi, khởi chạy tìm kiếm
    LaunchedEffect(query) {
        loading = true
        error = false

        delay(1000) // Chờ 1s để tránh gửi request quá nhanh (debounce)

        // Gửi request tìm lyrics có đồng bộ (synced = true)
        LrcLib.lyrics(
            query = query,
            synced = true
        )?.onSuccess { newTracks ->
            // Nếu thành công, lọc các track có lyrics đồng bộ
            tracks.clear()
            tracks.addAll(newTracks.filter { !it.syncedLyrics.isNullOrBlank() })
            loading = false
            error = false
        }?.onFailure {
            // Nếu lỗi, cập nhật trạng thái lỗi và in lỗi
            loading = false
            error = true
            it.printStackTrace()
        } ?: run { loading = false } // Không có kết quả
    }

    // Thanh tìm kiếm
    TextField(
        value = query,
        onValueChange = setQuery,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        maxLines = 1,
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Hiển thị theo từng trạng thái
    when {
        loading -> CircularProgressIndicator(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        error || tracks.isEmpty() -> BasicText(
            text = stringResource(R.string.no_lyrics_found), // Không tìm thấy lyrics
            style = typography.s.semiBold.center,
            modifier = Modifier
                .padding(all = 24.dp)
                .align(Alignment.CenterHorizontally)
        )

        else -> ValueSelectorDialogBody(
            onDismiss = onDismiss,
            title = stringResource(R.string.choose_lyric_track), // Tiêu đề dialog
            selectedValue = null,
            values = tracks.toImmutableList(), // Danh sách track hiển thị
            onValueSelect = {
                // Khi người dùng chọn 1 track
                transaction {
                    onPick(it) // Gọi callback trả track về
                    onDismiss() // Đóng dialog
                }
            },
            valueText = { track ->
                // Format hiển thị mỗi track: Artist - Title (mm:ss)
                "${track.artistName} - ${track.trackName} (${
                    track.duration.seconds.toComponents { minutes, seconds, _ ->
                        "$minutes:${seconds.toString().padStart(2, '0')}"
                    }
                })"
            }
        )
    }
}
