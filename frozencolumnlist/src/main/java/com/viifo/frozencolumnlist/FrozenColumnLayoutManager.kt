package com.viifo.frozencolumnlist

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 一个支持冻结列的 RecyclerView 布局管理器
 */
class FrozenColumnLayoutManager(
    context: Context,
    private val frozenColumnCount: Int
) : LinearLayoutManager(context, VERTICAL, false) {

    /** 水平滚动偏移量 */
    private var horizontalOffset = 0
    /** 固定列的宽度 */
    private var fixedColumnWidth = 0
    /** 最大可滚动距离 */
    private var maxScrollWidth = 0
    /** item 裁剪区域 */
    private val clipRect = Rect()

    /** 水平滑动时回调，用于同步表头 */
    var onHorizontalScroll: ((Int) -> Unit)? = null

    /** 是否支持水平滚动 */
    override fun canScrollHorizontally(): Boolean = true

    /**
     * 水平滚动处理
     */
    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        if (childCount <= 0) return 0
        // 动态计算最大滚动范围 (只需计算一次)
        if (maxScrollWidth <= 0) {
            calculateMaxScrollWidth()
        }

        // 限制滚动边界：确保 horizontalOffset 在 [0, maxScrollWidth] 之间
        val prevOffset = horizontalOffset
        horizontalOffset = (horizontalOffset + dx).coerceIn(0, maxScrollWidth)

        // 计算实际的位移量
        val consumed = horizontalOffset - prevOffset

        // 遍历当前可见的 Item，平移内部的滚动容器
        syncColumns()
        // 触发外部联动
        onHorizontalScroll?.invoke(horizontalOffset)

        return consumed
    }

    /**
     * 布局子项时同步冻结列
     */
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        syncColumns()
    }

    /**
     * 同步每行中可滚动列的位置
     */
    private fun syncColumns() {
        // 遍历 RecyclerView 当前屏幕上可见的所有行
        for (i in 0 until childCount) {
            val rowView = getChildAt(i) as? ViewGroup ?: continue
            // 处理滚动列
            for (j in frozenColumnCount until rowView.childCount) {
                val columnView = rowView.getChildAt(j)
                columnView.translationX = -horizontalOffset.toFloat()
                // 如果 View 的左边缘 < 固定列宽度，说明它越界了
                if (columnView.x  < fixedColumnWidth) {
                    // 计算需要裁剪掉的左侧宽度
                    val clipLeft = (fixedColumnWidth - columnView.x).toInt()
                    // 使用 View 提供的矩形裁剪（API 21+）
                    clipRect.set(clipLeft, 0, columnView.width, columnView.height)
                    columnView.clipBounds = clipRect
                } else {
                    // 恢复不裁剪
                    columnView.clipBounds = null
                }
            }
        }
    }

    /**
     * 动态计算最大滚动范围
     */
    private fun calculateMaxScrollWidth() {
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
        Log.e("TAG", "maxScrollWidth = $maxScrollWidth, fixedWidth = $fixedWidth, totalWidth = $totalWidth, width = $width")
    }

    /**
     * dp值转换为px
     */
    private fun Context.dp2px(dp: Int): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

}