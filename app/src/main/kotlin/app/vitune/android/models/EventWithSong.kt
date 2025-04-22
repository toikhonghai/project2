package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Relation

/*
EventWithSong không phải là một bảng trong cơ sở dữ liệu. Nó chỉ là một liên kết (mối quan hệ - relationship)
được Room tạo ra để truy vấn có tổ chức hơn, thường được gọi là Data Class Projection hay POJO kết hợp (Plain Old Java Object).
 */
@Immutable
data class EventWithSong(
    @Embedded val event: Event,
    @Relation(
        entity = Song::class, // Lớp Song được sử dụng để tạo mối quan hệ
        parentColumn = "songId", // parentColumn	Cột trong bảng đang chứa @Relation (ở đây là Event) để tham chiếu đến bảng liên kết.
        entityColumn = "id" // Cột trong bảng được liên kết (ở đây là Song) dùng để so sánh với parentColumn.
    )
    val song: Song
)
