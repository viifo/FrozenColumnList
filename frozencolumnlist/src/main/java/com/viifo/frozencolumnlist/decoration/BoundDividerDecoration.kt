package com.viifo.frozencolumnlist.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.FrozenColumnPosition
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
            if (lm.frozenColumnPosition == FrozenColumnPosition.END) {
                updateEndDivider(c, child, lm)
            } else {
                updateStartDivider(c, child, lm)
            }
            if (rect.right > rect.left) {
                c.drawRect(rect, paint)
            }
        }
    }

    private fun updateStartDivider(
        canvas: Canvas,
        child: View,
        layoutManager: FrozenColumnLayoutManager
    ) {
        when {
            layoutManager.horizontalOffset < 0 -> {
                rect.right = child.left + layoutManager.frozenColumnWidth.toFloat()
                if (rect.right > rect.left) canvas.drawRect(rect, paint)
                rect.left = rect.right + abs(layoutManager.horizontalOffset)
                rect.right = child.right.toFloat()
            }
            layoutManager.horizontalOffset > layoutManager.maxScrollWidth -> {
                rect.right = (
                    child.right -
                        (layoutManager.horizontalOffset - layoutManager.maxScrollWidth)
                    ).toFloat()
            }
        }
    }

    private fun updateEndDivider(
        canvas: Canvas,
        child: View,
        layoutManager: FrozenColumnLayoutManager
    ) {
        val frozenStart = child.right - layoutManager.frozenColumnWidth.toFloat()
        when {
            layoutManager.horizontalOffset < 0 -> {
                rect.left = frozenStart
                rect.right = child.right.toFloat()
                if (rect.right > rect.left) canvas.drawRect(rect, paint)
                rect.left = child.left + abs(layoutManager.horizontalOffset).toFloat()
                rect.right = frozenStart
            }
            layoutManager.horizontalOffset > layoutManager.maxScrollWidth -> {
                rect.left = frozenStart
                rect.right = child.right.toFloat()
                if (rect.right > rect.left) canvas.drawRect(rect, paint)
                rect.left = child.left.toFloat()
                rect.right = frozenStart -
                    (layoutManager.horizontalOffset - layoutManager.maxScrollWidth)
            }
        }
    }
}
