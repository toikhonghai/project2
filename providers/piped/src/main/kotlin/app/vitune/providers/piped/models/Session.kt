package app.vitune.providers.piped.models

import io.ktor.http.Url

// marker class
@JvmInline
// đây là một lớp giá trị (value class) có tên Session, nó chứa một cặp giá trị gồm Url và String
value class Session internal constructor(private val value: Pair<Url, String>) {
    val apiBaseUrl get() = value.first // lấy giá trị đầu tiên của cặp giá trị (Url)
    val token get() = value.second // lấy giá trị thứ hai của cặp giá trị (String)
}

infix fun Url.authenticatedWith(token: String) = Session(this to token) // tạo một phiên làm việc (session) với Url và token
infix fun String.authenticatedWith(token: String) = Url(this) authenticatedWith token // tạo một phiên làm việc (session) với Url và token
