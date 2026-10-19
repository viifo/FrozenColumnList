package com.viifo.frozencolumnlist.provider

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.FrozenTextColumn
import com.viifo.frozencolumnlist.ext.dp2px
import com.viifo.frozencolumnlist.layout.FrozenTextRowLayout

/** Optional text renderer. Existing ColumnProvider / DefaultColumnProvider implementations still work. */
abstract class TextColumnProvider<T : FrozenColumnData> : ColumnProvider<T> {
    abstract fun getHeaderColumns(context: Context, data: List<FrozenHeaderData>): List<FrozenTextColumn>
    abstract fun getItemColumns(context: Context, data: T): List<FrozenTextColumn>

    open fun getMinimumRowHeight(context: Context, header: Boolean): Int = context.dp2px(if (header) 35 else 48)
    open fun createFrozenColumnView(context: Context, columnIndex: Int, header: Boolean): TextView =
        AppCompatTextView(context)

    /** Runs after standard text binding. Set native backgrounds, corners, elevation, etc. here. */
    open fun styleFrozenColumnView(view: TextView, columnIndex: Int, header: Boolean) = Unit

    private fun createRow(parent: ViewGroup, count: Int, header: Boolean) = FrozenTextRowLayout(
        parent.context, count, getMinimumRowHeight(parent.context, header),
        { context, index -> createFrozenColumnView(context, index, header) },
        { view, index -> styleFrozenColumnView(view, index, header) }
    )

    override fun createHeaderRowView(parent: ViewGroup, columnCount: Int): ViewGroup = createRow(parent, columnCount, true)
    override fun createItemRowView(parent: ViewGroup, viewType: Int, columnCount: Int): ViewGroup = createRow(parent, columnCount, false)

    override fun createHeaderViewHolder(rowView: ViewGroup): FrozenHeaderViewHolder {
        val row = rowView as FrozenTextRowLayout
        return object : FrozenHeaderViewHolder {
            override val rowView: ViewGroup = row
            private var headers: List<FrozenHeaderData> = emptyList()
            override fun bind(data: List<FrozenHeaderData>) {
                headers = data.toList()
                row.submitColumns(getHeaderColumns(row.context, headers))
            }
            override fun bindColumn(position: Int, data: FrozenHeaderData) {
                bind(headers.toMutableList().also { it[position] = data })
            }
        }
    }

    override fun createItemViewHolder(rowView: ViewGroup, viewType: Int): FrozenColumnViewHolder<T> {
        val row = rowView as FrozenTextRowLayout
        return object : FrozenColumnViewHolder<T> {
            override val rowView: ViewGroup = row
            override fun bind(data: T, payloads: List<Any?>) {
                // submitColumns keeps cached layouts for all unchanged fields, including payload binds.
                row.submitColumns(getItemColumns(row.context, data))
            }
        }
    }
}
