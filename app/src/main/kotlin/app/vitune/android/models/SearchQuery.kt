package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    indices = [ //  tạo chỉ mục trên cột query.
        Index( // chỉ mục này sẽ giúp tăng tốc độ tìm kiếm các truy vấn trong bảng.
            value = ["query"],
            unique = true
        )
    ]
)
data class SearchQuery( // lớp này lưu trữ các truy vấn tìm kiếm
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String // query: chuỗi truy vấn mà người dùng đã tìm kiếm (ví dụ: "Taylor Swift" hay "Lofi chill").
)
