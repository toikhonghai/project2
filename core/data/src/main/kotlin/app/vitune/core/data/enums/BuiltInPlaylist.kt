package app.vitune.core.data.enums

// hàm này định nghĩa các danh sách phát tích hợp sẵn trong ứng dụng
enum class BuiltInPlaylist(val sortable: Boolean) {
    Favorites(sortable = true),
    Offline(sortable = true),
    Top(sortable = false),
    History(sortable = false)
}
