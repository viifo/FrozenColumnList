package com.viifo.frozencolumnlist.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.layout.FrozenColumnLayoutManager
import kotlin.math.abs

/**
 * FrozenColumnList 边界分割线装饰器
 * @param context 上下文
 * @param dividerColor 分割线颜色
 * @param dividerHeightPx 分割线高度（像素, 默认 1px）
 */
class BoundDividerDecoration(
    context: Context?,
    dividerColor: Int,
    private val dividerHeightPx: Int = 1
) : RecyclerView.ItemDecoration() {

    private val rect: RectF = RectF()
    private val paint by lazy {
        Paint().apply { color = dividerColor }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = dividerHeightPx
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val lm = parent.layoutManager as? FrozenColumnLayoutManager ?: return
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i) ?: continue
            // 默认边界
            rect.set(
                child.left.toFloat(),
                child.bottom.toFloat(),
                child.right.toFloat(),
                (child.bottom + dividerHeightPx).toFloat()
            )
            // 根据 horizontalOffset 处理边界缩进
            when {
                lm.horizontalOffset < 0 -> {
                    // 手指向右滑动越界（滑动到第一列左侧）
                    rect.right = lm.frozenColumnWidth.toFloat()
                    if (rect.right > rect.left) {
                        // 绘制冻结(固定)列的分割线
                        c.drawRect(rect, paint)
                    }
                    // 计算可滚动列的边界
                    rect.left = lm.frozenColumnWidth + abs(lm.horizontalOffset).toFloat()
                    rect.right = child.right.toFloat()
                }
                lm.horizontalOffset > lm.maxScrollWidth -> {
                    // 手指向左滑动滑动越界（滑动到最后一列）
                    rect.right = (child.right - (lm.horizontalOffset - lm.maxScrollWidth)).toFloat()
                }
            }
            if (rect.right > rect.left) {
                c.drawRect(rect, paint)
            }
        }
    }
}