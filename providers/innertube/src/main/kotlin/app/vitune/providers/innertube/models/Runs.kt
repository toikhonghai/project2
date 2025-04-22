package app.vitune.providers.innertube.models

import kotlinx.serialization.Serializable

//xử lý danh sách văn bản có thể chứa các ký tự phân tách (SEPARATOR = " • ").
@Serializable
data class Runs(
    val runs: List<Run> = listOf() // Danh sách các đối tượng Run, mỗi đối tượng Run có thể chứa văn bản và một endpoint điều hướng.
) {
    companion object { // Đối tượng companion cho phép truy cập các thuộc tính và phương thức tĩnh.
        const val SEPARATOR = " • " // Ký tự phân tách được sử dụng để chia danh sách các đối tượng Run thành các nhóm.
    }

    val text: String
        get() = runs.joinToString("") { it.text.orEmpty() } // Phương thức này nối tất cả các văn bản trong danh sách runs thành một chuỗi duy nhất.

    fun splitBySeparator(): List<List<Run>> { // Phương thức này chia danh sách các đối tượng Run thành các nhóm dựa trên ký tự phân tách (SEPARATOR).
        return runs.flatMapIndexed { index, run -> // Sử dụng flatMapIndexed để lấy chỉ số và đối tượng Run.
            when {
                index == 0 || index == runs.lastIndex -> listOf(index)  // Nếu là phần tử đầu tiên hoặc cuối cùng, chỉ trả về chỉ số của nó.
                run.text == SEPARATOR -> listOf(index - 1, index + 1)  // Nếu là ký tự phân tách, trả về chỉ số của phần tử trước và sau nó.
                else -> emptyList() // Nếu không phải là ký tự phân tách, trả về danh sách rỗng.
            }
        }.windowed(size = 2, step = 2) { (from, to) -> runs.slice(from..to) }.let { // Sử dụng windowed để tạo danh sách các nhóm từ các chỉ số đã tìm được.
            it.ifEmpty { // Nếu danh sách rỗng, trả về danh sách chứa tất cả các đối tượng Run.
                listOf(runs)
            }
        }
    }

    @Serializable
    data class Run( // Đối tượng Run đại diện cho một đoạn văn bản có thể chứa một endpoint điều hướng.
        val text: String?,
        val navigationEndpoint: NavigationEndpoint? // Đối tượng chứa thông tin điều hướng, dùng khi đoạn văn bản có thể nhấp vào
    )
}

fun List<Runs.Run>.splitBySeparator(): List<List<Runs.Run>> { // Hàm mở rộng cho danh sách các đối tượng Run, chia danh sách thành các nhóm dựa trên ký tự phân tách " • ".
    val res = mutableListOf<List<Runs.Run>>() // Danh sách chứa các nhóm các đối tượng Run.
    var tmp = mutableListOf<Runs.Run>() // Danh sách tạm thời để chứa các đối tượng Run trong một nhóm.
    forEach { run ->
        if (run.text == " • ") {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}

fun <T> List<T>.oddElements() = filterIndexed { index, _ -> index % 2 == 0 } // Lọc các phần tử có chỉ số lẻ trong danh sách.
