package com.viifo.frozencolumnlist

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.IdRes
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.ext.dp2px
import com.viifo.frozencolumnlist.layout.FrozenColumnLayoutManager
import com.viifo.frozencolumnlist.layout.GenericStockAdapter
import com.viifo.frozencolumnlist.layout.MiddleFrozenColumnLayoutManager
import com.viifo.frozencolumnlist.layout.MiddleFrozenRowLayout
import com.viifo.frozencolumnlist.provider.ColumnProvider
import com.viifo.frozencolumnlist.provider.SpringBackAnimatorProvider
import kotlin.math.abs

/**
 * 统一固定列列表。通过 [FrozenColumnConfig] 支持前、中间或末尾 n 列固定。
 */
class FrozenColumnList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    var columnConfig: FrozenColumnConfig = FrozenColumnConfig()
        private set

    private var leadingLayoutManager: FrozenColumnLayoutManager? = null
    private var middleLayoutManager: MiddleFrozenColumnLayoutManager? = null
    private var genericStockAdapter: GenericStockAdapter<out FrozenColumnData>? = null
    private var attachedHeader: FrozenColumnHeader? = null
    var provider: ColumnProvider<out FrozenColumnData>? = null
        private set

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    var horizontalScrollThreshold: Int = touchSlop
        set(value) { field = value.coerceAtLeast(0) }

    private var startX = 0f
    private var startY = 0f
    private var setupViewPager2TouchConflictResolution = false
    private var prevMaxOverScrollDistance = context.dp2px(80)
    private val headerLeadingScrollListener: (Int) -> Unit = {
        attachedHeader?.headerRowView?.let { row -> leadingLayoutManager?.syncColumns(row) }
    }
    private val headerMiddleOffsetListener: (Int, Int) -> Unit = { left, right ->
        attachedHeader?.updateMiddleOffsets(left, right)
    }

    var overScrollDamping: Float = FrozenColumnConfig.DEFAULT_OVER_SCROLL_DAMPING
        get() = leadingLayoutManager?.overScrollDamping ?: field
        set(value) {
            field = value
            leadingLayoutManager?.overScrollDamping = value
        }

    var overScrollAnimatorThreshold: Int =
        context.dp2px(FrozenColumnConfig.DEFAULT_OVER_SCROLL_ANIMATOR_THRESHOLD_DP)
        get() = leadingLayoutManager?.overScrollAnimatorThreshold
            ?: field
        set(value) {
            field = value
            leadingLayoutManager?.overScrollAnimatorThreshold = value
        }

    var maxOverScrollDistance: Int =
        context.dp2px(FrozenColumnConfig.DEFAULT_MAX_OVER_SCROLL_DISTANCE_DP)
        get() = leadingLayoutManager?.maxOverScrollDistance
            ?: field
        set(value) {
            field = value
            leadingLayoutManager?.maxOverScrollDistance = value
        }

    var canScrollHorizontally: Boolean = true
        get() = leadingLayoutManager?.canScrollHorizontally
            ?: middleLayoutManager?.horizontalScrollEnabled
            ?: field
        set(value) {
            field = value
            leadingLayoutManager?.canScrollHorizontally = value
            middleLayoutManager?.horizontalScrollEnabled = value
        }

    init {
        overScrollMode = OVER_SCROLL_NEVER
        var initialDamping = FrozenColumnConfig.DEFAULT_OVER_SCROLL_DAMPING
        var initialAnimatorThreshold =
            context.dp2px(FrozenColumnConfig.DEFAULT_OVER_SCROLL_ANIMATOR_THRESHOLD_DP)
        var initialMaxOverScroll =
            context.dp2px(FrozenColumnConfig.DEFAULT_MAX_OVER_SCROLL_DISTANCE_DP)
        context.withStyledAttributes(attrs, R.styleable.FrozenColumnList) {
            initialDamping = getFloat(
                R.styleable.FrozenColumnList_fclOverScrollDamping,
                initialDamping
            )
            initialAnimatorThreshold = getDimensionPixelSize(
                R.styleable.FrozenColumnList_fclOverScrollAnimatorThreshold,
                initialAnimatorThreshold
            )
            initialMaxOverScroll = getDimensionPixelSize(
                R.styleable.FrozenColumnList_fclMaxOverScrollDistance,
                initialMaxOverScroll
            )
            horizontalScrollThreshold = getDimensionPixelSize(
                R.styleable.FrozenColumnList_fclHorizontalScrollThreshold,
                touchSlop
            )
        }
        setColumnConfig(columnConfig)
        overScrollDamping = initialDamping
        overScrollAnimatorThreshold = initialAnimatorThreshold
        maxOverScrollDistance = initialMaxOverScroll
    }

    fun setColumnConfig(config: FrozenColumnConfig) {
        val previous = columnConfig
        val structuralChange = layoutManager == null ||
            previous.frozenColumnPosition != config.frozenColumnPosition ||
            previous.frozenColumnCount != config.frozenColumnCount ||
            previous.middleColumnStart != config.middleColumnStart ||
            previous.frozenViewportStart != config.frozenViewportStart ||
            previous.visibleColumnCount != config.visibleColumnCount
        columnConfig = config
        horizontalScrollThreshold = config.horizontalScrollThreshold ?: horizontalScrollThreshold
        if (!structuralChange) {
            middleLayoutManager?.scrollMode = config.scrollMode
            genericStockAdapter?.columnConfig = config
            attachedHeader?.setColumnConfig(config)
            return
        }

        leadingLayoutManager?.removeHorizontalScrollListener(headerLeadingScrollListener)
        middleLayoutManager?.removeOffsetListener(headerMiddleOffsetListener)
        if (config.frozenColumnPosition != FrozenColumnPosition.MIDDLE) {
            val manager = FrozenColumnLayoutManager(context).also {
                it.frozenColumnCount = config.frozenColumnCount
                it.frozenColumnPosition = config.frozenColumnPosition
                it.overScrollDamping = overScrollDamping
                it.overScrollAnimatorThreshold = overScrollAnimatorThreshold
                it.maxOverScrollDistance = maxOverScrollDistance
                it.canScrollHorizontally = canScrollHorizontally
            }
            leadingLayoutManager = manager
            middleLayoutManager = null
            layoutManager = manager
        } else {
            val manager = MiddleFrozenColumnLayoutManager(context).also {
                it.scrollMode = config.scrollMode
                it.horizontalScrollEnabled = canScrollHorizontally
            }
            middleLayoutManager = manager
            leadingLayoutManager = null
            layoutManager = manager
        }
        genericStockAdapter?.columnConfig = config
        attachedHeader?.let(::attachHeader)

        // 固定位置或范围变化时，旧 ViewHolder 的行根类型和列状态可能已不再适用。
        genericStockAdapter?.let { currentAdapter ->
            adapter = null
            recycledViewPool.clear()
            adapter = currentAdapter
        }
    }

    fun <T : FrozenColumnData> setProvider(provider: ColumnProvider<T>) {
        this.provider = provider
        adapter = GenericStockAdapter(provider).also {
            it.columnConfig = columnConfig
            it.setupEmptyView(context, provider.createEmptyView(context))
            it.setupFooterView(context, provider.createFooterView(context))
            genericStockAdapter = it
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : FrozenColumnData> submitList(list: List<T>, commitCallback: Runnable? = null) {
        (adapter as? ListAdapter<T, *>)?.submitList(list, commitCallback)
    }

    fun attachHeader(header: FrozenColumnHeader?) {
        if (header == null) return
        leadingLayoutManager?.removeHorizontalScrollListener(headerLeadingScrollListener)
        middleLayoutManager?.removeOffsetListener(headerMiddleOffsetListener)
        if (attachedHeader !== header) {
            attachedHeader?.onHeaderRowLayoutListener = null
            attachedHeader?.onHorizontalScrollListener = null
        }
        attachedHeader = header
        header.onHeaderRowLayoutListener = { row ->
            leadingLayoutManager?.syncColumns(row)
            middleLayoutManager?.let {
                header.updateMiddleOffsets(it.leftOffset, it.rightOffset)
            }
        }
        header.setColumnConfig(columnConfig)
        leadingLayoutManager?.addHorizontalScrollListener(headerLeadingScrollListener)
        middleLayoutManager?.addOffsetListener(headerMiddleOffsetListener)
        header.onHorizontalScrollListener = { dispatchTouchEvent(it) }
        syncHeaderOffset(header)
    }

    fun resetHorizontalOffsets() {
        leadingLayoutManager?.updateHorizontalOffset(0)
        middleLayoutManager?.resetOffsets()
    }

    /** 立即将列表当前偏移同步到指定表头。通常使用 [attachHeader] 即可。 */
    fun syncHeaderOffset(header: FrozenColumnHeader?) {
        val row = header?.headerRowView ?: return
        leadingLayoutManager?.syncColumns(row)
        middleLayoutManager?.let { header.updateMiddleOffsets(it.leftOffset, it.rightOffset) }
    }

    fun updateHorizontalOffset(newOffset: Int) {
        leadingLayoutManager?.updateHorizontalOffset(newOffset)
    }

    fun updateHorizontalOffsets(left: Int, right: Int) {
        middleLayoutManager?.updateOffsets(left, right)
    }

    fun addHorizontalScrollListener(listener: (Int) -> Unit) {
        leadingLayoutManager?.addHorizontalScrollListener(listener)
    }

    fun addMiddleHorizontalScrollListener(listener: (Int, Int) -> Unit) {
        middleLayoutManager?.addOffsetListener(listener)
    }

    fun removeHorizontalScrollListener(listener: (Int) -> Unit) {
        leadingLayoutManager?.removeHorizontalScrollListener(listener)
    }

    fun setSpringBackAnimatorProvider(provider: SpringBackAnimatorProvider?) {
        leadingLayoutManager?.springBackAnimatorProvider = provider
    }

    fun getFrozenColumnLayoutManager(): FrozenColumnLayoutManager? = leadingLayoutManager
    fun getMiddleFrozenLayoutManager(): MiddleFrozenColumnLayoutManager? = middleLayoutManager
    fun getFrozenColumnAdapter(): GenericStockAdapter<out FrozenColumnData>? = genericStockAdapter

    @Suppress("UNCHECKED_CAST")
    fun <T : FrozenColumnData> getData(): List<T>? = (adapter as? ListAdapter<T, *>)?.currentList

    fun <T : FrozenColumnData> getItem(position: Int): T? = getData<T>()?.getOrNull(position)

    fun addChildClickViewIds(@IdRes vararg ids: Int) = genericStockAdapter?.addChildClickViewIds(*ids) ?: Unit
    fun setOnItemClickListener(listener: ((View, Int, Int) -> Unit)?) {
        genericStockAdapter?.onItemClickListener = listener
    }
    fun setOnItemChildClickListener(listener: ((View, Int, Int) -> Unit)?) {
        genericStockAdapter?.onItemChildClickListener = listener
    }
    fun setOnEmptyViewClickListener(listener: ((View) -> Unit)?) {
        genericStockAdapter?.onEmptyViewClickListener = listener
    }
    fun setOnEmptyViewChildClickListener(listener: ((View) -> Unit)?) {
        genericStockAdapter?.onEmptyViewChildClickListener = listener
    }
    fun setOnFooterViewClickListener(listener: ((View) -> Unit)?) {
        genericStockAdapter?.onFooterViewClickListener = listener
    }
    fun setOnFooterViewChildClickListener(listener: ((View) -> Unit)?) {
        genericStockAdapter?.onFooterViewChildClickListener = listener
    }
    fun setOnSideClickListener(listener: ((View, Int, FrozenColumnSide) -> Unit)?) {
        genericStockAdapter?.onSideClickListener = listener
    }
    fun setOnSideDoubleClickListener(listener: ((View, Int, FrozenColumnSide) -> Unit)?) {
        genericStockAdapter?.onSideDoubleClickListener = listener
    }
    fun toggleSideSelected(position: Int, side: FrozenColumnSide): Boolean =
        genericStockAdapter?.toggleSideSelected(position, side) ?: false
    fun setSideSelected(position: Int, side: FrozenColumnSide, selected: Boolean) =
        genericStockAdapter?.setSideSelected(position, side, selected) ?: Unit
    fun clearSideSelection(side: FrozenColumnSide? = null) =
        genericStockAdapter?.clearSideSelection(side) ?: Unit

    fun setupViewPager2TouchConflictResolution(value: Boolean) {
        if (value) {
            leadingLayoutManager?.let {
                prevMaxOverScrollDistance = it.maxOverScrollDistance
                it.maxOverScrollDistance = 0
            }
        } else {
            leadingLayoutManager?.maxOverScrollDistance = prevMaxOverScrollDistance
        }
        setupViewPager2TouchConflictResolution = value
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val middle = middleLayoutManager
        if (middle != null) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    middle.beginGesture(middleSideAt(event.x))
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    middle.updateGestureDirection(
                        event.x - startX,
                        event.y - startY,
                        horizontalScrollThreshold
                    )
                    when {
                        middle.isHorizontalGesture() -> parent?.requestDisallowInterceptTouchEvent(true)
                        middle.isVerticalGesture() -> parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
            val handled = super.dispatchTouchEvent(event)
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (scrollState != SCROLL_STATE_SETTLING) middle.endGestureWithoutFling()
            }
            return handled
        }

        if (!setupViewPager2TouchConflictResolution) return super.dispatchTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - startX
                val dy = event.y - startY
                if (abs(dx) > abs(dy) && abs(dx) > touchSlop) {
                    parent?.requestDisallowInterceptTouchEvent(canScrollHorizontally(if (dx > 0) -1 else 1))
                } else if (abs(dy) > touchSlop) {
                    parent?.requestDisallowInterceptTouchEvent(canScrollVertically(if (dy > 0) -1 else 1))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // add... 内部会去重。临时离开 Window 不应丢失 header 和业务侧监听。
        attachedHeader?.let(::attachHeader)
        post {
            middleLayoutManager?.restoreVisibleRows()
            syncHeaderOffset(attachedHeader)
        }
    }

    private fun middleSideAt(x: Float): FrozenColumnSide? {
        val row = (0 until childCount).asSequence()
            .mapNotNull { getChildAt(it) as? MiddleFrozenRowLayout }
            .firstOrNull() ?: return null
        return when {
            x < row.frozenStart -> FrozenColumnSide.LEFT
            x >= row.frozenEnd -> FrozenColumnSide.RIGHT
            else -> null
        }
    }
}
