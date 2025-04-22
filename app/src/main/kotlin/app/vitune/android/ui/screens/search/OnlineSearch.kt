package app.vitune.android.ui.screens.search

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.vitune.android.Database
import app.vitune.android.LocalPlayerAwareWindowInsets
import app.vitune.android.R
import app.vitune.android.models.SearchQuery
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.query
import app.vitune.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.vitune.android.ui.components.themed.Header
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.utils.align
import app.vitune.android.utils.center
import app.vitune.android.utils.disabled
import app.vitune.android.utils.medium
import app.vitune.android.utils.secondary
import app.vitune.compose.persist.persist
import app.vitune.compose.persist.persistList
import app.vitune.core.ui.LocalAppearance
import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.bodies.SearchSuggestionsBody
import app.vitune.providers.innertube.requests.searchSuggestions
import io.ktor.http.Url
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun OnlineSearch(
    textFieldValue: TextFieldValue, // Giá trị hiện tại của ô nhập liệu
    onTextFieldValueChange: (TextFieldValue) -> Unit, // Callback khi nội dung TextField thay đổi
    onSearch: (String) -> Unit, // Hàm thực hiện tìm kiếm
    onViewPlaylist: (String) -> Unit, // Hàm mở playlist nếu người dùng nhập link
    decorationBox: @Composable (@Composable () -> Unit) -> Unit, // Box để hiển thị placeholder trong TextField
    focused: Boolean, // Xác định TextField có đang focus hay không
    modifier: Modifier = Modifier // Modifier cho Box chứa toàn bộ UI
) = Box(modifier = modifier) {
    val (colorPalette, typography) = LocalAppearance.current // Lấy theme màu và font chữ từ context dùng chung
    val context = LocalContext.current

    var history by persistList<SearchQuery>("search/online/history") // Lưu và tự động khôi phục lịch sử tìm kiếm
    var suggestionsResult by persist<Result<List<String>?>?>("search/online/suggestionsResult") // Kết quả gợi ý từ API, có thể null hoặc lỗi
    var showVoiceSearchButton by remember { mutableStateOf(true) }


    // tao launcher cho speech recognizer
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult() // khởi tạo một intent để nhận diện giọng nói, rôi xử lý kết quả người dùng nói vào
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val speechResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) // Lấy danh sách kết quả nhận diện giọng nói
            speechResults?.getOrNull(0)?.let { recognizedText -> // lấy kết quả đầu tiên thường là chính xác nhất
                if (recognizedText.isNotEmpty()) {
                    onTextFieldValueChange(
                        TextFieldValue(
                            text = recognizedText,
                            selection = TextRange(recognizedText.length) // Đặt con trỏ ở cuối
                        )
                    )
                    onSearch(recognizedText) // Gọi hàm tìm kiếm với văn bản đã nhận diện
                }
            }
        }
    }
    // Launcher cho yeu cau microphone
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission() // Yêu cầu quyền truy cập microphone
    ) { isGranted -> // Xử lý kết quả yêu cầu quyền
        if (isGranted) {
            startVoiceRecognition(
                context,
                speechRecognizerLauncher
            ) // Nếu được cấp quyền, bắt đầu nhận diện giọng nói
        }
    }

    // Lắng nghe khi nội dung TextField thay đổi để lấy lịch sử tìm kiếm phù hợp
    LaunchedEffect(textFieldValue.text) {
        if (DataPreferences.pauseSearchHistory) return@LaunchedEffect // Nếu đang tắt lưu lịch sử thì bỏ qua

        Database.queries("%${textFieldValue.text}%") // Query dữ liệu có chứa text nhập
            .distinctUntilChanged { old, new -> old.size == new.size } // Tránh cập nhật nếu kết quả không thay đổi
            .collect { history = it.toImmutableList() } // Gán kết quả mới vào biến history
    }

    // Lắng nghe thay đổi nội dung để gọi API gợi ý tìm kiếm
    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.isEmpty()) return@LaunchedEffect // Không gọi nếu text rỗng

        delay(500) // Đợi 500ms để debounce tránh gọi API liên tục khi người dùng đang gõ

        suggestionsResult = Innertube.searchSuggestions(
            body = SearchSuggestionsBody(input = textFieldValue.text) // Gửi input để lấy gợi ý
        )
    }

    // Nếu nội dung nhập là link YouTube Playlist → trích playlistId từ URL
    val playlistId = remember(textFieldValue.text) {
        runCatching {
            Url(textFieldValue.text).takeIf {
                it.host.endsWith("youtube.com", ignoreCase = true) && // Đảm bảo là link YouTube
                        it.segments.lastOrNull()?.equals(
                            "playlist",
                            ignoreCase = true
                        ) == true // Có segment cuối là "playlist"
            }?.parameters?.get("list") // Lấy giá trị tham số "list" → playlistId
        }.getOrNull()
    }

    val focusRequester =
        remember { FocusRequester() } // Dùng để yêu cầu focus vào TextField khi cần
    val lazyListState =
        rememberLazyListState() // Lưu trạng thái cuộn của danh sách (nếu có LazyColumn bên dưới)

    LaunchedEffect(Unit) {
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
            0
        )
        showVoiceSearchButton = activities.size > 0
    }
    LazyColumn(
        state = lazyListState, // Sử dụng trạng thái cuộn đã được remember trước đó
        contentPadding = LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Vertical + WindowInsetsSides.End) // Chỉ lấy padding ở chiều dọc + phía phải (để tránh trùng player)
            .asPaddingValues(),
        modifier = Modifier.fillMaxSize() // Chiếm toàn bộ kích thước có thể
    ) {
        item(
            key = "header", // Key cố định cho item đầu tiên
            contentType = 0 // Type = 0 giúp tối ưu nếu có nhiều content type
        ) {
            val container =
                LocalPinnableContainer.current // Dùng để ghim giao diện header không bị mất khi cuộn

            // Ghim header lại khi composable được tạo
            DisposableEffect(Unit) {
                val handle =
                    container?.pin() // Ghim container (ví dụ header sẽ không bị pop ra khi scroll)

                onDispose {
                    handle?.release() // Bỏ ghim khi composable bị huỷ
                }
            }

            // Khi được focus, đợi 300ms rồi yêu cầu focus vào TextField
            LaunchedEffect(focused) {
                if (!focused) return@LaunchedEffect

                delay(300)
                focusRequester.requestFocus() // Đặt focus vào ô tìm kiếm
            }

            // Header bao gồm TextField và các nút phụ
            Header(
                titleContent = {
                    BasicTextField(
                        value = textFieldValue, // Giá trị hiện tại của ô nhập liệu
                        onValueChange = onTextFieldValueChange, // Callback khi người dùng gõ
                        textStyle = typography.xxl.medium.align(TextAlign.End), // Cỡ chữ + căn phải
                        singleLine = true, // Chỉ cho phép 1 dòng
                        maxLines = 1, // Không vượt quá 1 dòng
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), // Gợi ý nút tìm kiếm trên bàn phím
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (textFieldValue.text.isNotEmpty()) onSearch(textFieldValue.text) // Gọi hàm tìm kiếm khi người dùng nhấn nút "Search"
                            }
                        ),
                        cursorBrush = SolidColor(colorPalette.text), // Màu con trỏ nhập
                        decorationBox = decorationBox, // Box chứa placeholder hiển thị khi text rỗng
                        modifier = Modifier.focusRequester(focusRequester) // Cho phép điều khiển focus từ bên ngoài
                    )
                },
                actionsContent = {
                    // Nếu nội dung nhập là playlist link hợp lệ → hiện nút chuyển đến playlist
                    if (playlistId != null) {
                        val isAlbum =
                            playlistId.startsWith("OLAK5uy_") // Kiểm tra nếu là Album YouTube

                        SecondaryTextButton(
                            text = if (isAlbum) stringResource(R.string.view_album)
                            else stringResource(R.string.view_playlist),
                            onClick = { onViewPlaylist(textFieldValue.text) } // Mở playlist với URL đã nhập
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f)) // Đẩy các nút sang phải

                    // Them nut tim kiem bang giong noi
                    if (showVoiceSearchButton) {
                        Image(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = null,
                            colorFilter = ColorFilter.disabled,
                            modifier = Modifier
                                .clickable(
                                    indication = ripple(bounded = false),
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        when (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        )) {
                                            PackageManager.PERMISSION_GRANTED -> {
                                                startVoiceRecognition(
                                                    context,
                                                    speechRecognizerLauncher
                                                ) // Nếu đã có quyền, bắt đầu nhận diện giọng nói
                                            }

                                            else -> {
                                                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                ).padding(horizontal = 16.dp)
                                .size(24.dp)
                        )
                    }

                    // Nếu có text → hiện nút "Clear" để xoá
                    if (textFieldValue.text.isNotEmpty()) SecondaryTextButton(
                        text = stringResource(R.string.clear),
                        onClick = { onTextFieldValueChange(TextFieldValue()) } // Reset lại textField
                    )
                }
            )
        }

        items(
            items = history, // Danh sách lịch sử tìm kiếm, được lưu từ trước đó
            key = SearchQuery::id // Dùng ID làm key để tối ưu hiệu suất khi danh sách thay đổi
        ) { searchQuery -> // Mỗi phần tử là một SearchQuery

            Row(
                verticalAlignment = Alignment.CenterVertically, // Căn giữa theo chiều dọc
                modifier = Modifier
                    .clickable { onSearch(searchQuery.query) } // Khi click vào hàng → thực hiện tìm kiếm lại với query này
                    .fillMaxWidth() // Chiều rộng chiếm toàn bộ
                    .padding(all = 16.dp) // Padding đều 16dp
                    .animateItem() // Hiệu ứng chuyển động mượt mà khi thêm/xoá item
            ) {

                // Icon đồng hồ biểu tượng lịch sử tìm kiếm
                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(20.dp)
                        .paint(
                            painter = painterResource(R.drawable.time),
                            colorFilter = ColorFilter.disabled // Không đổi màu (hiển thị theo mặc định icon)
                        )
                )

                // Hiển thị nội dung truy vấn đã tìm
                BasicText(
                    text = searchQuery.query, // Văn bản là query
                    style = typography.s.secondary, // Kiểu chữ nhỏ, phụ
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .weight(1f) // Chiếm phần còn lại của hàng
                )

                // Nút xoá query khỏi lịch sử (icon hình dấu x)
                Image(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    colorFilter = ColorFilter.disabled,
                    modifier = Modifier
                        .clickable(
                            indication = ripple(bounded = false), // Hiệu ứng ripple không giới hạn vùng
                            interactionSource = remember { MutableInteractionSource() }, // Nguồn tương tác riêng
                            onClick = {
                                query {
                                    Database.delete(searchQuery) // Xoá khỏi database lịch sử tìm kiếm
                                }
                            }
                        )
                        .padding(horizontal = 8.dp)
                        .size(20.dp)
                )

                // Nút "chuyển query vào ô tìm kiếm" (icon mũi tên xoay lại)
                Image(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    colorFilter = ColorFilter.disabled,
                    modifier = Modifier
                        .clickable(
                            indication = ripple(bounded = false),
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                onTextFieldValueChange(
                                    TextFieldValue(
                                        text = searchQuery.query,
                                        selection = TextRange(searchQuery.query.length) // Đặt con trỏ ở cuối
                                    )
                                )
                            }
                        )
                        .rotate(225f) // Xoay icon mũi tên thành hình "quay lại"
                        .padding(horizontal = 8.dp)
                        .size(22.dp)
                )
            }
        }

        // Nếu suggestionsResult là thành công và không null → hiển thị danh sách gợi ý
        suggestionsResult?.getOrNull()?.let { suggestions ->
            items(items = suggestions) { suggestion -> // Duyệt từng gợi ý

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onSearch(suggestion) } // Click để tìm ngay với gợi ý
                        .fillMaxWidth()
                        .padding(all = 16.dp)
                ) {

                    // Icon tìm kiếm đứng trước
                    Spacer(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(20.dp)
                            .paint(
                                painter = painterResource(R.drawable.search),
                                colorFilter = ColorFilter.disabled // Không tô màu lại icon
                            )
                    )

                    // Hiển thị nội dung gợi ý
                    BasicText(
                        text = suggestion,
                        style = typography.s.secondary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .weight(1f)
                    )

                    // Nút "gán gợi ý vào TextField" (mũi tên quay lại)
                    Image(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        colorFilter = ColorFilter.disabled,
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    onTextFieldValueChange(
                                        TextFieldValue(
                                            text = suggestion,
                                            selection = TextRange(suggestion.length) // Đặt con trỏ cuối chuỗi
                                        )
                                    )
                                }
                            )
                            .rotate(225f) // Xoay mũi tên để biểu thị "điền lại"
                            .padding(horizontal = 8.dp)
                            .size(22.dp)
                    )
                }
            }

// Nếu `suggestionsResult` gặp lỗi thì hiển thị thông báo lỗi
        } ?: suggestionsResult?.exceptionOrNull()?.let {
            item {
                Box(modifier = Modifier.fillMaxSize()) {
                    BasicText(
                        text = stringResource(R.string.error_message), // Thông báo lỗi (ví dụ: "Đã xảy ra lỗi")
                        style = typography.s.secondary.center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState) // Nút cuộn lên đầu danh sach
}

private fun startVoiceRecognition(
    context: Context,
    launcher: ActivityResultLauncher<Intent>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) // Mô hình ngôn ngữ tự do
        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.speak_now)) // Thông báo yêu cầu người dùng nói
//        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN") // Ngôn ngữ nhận diện
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Nhận diện kết quả tạm thời
    }
    try {
        launcher.launch(intent)
    } catch (e: Exception) {
        e.printStackTrace() // In ra lỗi nếu có
    }
}
