package com.viifo.frozencolumnlist.layout

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.FrozenColumnConfig
import com.viifo.frozencolumnlist.FrozenColumnPosition
import com.viifo.frozencolumnlist.FrozenColumnSide
import com.viifo.frozencolumnlist.R
import com.viifo.frozencolumnlist.layout.GenericStockAdapter.BaseViewHolder
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.provider.ColumnProvider
import com.viifo.frozencolumnlist.provider.FrozenColumnViewHolder
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
    /** 中间固定模式的左右区域点击回调 */
    var onSideClickListener: ((View, position: Int, FrozenColumnSide) -> Unit)? = null
    /** 中间固定模式的左右区域双击回调；设置后单击回调会延迟到双击判定结束。 */
    var onSideDoubleClickListener: ((View, position: Int, FrozenColumnSide) -> Unit)? = null

    /** EmptyView 点击事件监听回调 */
    var onEmptyViewClickListener: ((View) -> Unit)? = null
    /** EmptyView 子 View 点击事件监听回调 */
    var onEmptyViewChildClickListener: ((View) -> Unit)? = null

    /** FooterView 点击事件监听回调 */
    var onFooterViewClickListener: ((View) -> Unit)? = null
    /** FooterView 子 View 点击事件监听回调 */
    var onFooterViewChildClickListener: ((View) -> Unit)? = null

    var columnConfig: FrozenColumnConfig = FrozenColumnConfig()

    private val selectedLeftIds = mutableSetOf<String>()
    private val selectedRightIds = mutableSetOf<String>()

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
        return provider.getItemViewType(getItem(position))
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
                    it.itemView.setTag(R.id.tag_frozencolumnlist_content_row, false)
                    bindViewClickListener(it, viewType)
                }
            }
            FOOTER_VIEW -> {
                val footerLayoutVp: ViewParent? = footerLayout?.parent
                if (footerLayoutVp is ViewGroup) {
                    footerLayoutVp.removeView(footerLayout)
                }
                BaseViewHolder<T>(footerLayout ?: FrameLayout(parent.context)).also {
                    it.itemView.setTag(R.id.tag_frozencolumnlist_content_row, false)
                    bindViewClickListener(it, viewType)
                }
            }
            else -> {
                val columnCount = currentList.firstOrNull {
                    provider.getItemViewType(it) == viewType
                }?.columnCount ?: error("No data found for item viewType=$viewType")
                val rowContainer = provider.createItemRowView(parent, viewType, columnCount)
                require(rowContainer.childCount == columnCount) {
                    "createItemRowView must contain $columnCount direct column children, " +
                        "but was ${rowContainer.childCount}"
                }
                val frozenColumnStart = columnConfig.resolveFrozenStart(columnCount)
                rowContainer.setTag(R.id.tag_frozencolumnlist_content_row, true)
                rowContainer.layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    rowContainer.layoutParams?.height ?: RecyclerView.LayoutParams.WRAP_CONTENT
                )
                if (columnConfig.frozenColumnPosition == FrozenColumnPosition.MIDDLE) {
                    require(rowContainer is MiddleFrozenRowLayout) {
                        "MIDDLE mode row root must be MiddleFrozenRowLayout"
                    }
                    rowContainer.frozenColumnIndex = frozenColumnStart
                    rowContainer.frozenColumnCount = columnConfig.frozenColumnCount
                    rowContainer.frozenColumnStart = columnConfig.frozenViewportStart
                    rowContainer.visibleColumnCount = columnConfig.visibleColumnCount
                } else {
                    VisibleColumnWidthFitter.configure(
                        row = rowContainer,
                        frozenColumnStart = frozenColumnStart,
                        frozenColumnCount = columnConfig.frozenColumnCount,
                        visibleColumnCount = columnConfig.visibleColumnCount
                    )
                }
                val delegate = provider.createItemViewHolder(rowContainer, viewType)
                require(delegate.rowView === rowContainer) {
                    "FrozenColumnViewHolder.rowView must be the View returned by createItemRowView"
                }
                GenericViewHolder(itemView = rowContainer, delegate = delegate).also {
                    bindViewClickListener(it, viewType)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        if (holder is GenericViewHolder<T>) {
            holder.bind(getItem(position), emptyList())
            bindSideBackgrounds(holder, position)
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
                if (payloads.any { it != PAYLOAD_SIDE_BACKGROUND }) {
                    holder.bind(getItem(position), payloads)
                }
                bindSideBackgrounds(holder, position)
            }
        }
    }

    fun getChildClickViewIds(): LinkedHashSet<Int> {
        return childClickViewIds
    }

    fun setSideSelected(position: Int, side: FrozenColumnSide, selected: Boolean) {
        val id = currentList.getOrNull(position)?.id ?: return
        val set = if (side == FrozenColumnSide.LEFT) selectedLeftIds else selectedRightIds
        val changed = if (selected) set.add(id) else set.remove(id)
        if (changed) notifyItemChanged(position, PAYLOAD_SIDE_BACKGROUND)
    }

    fun toggleSideSelected(position: Int, side: FrozenColumnSide): Boolean {
        val selected = !isSideSelected(position, side)
        setSideSelected(position, side, selected)
        return selected
    }

    fun isSideSelected(position: Int, side: FrozenColumnSide): Boolean {
        val id = currentList.getOrNull(position)?.id ?: return false
        return (if (side == FrozenColumnSide.LEFT) selectedLeftIds else selectedRightIds).contains(id)
    }

    fun clearSideSelection(side: FrozenColumnSide? = null) {
        if (side == null || side == FrozenColumnSide.LEFT) selectedLeftIds.clear()
        if (side == null || side == FrozenColumnSide.RIGHT) selectedRightIds.clear()
        notifyItemRangeChanged(0, itemCount, PAYLOAD_SIDE_BACKGROUND)
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
        val isContentView = viewType != EMPTY_VIEW && viewType != FOOTER_VIEW
        val doubleTapSlop = ViewConfiguration.get(viewHolder.itemView.context).scaledDoubleTapSlop
        val doubleTapSlopSquared = doubleTapSlop * doubleTapSlop
        // 绑定 item 点击事件
        if (isContentView
            || (onEmptyViewClickListener != null && viewType == EMPTY_VIEW)
            || (onFooterViewClickListener != null && viewType == FOOTER_VIEW)) {
            var lastSideClickTime = 0L
            var lastSideClickItemId: String? = null
            var lastSideClick: FrozenColumnSide? = null
            var lastSideClickX = 0f
            var lastSideClickY = 0f
            var pendingSingleSideClick: Runnable? = null
            viewHolder.itemView.setOnClickListener { v ->
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                when (viewType) {
                    EMPTY_VIEW -> onEmptyViewClickListener?.invoke(v)
                    FOOTER_VIEW -> onFooterViewClickListener?.invoke(v)
                    else -> {
                        onItemClickListener?.invoke(v, position, viewType)
                        val row = v as? MiddleFrozenRowLayout
                        row?.sideAt(row.lastTouchX)?.let { side ->
                            val itemId = currentList.getOrNull(position)?.id ?: return@let
                            val doubleClickListener = onSideDoubleClickListener
                            if (doubleClickListener == null) {
                                pendingSingleSideClick?.let(v::removeCallbacks)
                                pendingSingleSideClick = null
                                lastSideClickTime = 0L
                                lastSideClickItemId = null
                                lastSideClick = null
                                onSideClickListener?.invoke(v, position, side)
                                return@let
                            }

                            val now = SystemClock.uptimeMillis()
                            val deltaX = row.lastTouchX - lastSideClickX
                            val deltaY = row.lastTouchY - lastSideClickY
                            val isDoubleClick = itemId == lastSideClickItemId &&
                                side == lastSideClick &&
                                now - lastSideClickTime in 0..DOUBLE_TAP_TIMEOUT_MS &&
                                deltaX * deltaX + deltaY * deltaY <= doubleTapSlopSquared.toFloat()
                            if (isDoubleClick) {
                                pendingSingleSideClick?.let(v::removeCallbacks)
                                pendingSingleSideClick = null
                                lastSideClickTime = 0L
                                lastSideClickItemId = null
                                lastSideClick = null
                                doubleClickListener.invoke(v, position, side)
                            } else {
                                lastSideClickTime = now
                                lastSideClickItemId = itemId
                                lastSideClick = side
                                lastSideClickX = row.lastTouchX
                                lastSideClickY = row.lastTouchY
                                if (onSideClickListener != null) {
                                    lateinit var singleClick: Runnable
                                    singleClick = Runnable {
                                        if (pendingSingleSideClick === singleClick) {
                                            pendingSingleSideClick = null
                                        }
                                        val currentPosition = viewHolder.bindingAdapterPosition
                                        if (currentPosition != RecyclerView.NO_POSITION &&
                                            currentList.getOrNull(currentPosition)?.id == itemId) {
                                            onSideClickListener?.invoke(v, currentPosition, side)
                                        }
                                    }
                                    pendingSingleSideClick = singleClick
                                    v.postDelayed(singleClick, DOUBLE_TAP_TIMEOUT_MS)
                                }
                            }
                        }
                    }
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

    private fun bindSideBackgrounds(holder: GenericViewHolder<T>, position: Int) {
        val row = holder.itemView as? MiddleFrozenRowLayout ?: return
        val data = getItem(position)
        row.leftBackgroundColor = provider.getSideBackgroundColor(
            row.context,
            data,
            FrozenColumnSide.LEFT,
            isSideSelected(position, FrozenColumnSide.LEFT)
        )
        row.rightBackgroundColor = provider.getSideBackgroundColor(
            row.context,
            data,
            FrozenColumnSide.RIGHT,
            isSideSelected(position, FrozenColumnSide.RIGHT)
        )
    }

    class GenericViewHolder<T: FrozenColumnData>(
        itemView: View,
        private val delegate: FrozenColumnViewHolder<T>
    ) : BaseViewHolder<T>(itemView) {

        fun bind(data: T, payloads: List<Any?>) = delegate.bind(data, payloads)

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
        private const val PAYLOAD_SIDE_BACKGROUND = "frozen_column_side_background"
        private val DOUBLE_TAP_TIMEOUT_MS = ViewConfiguration.getDoubleTapTimeout().toLong()

        /** 默认 DiffCallback 实现 */
        fun <T : FrozenColumnData> genericDiffCallback() = object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
        } as DiffUtil.ItemCallback<T>
    }

}
