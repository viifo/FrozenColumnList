package com.viifo.frozencolumnlist.data

/**
 * 列表数据基础模型
 */
open class FrozenColumnData(open val id: String?) {

    override fun equals(other: Any?): Boolean {
        return (other is FrozenColumnData) && (other.id == this.id)
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}