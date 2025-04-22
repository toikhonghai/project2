package app.vitune.providers.innertube

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

// JavaScriptChallenge là một lớp đại diện cho một thử thách mã hóa JavaScript, thường được sử dụng trong các ứng dụng web để bảo vệ nội dung hoặc dữ liệu khỏi việc bị truy cập trái phép.
data class JavaScriptChallenge(
    val timestamp: String, // Dấu thời gian khi mã JavaScript được tải
    val source: String, // Đoạn mã JavaScript chứa logic giải mã.
    val functionName: String // Tên hàm trong mã JavaScript sẽ được gọi để giải mã
) {
    private val cache = mutableMapOf<String, String>() // Bộ nhớ cache để lưu trữ kết quả giải mã
    private val mutex = Mutex() // Đảm bảo tính đồng bộ khi nhiều coroutine gọi decode() cùng lúc.

    suspend fun decode(cipher: String) = mutex.withLock { // Hàm decode() nhận một chuỗi cipher và giải mã nó, Dùng mutex.withLock để tránh xung đột nếu có nhiều coroutine truy cập đồng thời.
        cache.getOrPut(cipher) { // kiểm tra nếu chuỗi đã được giải mã trước đó, thì không cần gọi JavaScript lại.
            with(Context.enter()) { // Nhập vào ngữ cảnh JavaScript để thực thi mã.
                languageVersion = Context.VERSION_ES6
                optimizationLevel = -1 // Tắt tối ưu hóa để dễ dàng gỡ lỗi
                val scope = initSafeStandardObjects() // Khởi tạo đối tượng ngữ cảnh an toàn
                //Ngăn chặn mã JavaScript truy cập vào document, window, hoặc XMLHttpRequest.
                scope.defineProperty( // Định nghĩa các thuộc tính trong ngữ cảnh JavaScript
                    "document",
                    Context.getUndefinedValue(),
                    ScriptableObject.EMPTY
                )
                scope.defineProperty(
                    "window",
                    Context.getUndefinedValue(),
                    ScriptableObject.EMPTY
                )
                scope.defineProperty(
                    "XMLHttpRequest",
                    Context.getUndefinedValue(),
                    ScriptableObject.EMPTY
                )
                evaluateString(scope, source, functionName, 1, null) // Thực thi mã JavaScript
                val function = scope.get(functionName, scope) as Function
                function.call(this, scope, scope, arrayOf(cipher)).toString()
                    .also { Context.exit() }
            }
        }
    }
}
