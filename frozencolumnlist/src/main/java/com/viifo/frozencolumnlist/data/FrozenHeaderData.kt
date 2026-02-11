package com.viifo.frozencolumnlist.data

/**
 * 列表表头数据基础模型
 */
open class FrozenHeaderData(
    open val id: Int?, // 列头 ID
    open val name: String?, // 列头名称
    open val sort: SortDirection? = null, // 排序方向，null 表示不支持排序
) {

    override fun equals(other: Any?): Boolean {
        return (other is FrozenHeaderData) && (other.id == this.id) && (other.sort == this.sort)
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (sort?.hashCode() ?: 0)
        return result
    }

}

