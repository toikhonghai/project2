package app.vitune.core.data.enums

enum class SortOrder {
    Ascending, // tăng dần
    Descending;

    operator fun not() = when (this) { // hàm này trả về giá trị ngược lại của SortOrder
        Ascending -> Descending
        Descending -> Ascending
    }
}
