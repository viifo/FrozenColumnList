package com.viifo.frozencolumnlist.provider

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import com.viifo.frozencolumnlist.FrozenColumnSide
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData

/** 自定义行 ViewHolder。整行 View 只在 createItemRowView 中创建或 inflate 一次。 */
interface FrozenColumnViewHolder<T : FrozenColumnData> {
    val rowView: ViewGroup
    fun bind(data: T, payloads: List<Any?>)

    @Suppress("UNCHECKED_CAST")
    fun <V : View> getView(@IdRes id: Int): V = rowView.findViewById(id)

    @Suppress("UNCHECKED_CAST")
    fun <V : View> getColumnView(columnIndex: Int): V = rowView.getChildAt(columnIndex) as V
}

/** 表头整行 ViewHolder。 */
interface FrozenHeaderViewHolder {
    val rowView: ViewGroup
    fun bind(data: List<FrozenHeaderData>)
    fun bindColumn(position: Int, data: FrozenHeaderData)

    @Suppress("UNCHECKED_CAST")
    fun <V : View> getView(@IdRes id: Int): V = rowView.findViewById(id)

    @Suppress("UNCHECKED_CAST")
    fun <V : View> getColumnView(columnIndex: Int): V = rowView.getChildAt(columnIndex) as V
}

/**
 * 2.0 整行 Provider API。
 *
 * 与旧版逐列 create/bind API 不兼容。XML 的直接子 View 必须与数据列一一对应；调用方仍可
 * 在 XML、createViewHolder 或 bind 中为每个子 View 设置样式。
 */
interface ColumnProvider<T : FrozenColumnData> {
    fun createEmptyView(context: Context): View? = null
    fun createFooterView(context: Context): View? = null

    /** 创建或从 XML inflate 完整表头，只调用一次。 */
    fun createHeaderRowView(parent: ViewGroup, columnCount: Int): ViewGroup
    fun createHeaderViewHolder(rowView: ViewGroup): FrozenHeaderViewHolder

    /** 每个 RecyclerView.ViewHolder 创建时只调用一次，可直接 inflate 完整行 XML。 */
    fun getItemViewType(data: T): Int = data.columnCount
    fun createItemRowView(parent: ViewGroup, viewType: Int, columnCount: Int): ViewGroup
    fun createItemViewHolder(rowView: ViewGroup, viewType: Int): FrozenColumnViewHolder<T>

    fun getSideBackgroundColor(
        context: Context,
        data: T,
        side: FrozenColumnSide,
        selected: Boolean
    ): Int = Color.TRANSPARENT
}
