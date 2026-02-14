package com.viifo.frozencolumnlist.layout

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.FrozenColumConfig
import com.viifo.frozencolumnlist.layout.GenericStockAdapter.GenericViewHolder
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.ext.dp2px
import com.viifo.frozencolumnlist.provider.ColumnProvider

/**
 * 通用 FrozenColumnList 列表适配器
 * @param provider 列视图提供器
 */
class GenericStockAdapter<T: FrozenColumnData>(
    val provider: ColumnProvider<T>,
    diffCallback: DiffUtil.ItemCallback<T> = genericDiffCallback()
) : ListAdapter<T, GenericViewHolder<T>>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder<T> {
        // 固定列数量
        val frozenColumnCount = provider.getFrozenColumnCount()
        // 可滚动列数量
        val scrollableColumnCount = getItem(0).columnCount - frozenColumnCount
        // 外部行容器，使用 LinearLayoutCompat 水平排列
        val rowContainer = provider.createRowContainer(parent)
        // 调用 Provider 预生成 View
        val frozenViews = provider.createRowFrozenViews(rowContainer, frozenColumnCount)
        val scrollViews = provider.createRowScrollableViews(rowContainer, scrollableColumnCount)
        val viewWidths = provider.getColumnWidths(rowContainer, frozenViews.size + scrollViews.size)
        // 将 View 添加到外部行容器
        frozenViews.forEachIndexed { index, view ->
            rowContainer.addView(
                view,
                viewWidths.getOrNull(index) ?: parent.context.dp2px(FrozenColumConfig.DEFAULT_FROZEN_COLUMN_WITH_DP),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        scrollViews.forEachIndexed { index, view ->
            rowContainer.addView(
                view,
                viewWidths.getOrNull(frozenViews.size + index) ?: parent.context.dp2px(FrozenColumConfig.DEFAULT_COLUMN_WITH_DP),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        // 返回 ViewHolder
        return GenericViewHolder(itemView = rowContainer, provider = provider)
    }

    override fun onBindViewHolder(holder: GenericViewHolder<T>, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: GenericViewHolder<T>,
        position: Int,
        payloads: List<Any?>
    ) {
        if (payloads.isEmpty()) {
            // payloads 为空，执行全量刷新
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // payloads 不为空，执行局部刷新
            holder.diffBind(getItem(position), payloads)
        }
    }

    class GenericViewHolder<T: FrozenColumnData>(
        itemView: View,
        private val provider: ColumnProvider<T>
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(data: T) {
            provider.bindRowFrozenViews(this, data, emptyList())
            provider.bindRowScrollableViews(this, data, emptyList())
        }

        fun diffBind(data: T, payloads: List<Any?>) {
            provider.bindRowFrozenViews(this, data, payloads)
            provider.bindRowScrollableViews(this, data, payloads)
        }

        fun <V: View> getView(@IdRes id: Int): V = itemView.findViewById(id)

        fun setText(@IdRes id: Int, text: CharSequence?) {
            itemView.findViewById<TextView>(id)?.text = text
        }

    }

    companion object {
        /** 默认 DiffCallback 实现 */
        fun <T : FrozenColumnData> genericDiffCallback() = object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
        } as DiffUtil.ItemCallback<T>
    }

}