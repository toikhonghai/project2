package app.vitune.providers.innertube.utils

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.SectionListRenderer

// Đây là một số hàm mở rộng và toán tử để xử lý các đối tượng trong thư viện Innertube.
internal fun SectionListRenderer.findSectionByTitle(text: String) = contents?.find { // tìm kiếm trong danh sách nội dung
    val title = it
        .musicCarouselShelfRenderer
        ?.header
        ?.musicCarouselShelfBasicHeaderRenderer // thông tin của một tiêu đề trong danh sách nhạc.
        ?.title
        ?: it
            .musicShelfRenderer // thông tin của một danh sách nhạc.
            ?.title

    title
        ?.runs
        ?.firstOrNull()
        ?.text == text
}

internal fun SectionListRenderer.findSectionByStrapline(text: String) = contents?.find { // tìm kiếm trong danh sách nội dung
    it
        .musicCarouselShelfRenderer
        ?.header
        ?.musicCarouselShelfBasicHeaderRenderer // thông tin của một tiêu đề trong danh sách nhạc.
        ?.strapline // thông tin của một tiêu đề trong danh sách nhạc.
        ?.runs
        ?.firstOrNull()
        ?.text == text
}
/*
infix	Cho phép gọi theo kiểu page1 + page2 thay vì page1.plus(page2).
operator fun	Xác định đây là một toán tử (+).
<T : Innertube.Item>	Khai báo T là kiểu dữ liệu phải kế thừa từ Innertube.Item.
Innertube.ItemsPage<T>?	this là một ItemsPage<T> (có thể null).
other: Innertube.ItemsPage<T>	other là một ItemsPage<T> không null.
: Innertube.ItemsPage<T>	Hàm trả về một ItemsPage<T> mới.
 */
infix operator fun <T : Innertube.Item> Innertube.ItemsPage<T>?.plus(other: Innertube.ItemsPage<T>) = // toán tử cộng để kết hợp hai trang nội dung
    other.copy(
        items = (this?.items?.plus(other.items ?: emptyList()) ?: other.items)
            ?.distinctBy(Innertube.Item::key), // loại bỏ các mục trùng lặp dựa trên khóa
        continuation = other.continuation ?: this?.continuation
    )
