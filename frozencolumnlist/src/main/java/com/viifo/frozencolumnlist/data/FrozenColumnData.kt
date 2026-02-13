package com.viifo.frozencolumnlist.data

/**
 * 列表数据基础模型
 * @param id 唯一标识
 * @param columnCount 列数量 (一行有多少列)
 */
open class FrozenColumnData(
    open val id: String?,
    open val columnCount: Int
) {

    override fun equals(other: Any?): Boolean {
        return (other is FrozenColumnData) && (other.id == this.id)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}