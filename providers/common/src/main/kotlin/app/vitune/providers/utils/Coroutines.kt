package app.vitune.providers.utils

import kotlinx.coroutines.CancellationException

/*
Xử lý luồng bất đồng bộ
Hàm runCatchingCancellable trong Kotlin được viết dưới dạng inline function với kiểu tổng quát <T>.
Nó dùng để thực thi một khối lệnh (block) và bắt các ngoại lệ phát sinh trong quá trình thực thi.
 */
inline fun <T> runCatchingCancellable(block: () -> T) =
    runCatching(block).takeIf { it.exceptionOrNull() !is CancellationException }
