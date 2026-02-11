package com.viifo.frozencolumnlist.data

/**
 * 排序方向
 */
sealed class SortDirection(val value: Int) {
    object None : SortDirection(-1) // 默认排序
    object Desc : SortDirection(0) // 降序
    object Asc : SortDirection(1) // 升序
}