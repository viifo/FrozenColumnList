package com.viifo.frozencolumnlist

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isEmpty
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.layout.MiddleFrozenRowLayout
import com.viifo.frozencolumnlist.layout.VisibleColumnWidthFitter
import com.viifo.frozencolumnlist.provider.ColumnProvider
import com.viifo.frozencolumnlist.provider.FrozenHeaderViewHolder
import kotlin.math.abs

/** 统一表头：Provider 一次创建/ inflate 完整表头行。 */
class FrozenColumnHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onHorizontalScrollListener: ((MotionEvent) -> Unit)? = null
    var onHeaderItemClickListener: ((view: View, position: Int) -> Unit)? = null
    val headerData: MutableList<FrozenHeaderData> = mutableListOf()

    var columnConfig: FrozenColumnConfig = FrozenColumnConfig()
        private set

    internal var headerRowView: ViewGroup? = null
        private set

    private var provider: ColumnProvider<out FrozenColumnData>? = null
    private var headerHolder: FrozenHeaderViewHolder? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f
    private var horizontalDragging = false
    private var syntheticDownSent = false

    fun setColumnConfig(config: FrozenColumnConfig) {
        require(config.frozenColumnPosition != FrozenColumnPosition.END) {
            "FrozenColumnPosition.END is reserved and not implemented yet"
        }
        columnConfig = config
        configureRow()
    }

    fun setProvider(provider: ColumnProvider<out FrozenColumnData>) {
        if (this.provider !== provider) {
            removeAllViews()
            headerRowView = null
            headerHolder = null
        }
        this.provider = provider
    }

    fun setHeaderData(data: List<FrozenHeaderData>) {
        val provider = provider ?: error("ColumnProvider is not set")
        headerData.clear()
        headerData.addAll(data)

        val existingRow = headerRowView
        val existingHolder = headerHolder
        if (existingRow != null && existingHolder != null && existingRow.childCount == data.size) {
            existingHolder.bind(data)
            return
        }

        removeAllViews()
        val row = provider.createHeaderRowView(this, data.size)
        require(row.childCount == data.size) {
            "createHeaderRowView must contain ${data.size} direct column children, but was ${row.childCount}"
        }
        headerRowView = row
        configureRow()
        data.indices.forEach { index ->
            row.getChildAt(index).setOnClickListener { view ->
                onHeaderItemClickListener?.invoke(view, index)
            }
        }
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        headerHolder = provider.createHeaderViewHolder(row).also { holder ->
            require(holder.rowView === row) {
                "FrozenHeaderViewHolder.rowView must be the View returned by createHeaderRowView"
            }
            holder.bind(data)
        }
    }

    fun refreshHeader(position: Int, item: FrozenHeaderData) {
        if (position !in headerData.indices) return
        headerData[position] = item
        headerHolder?.bindColumn(position, item)
    }

    internal fun updateMiddleOffsets(left: Int, right: Int) {
        (headerRowView as? MiddleFrozenRowLayout)?.applyHorizontalOffsets(left, right)
    }

    private fun configureRow() {
        val row = headerRowView ?: return
        if (row.isEmpty()) return
        val frozenStart = columnConfig.resolveFrozenStart(row.childCount)
        if (columnConfig.frozenColumnPosition == FrozenColumnPosition.START) {
            VisibleColumnWidthFitter.configure(
                row = row,
                frozenColumnStart = frozenStart,
                frozenColumnCount = columnConfig.frozenColumnCount,
                visibleColumnCount = columnConfig.visibleColumnCount
            )
            return
        }
        require(row is MiddleFrozenRowLayout) {
            "MIDDLE mode header root must be MiddleFrozenRowLayout"
        }
        row.frozenColumnIndex = frozenStart
        row.frozenColumnCount = columnConfig.frozenColumnCount
        row.frozenColumnStart = columnConfig.frozenViewportStart
        row.visibleColumnCount = columnConfig.visibleColumnCount
        row.requestLayout()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                horizontalDragging = false
                syntheticDownSent = false
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - startX)
                val dy = abs(event.y - startY)
                val threshold = columnConfig.horizontalScrollThreshold ?: touchSlop
                if (dx > threshold && dx > dy) {
                    horizontalDragging = true
                    if (!syntheticDownSent) {
                        MotionEvent.obtain(
                            event.downTime,
                            event.eventTime,
                            MotionEvent.ACTION_DOWN,
                            startX,
                            startY,
                            event.metaState
                        ).also {
                            onHorizontalScrollListener?.invoke(it)
                            it.recycle()
                        }
                        syntheticDownSent = true
                    }
                    return true
                }
            }
        }
        return horizontalDragging || super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (syntheticDownSent) onHorizontalScrollListener?.invoke(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            if (event.actionMasked == MotionEvent.ACTION_UP && !horizontalDragging) performClick()
            horizontalDragging = false
            syntheticDownSent = false
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
