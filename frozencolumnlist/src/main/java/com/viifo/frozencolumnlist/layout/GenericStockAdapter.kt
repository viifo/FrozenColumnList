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
import java.util.LinkedHashSet

/**
 * 通用 FrozenColumnList 列表适配器
 * @param provider 列视图提供器
 */
open class GenericStockAdapter<T: FrozenColumnData>(
    val provider: ColumnProvider<T>,
    diffCallback: DiffUtil.ItemCallback<T> = genericDiffCallback()
) : ListAdapter<T, GenericViewHolder<T>>(diffCallback) {

    var onItemClickListener: ((View, Int) -> Unit)? = null
    var onItemChildClickListener: ((View, Int) -> Unit)? = null

    /** 子项点击事件监听的 View ID 集合 */
    private val childClickViewIds = LinkedHashSet<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder<T> {
        // 固定列数量
        val frozenColumnCount = provider.getFrozenColumnCount()
        // 可滚动列数量
        val scrollableColumnCount = getItem(0).columnCount - frozenColumnCount
        // 外部行容器，使用 LinearLayoutCompat 水平排列
        val rowContainer = provider.createRowContainer(parent, viewType)
        // 调用 Provider 预生成 View
        val frozenViews = provider.createRowFrozenViews(rowContainer, viewType, frozenColumnCount)
        val scrollViews = provider.createRowScrollableViews(rowContainer, viewType, scrollableColumnCount)
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
        return GenericViewHolder(itemView = rowContainer, provider = provider).also {
            bindViewClickListener(it, viewType)
        }
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

    /**
     * 获取指定位置的 Item 数据
     * @param position Item 位置
     * @return Item 数据
     */
    fun getItemByPosition(position: Int): T? {
        return currentList.getOrNull(position)
    }

    fun getChildClickViewIds(): LinkedHashSet<Int> {
        return childClickViewIds
    }

    /**
     * 添加子项点击事件监听的 View ID 集合
     */
    fun addChildClickViewIds(@IdRes vararg viewIds: Int) {
        for (viewId in viewIds) {
            childClickViewIds.add(viewId)
        }
    }

    /**
     * 绑定点击事件
     */
    protected open fun bindViewClickListener(
        viewHolder: GenericViewHolder<out FrozenColumnData>,
        viewType: Int
    ) {
        // 绑定 item 点击事件
        onItemClickListener?.let { listener ->
            viewHolder.itemView.setOnClickListener { v ->
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                listener.invoke(v, position)
            }
        }
        // 绑定 item 子项点击事件
        onItemChildClickListener?.let { listener ->
            for (id in getChildClickViewIds()) {
                viewHolder.itemView.findViewById<View>(id)?.let { childView ->
                    if (!childView.isClickable) {
                        childView.isClickable = true
                    }
                    childView.setOnClickListener { v ->
                        val position = viewHolder.bindingAdapterPosition
                        if (position == RecyclerView.NO_POSITION) {
                            return@setOnClickListener
                        }
                        listener.invoke(v, position)
                    }
                }
            }
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