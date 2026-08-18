package com.viifo.frozencolumnlist.provider

import android.view.ViewGroup
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData

/** 提供整行默认 ViewHolder，调用方只需实现 create...RowView 和 bind...Row。 */
abstract class DefaultColumnProvider<T : FrozenColumnData> : ColumnProvider<T> {

    final override fun createHeaderViewHolder(rowView: ViewGroup): FrozenHeaderViewHolder {
        return object : FrozenHeaderViewHolder {
            override val rowView: ViewGroup = rowView
            override fun bind(data: List<FrozenHeaderData>) = bindHeaderRow(this, data)
            override fun bindColumn(position: Int, data: FrozenHeaderData) {
                bindHeaderColumn(this, position, data)
            }
        }
    }

    final override fun createItemViewHolder(
        rowView: ViewGroup,
        viewType: Int
    ): FrozenColumnViewHolder<T> {
        return object : FrozenColumnViewHolder<T> {
            override val rowView: ViewGroup = rowView
            override fun bind(data: T, payloads: List<Any?>) = bindItemRow(this, data, payloads)
        }
    }

    abstract fun bindHeaderRow(holder: FrozenHeaderViewHolder, data: List<FrozenHeaderData>)

    open fun bindHeaderColumn(
        holder: FrozenHeaderViewHolder,
        position: Int,
        data: FrozenHeaderData
    ) {
        holder.getColumnView<android.widget.TextView>(position).text = data.name
    }

    abstract fun bindItemRow(
        holder: FrozenColumnViewHolder<T>,
        data: T,
        payloads: List<Any?>
    )
}
