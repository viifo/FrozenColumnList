package com.viifo.frozencolumnlist.data

/**
 * 列表数据基础模型
 * @param id 唯一标识
 * @param columnCount 总列数量 (包括冻结(固定)列和可滚动列)
 */
open class FrozenColumnData(
    open val id: String?,
    open val columnCount: Int
) {

    override fun equals(other: Any?): Boolean {
        return (other is FrozenColumnData)
                && (other.id == this.id)
                && (other.columnCount == this.columnCount)
    }

    override fun hashCode(): Int {
        var result = columnCount
        result = 31 * result + (id?.hashCode() ?: 0)
        return result
    }

}