package com.viifo.frozencolumnlist.layout

import android.content.Context
import android.graphics.Rect
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.FrozenColumnHeader
import com.viifo.frozencolumnlist.R
import com.viifo.frozencolumnlist.ext.dp2px
import com.viifo.frozencolumnlist.provider.DefaultSpringBackAnimatorProvider
import com.viifo.frozencolumnlist.provider.SpringBackAnimatorProvider
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * 一个支持冻结（固定）列的 RecyclerView 布局管理器
 */
class FrozenColumnLayoutManager(
    context: Context
) : LinearLayoutManager(context, VERTICAL, false) {

    companion object {
        // 滚动方向锁, 0: 无锁, 1: 水平锁, 2: 垂直锁
        private const val SCROLL_LOCK_NONE = 0
        private const val SCROLL_LOCK_HORIZONTAL = 1
        private const val SCROLL_LOCK_VERTICAL = 2
    }

    /** 越界回弹阻尼系数，默认为 0.6f */
    internal var overScrollDamping = 0.6f
    /** 越界回弹动画触发阈值（像素, 默认 10dp） */
    internal var overScrollAnimatorThreshold = context.dp2px(10)
    /** 最大越界距离（像素, 默认 80dp） */
    internal var maxOverScrollDistance = context.dp2px(80)
    /** 越界回弹动画, 默认使用 [DefaultSpringBackAnimatorProvider], 设置为 null 禁用 */
    internal var springBackAnimatorProvider: SpringBackAnimatorProvider? = DefaultSpringBackAnimatorProvider()

    /** 冻结(固定)的列数 */
    internal var frozenColumnCount: Int = 1

    /** 已添加的 RecyclerView */
    private var attachedRecyclerView: RecyclerView? = null
    /** 水平滑动时回调，用于同步表头 */
    private val horizontalScrollListeners = CopyOnWriteArrayList<(Int) -> Unit>()

    /** 水平滚动偏移量 */
    internal var horizontalOffset = 0
        private set
    /** 固定列的宽度 */
    internal var frozenColumnWidth = 0
        private set
    /** 最大可滚动距离 */
    internal var maxScrollWidth = 0
        private set
    /** item 裁剪区域 */
    private val clipRect = Rect()

    /** 滚动方向锁定 */
    private var totalDx = 0
    private var totalDy = 0
    private var scrollDirectionLock = 0

    /** 触发阈值（像素） */
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    /** 是否正在执行 fling 动画 */
    private val isFling = AtomicBoolean(false)

    /** 是否允许水平滚动，默认为 true */
    var canScrollHorizontally = true

    /** 是否支持水平滚动 */
    override fun canScrollHorizontally() = canScrollHorizontally

    /** 滚动状态改变 */
    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        when (state) {
            RecyclerView.SCROLL_STATE_IDLE -> {
                resetScrollLock()
                checkAndSpringBack()
                isFling.set(false)
            }
            RecyclerView.SCROLL_STATE_DRAGGING -> {
                stopSpringBack()
                isFling.set(false)
            }
            RecyclerView.SCROLL_STATE_SETTLING -> {
                isFling.set(true)
            }
        }
    }

    /** 当 RecyclerView 添加到窗口时调用 */
    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        attachedRecyclerView = view
    }

    /** 当 RecyclerView 从窗口中分离时调用 */
    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
        super.onDetachedFromWindow(view, recycler)
        attachedRecyclerView = null
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
        if (scrollDirectionLock == SCROLL_LOCK_NONE) {
            if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx)) {
                scrollDirectionLock = SCROLL_LOCK_VERTICAL
            } else {
                // 还没达到阈值, 判定不属于垂直滑动，不消耗 dy
                return 0
            }
        }
        // 执行垂直滚动
        return if (scrollDirectionLock == SCROLL_LOCK_VERTICAL) {
            val consumed = super.scrollVerticallyBy(dy, recycler, state)
            // 如果发生了位移，需要同步一次当前屏幕上的所有列的位移位置
            if (consumed != 0) syncColumns()
            consumed
        } else {
            0
        }
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
        if (scrollDirectionLock == SCROLL_LOCK_NONE) {
            if (abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy)) {
                scrollDirectionLock = SCROLL_LOCK_HORIZONTAL
            } else {
                // 还没达到阈值, 判定不属于水平滑动，不消耗 dx
                return 0
            }
        }
        // 执行水平滚动
        return if (scrollDirectionLock == SCROLL_LOCK_HORIZONTAL) {
            performHorizontalScroll(dx)
        } else {
            0
        }
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
        if (childCount <= 0 || dx == 0) return 0
        val prevOffset = horizontalOffset // 记录当前偏移量
        val nextOffset = horizontalOffset + dx // 计算下一个偏移量

        if (springBackAnimatorProvider == null || nextOffset in 0..maxScrollWidth) {
            // 未开启越界回弹动画 | 在正常区间滑动时，滑动范围为 [0, maxScrollWidth]
            horizontalOffset = nextOffset.coerceIn(0, maxScrollWidth)
        } else if (nextOffset < 0) {
            // 手指向右滑动越界（滑动到第一列左侧）
            val overScroll = -nextOffset
            if (isFling.get() && (overScroll <= overScrollAnimatorThreshold)) {
                horizontalOffset = 0
            } else if (overScroll < maxOverScrollDistance) {
                horizontalOffset += (dx * overScrollDamping).toInt()
            } else {
                horizontalOffset = -maxOverScrollDistance
            }
        } else {
            // 手指向左滑动滑动越界（滑动到最后一列）
            val overScroll = nextOffset - maxScrollWidth
            if (isFling.get() && (overScroll <= overScrollAnimatorThreshold)) {
                horizontalOffset = maxScrollWidth
            } else if (overScroll < maxOverScrollDistance) {
                horizontalOffset += (dx * overScrollDamping).toInt()
            } else {
                horizontalOffset = maxScrollWidth + maxOverScrollDistance
            }
        }
        // 计算实际的位移量
        val consumed = horizontalOffset - prevOffset
        if (consumed != 0) {
            // 如果发生了位移，需要同步一次当前屏幕上的所有列的位移位置
            syncColumns()
            // 通知所有水平滑动监听回调
            dispatchScrollListener(horizontalOffset)
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
    internal fun syncColumns(viewGroup: ViewGroup) {
        if (viewGroup.getTag(R.id.tag_frozencolumnlist_last_offset) == horizontalOffset) {
            return
        }
        // 处理 header 的偏移量
        // 如果 FrozenColumnHeader 使用了 padding，需要额外处理
        // 否则 View.x 会包含 padding，与 FrozenColumnList 中的 view 坐标系不一致
        val offset = if (viewGroup is FrozenColumnHeader) viewGroup.paddingLeft else 0
        // 处理滚动列
        for (j in frozenColumnCount until viewGroup.childCount) {
            val columnView = viewGroup.getChildAt(j)
            if (columnView.translationX == -horizontalOffset.toFloat()) {
                // 跳过已经处理过的列
                continue
            }
            columnView.translationX = -horizontalOffset.toFloat()
            // 如果 View 的左边缘 < 固定列宽度，说明它越界了
            // horizontalOffset > 0 手指向右滑动越界时 columnView.x 会变大，通常不需要裁剪左侧
            if (horizontalOffset > 0 && columnView.x  < frozenColumnWidth + offset) {
                // 计算需要裁剪掉的左侧宽度
                val clipLeft = (frozenColumnWidth + offset - columnView.x).toInt()
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
        if (row.width <= 0) return
        var totalWidth = 0
        var fixedWidth = 0
        for (i in 0 until row.childCount) {
            val childWidth = row.getChildAt(i).width
            totalWidth += childWidth
            if (i < frozenColumnCount) {
                fixedWidth += childWidth
            }
        }
        // 固定列宽度 = 所有冻结(固定)列的宽度总和
        frozenColumnWidth = fixedWidth
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

    /**
     * 检查是否需要回弹
     */
    private fun checkAndSpringBack() {
        val provider = springBackAnimatorProvider ?: return
        val targetOffset = when {
            (horizontalOffset < 0) -> 0
            (horizontalOffset > maxScrollWidth) -> maxScrollWidth
            else -> return // 无需回弹
        }
        stopSpringBack()
        provider.startSpringBack(
            startOffset = horizontalOffset,
            targetOffset = targetOffset,
            onUpdate = { value -> updateHorizontalOffset(value) },
            onCancelOrEnd = {}
        )
    }

    /**
     * 更新水平滚动偏移量并同步所有可见行
     */
    internal fun updateHorizontalOffset(newOffset: Int) {
        horizontalOffset = newOffset
        syncColumns()
        // 通知 RecyclerView 刷新 ItemDecoration
        attachedRecyclerView?.postInvalidateOnAnimation()
        // 通知所有水平滑动监听回调
        dispatchScrollListener(horizontalOffset)
    }

    /**
     * 停止回弹动画
     */
    private fun stopSpringBack() {
        springBackAnimatorProvider?.stop()
    }

    /**
     * 通知所有水平滑动监听回调
     */
    private fun dispatchScrollListener(offset: Int) {
        horizontalScrollListeners.forEach { it.invoke(offset) }
    }

    /**
     * 添加水平滑动监听
     * @param listener 水平滑动监听回调
     */
    internal fun addHorizontalScrollListener(listener: (Int) -> Unit) {
        if (!horizontalScrollListeners.contains(listener)) {
            horizontalScrollListeners.add(listener)
        }
    }

    /**
     * 移除水平滑动监听
     * @param listener 要移除的水平滑动监听回调
     */
    internal fun removeHorizontalScrollListener(listener: (Int) -> Unit) {
        horizontalScrollListeners.remove(listener)
    }

    /**
     * 清空所有水平滑动监听
     */
    internal fun removeAllHorizontalScrollListener() {
        horizontalScrollListeners.clear()
    }

}