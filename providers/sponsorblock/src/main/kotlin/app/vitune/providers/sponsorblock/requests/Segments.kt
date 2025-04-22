package app.vitune.providers.sponsorblock.requests

import app.vitune.providers.sponsorblock.SponsorBlock
import app.vitune.providers.sponsorblock.models.Action
import app.vitune.providers.sponsorblock.models.Category
import app.vitune.providers.sponsorblock.models.Segment
import app.vitune.providers.utils.SerializableUUID
import app.vitune.providers.utils.runCatchingCancellable
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/// Hàm mở rộng `segments` cho đối tượng `SponsorBlock` để lấy thông tin các đoạn skip trong video
suspend fun SponsorBlock.segments(
    videoId: String, // ID của video cần lấy thông tin các đoạn skip
    categories: List<Category>? = listOf(Category.Sponsor, Category.OfftopicMusic, Category.PoiHighlight), // Các danh mục muốn lọc (mặc định là Sponsor, OfftopicMusic, PoiHighlight)
    actions: List<Action>? = listOf(Action.Skip, Action.POI), // Các hành động muốn lọc (mặc định là Skip và POI)
    segments: List<SerializableUUID>? = null // Danh sách các UUID của các đoạn video cần tìm kiếm (mặc định là null)
) = runCatchingCancellable {
    // Gửi một yêu cầu GET tới API của SponsorBlock để lấy thông tin các đoạn skip
    httpClient.get("/api/skipSegments") {
        // Thêm tham số videoId vào URL
        parameter("videoID", videoId)

        // Nếu danh sách categories không rỗng, thêm từng category vào tham số URL
        if (!categories.isNullOrEmpty()) categories.forEach { parameter("category", it.serialName) }

        // Nếu danh sách actions không rỗng, thêm từng action vào tham số URL
        if (!actions.isNullOrEmpty()) actions.forEach { parameter("action", it.serialName) }

        // Nếu danh sách segments không rỗng, thêm từng UUID vào tham số URL
        if (!segments.isNullOrEmpty()) segments.forEach { parameter("requiredSegment", it) }

        // Thêm tham số service với giá trị "YouTube"
        parameter("service", "YouTube")
    }.body<List<Segment>>() // Lấy kết quả trả về và chuyển thành danh sách các đối tượng `Segment`
}

