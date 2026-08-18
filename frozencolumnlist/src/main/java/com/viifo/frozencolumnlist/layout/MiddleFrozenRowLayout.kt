package com.viifo.frozencolumnlist.layout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isEmpty
import androidx.core.view.isGone
import com.viifo.frozencolumnlist.FrozenColumnSide
import kotlin.math.max

/**
 * 中间固定列列表的一行。子 View 按原始列顺序直接添加。
 *
 * 左右背景由本 ViewGroup 绘制，因此单元格应使用透明背景；固定列不受此限制。
 */
open class MiddleFrozenRowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    internal var frozenColumnIndex: Int = 0
    internal var frozenColumnCount: Int = 1
    internal var frozenColumnStart: Int? = null
    internal var visibleColumnCount: Int? = null
    internal var leftOffset: Int = 0
    internal var rightOffset: Int = 0

    internal val frozenStart: Int
        get() = resolvedFrozenStart
    internal val frozenEnd: Int
        get() = resolvedFrozenStart + frozenWidth
    internal val maxLeftOffset: Int
        get() = (leftContentWidth - resolvedFrozenStart).coerceAtLeast(0)
    internal val maxRightOffset: Int
        get() = (rightContentWidth - (width - frozenEnd)).coerceAtLeast(0)

    var leftBackgroundColor: Int = Color.TRANSPARENT
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var rightBackgroundColor: Int = Color.TRANSPARENT
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    internal var lastTouchX: Float = 0f
        private set
    internal var lastTouchY: Float = 0f
        private set

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val childClipRect = Rect()
    private var resolvedFrozenStart = 0
    private var frozenWidth = 0
    private var leftContentWidth = 0
    private var rightContentWidth = 0

    init {
        setWillNotDraw(false)
        clipChildren = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val safeFrozenIndex = frozenColumnIndex.coerceIn(0, (childCount - 1).coerceAtLeast(0))
        val safeFrozenCount = frozenColumnCount.coerceIn(1, (childCount - safeFrozenIndex).coerceAtLeast(1))
        val frozenEndIndex = safeFrozenIndex + safeFrozenCount
        val configuredVisibleCount = visibleColumnCount

        // 固定列保留 XML 中声明的宽度；配置完整可见列数后，用剩余空间等分普通列。
        // 先测量固定列，才能得到普通列真正可用的像素宽度。
        var measuredFrozenWidth = 0
        if (childCount > 0 && configuredVisibleCount != null) {
            for (index in safeFrozenIndex until frozenEndIndex) {
                val child = getChildAt(index)
                if (child.isGone) continue
                measureColumn(child, widthMeasureSpec, heightMeasureSpec, null)
                measuredFrozenWidth += child.measuredWidth
            }
        }
        val fittedScrollableColumnWidth = configuredVisibleCount?.let { visibleCount ->
            val availableWidth = (
                MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight - measuredFrozenWidth
                ).coerceAtLeast(0)
            (availableWidth / (visibleCount - safeFrozenCount)).coerceAtLeast(1)
        }

        var maxHeight = paddingTop + paddingBottom
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.isGone) continue
            val isFrozenColumn = index in safeFrozenIndex until frozenEndIndex
            if (configuredVisibleCount == null || !isFrozenColumn) {
                measureColumn(
                    child = child,
                    widthMeasureSpec = widthMeasureSpec,
                    heightMeasureSpec = heightMeasureSpec,
                    overrideWidth = fittedScrollableColumnWidth.takeUnless { isFrozenColumn }
                )
            }
            maxHeight = max(maxHeight, child.measuredHeight + paddingTop + paddingBottom)
        }
        setMeasuredDimension(
            resolveSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(maxHeight.coerceAtLeast(suggestedMinimumHeight), heightMeasureSpec)
        )
        // RecyclerView 行高通常是 wrap_content。第一遍先由内容确定行高，再让 MATCH_PARENT
        // 单元格补齐整行，确保固定列背景、elevation 和左右点击区域高度一致。
        val contentHeight = (measuredHeight - paddingTop - paddingBottom).coerceAtLeast(0)
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.isGone || child.layoutParams.height != LayoutParams.MATCH_PARENT) continue
            if (child.measuredHeight == contentHeight) continue
            child.measure(
                MeasureSpec.makeMeasureSpec(child.measuredWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (isEmpty()) return
        val safeFrozenIndex = frozenColumnIndex.coerceIn(0, childCount - 1)
        val safeFrozenCount = frozenColumnCount.coerceIn(1, childCount - safeFrozenIndex)
        val frozenEndIndex = safeFrozenIndex + safeFrozenCount
        frozenWidth = (safeFrozenIndex until frozenEndIndex).sumOf { getChildAt(it).measuredWidth }
        resolvedFrozenStart = (frozenColumnStart ?: ((width - frozenWidth) / 2))
            .coerceIn(paddingLeft, (width - paddingRight - frozenWidth).coerceAtLeast(paddingLeft))

        leftContentWidth = 0
        for (index in 0 until safeFrozenIndex) {
            leftContentWidth += getChildAt(index).measuredWidth
        }
        rightContentWidth = 0
        for (index in frozenEndIndex until childCount) {
            rightContentWidth += getChildAt(index).measuredWidth
        }

        leftOffset = leftOffset.coerceIn(0, maxLeftOffset)
        rightOffset = rightOffset.coerceIn(0, maxRightOffset)

        var childLeft = paddingLeft
        for (index in 0 until safeFrozenIndex) {
            val child = getChildAt(index)
            layoutChild(child, childLeft, paddingTop)
            childLeft += child.measuredWidth
        }

        childLeft = resolvedFrozenStart
        for (index in safeFrozenIndex until frozenEndIndex) {
            val child = getChildAt(index)
            layoutChild(child, childLeft, paddingTop)
            childLeft += child.measuredWidth
        }

        childLeft = frozenEnd
        for (index in frozenEndIndex until childCount) {
            val child = getChildAt(index)
            layoutChild(child, childLeft, paddingTop)
            childLeft += child.measuredWidth
        }
        applyChildOffsets(safeFrozenIndex, frozenEndIndex)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        backgroundPaint.color = leftBackgroundColor
        canvas.drawRect(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            resolvedFrozenStart.toFloat(),
            (height - paddingBottom).toFloat(),
            backgroundPaint
        )
        backgroundPaint.color = rightBackgroundColor
        canvas.drawRect(
            frozenEnd.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat(),
            backgroundPaint
        )
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            lastTouchX = event.x
            lastTouchY = event.y
        }
        return super.dispatchTouchEvent(event)
    }

    fun sideAt(x: Float): FrozenColumnSide? {
        return when {
            x < frozenStart -> FrozenColumnSide.LEFT
            x >= frozenEnd -> FrozenColumnSide.RIGHT
            else -> null
        }
    }

    internal fun applyHorizontalOffsets(left: Int, right: Int) {
        // ViewPager/Fragment 切换回来时，View 可能已保留有效几何信息，但因为 requestLayout
        // 导致 isLaidOut=false。此时仍应立即校正实际位移，不能只更新 offset 字段。
        if (isEmpty() || width <= 0 || height <= 0 || frozenWidth <= 0) {
            leftOffset = left.coerceAtLeast(0)
            rightOffset = right.coerceAtLeast(0)
            return
        }

        leftOffset = left.coerceIn(0, maxLeftOffset)
        rightOffset = right.coerceIn(0, maxRightOffset)

        // 不能仅凭 offset 字段相等就跳过：行可能在 requestLayout、回收复用或 ItemAnimator
        // 期间只更新了状态，实际 translationX/clipBounds 仍是旧值。每次同步都重新校正实际 View。
        val safeFrozenIndex = frozenColumnIndex.coerceIn(0, childCount - 1)
        val frozenEndIndex = safeFrozenIndex + frozenColumnCount.coerceIn(1, childCount - safeFrozenIndex)
        applyChildOffsets(safeFrozenIndex, frozenEndIndex)
        postInvalidateOnAnimation()
    }

    private fun applyChildOffsets(safeFrozenIndex: Int, frozenEndIndex: Int) {
        for (index in 0 until safeFrozenIndex) {
            val child = getChildAt(index)
            child.translationX = -leftOffset.toFloat()
            clipChildToRegion(child, paddingLeft, resolvedFrozenStart)
        }

        for (index in safeFrozenIndex until frozenEndIndex) {
            getChildAt(index).apply {
                translationX = 0f
                clipBounds = null
            }
        }

        for (index in frozenEndIndex until childCount) {
            val child = getChildAt(index)
            child.translationX = -rightOffset.toFloat()
            clipChildToRegion(child, frozenEnd, width - paddingRight)
        }
    }

    private fun layoutChild(child: View, left: Int, top: Int) {
        child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
    }

    private fun measureColumn(
        child: View,
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
        overrideWidth: Int?
    ) {
        val lp = child.layoutParams
        val childWidthSpec = when {
            overrideWidth != null -> MeasureSpec.makeMeasureSpec(overrideWidth, MeasureSpec.EXACTLY)
            lp.width >= 0 -> MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY)
            else -> getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight, lp.width)
        }
        val childHeightSpec = getChildMeasureSpec(
            heightMeasureSpec,
            paddingTop + paddingBottom,
            lp.height
        )
        child.measure(childWidthSpec, childHeightSpec)
    }

    private fun clipChildToRegion(child: View, regionLeft: Int, regionRight: Int) {
        val displayedLeft = child.left + child.translationX.toInt()
        val clipLeft = (regionLeft - displayedLeft).coerceIn(0, child.width)
        val clipRight = (regionRight - displayedLeft).coerceIn(0, child.width)
        childClipRect.set(clipLeft, 0, max(clipLeft, clipRight), child.height)
        // View#setClipBounds 会复制矩形内容，同一行可安全复用该 Rect，避免横向滚动时逐列分配。
        child.clipBounds = childClipRect
    }
}
