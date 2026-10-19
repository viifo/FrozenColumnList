package com.viifo.frozencolumnlist.layout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.viifo.frozencolumnlist.FrozenColumnConfig
import com.viifo.frozencolumnlist.FrozenColumnPosition
import com.viifo.frozencolumnlist.FrozenColumnSide
import com.viifo.frozencolumnlist.data.FrozenTextColumn

/**
 * A row with one text View per scrolling region and real TextViews for frozen columns.
 * With one middle frozen column this has exactly three children, regardless of logical column count.
 * Use [columnAt] and [getColumnBounds] for logical cells; child indices are not column indices.
 */
@SuppressLint("ViewConstructor") // Logical column count is supplied by the provider, not XML.
class FrozenTextRowLayout(
    context: Context,
    val columnCount: Int,
    minimumRowHeight: Int = 0,
    private val createFrozenView: (Context, Int) -> TextView = { ctx, _ -> AppCompatTextView(ctx) },
    private val styleFrozenView: (TextView, Int) -> Unit = { _, _ -> }
) : ViewGroup(context), FrozenRowViewport {
    var columns: List<FrozenTextColumn> = emptyList()
        private set
    private var config = FrozenColumnConfig()
    internal var geometry: TextColumnGeometry? = null
        private set
    internal var leftOffset = 0
        private set
    internal var rightOffset = 0
        private set
    private var leftText: FrozenRowTextView? = null
    private var rightText: FrozenRowTextView? = null
    private val frozenViews = linkedMapOf<Int, TextView>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempBounds = Rect()
    private var geometryDirty = true

    override val frozenStart: Int get() = geometry?.frozenStart ?: paddingLeft
    override val frozenEnd: Int get() = geometry?.frozenEnd ?: paddingLeft
    override val maxLeftOffset: Int get() = geometry?.maxLeftOffset ?: 0
    override val maxRightOffset: Int get() = geometry?.maxRightOffset ?: 0
    override var lastTouchX = 0f
        private set
    override var lastTouchY = 0f
        private set
    override var leftBackgroundColor: Int = Color.TRANSPARENT
        set(value) { if (field != value) { field = value; invalidate() } }
    override var rightBackgroundColor: Int = Color.TRANSPARENT
        set(value) { if (field != value) { field = value; invalidate() } }

    /** Optional transient highlight, e.g. a price-change animation. Reset on recycling. */
    var highlightColor: Int = Color.TRANSPARENT
        set(value) { field = value; invalidate() }
    var highlightAlpha: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    init {
        require(columnCount > 0)
        minimumHeight = minimumRowHeight
        setWillNotDraw(false)
        clipChildren = false // Native elevation on frozen columns can extend over either side.
    }

    internal fun configure(config: FrozenColumnConfig) {
        config.resolveFrozenStart(columnCount)
        if (this.config == config && childCount > 0) return
        this.config = config
        ensureChildren()
        geometryDirty = true
        requestLayout()
    }

    /** Unchanged column layouts are reused. Call with immutable data; do not mutate a submitted list. */
    fun submitColumns(columns: List<FrozenTextColumn>) {
        require(columns.size == columnCount) { "Expected $columnCount columns, got ${columns.size}" }
        require(columns.map { it.id }.toSet().size == columnCount) { "Column IDs must be unique within a row" }
        val previous = this.columns
        if (previous == columns) return
        this.columns = columns.toList()
        ensureChildren()
        frozenViews.forEach { (index, view) ->
            if (previous.getOrNull(index) != columns[index]) bindFrozen(view, index)
        }
        if (previous.size != columns.size || columns.indices.any { previous[it].width != columns[it].width }) {
            geometryDirty = true
            requestLayout()
        } else {
            val oldHeight = maxOf(leftText?.desiredTextHeight ?: 0, rightText?.desiredTextHeight ?: 0)
            geometry?.let { leftText?.prepare(it); rightText?.prepare(it) }
            if (oldHeight != maxOf(leftText?.desiredTextHeight ?: 0, rightText?.desiredTextHeight ?: 0)) {
                requestLayout()
            }
        }
        invalidate()
    }

    fun updateColumn(index: Int, column: FrozenTextColumn) {
        require(index in columns.indices)
        submitColumns(columns.toMutableList().also { it[index] = column })
    }

    fun getFrozenColumnView(index: Int): TextView? = frozenViews[index]
    fun getColumnById(id: Int): FrozenTextColumn? = columns.firstOrNull { it.id == id }

    /** Logical column index at row-local coordinates; clipped/empty areas return null. */
    fun columnAt(x: Float, y: Float): Int? {
        if (y < paddingTop || y >= height - paddingBottom) return null
        return geometry?.columnAt(x, leftOffset, rightOffset)
    }

    /** Visible bounds in row-local coordinates, suitable for highlights and popup anchors. */
    fun getColumnBounds(index: Int, outBounds: Rect): Boolean {
        outBounds.setEmpty()
        val geometry = geometry ?: return false
        if (index !in columns.indices) return false
        val x = geometry.left(index, leftOffset, rightOffset)
        val left = maxOf(x, geometry.regionStart(index))
        val right = minOf(x + geometry.width(index), geometry.regionEnd(index))
        if (left >= right || height <= paddingTop + paddingBottom) return false
        outBounds.set(left, paddingTop, right, height - paddingBottom)
        return true
    }

    fun getColumnBoundsById(id: Int, outBounds: Rect): Boolean =
        getColumnBounds(columns.indexOfFirst { it.id == id }, outBounds)

    internal fun activateColumn(index: Int): Boolean {
        if (!getColumnBounds(index, tempBounds)) return false
        lastTouchX = tempBounds.exactCenterX()
        lastTouchY = tempBounds.exactCenterY()
        return performClick()
    }

    override fun sideAt(x: Float): FrozenColumnSide? = when {
        x < paddingLeft || x >= width - paddingRight -> null
        x < frozenStart -> FrozenColumnSide.LEFT
        x >= frozenEnd -> FrozenColumnSide.RIGHT
        else -> null
    }

    override fun applyHorizontalOffsets(left: Int, right: Int) {
        // Leading/trailing mode deliberately accepts overscroll for the existing spring animator.
        leftOffset = if (config.frozenColumnPosition == FrozenColumnPosition.MIDDLE && geometry != null)
            left.coerceIn(0, maxLeftOffset) else left
        rightOffset = if (config.frozenColumnPosition == FrozenColumnPosition.MIDDLE && geometry != null)
            right.coerceIn(0, maxRightOffset) else right
        leftText?.offsetsChanged()
        rightText?.offsetsChanged()
    }

    private fun ensureChildren() {
        val start = config.resolveFrozenStart(columnCount)
        val end = start + config.frozenColumnCount
        if (frozenViews.keys.toList() == (start until end).toList() && childCount > 0) return
        removeAllViews()
        frozenViews.clear()
        leftText = if (start > 0) FrozenRowTextView(context, this).also {
            it.fromColumn = 0; it.untilColumn = start; addView(it)
        } else null
        rightText = if (end < columnCount) FrozenRowTextView(context, this).also {
            it.fromColumn = end; it.untilColumn = columnCount
        } else null
        for (index in start until end) {
            val view = createFrozenView(context, index)
            require(view.parent == null) { "createFrozenColumnView must return an unattached TextView" }
            frozenViews[index] = view
            addView(view)
            view.setOnClickListener { activateColumn(index) }
            if (columns.isNotEmpty()) bindFrozen(view, index)
        }
        rightText?.let(::addView)
    }

    private fun bindFrozen(view: TextView, index: Int) {
        view.bindTextColumn(columns[index])
        styleFrozenView(view, index)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (columns.isEmpty()) {
            setMeasuredDimension(resolveSize(suggestedMinimumWidth, widthMeasureSpec),
                resolveSize(minimumHeight, heightMeasureSpec))
            return
        }
        val measuredRowWidth = resolveSize(
            columns.sumOf { it.width } + paddingLeft + paddingRight, widthMeasureSpec
        )
        val oldGeometry = geometry
        if (geometryDirty || oldGeometry == null ||
            oldGeometry.viewportEnd != measuredRowWidth - paddingRight || oldGeometry.paddingLeft != paddingLeft) {
            geometry = TextColumnGeometry(IntArray(columnCount) { columns[it].width }, config,
                measuredRowWidth, paddingLeft, paddingRight)
            geometryDirty = false
        }
        val geometry = requireNotNull(geometry)
        leftText?.prepare(geometry)
        rightText?.prepare(geometry)
        var contentHeight = maxOf(leftText?.desiredTextHeight ?: 0, rightText?.desiredTextHeight ?: 0)
        frozenViews.forEach { (index, view) ->
            view.measure(MeasureSpec.makeMeasureSpec(geometry.width(index), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            contentHeight = maxOf(contentHeight, view.measuredHeight)
        }
        val rowHeight = resolveSize(maxOf(minimumHeight, contentHeight + paddingTop + paddingBottom), heightMeasureSpec)
        setMeasuredDimension(measuredRowWidth, rowHeight)
        val heightSpec = MeasureSpec.makeMeasureSpec((rowHeight - paddingTop - paddingBottom).coerceAtLeast(0), MeasureSpec.EXACTLY)
        leftText?.measure(MeasureSpec.makeMeasureSpec((geometry.frozenStart - paddingLeft).coerceAtLeast(0), MeasureSpec.EXACTLY), heightSpec)
        rightText?.measure(MeasureSpec.makeMeasureSpec((geometry.viewportEnd - geometry.frozenEnd).coerceAtLeast(0), MeasureSpec.EXACTLY), heightSpec)
        frozenViews.forEach { (index, view) ->
            view.measure(MeasureSpec.makeMeasureSpec(geometry.width(index), MeasureSpec.EXACTLY), heightSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val geometry = geometry ?: return
        val bottom = height - paddingBottom
        leftText?.layout(paddingLeft, paddingTop, geometry.frozenStart, bottom)
        rightText?.layout(geometry.frozenEnd, paddingTop, maxOf(geometry.frozenEnd, geometry.viewportEnd), bottom)
        frozenViews.forEach { (index, view) ->
            val x = geometry.left(index, 0, 0)
            view.layout(x, paddingTop, x + geometry.width(index), bottom)
        }
        applyHorizontalOffsets(leftOffset, rightOffset)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = leftBackgroundColor
        canvas.drawRect(paddingLeft.toFloat(), paddingTop.toFloat(), frozenStart.toFloat(),
            (height - paddingBottom).toFloat(), paint)
        paint.color = rightBackgroundColor
        canvas.drawRect(frozenEnd.toFloat(), paddingTop.toFloat(), (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat(), paint)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (highlightAlpha > 0f) {
            paint.color = highlightColor
            paint.alpha = (Color.alpha(highlightColor) * highlightAlpha).toInt()
            canvas.drawRect(paddingLeft.toFloat(), paddingTop.toFloat(), (width - paddingRight).toFloat(),
                (height - paddingBottom).toFloat(), paint)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            lastTouchX = event.x
            lastTouchY = event.y
        }
        return super.dispatchTouchEvent(event)
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
}
