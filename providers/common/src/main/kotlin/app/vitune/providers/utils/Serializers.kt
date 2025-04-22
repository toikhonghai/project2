package app.vitune.providers.utils

import io.ktor.http.Url
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

//Quản lý serialize/deserialize dữ liệu (chuyển đổi JSON).
//KSerializer là một interface trong Kotlin Serialization giúp định nghĩa cách serialize và deserialize các kiểu dữ liệu tùy chỉnh.
//KSerializer<Url>: Định nghĩa cách serialize và deserialize cho kiểu Url.
object UrlSerializer : KSerializer<Url> {
    override val descriptor = PrimitiveSerialDescriptor("Url", PrimitiveKind.STRING)// Mô tả kiểu dữ liệu trong JSON (dạng chuỗi)
    override fun deserialize(decoder: Decoder) = Url(decoder.decodeString())// Chuyển một chuỗi JSON thành đối tượng `URL`
    override fun serialize(encoder: Encoder, value: Url) = encoder.encodeString(value.toString()) // Chuyển đối tượng `URL` thành chuỗi JSON
}
// Tạo alias cho `URL`, giúp sử dụng @Serializable dễ dàng hơn
typealias SerializableUrl = @Serializable(with = UrlSerializer::class) Url
// Serializer cho `LocalDateTime` theo chuẩn ISO 8601
object Iso8601DateSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("Iso8601LocalDateTime", PrimitiveKind.STRING)// Mô tả kiểu dữ liệu trong JSON (dạng chuỗi)
    override fun deserialize(decoder: Decoder) = LocalDateTime.parse(decoder.decodeString().removeSuffix("Z")) // Chuyển chuỗi JSON (ISO 8601) thành `LocalDateTime`
    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.toString()) // Chuyển `LocalDateTime` thành chuỗi JSON
}
// Tạo alias giúp sử dụng @Serializable dễ dàng hơn
typealias SerializableIso8601Date = @Serializable(with = Iso8601DateSerializer::class) LocalDateTime
// Serializer cho `UUID`
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)// Mô tả kiểu dữ liệu trong JSON (dạng chuỗi)
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())// Chuyển chuỗi JSON thành `UUID`
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())// Chuyển `UUID` thành chuỗi JSON
}

typealias SerializableUUID = @Serializable(with = UUIDSerializer::class) UUID // Tạo alias giúp sử dụng @Serializable dễ dàng hơn

/*
UUID (Universally Unique Identifier) là một định danh duy nhất toàn cầu,
thường được sử dụng để nhận diện một thực thể nào đó trong hệ thống máy tính,
như người dùng, phiên làm việc (session), đối tượng trong cơ sở dữ liệu, v.v.
 */