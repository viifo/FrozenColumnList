package com.viifo.frozencolumnlist

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.GenericStockAdapter.GenericViewHolder
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.provider.ColumnProvider

/**
 * 通用 FrozenColumnList 列表适配器
 * @param provider 列视图提供器
 */
class GenericStockAdapter<T: FrozenColumnData>(
    val provider: ColumnProvider<T>,
    diffCallback: DiffUtil.ItemCallback<T> = genericDiffCallback()
) : ListAdapter<T, GenericViewHolder<T>>(diffCallback) {

    /** 固定列数量 */
    var mFrozenColumnCount: Int = 1
    /** 可滚动列数量 */
    var mScrollableColumnCount: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder<T> {
        // 外部行容器，使用 LinearLayoutCompat 水平排列
        val rowContainer = provider.createRowContainer(parent)
        // 调用 Provider 预生成 View
        val frozenViews = provider.createFrozenViews(rowContainer, mFrozenColumnCount)
        val scrollViews = provider.createScrollableViews(rowContainer, mScrollableColumnCount)
        val viewWidths = provider.getColumnWidths(rowContainer, frozenViews.size + scrollViews.size)
        // 将 View 添加到外部行容器
        frozenViews.forEachIndexed { index, view ->
            rowContainer.addView(
                view,
                viewWidths[index],
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        scrollViews.forEachIndexed { index, view ->
            rowContainer.addView(
                view,
                viewWidths[frozenViews.size + index],
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        // 返回 ViewHolder
        return GenericViewHolder(
            itemView = rowContainer,
            frozenViews = frozenViews,
            scrollViews = scrollViews,
            provider = provider
        )
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
        private val frozenViews: List<View>,
        private val scrollViews: List<View>,
        private val provider: ColumnProvider<T>
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(data: T) {
            provider.bindFrozenViews(frozenViews, data, emptyList())
            provider.bindScrollableViews(scrollViews, data, emptyList())
        }

        fun diffBind(data: T, payloads: List<Any?>) {
            provider.bindFrozenViews(frozenViews, data, payloads)
            provider.bindScrollableViews(scrollViews, data, payloads)
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