package app.vitune.compose.routing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
//Annotation @Stable đến từ Jetpack Compose, cho biết class này ổn định, không có side-effect bất ngờ
// và Compose có thể tối ưu việc recomposition (vẽ lại màn hình) khi nó được sử dụng trong @Composable
@Stable
class RouteHandlerScope( // RouteHandlerScope là một lớp trong Jetpack Compose, dùng để quản lý các route (đường dẫn) trong ứng dụng.
    val child: Route?, // Route là một lớp đại diện cho một đường dẫn trong ứng dụng, có thể chứa các tham số và trạng thái khác nhau.
    val args: Array<Any?>, // args là một mảng các tham số được truyền vào route, có thể là bất kỳ kiểu dữ liệu nào (Any?).
    val replace: (Route?) -> Unit, // replace là một hàm dùng để thay thế route hiện tại bằng một route mới.
    override val pop: () -> Unit, // pop là một hàm dùng để quay lại route trước đó trong ngăn xếp (stack) của các route.
    val root: RootRouter // root là một đối tượng Router đại diện cho router gốc trong ứng dụng.
) : Router by root { // Router là một giao diện (interface) đại diện cho các chức năng điều hướng trong ứng dụng.
    @Composable
    inline fun Content(content: @Composable () -> Unit) { // content là một hàm composable được truyền vào, dùng để hiển thị nội dung của route hiện tại.
        if (child == null) content()
    }
}
