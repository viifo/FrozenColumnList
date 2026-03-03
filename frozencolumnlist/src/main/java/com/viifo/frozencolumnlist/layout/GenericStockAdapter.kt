package com.viifo.frozencolumnlist.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.FrozenColumConfig
import com.viifo.frozencolumnlist.layout.GenericStockAdapter.BaseViewHolder
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
) : ListAdapter<T, BaseViewHolder<T>>(diffCallback) {

    /** Item 点击事件监听回调 */
    var onItemClickListener: ((View, position: Int, itemViewType: Int) -> Unit)? = null
    /** Item 子 View 点击事件监听回调 */
    var onItemChildClickListener: ((View, position: Int, itemViewType: Int) -> Unit)? = null

    /** EmptyView 点击事件监听回调 */
    var onEmptyViewClickListener: ((View) -> Unit)? = null
    /** EmptyView 子 View 点击事件监听回调 */
    var onEmptyViewChildClickListener: ((View) -> Unit)? = null

    /** FooterView 点击事件监听回调 */
    var onFooterViewClickListener: ((View) -> Unit)? = null
    /** FooterView 子 View 点击事件监听回调 */
    var onFooterViewChildClickListener: ((View) -> Unit)? = null

    /** 默认 Item 视图宽度 */
    var defaultItemWidth: Int = LinearLayoutCompat.LayoutParams.WRAP_CONTENT

    /** 默认 Item 冻结(固定)视图宽度*/
    var defaultItemFrozenWidth: Int = LinearLayoutCompat.LayoutParams.WRAP_CONTENT

    /** 子项点击事件监听的 View ID 集合 */
    private val childClickViewIds = LinkedHashSet<Int>()

    /** 空视图容器 */
    private var emptyLayout: FrameLayout? = null
    /** 底部视图容器 */
    private var footerLayout: FrameLayout? = null

    open fun getContentItemCount(): Int {
        return super.getItemCount()
    }

    override fun getItemCount(): Int {
        return if (hasEmptyView()) {
            1
        } else {
            val footerViewViewCount = if (hasFooterLayout()) 1 else 0
            getContentItemCount() + footerViewViewCount
        }
    }

    open fun getContentItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasEmptyView()) {
            EMPTY_VIEW
        } else if (position < currentList.size) {
            getContentItemViewType(position)
        } else {
            FOOTER_VIEW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
        return when (viewType) {
            EMPTY_VIEW -> {
                val emptyLayoutVp: ViewParent? = emptyLayout?.parent
                if (emptyLayoutVp is ViewGroup) {
                    emptyLayoutVp.removeView(emptyLayout)
                }
                BaseViewHolder<T>(emptyLayout ?: FrameLayout(parent.context)).also {
                    bindViewClickListener(it, viewType)
                }
            }
            FOOTER_VIEW -> {
                val footerLayoutVp: ViewParent? = footerLayout?.parent
                if (footerLayoutVp is ViewGroup) {
                    footerLayoutVp.removeView(footerLayout)
                }
                BaseViewHolder<T>(footerLayout ?: FrameLayout(parent.context)).also {
                    bindViewClickListener(it, viewType)
                }
            }
            else -> {
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
                        viewWidths.getOrNull(index) ?: defaultItemFrozenWidth,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                scrollViews.forEachIndexed { index, view ->
                    rowContainer.addView(
                        view,
                        viewWidths.getOrNull(frozenViews.size + index) ?: defaultItemWidth,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                // 返回 ViewHolder
                GenericViewHolder(itemView = rowContainer, provider = provider).also {
                    bindViewClickListener(it, viewType)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        if (holder is GenericViewHolder<T>) {
            holder.bind(getItem(position))
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<T>,
        position: Int,
        payloads: List<Any?>
    ) {
        if (payloads.isEmpty()) {
            // payloads 为空，执行全量刷新
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // payloads 不为空，执行局部刷新
            if (holder is GenericViewHolder<T>) {
                holder.diffBind(getItem(position), payloads)
            }
        }
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
        viewHolder: BaseViewHolder<T>,
        viewType: Int
    ) {
        // 绑定 item 点击事件
        if (onItemClickListener != null
            || (onEmptyViewClickListener != null && viewType == EMPTY_VIEW)
            || (onFooterViewClickListener != null && viewType == FOOTER_VIEW)) {
            viewHolder.itemView.setOnClickListener { v ->
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                when (viewType) {
                    EMPTY_VIEW -> onEmptyViewClickListener?.invoke(v)
                    FOOTER_VIEW -> onFooterViewClickListener?.invoke(v)
                    else -> onItemClickListener?.invoke(v, position, viewType)
                }
            }
        }
        // 绑定 item 子项点击事件
        if (onItemChildClickListener != null
            || (onEmptyViewChildClickListener != null && viewType == EMPTY_VIEW)
            || (onFooterViewChildClickListener != null && viewType == FOOTER_VIEW)) {
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
                        when (viewType) {
                            EMPTY_VIEW -> onEmptyViewChildClickListener?.invoke(v)
                            FOOTER_VIEW -> onFooterViewChildClickListener?.invoke(v)
                            else -> onItemChildClickListener?.invoke(v, position, viewType)
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置空视图
     */
    fun setupEmptyView(context: Context, emptyView: View?) {
        if (emptyView == null) return
        val container = emptyLayout
        val finalContainer = if (container == null) {
            val newContainer = FrameLayout(context)
            emptyLayout = newContainer
            newContainer.layoutParams = ViewGroup.LayoutParams(
                emptyView.layoutParams?.width ?: ViewGroup.LayoutParams.MATCH_PARENT,
                emptyView.layoutParams?.height ?: ViewGroup.LayoutParams.MATCH_PARENT
            )
            newContainer
        } else {
            emptyView.layoutParams?.let {
                val lp: ViewGroup.LayoutParams? = container.layoutParams
                lp?.width = it.width
                lp?.height = it.height
                container.layoutParams = lp
            }
            container
        }
        finalContainer.removeAllViews()
        finalContainer.addView(emptyView)
    }

    /**
     * 设置底部视图
     */
    fun setupFooterView(context: Context, footerView: View?) {
        if (footerView == null) return
        val container = footerLayout
        val finalContainer = if (container == null) {
            val newContainer = FrameLayout(context)
            footerLayout = newContainer
            newContainer.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            newContainer
        } else {
            container
        }
        finalContainer.removeAllViews()
        finalContainer.addView(footerView)
    }

    /**
     * 是否有空视图
     */
    fun hasEmptyView(): Boolean {
        if (emptyLayout == null || (emptyLayout?.childCount ?: 0) == 0) {
            return false
        }
        return currentList.isEmpty()
    }

    /**
     * 是否有底部视图
     */
    fun hasFooterLayout(): Boolean {
        return (footerLayout?.childCount ?: 0) > 0
    }

    class GenericViewHolder<T: FrozenColumnData>(
        itemView: View,
        private val provider: ColumnProvider<T>
    ) : BaseViewHolder<T>(itemView) {

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

    open class BaseViewHolder<T: FrozenColumnData>(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        const val EMPTY_VIEW = 0x1000001
        const val HEADER_VIEW = 0x1000002 // 保留
        const val FOOTER_VIEW = 0x1000003

        /** 默认 DiffCallback 实现 */
        fun <T : FrozenColumnData> genericDiffCallback() = object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
        } as DiffUtil.ItemCallback<T>
    }

}