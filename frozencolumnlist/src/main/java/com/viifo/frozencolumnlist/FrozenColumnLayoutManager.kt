package com.viifo.frozencolumnlist

import android.content.Context
import android.graphics.Rect
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * 一个支持冻结（固定）列的 RecyclerView 布局管理器
 */
class FrozenColumnLayoutManager(
    context: Context,
    private val frozenColumnCount: Int
) : LinearLayoutManager(context, VERTICAL, false) {

    companion object {
        // 滚动方向锁, 0: 无锁, 1: 水平锁, 2: 垂直锁
        private const val SCROLL_LOCK_NONE = 0
        private const val SCROLL_LOCK_HORIZONTAL = 1
        private const val SCROLL_LOCK_VERTICAL = 2
    }

    /** 水平滑动时回调，用于同步表头 */
    var onHorizontalScroll: ((Int) -> Unit)? = null

    /** 水平滚动偏移量 */
    private var horizontalOffset = 0
    /** 固定列的宽度 */
    private var fixedColumnWidth = 0
    /** 最大可滚动距离 */
    private var maxScrollWidth = 0
    /** item 裁剪区域 */
    private val clipRect = Rect()

    /** 滚动方向锁定 */
    private var totalDx = 0
    private var totalDy = 0
    private var scrollDirectionLock = 0

    /** 触发阈值（像素） */
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    /** 是否支持水平滚动 */
    override fun canScrollHorizontally(): Boolean = true

    /** 滚动状态改变 */
    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            resetScrollLock()
        }
    }

    /** 当 RecyclerView 从窗口中分离时调用 */
    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
        super.onDetachedFromWindow(view, recycler)
        resetScrollLock()
    }

    /**
     * 子 Item 布局
     */
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        // 计算最大可滚动宽度
        calculateMaxScrollWidth()
        // 如果数据刷新后发现之前滚动的距离超出了新范围，需要修正
        if (horizontalOffset > maxScrollWidth) {
            horizontalOffset = maxScrollWidth
        }
        // 同步一次当前屏幕上的所有列的位移位置
        syncColumns()
    }

    /**
     * 垂直滚动处理
     */
    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        // 如果当前已经锁定了水平方向，则禁止垂直滑动
        if (scrollDirectionLock == SCROLL_LOCK_HORIZONTAL) return 0
        totalDy += dy
        // 如果还未锁定方向，且垂直偏移超过阈值，则锁定为垂直
        if (scrollDirectionLock == SCROLL_LOCK_NONE
            && abs(totalDy) > touchSlop
            && abs(totalDy) > abs(totalDx)) {
            scrollDirectionLock = SCROLL_LOCK_VERTICAL
        }

        val consumed = super.scrollVerticallyBy(dy, recycler, state)
        if (consumed != 0) {
            // 如果发生了位移，需要同步一次当前屏幕上的所有列的位移位置
            syncColumns()
        }
        return consumed
    }

    /**
     * 水平滚动处理
     */
    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        // 如果当前已经锁定了垂直方向，则禁止水平滑动
        if (scrollDirectionLock == SCROLL_LOCK_VERTICAL) return 0
        totalDx += dx
        // 如果还未锁定方向，且水平偏移超过阈值，则锁定为水平
        if (scrollDirectionLock == SCROLL_LOCK_NONE
            && abs(totalDx) > touchSlop / 2
            && abs(totalDx) > abs(totalDy)) {
            scrollDirectionLock = SCROLL_LOCK_HORIZONTAL
        }
        // 执行水平滚动
        return performHorizontalScroll(dx)
    }

    /** 内容总宽度（所有列加起来） */
    override fun computeHorizontalScrollRange(state: RecyclerView.State): Int {
        return maxOf(maxScrollWidth + width, width)
    }

    /** 当前滚动的偏移量 */
    override fun computeHorizontalScrollOffset(state: RecyclerView.State): Int {
        return horizontalOffset
    }

    /** 屏幕可见的宽度 */
    override fun computeHorizontalScrollExtent(state: RecyclerView.State): Int {
        return width
    }

    /**
     * 执行水平滚动
     */
    private fun performHorizontalScroll(dx: Int): Int {
        if (childCount <= 0) return 0

        // 限制滚动边界：确保 horizontalOffset 在 [0, maxScrollWidth] 之间
        val prevOffset = horizontalOffset
        horizontalOffset = (horizontalOffset + dx).coerceIn(0, maxScrollWidth)
        // 计算实际的位移量
        val consumed = horizontalOffset - prevOffset

        if (consumed != 0) {
            // 如果发生了位移，需要同步一次当前屏幕上的所有列的位移位置
            syncColumns()
            // 触发外部联动
            onHorizontalScroll?.invoke(horizontalOffset)
        }

        return consumed
    }

    /**
     * 同步每行中可滚动列的位置
     */
    private fun syncColumns() {
        // 遍历 RecyclerView 当前屏幕上可见的所有行
        for (i in 0 until childCount) {
            val rowView = getChildAt(i) as? ViewGroup ?: continue
            syncColumns(rowView)
        }
    }

    /**
     * 同步每行中可滚动列的位置
     * @param viewGroup 行视图组
     */
    fun syncColumns(viewGroup: ViewGroup) {
        if (viewGroup.getTag(R.id.tag_frozencolumnlist_last_offset) == horizontalOffset) {
            return
        }
        // 处理滚动列
        for (j in frozenColumnCount until viewGroup.childCount) {
            val columnView = viewGroup.getChildAt(j)
            if (columnView.translationX == -horizontalOffset.toFloat()) {
                // 跳过已经处理过的列
                continue
            }
            columnView.translationX = -horizontalOffset.toFloat()
            // 如果 View 的左边缘 < 固定列宽度，说明它越界了
            if (columnView.x  < fixedColumnWidth) {
                // 计算需要裁剪掉的左侧宽度
                val clipLeft = (fixedColumnWidth - columnView.x).toInt()
                // 使用 View 提供的矩形裁剪（API 21+）
                clipRect.set(clipLeft, 0, columnView.width, columnView.height)
                columnView.clipBounds = clipRect
            } else {
                // 当之前有过裁剪时设置为 null
                if (columnView.clipBounds != null) {
                    columnView.clipBounds = null
                }
            }
        }
        // 记录当前行的滚动偏移量
        viewGroup.setTag(R.id.tag_frozencolumnlist_last_offset, horizontalOffset)
    }

    /**
     * 动态计算最大滚动范围
     */
    private fun calculateMaxScrollWidth() {
        // 确保有 item 和子项
        if (itemCount <= 0 || childCount <= 0) return
        // 计算横向总宽度, 只需要取第一行（第 0 个 child）来计算即可，因为每一行结构是一样的
        val row = getChildAt(0) as? ViewGroup ?: return
        var totalWidth = 0
        var fixedWidth = 0
        for (i in 0 until row.childCount) {
            val childWidth = row.getChildAt(i).width
            totalWidth += childWidth
            if (i < frozenColumnCount) {
                fixedWidth += childWidth
            }
        }
        // 固定列宽度 = 所有冻结列的宽度总和
        fixedColumnWidth = fixedWidth
        // 最大滚动距离 = 总宽度 - 固定列宽度
        maxScrollWidth = (totalWidth - width).coerceAtLeast(0)
    }

    /**
     * 重置滚动锁
     */
    private fun resetScrollLock() {
        scrollDirectionLock = SCROLL_LOCK_NONE
        totalDx = 0
        totalDy = 0
    }

}