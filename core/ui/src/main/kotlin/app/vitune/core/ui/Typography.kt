package app.vitune.core.ui

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.isAvailableOnDevice
import androidx.compose.ui.unit.sp
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.parcelableCreator

// Annotation để đánh dấu rằng class này có thể được serialize/deserialized khi sử dụng Parcelable
// @Parcelize: Giúp tự động triển khai Parcelable mà không cần viết nhiều code.
@Parcelize
// Đánh dấu class là Immutable để giúp đảm bảo tính bất biến
@Immutable
data class Typography(
    // Thuộc tính nội bộ để lưu trữ kiểu văn bản (TextStyle)
    internal val style: TextStyle,

    // Thuộc tính nội bộ để lưu trữ font family được sử dụng
    internal val fontFamily: BuiltInFontFamily
) : Parcelable { // Kế thừa Parcelable để có thể truyền dữ liệu giữa các thành phần của Android

    // Các biến lazy được khởi tạo khi cần, để tạo ra các kiểu chữ với kích thước font khác nhau
    val xxs by lazy { style.copy(fontSize = 12.sp) }
    val xs by lazy { style.copy(fontSize = 14.sp) }
    val s by lazy { style.copy(fontSize = 16.sp) }
    val m by lazy { style.copy(fontSize = 18.sp) }
    val l by lazy { style.copy(fontSize = 20.sp) }
    val xxl by lazy { style.copy(fontSize = 32.sp) }

    // Hàm copy để tạo một bản sao của Typography với màu sắc mới
    fun copy(color: Color) = Typography(
        style = style.copy(color = color),
        fontFamily = fontFamily
    )

    // Companion object đóng vai trò như một Parceler để hỗ trợ việc serialize/deserialize đối tượng Typography
    companion object : Parceler<Typography> {

        // Ghi dữ liệu của Typography vào Parcel
        override fun Typography.write(parcel: Parcel, flags: Int) = SavedTypography(
            color = style.color, // Lưu màu sắc của văn bản
            fontFamily = fontFamily, // Lưu font family
            includeFontPadding = style.platformStyle?.paragraphStyle?.includeFontPadding ?: false // Kiểm tra padding của font
        ).writeToParcel(parcel, flags) // Viết vào Parcel

        // Tạo một instance của Typography từ Parcel
        override fun create(parcel: Parcel) =
            parcelableCreator<SavedTypography>().createFromParcel(parcel).let {
                typographyOf(
                    color = it.color, // Đọc lại màu sắc từ parcel
                    fontFamily = it.fontFamily, // Đọc lại font family từ parcel
                    applyFontPadding = it.includeFontPadding // Đọc lại thông tin padding
                )
            }
    }
}


// Đánh dấu class này có thể được serialize/deserialized bằng Parcelable
@Parcelize
data class SavedTypography(
    val color: ParcelableColor, // Màu sắc của kiểu chữ, sử dụng một kiểu Parcelable để lưu trữ
    val fontFamily: BuiltInFontFamily, // Font family của kiểu chữ
    val includeFontPadding: Boolean // Có bao gồm font padding hay không
) : Parcelable // Kế thừa Parcelable để có thể truyền dữ liệu dễ dàng

// Tạo một provider để lấy font từ Google Fonts
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts", // Authority của Google Fonts trên thiết bị
    providerPackage = "com.google.android.gms", // Package name của dịch vụ Google Fonts
    certificates = R.array.com_google_android_gms_fonts_certs // Chứng chỉ để xác thực Google Fonts
)

// Hàm kiểm tra xem Google Fonts có sẵn trên thiết bị hay không
@Composable
fun googleFontsAvailable(): Boolean {
    val context = LocalContext.current // Lấy context hiện tại

    return runCatching {
        googleFontsProvider.isAvailableOnDevice(context.applicationContext) // Kiểm tra xem Google Fonts có trên thiết bị không
    }.getOrElse {
        it.printStackTrace() // In lỗi ra log nếu xảy ra exception
        if (it is IllegalStateException) Log.e(
            "Typography",
            "Google Fonts certificates don't match. Is the user using a VPN?" // Ghi log lỗi nếu chứng chỉ không khớp, có thể do VPN
        )
        false // Nếu có lỗi, trả về false
    }
}

// Danh sách các font Poppins với các độ dày (weight) khác nhau
private val poppinsFonts = listOf(
    Font(
        resId = R.font.poppins_w300, // Đường dẫn đến file font trong tài nguyên (res)
        weight = FontWeight.Light // Light (300)
    ),
    Font(
        resId = R.font.poppins_w400, // Font Normal (400)
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.poppins_w500, // Font Medium (500)
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.poppins_w600, // Font SemiBold (600)
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.poppins_w700, // Font Bold (700)
        weight = FontWeight.Bold
    )
)

// Tạo một FontFamily từ danh sách các font Poppins ở trên
private val poppinsFontFamily = FontFamily(poppinsFonts)

// Đánh dấu enum class này có thể được serialize/deserialized bằng Parcelable
@Parcelize
enum class BuiltInFontFamily(internal val googleFont: GoogleFont?) : Parcelable {
    Poppins(null), // Không sử dụng Google Fonts (sẽ dùng font Poppins được nhúng trong ứng dụng)
    Roboto(GoogleFont("Roboto")), // Sử dụng Google Font "Roboto"
    Montserrat(GoogleFont("Montserrat")), // Sử dụng Google Font "Montserrat"
    Nunito(GoogleFont("Nunito")), // Sử dụng Google Font "Nunito"
    Rubik(GoogleFont("Rubik")), // Sử dụng Google Font "Rubik"
    System(null); // Font mặc định của hệ thống

    // Companion object để hỗ trợ Parcelable cho enum class
    companion object : Parceler<BuiltInFontFamily> {
        // Ghi dữ liệu của enum vào Parcel bằng cách lưu tên enum dưới dạng String
        override fun BuiltInFontFamily.write(parcel: Parcel, flags: Int) = parcel.writeString(name)

        // Đọc dữ liệu từ Parcel và khôi phục về enum tương ứng
        override fun create(parcel: Parcel) = BuiltInFontFamily.valueOf(parcel.readString()!!)
    }
}

// Hàm tạo FontFamily từ một BuiltInFontFamily sử dụng Google Fonts (nếu có)
private fun googleFontsFamilyFrom(font: BuiltInFontFamily) = font.googleFont?.let {
    FontFamily(
        listOf(
            Font(
                googleFont = it, // Font từ Google
                fontProvider = googleFontsProvider, // Nhà cung cấp font (Google Fonts)
                weight = FontWeight.Light // Font nhẹ (Light)
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Normal // Font bình thường (Normal)
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Medium // Font trung bình (Medium)
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.SemiBold // Font đậm vừa (SemiBold)
            ),
            Font(
                googleFont = it,
                fontProvider = googleFontsProvider,
                weight = FontWeight.Bold // Font đậm (Bold)
            )
        ) + poppinsFonts // Thêm danh sách font Poppins vào FontFamily
    )
}

// Hàm tạo đối tượng Typography dựa trên tham số truyền vào
fun typographyOf(
    color: Color, // Màu sắc của văn bản
    fontFamily: BuiltInFontFamily, // Font family được sử dụng
    applyFontPadding: Boolean // Có sử dụng padding cho font hay không
): Typography {
    val textStyle = TextStyle(
        fontFamily = when {
            fontFamily == BuiltInFontFamily.System -> FontFamily.Default // Nếu là System, dùng font mặc định của hệ thống
            fontFamily.googleFont != null -> googleFontsFamilyFrom(fontFamily) // Nếu có Google Font, tạo từ Google Fonts
            else -> poppinsFontFamily // Nếu không, sử dụng font Poppins mặc định
        },
        fontWeight = FontWeight.Normal, // Trọng số mặc định là Normal
        color = color, // Áp dụng màu sắc truyền vào
        platformStyle = PlatformTextStyle(includeFontPadding = applyFontPadding) // Áp dụng padding font nếu cần
    )

    return Typography(
        style = textStyle,
        fontFamily = fontFamily
    )
}
