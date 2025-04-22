package app.vitune.android.ui.screens.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.vitune.android.Database
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.models.ui.toUiMedia
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.query
import app.vitune.android.service.PlayerService
import app.vitune.android.transaction
import app.vitune.android.ui.components.BottomSheet
import app.vitune.android.ui.components.BottomSheetState
import app.vitune.android.ui.components.LocalMenuState
import app.vitune.android.ui.components.rememberBottomSheetState
import app.vitune.android.ui.components.themed.BaseMediaItemMenu
import app.vitune.android.ui.components.themed.IconButton
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.components.themed.SliderDialog
import app.vitune.android.ui.components.themed.SliderDialogBody
import app.vitune.android.ui.modifiers.PinchDirection
import app.vitune.android.ui.modifiers.onSwipe
import app.vitune.android.ui.modifiers.pinchToToggle
import app.vitune.android.utils.DisposableListener
import app.vitune.android.utils.Pip
import app.vitune.android.utils.forceSeekToNext
import app.vitune.android.utils.forceSeekToPrevious
import app.vitune.android.utils.positionAndDurationState
import app.vitune.android.utils.rememberEqualizerLauncher
import app.vitune.android.utils.rememberPipHandler
import app.vitune.android.utils.seamlessPlay
import app.vitune.android.utils.secondary
import app.vitune.android.utils.semiBold
import app.vitune.android.utils.shouldBePlaying
import app.vitune.android.utils.thumbnail
import app.vitune.compose.persist.PersistMapCleanup
import app.vitune.compose.routing.OnGlobalRoute
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.ThumbnailRoundness
import app.vitune.core.ui.collapsedPlayerProgressBar
import app.vitune.core.ui.utils.isLandscape
import app.vitune.core.ui.utils.px
import app.vitune.core.ui.utils.roundedShape
import app.vitune.core.ui.utils.songBundle
import app.vitune.providers.innertube.models.NavigationEndpoint
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue

@Composable
fun Player(
    layoutState: BottomSheetState, // lưu trạng thái của BottomSheet
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp
    ),
    windowInsets: WindowInsets = WindowInsets.systemBars
) =
    with(PlayerPreferences) { // with là một hàm mở rộng giúp truy cập các thuộc tính của PlayerPreferences
        val menuState = LocalMenuState.current
// Lấy trạng thái hiện tại của menu (mở/đóng, đang ở đâu, v.v.)

        val (colorPalette, typography, thumbnailCornerSize) = LocalAppearance.current
// Truy cập cấu hình giao diện như bảng màu, kiểu chữ và bo góc thumbnail

        val binder = LocalPlayerServiceBinder.current
// Lấy binder hiện tại để giao tiếp với dịch vụ phát nhạc (PlayerService)

        val pipHandler = rememberPipHandler()
// Tạo và ghi nhớ một PipHandler để điều khiển Picture-in-Picture (PiP)

        PersistMapCleanup(prefix = "queue/suggestions")
// Dọn dẹp các mục suggestion (gợi ý) trong hàng đợi có prefix là "queue/suggestions"

        var mediaItem by remember(binder) {
            mutableStateOf(
                value = binder?.player?.currentMediaItem,
                policy = neverEqualPolicy()
            )
        }
// Trạng thái bài hát hiện tại đang phát, sử dụng neverEqualPolicy để luôn trigger recomposition khi gán giá trị mới

        var shouldBePlaying by remember(binder) {
            mutableStateOf(binder?.player?.shouldBePlaying == true)
        }
// Biến cờ để biết player có đang phát nhạc không

        var likedAt by remember(mediaItem) {
            mutableStateOf(
                value = null,
                policy = object : SnapshotMutationPolicy<Long?> {
                    override fun equivalent(a: Long?, b: Long?): Boolean {
                        mediaItem?.mediaId?.let {
                            query {
                                Database.like(it, b)
                            }
                        }
                        return a == b
                    }
                }
            )
        }
// Trạng thái thời điểm "like" bài hát, gắn liền với mediaId, đồng thời tự động truy vấn DB khi thay đổi

        LaunchedEffect(mediaItem) {
            mediaItem?.mediaId?.let { mediaId ->
                Database
                    .likedAt(mediaId)
                    .distinctUntilChanged()
                    .collect { likedAt = it }
            }
        }
// Tự động lắng nghe sự thay đổi likedAt trong database tương ứng với bài hát đang phát

        binder?.player.DisposableListener { // DisposableListener giúp tự động hủy đăng ký khi không còn sử dụng
            object : Player.Listener {
                override fun onMediaItemTransition(newMediaItem: MediaItem?, reason: Int) {
                    mediaItem = newMediaItem
                }

                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    shouldBePlaying = player.shouldBePlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    shouldBePlaying = player.shouldBePlaying
                }
            }
        }
// Đăng ký listener cho player để cập nhật trạng thái bài hát và trạng thái phát khi có thay đổi

        val (position, duration) = binder?.player.positionAndDurationState()
// Lấy vị trí hiện tại và thời lượng tổng của bài hát đang phát

        val metadata = remember(mediaItem) { mediaItem?.mediaMetadata }
// Trích xuất metadata từ bài hát (ví dụ: tên, ca sĩ, album, ...)

        val extras = remember(metadata) { metadata?.extras?.songBundle }
// Trích xuất thông tin bổ sung từ metadata (nếu có)

        val horizontalBottomPaddingValues = windowInsets
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .asPaddingValues()
// Tính toán padding dựa trên insets của hệ thống (ví dụ tránh trùng thanh điều hướng)

        OnGlobalRoute {
            if (layoutState.expanded) layoutState.collapseSoft()
        }
// Khi chuyển màn hình (route), nếu layout đang mở rộng thì tự động thu gọn

        if (mediaItem != null) BottomSheet( // nếu mediaItem không null thì hiển thị BottomSheet
            state = layoutState,
            modifier = modifier.fillMaxSize(),
            onDismiss = {
                binder?.let { onDismiss(it) }
                layoutState.dismissSoft()
            },
            backHandlerEnabled = !menuState.isDisplayed,
            collapsedContent = { innerModifier ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // Khoảng cách ngang giữa các phần tử
                    verticalAlignment = Alignment.Top, // Căn các phần tử theo đỉnh trên
                    modifier = Modifier
                        .let { modifier ->
                            // Nếu cho phép vuốt để đóng player
                            if (horizontalSwipeToClose) modifier.onSwipe(
                                animateOffset = true, // Vuốt có hiệu ứng
                                onSwipeOut = { animationJob ->
                                    binder?.let { onDismiss(it) } // Gọi callback khi vuốt đóng
                                    animationJob.join() // Đợi hiệu ứng hoàn thành
                                    layoutState.dismissSoft() // Thu gọn lại giao diện mềm
                                }
                            ) else modifier
                        }
                        .fillMaxSize() // Chiếm toàn bộ diện tích của parent
                        .clip(shape) // Bo viền theo hình dạng (ví dụ: rounded corners)
                        .background(colorPalette.background1) // Màu nền lấy từ theme
                        .drawBehind {
                            // Vẽ tiến trình của bài nhạc phía dưới background
                            drawRect(
                                color = colorPalette.collapsedPlayerProgressBar, // Màu thanh tiến trình
                                topLeft = Offset.Zero, // Bắt đầu từ góc trên bên trái
                                size = Size(
                                    width = runCatching {
                                        size.width * (position.toFloat() / duration.absoluteValue) // Tính chiều dài theo % bài nhạc
                                    }.getOrElse { 0f }, // Nếu lỗi (ví dụ chia 0), dùng 0f
                                    height = size.height // Chiều cao toàn bộ khối
                                )
                            )
                        }
                        .then(innerModifier) // Gộp thêm Modifier khác nếu có
                        .padding(horizontalBottomPaddingValues) // Padding theo các cạnh dưới + ngang từ windowInsets
                ) {
                    Spacer(modifier = Modifier.width(2.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.height(Dimensions.items.collapsedPlayerHeight) // Chiều cao box bằng chiều cao player thu gọn
                    ) {
                        AsyncImage(
                            model = metadata?.artworkUri?.thumbnail(Dimensions.thumbnails.song.px), // Ảnh bài hát (nếu có)
                            contentDescription = null,
                            contentScale = ContentScale.Crop, // Cắt ảnh cho vừa khung, không làm méo
                            modifier = Modifier
                                .clip(thumbnailCornerSize.coerceAtMost(ThumbnailRoundness.Heavy.dp).roundedShape) // Bo góc ảnh
                                .background(colorPalette.background0) // Nền phía sau ảnh
                                .size(48.dp) // Kích thước cố định 48dp
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.Center, // Căn giữa theo chiều dọc
                        modifier = Modifier
                            .height(Dimensions.items.collapsedPlayerHeight) // Chiều cao giống box ảnh bên trái
                            .weight(1f) // Chiếm phần còn lại của hàng (sau Box ảnh)
                    ) {
                        // AnimatedContent dùng để chuyển đổi nội dung có hiệu ứng mượt mà (fade, slide, scale,...)
                        AnimatedContent(
                            targetState = metadata?.title?.toString().orEmpty(), // Tên bài hát
                            label = "",
                            transitionSpec = { fadeIn() togetherWith fadeOut() } // Hiệu ứng chuyển cảnh
                        ) { text ->
                            BasicText(
                                text = text,
                                style = typography.xs.semiBold, // Kiểu chữ nhỏ, in đậm
                                maxLines = 1, // Hiển thị 1 dòng
                                overflow = TextOverflow.Ellipsis // Nếu quá dài thì hiển thị dấu "..."
                            )
                        }
                        // AnimatedVisibility dùng để hiển thị hoặc ẩn một thành phần với hiệu ứng chuyển động
                        AnimatedVisibility(visible = metadata?.artist != null) { // Nếu có tên nghệ sĩ
                            AnimatedContent(
                                targetState = metadata?.artist?.toString().orEmpty(), // Tên nghệ sĩ
                                label = "",
                                transitionSpec = { fadeIn() togetherWith fadeOut() } // Hiệu ứng chuyển cảnh
                            ) { text ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, // Căn giữa theo trục dọc
                                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Khoảng cách giữa các item trong Row
                                ) {
                                    BasicText(
                                        text = text,
                                        style = typography.xs.semiBold.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    AnimatedVisibility(visible = extras?.explicit == true) { // Nếu bài hát là explicit (18+)
                                        Image(
                                            painter = painterResource(R.drawable.explicit), // Icon explicit
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(colorPalette.text), // Tint màu theo giao diện
                                            modifier = Modifier.size(15.dp) // Kích thước nhỏ
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp)) // Spacer tạo khoảng trống ngang 2dp ở đầu hàng nút điều khiển

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // Khoảng cách ngang giữa các nút là 12dp
                        verticalAlignment = Alignment.CenterVertically, // Căn giữa các nút theo chiều dọc
                        modifier = Modifier.height(Dimensions.items.collapsedPlayerHeight) // Chiều cao của hàng bằng chiều cao player thu gọn
                    ) {
                        // Nút tua lại bài trước, chỉ hiển thị nếu isShowingPrevButtonCollapsed = true
                        AnimatedVisibility(visible = isShowingPrevButtonCollapsed) {
                            IconButton(
                                icon = R.drawable.play_skip_back, // Icon tua lại
                                color = colorPalette.text, // Màu icon
                                onClick = { binder?.player?.forceSeekToPrevious() }, // Khi nhấn thì gọi hàm tua lại bài
                                modifier = Modifier
                                    .padding(
                                        horizontal = 4.dp,
                                        vertical = 8.dp
                                    ) // Padding bên trong nút
                                    .size(20.dp) // Kích thước nút
                            )
                        }

                        // Nút play/pause với hiệu ứng bo tròn và ripple khi click
                        Box(
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        // Nếu đang phát nhạc thì pause, ngược lại thì play
                                        if (shouldBePlaying) binder?.player?.pause()
                                        else {
                                            // Nếu player đang ở trạng thái IDLE (chưa phát gì), thì chuẩn bị lại player
                                            if (binder?.player?.playbackState == Player.STATE_IDLE) binder.player.prepare()
                                            binder?.player?.play()
                                        }
                                    },
                                    indication = ripple(bounded = false), // Hiệu ứng ripple không giới hạn
                                    interactionSource = remember { MutableInteractionSource() } // Nguồn tương tác riêng cho ripple
                                )
                                .clip(CircleShape) // Bo tròn nút
                        ) {
                            // Nút play/pause có hiệu ứng hoạt hình chuyển đổi trạng thái
                            AnimatedPlayPauseButton(
                                playing = shouldBePlaying, // Trạng thái phát hiện tại
                                modifier = Modifier
                                    .align(Alignment.Center) // Căn giữa trong Box
                                    .padding(
                                        horizontal = 4.dp,
                                        vertical = 8.dp
                                    ) // Padding trong nút
                                    .size(23.dp) // Kích thước nút
                            )
                        }

                        // Nút tua tới bài tiếp theo
                        IconButton(
                            icon = R.drawable.play_skip_forward, // Icon tua tới
                            color = colorPalette.text, // Màu icon
                            onClick = { binder?.player?.forceSeekToNext() }, // Khi nhấn thì gọi hàm tua tới bài tiếp
                            modifier = Modifier
                                .padding(
                                    horizontal = 4.dp,
                                    vertical = 8.dp
                                ) // Padding bên trong nút
                                .size(20.dp) // Kích thước nút
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        ) {
            // Biến trạng thái cho việc hiển thị các dialog hoặc overlay
            var isShowingStatsForNerds by rememberSaveable { mutableStateOf(false) } // Hiện thông tin kỹ thuật của nhạc
            var isShowingLyricsDialog by rememberSaveable { mutableStateOf(false) } // Hiện hộp thoại lời bài hát

// Nếu đang bật dialog lời bài hát thì hiển thị dialog
            if (isShowingLyricsDialog) LyricsDialog(onDismiss = { isShowingLyricsDialog = false })

// Tạo trạng thái cho bottom sheet player (để điều khiển việc kéo mở/mở rộng)
            val playerBottomSheetState = rememberBottomSheetState(
                dismissedBound = 64.dp + horizontalBottomPaddingValues.calculateBottomPadding(), // Chiều cao khi thu gọn
                expandedBound = layoutState.expandedBound // Chiều cao tối đa khi mở rộng
            )

// Modifier cho vùng chứa tổng thể, dùng để bo góc + nền gradient + padding
            val containerModifier = Modifier
                .clip(shape) // Bo góc layout
                .background(
                    Brush.verticalGradient( // Gradient từ background1 đến background0
                        0.5f to colorPalette.background1,
                        1f to colorPalette.background0
                    )
                )
                .padding( // Padding theo insets của hệ thống (status bar, navigation bar,...)
                    windowInsets
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                )
                .padding(bottom = playerBottomSheetState.collapsedBound) // Padding phía dưới bằng chiều cao thu gọn của player

            // Nội dung thumbnail của player, bao gồm ảnh, tương tác pinch, v.v...
            val thumbnailContent: @Composable (modifier: Modifier) -> Unit = { innerModifier ->
                Pip(
                    numerator = 1,
                    denominator = 1,
                    modifier = innerModifier
                ) {
                    Thumbnail(
                        isShowingLyrics = isShowingLyrics, // Trạng thái hiển thị lời bài hát nhỏ
                        onShowLyrics = { isShowingLyrics = it }, // Callback khi bật/tắt lời bài hát
                        isShowingStatsForNerds = isShowingStatsForNerds, // Trạng thái hiện stats
                        onShowStatsForNerds = {
                            isShowingStatsForNerds = it
                        }, // Callback khi bật/tắt stats
                        onOpenDialog = { isShowingLyricsDialog = true }, // Mở dialog lời bài hát
                        likedAt = likedAt, // Thời gian yêu thích bài hát
                        setLikedAt = { likedAt = it }, // Callback khi user yêu thích bài hát

                        modifier = Modifier
                            .nestedScroll(layoutState.preUpPostDownNestedScrollConnection) // Cho phép nested scroll khi player mở rộng
                            .pinchToToggle( // Pinch-out để mở dialog lời bài hát (zoom lớn)
                                key = isShowingLyricsDialog,
                                direction = PinchDirection.Out,
                                threshold = 1.05f,
                                onPinch = {
                                    if (isShowingLyrics) isShowingLyricsDialog = true
                                }
                            )
                            .pinchToToggle( // Pinch-in để bật Picture-in-Picture mode (thu nhỏ player)
                                key = isShowingLyricsDialog,
                                direction = PinchDirection.In,
                                threshold = .95f,
                                onPinch = {
                                    pipHandler.enterPictureInPictureMode()
                                }
                            )
                    )
                }
            }

            // Composable controlsContent dùng để chứa phần điều khiển nhạc như play/pause, seekbar,...
            val controlsContent: @Composable (modifier: Modifier) -> Unit = { innerModifier ->
                Controls(
                    media = mediaItem?.toUiMedia(duration), // Dữ liệu media chuyển sang UI format
                    binder = binder, // Binder tới player service
                    likedAt = likedAt, // Thời gian bài hát được thích
                    setLikedAt = { likedAt = it }, // Callback khi người dùng like
                    shouldBePlaying = shouldBePlaying, // Trạng thái đang phát hay tạm dừng
                    position = position, // Vị trí hiện tại của bài hát
                    modifier = innerModifier // Modifier truyền từ ngoài vào
                )
            }

// Nếu đang ở chế độ ngang (landscape)
            if (isLandscape) Row(
                verticalAlignment = Alignment.CenterVertically, // Căn giữa theo chiều dọc
                modifier = containerModifier.padding(top = 32.dp) // Padding phía trên 32dp
            ) {
                Box( // Box chứa thumbnail (ảnh bài hát, nút lyrics, stats,...)
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(0.66f) // Chiếm 2/3 chiều ngang
                        .padding(bottom = 16.dp)
                ) {
                    thumbnailContent(Modifier.padding(horizontal = 16.dp)) // Gọi thumbnail composable với padding
                }

                // Controls chiếm phần còn lại (1/3)
                controlsContent(
                    Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxHeight() // Chiều cao full parent
                        .weight(1f)
                )
            } else Column( // Nếu đang ở chế độ dọc (portrait)
                horizontalAlignment = Alignment.CenterHorizontally, // Căn giữa theo chiều ngang
                modifier = containerModifier.padding(top = 54.dp) // Padding top nhiều hơn chút để tránh status bar
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1.25f) // Thumbnail chiếm phần lớn hơn
                ) {
                    thumbnailContent(Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
                }

                controlsContent(
                    Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth() // Chiều ngang full parent
                        .weight(1f) // Chiếm phần còn lại
                )
            }

            // Biến trạng thái (được lưu qua process death) để kiểm soát việc mở dialog điều chỉnh âm thanh
            var audioDialogOpen by rememberSaveable { mutableStateOf(false) }

// Nếu dialog đang mở thì hiển thị hộp thoại chỉnh tốc độ phát và pitch (cao độ)
            if (audioDialogOpen) SliderDialog(
                onDismiss = { audioDialogOpen = false }, // Callback khi dialog đóng
                title = stringResource(R.string.playback_settings) // Tiêu đề dialog
            ) {
                // Thanh trượt điều chỉnh tốc độ phát (speed)
                SliderDialogBody(
                    provideState = {
                        // Ghi nhớ giá trị speed ban đầu để thanh trượt hiển thị đúng
                        remember(speed) { mutableFloatStateOf(speed) }
                    },
                    onSlideComplete = { speed = it }, // Khi người dùng kéo xong thì cập nhật
                    min = 0f,
                    max = 2f,
                    toDisplay = {
                        // Hiển thị giá trị dạng "1.00x", nếu nhỏ hơn gần 0 thì hiện "minimum"
                        if (it <= 0.01f) stringResource(R.string.minimum_speed_value)
                        else stringResource(R.string.format_multiplier, "%.2f".format(it))
                    },
                    steps = 39, // Số bước chia nhỏ trên thanh kéo (0.05 mỗi bước)
                    label = stringResource(R.string.playback_speed) // Nhãn hiển thị bên dưới thanh kéo
                )

                // Thanh trượt điều chỉnh cao độ (pitch)
                SliderDialogBody(
                    provideState = {
                        remember(pitch) { mutableFloatStateOf(pitch) }
                    },
                    onSlideComplete = { pitch = it }, // Cập nhật pitch sau khi kéo
                    min = 0f,
                    max = 2f,
                    toDisplay = {
                        if (it <= 0.01f) stringResource(R.string.minimum_speed_value)
                        else stringResource(R.string.format_multiplier, "%.2f".format(it))
                    },
                    steps = 39,
                    label = stringResource(R.string.playback_pitch)
                )

                // Nút "Reset" để khôi phục cả speed và pitch về mặc định (1.0)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.reset),
                        onClick = {
                            speed = 1f
                            pitch = 1f
                        }
                    )
                }
            }

            // Biến trạng thái kiểm soát hiển thị dialog tăng âm lượng
            var boostDialogOpen by rememberSaveable { mutableStateOf(false) }

            if (boostDialogOpen) {
                // Hàm lưu giá trị loudness boost vào cơ sở dữ liệu
                fun submit(state: Float) = transaction {
                    mediaItem?.mediaId?.let { mediaId ->
                        Database.setLoudnessBoost(
                            songId = mediaId,
                            loudnessBoost = state.takeUnless { it == 0f } // Nếu = 0f thì bỏ qua (coi như reset)
                        )
                    }
                }

                // Hiển thị dialog tùy chỉnh âm lượng
                SliderDialog(
                    onDismiss = { boostDialogOpen = false }, // Đóng dialog
                    title = stringResource(R.string.volume_boost)
                ) {
                    SliderDialogBody(
                        provideState = {
                            val state = remember { mutableFloatStateOf(0f) }

                            // Tự động load loudness boost hiện tại từ Database khi mediaItem thay đổi
                            LaunchedEffect(mediaItem) {
                                mediaItem?.mediaId?.let { mediaId ->
                                    Database
                                        .loudnessBoost(mediaId)
                                        .distinctUntilChanged()
                                        .collect { state.floatValue = it ?: 0f }
                                }
                            }

                            state
                        },
                        onSlideComplete = { submit(it) }, // Gửi giá trị khi người dùng chỉnh xong
                        min = -20f,
                        max = 20f,
                        toDisplay = { stringResource(R.string.format_db, "%.2f".format(it)) } // Hiển thị đơn vị dB
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Nút reset về 0 dB
                        SecondaryTextButton(
                            text = stringResource(R.string.reset),
                            onClick = { submit(0f) }
                        )
                    }
                }
            }

// Nếu player đang hoạt động thì hiển thị hàng đợi (queue)
            if (binder != null) Queue(
                layoutState = playerBottomSheetState,
                binder = binder,

                // Hiển thị trước danh sách phát (tùy vào layout)
                beforeContent = {
                    if (playerLayout == PlayerPreferences.PlayerLayout.New)
                        IconButton(
                            onClick = { trackLoopEnabled = !trackLoopEnabled }, // Bật/tắt chế độ lặp bài
                            icon = R.drawable.infinite,
                            enabled = trackLoopEnabled,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .size(20.dp)
                        )
                    else Spacer(modifier = Modifier.width(20.dp)) // Nếu không phải layout mới thì thêm khoảng trống
                },

                // Nút mở menu tùy chọn cho media hiện tại
                afterContent = {
                    IconButton(
                        icon = R.drawable.ellipsis_horizontal,
                        color = colorPalette.text,
                        onClick = {
                            mediaItem?.let {
                                // Mở menu phát nhạc (PlayerMenu)
                                menuState.display {
                                    PlayerMenu(
                                        onDismiss = menuState::hide, // Callback khi đóng menu
                                        mediaItem = it,
                                        binder = binder,

                                        // Mở dialog chỉnh tốc độ
                                        onShowSpeedDialog = { audioDialogOpen = true },

                                        // Mở dialog boost volume nếu người dùng bật volume normalization
                                        onShowNormalizationDialog = {
                                            boostDialogOpen = true
                                        }.takeIf { volumeNormalization }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .size(20.dp)
                    )
                },

                // Hiển thị dưới cùng màn hình
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = shape
            )
        }
    }

@Composable
@OptIn(UnstableApi::class)
// hiển thị các lựa chọn như equalizer, tốc độ, và tăng âm lượng tùy điều kiện.
private fun PlayerMenu(
    binder: PlayerService.Binder, // Kết nối với player service
    mediaItem: MediaItem, // Bài hát hoặc media hiện tại
    onDismiss: () -> Unit, // Callback khi menu đóng
    onShowSpeedDialog: (() -> Unit)? = null, // Optional: hiện dialog chỉnh tốc độ phát
    onShowNormalizationDialog: (() -> Unit)? = null // Optional: hiện dialog tăng giảm âm lượng
) {
    // Lấy launcher để mở trình chỉnh Equalizer với session ID của player
    val launchEqualizer by rememberEqualizerLauncher(audioSessionId = { binder.player.audioSessionId })

    // Hiển thị menu mặc định cho media item
    BaseMediaItemMenu(
        mediaItem = mediaItem, // Truyền bài hát hiện tại

        // Bắt đầu phát "radio mode" từ media hiện tại
        onStartRadio = {
            binder.stopRadio() // Dừng radio hiện tại (nếu có)
            binder.player.seamlessPlay(mediaItem) // Phát lại media mượt mà
            binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)) // Tạo danh sách radio
        },

        // Mở trình chỉnh equalizer
        onGoToEqualizer = launchEqualizer,

        // Tạm thời không xử lý sleep timer
        onShowSleepTimer = {},

        // Khi đóng menu
        onDismiss = onDismiss,

        // Truyền các callback tùy chọn để mở speed & normalization dialog
        onShowSpeedDialog = onShowSpeedDialog,
        onShowNormalizationDialog = onShowNormalizationDialog
    )
}

// Hàm đóng menu: dừng radio và xóa tất cả media đang phát
private fun onDismiss(binder: PlayerService.Binder) {
    binder.stopRadio() // Dừng radio
    binder.player.clearMediaItems() // Xóa toàn bộ danh sách phát
}

