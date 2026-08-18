package com.viifo.frozencolumnlist.layout

import android.content.Context
import android.view.ViewConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.FrozenColumnScrollMode
import com.viifo.frozencolumnlist.FrozenColumnSide
import java.util.concurrent.CopyOnWriteArrayList

/** 负责垂直回收布局以及左右两侧水平偏移的 LayoutManager。 */
class MiddleFrozenColumnLayoutManager(context: Context) :
    LinearLayoutManager(context, VERTICAL, false) {

    var scrollMode: FrozenColumnScrollMode = FrozenColumnScrollMode.INDEPENDENT
        set(value) {
            if (field == value) return
            field = value
            if (value == FrozenColumnScrollMode.SYNCHRONIZED) {
                projectOffsetsToSynchronizedProgress()
                syncRows()
                dispatchOffsets()
            }
        }

    var horizontalScrollEnabled: Boolean = true

    var leftOffset: Int = 0
        private set
    var rightOffset: Int = 0
        private set
    var maxLeftOffset: Int = 0
        private set
    var maxRightOffset: Int = 0
        private set

    private var attachedRecyclerView: RecyclerView? = null

    internal var activeSide: FrozenColumnSide? = null
        private set

    private val offsetListeners = CopyOnWriteArrayList<(Int, Int) -> Unit>()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var gestureDirection = MiddleFrozenGestureDirection.NONE
    private var touchGestureActive = false
    private var offsetsInitialized = false

    private val synchronizedMaxOffset: Int
        get() = minOf(maxLeftOffset, maxRightOffset)

    override fun canScrollHorizontally(): Boolean = horizontalScrollEnabled

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        attachedRecyclerView = view
        view?.post {
            if (attachedRecyclerView === view) restoreVisibleRows()
        }
    }

    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
        resetGesture()
        attachedRecyclerView = null
        super.onDetachedFromWindow(view, recycler)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        val previousMaxLeftOffset = maxLeftOffset
        val wasDefaultAligned = offsetsInitialized &&
            leftOffset == previousMaxLeftOffset && rightOffset == 0
        super.onLayoutChildren(recycler, state)
        updateScrollRanges()
        if (!offsetsInitialized || wasDefaultAligned) {
            applyDefaultOffsets()
            offsetsInitialized = true
        } else {
            normalizeOffsets()
        }
        syncRows()
        dispatchOffsets()
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        if (touchGestureActive && gestureDirection != MiddleFrozenGestureDirection.VERTICAL) {
            return 0
        }
        val consumed = super.scrollVerticallyBy(dy, recycler, state)
        if (consumed != 0) syncRows()
        return consumed
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        if (!horizontalScrollEnabled || activeSide == null) return 0
        if (!touchGestureActive || gestureDirection != MiddleFrozenGestureDirection.HORIZONTAL) return 0
        return performHorizontalScroll(dx)
    }

    override fun computeHorizontalScrollRange(state: RecyclerView.State): Int {
        val range = if (scrollMode == FrozenColumnScrollMode.SYNCHRONIZED) {
            synchronizedMaxOffset
        } else {
            maxOf(maxLeftOffset, maxRightOffset)
        }
        return maxOf(width + range, width)
    }

    override fun computeHorizontalScrollOffset(state: RecyclerView.State): Int {
        if (scrollMode == FrozenColumnScrollMode.SYNCHRONIZED && activeSide == null) {
            return rightOffset
        }
        return when (activeSide) {
            FrozenColumnSide.LEFT -> leftOffset
            FrozenColumnSide.RIGHT -> rightOffset
            null -> minOf(leftOffset, rightOffset)
        }
    }

    override fun computeHorizontalScrollExtent(state: RecyclerView.State): Int = width

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            resetGesture()
        }
    }

    internal fun beginGesture(side: FrozenColumnSide?) {
        activeSide = side
        touchGestureActive = true
        gestureDirection = MiddleFrozenGestureDirection.NONE
    }

    internal fun updateGestureDirection(dx: Float, dy: Float, horizontalThreshold: Int) {
        if (!touchGestureActive || gestureDirection != MiddleFrozenGestureDirection.NONE) return
        gestureDirection = MiddleFrozenGestureDirectionResolver.resolve(
            dx = dx,
            dy = dy,
            horizontalThreshold = horizontalThreshold,
            verticalThreshold = touchSlop
        )
    }

    internal fun isHorizontalGesture(): Boolean {
        return gestureDirection == MiddleFrozenGestureDirection.HORIZONTAL
    }

    internal fun isVerticalGesture(): Boolean {
        return gestureDirection == MiddleFrozenGestureDirection.VERTICAL
    }

    internal fun endGestureWithoutFling() {
        resetGesture()
    }

    fun updateOffsets(left: Int = leftOffset, right: Int = rightOffset) {
        offsetsInitialized = true
        if (scrollMode == FrozenColumnScrollMode.SYNCHRONIZED) {
            val progressFromLeft = maxLeftOffset - left.coerceIn(0, maxLeftOffset)
            val progressFromRight = right.coerceIn(0, maxRightOffset)
            val progress = ((progressFromLeft + progressFromRight) / 2)
                .coerceIn(0, synchronizedMaxOffset)
            applySynchronizedProgress(progress)
        } else {
            leftOffset = left.coerceIn(0, maxLeftOffset)
            rightOffset = right.coerceIn(0, maxRightOffset)
        }
        syncRows()
        dispatchOffsets()
    }

    /** 恢复以固定列为中心的默认位置：左侧末列、右侧首列紧邻固定列。 */
    fun resetOffsets() {
        if (childCount == 0) {
            offsetsInitialized = false
            leftOffset = 0
            rightOffset = 0
            requestLayout()
            return
        }
        offsetsInitialized = true
        applyDefaultOffsets()
        syncRows()
        dispatchOffsets()
    }

    fun addOffsetListener(listener: (Int, Int) -> Unit) {
        if (!offsetListeners.contains(listener)) offsetListeners.add(listener)
    }

    fun removeOffsetListener(listener: (Int, Int) -> Unit) {
        offsetListeners.remove(listener)
    }

    internal fun clearOffsetListeners() {
        offsetListeners.clear()
    }

    /** Fragment/ViewPager 重新显示后，重新校正已有行的真实位移并恢复表头监听。 */
    internal fun restoreVisibleRows() {
        resetGesture()
        updateScrollRanges()
        normalizeOffsets()
        syncRows()
        dispatchOffsets()
    }

    private fun performHorizontalScroll(dx: Int): Int {
        val scrollingSide = activeSide ?: return 0
        val previousLeft = leftOffset
        val previousRight = rightOffset
        if (scrollMode == FrozenColumnScrollMode.SYNCHRONIZED) {
            val progressDelta = MiddleFrozenOffsetCalculator.progressDelta(scrollingSide, dx)
            val nextProgress = (rightOffset + progressDelta)
                .coerceIn(0, synchronizedMaxOffset)
            applySynchronizedProgress(nextProgress)
        } else {
            when (scrollingSide) {
                FrozenColumnSide.LEFT -> leftOffset = (leftOffset + dx).coerceIn(0, maxLeftOffset)
                FrozenColumnSide.RIGHT -> rightOffset = (rightOffset + dx).coerceIn(0, maxRightOffset)
            }
        }
        val consumed = when (scrollingSide) {
            FrozenColumnSide.LEFT -> leftOffset - previousLeft
            FrozenColumnSide.RIGHT -> rightOffset - previousRight
        }
        if (leftOffset != previousLeft || rightOffset != previousRight) {
            syncRows()
            dispatchOffsets()
        }
        return consumed
    }

    private fun updateScrollRanges() {
        val row = (0 until childCount)
            .asSequence()
            .mapNotNull { getChildAt(it) as? MiddleFrozenRowLayout }
            .firstOrNull() ?: return
        maxLeftOffset = row.maxLeftOffset
        maxRightOffset = row.maxRightOffset
    }

    private fun normalizeOffsets() {
        if (scrollMode == FrozenColumnScrollMode.SYNCHRONIZED) {
            applySynchronizedProgress(rightOffset.coerceIn(0, synchronizedMaxOffset))
        } else {
            leftOffset = leftOffset.coerceIn(0, maxLeftOffset)
            rightOffset = rightOffset.coerceIn(0, maxRightOffset)
        }
    }

    private fun applyDefaultOffsets() {
        val offsets = MiddleFrozenOffsetCalculator.default(maxLeftOffset)
        leftOffset = offsets.left
        rightOffset = offsets.right
    }

    private fun projectOffsetsToSynchronizedProgress() {
        val progressFromLeft = (maxLeftOffset - leftOffset).coerceAtLeast(0)
        val progressFromRight = rightOffset.coerceAtLeast(0)
        val progress = ((progressFromLeft + progressFromRight) / 2)
            .coerceIn(0, synchronizedMaxOffset)
        applySynchronizedProgress(progress)
    }

    private fun applySynchronizedProgress(progress: Int) {
        val offsets = MiddleFrozenOffsetCalculator.synchronized(
            maxLeft = maxLeftOffset,
            maxRight = maxRightOffset,
            progress = progress
        )
        leftOffset = offsets.left
        rightOffset = offsets.right
    }

    private fun syncRows() {
        val recyclerView = attachedRecyclerView
        if (recyclerView != null) {
            // 使用 RecyclerView 的原始子 View 集合，覆盖预测布局和 ItemAnimator 期间被
            // LayoutManager 临时隐藏、但仍可能绘制在屏幕上的行。
            for (index in 0 until recyclerView.childCount) {
                (recyclerView.getChildAt(index) as? MiddleFrozenRowLayout)
                    ?.applyHorizontalOffsets(leftOffset, rightOffset)
            }
        } else {
            for (index in 0 until childCount) {
                (getChildAt(index) as? MiddleFrozenRowLayout)
                    ?.applyHorizontalOffsets(leftOffset, rightOffset)
            }
        }
    }

    private fun dispatchOffsets() {
        offsetListeners.forEach { it(leftOffset, rightOffset) }
    }

    private fun resetGesture() {
        activeSide = null
        touchGestureActive = false
        gestureDirection = MiddleFrozenGestureDirection.NONE
    }
}
