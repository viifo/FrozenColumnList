package com.viifo.frozencolumnlist.layout

import android.view.View
import android.view.ViewGroup
import com.viifo.frozencolumnlist.R

/** 为 START 模式按可用宽度计算普通列宽，并在尺寸变化时自动重新适配。 */
internal object VisibleColumnWidthFitter {

    fun configure(
        row: ViewGroup,
        frozenColumnStart: Int,
        frozenColumnCount: Int,
        visibleColumnCount: Int?
    ) {
        val existing = row.getTag(R.id.tag_frozencolumnlist_width_fitter) as? Binding
        if (visibleColumnCount == null) {
            existing?.restore()
            return
        }
        val binding = existing ?: Binding(row).also {
            row.setTag(R.id.tag_frozencolumnlist_width_fitter, it)
            row.addOnLayoutChangeListener(it)
        }
        binding.update(frozenColumnStart, frozenColumnCount, visibleColumnCount)
    }

    private class Binding(
        private val row: ViewGroup
    ) : View.OnLayoutChangeListener {

        private val originalWidths = IntArray(row.childCount) { index ->
            row.getChildAt(index).layoutParams.width
        }
        private var frozenColumnStart = 0
        private var frozenColumnCount = 1
        private var visibleColumnCount = 0

        fun update(frozenStart: Int, frozenCount: Int, visibleCount: Int) {
            frozenColumnStart = frozenStart
            frozenColumnCount = frozenCount
            visibleColumnCount = visibleCount
            fit()
        }

        fun restore() {
            var changed = false
            for (index in 0 until minOf(row.childCount, originalWidths.size)) {
                val child = row.getChildAt(index)
                if (child.layoutParams.width != originalWidths[index]) {
                    child.layoutParams.width = originalWidths[index]
                    changed = true
                }
            }
            if (changed) row.requestLayout()
        }

        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) = fit()

        private fun fit() {
            if (row.width <= 0 || row.childCount == 0) return
            val safeFrozenStart = frozenColumnStart.coerceIn(0, row.childCount - 1)
            val safeFrozenCount = frozenColumnCount.coerceIn(1, row.childCount - safeFrozenStart)
            val frozenEnd = safeFrozenStart + safeFrozenCount
            val scrollableVisibleCount = visibleColumnCount - safeFrozenCount
            if (scrollableVisibleCount <= 0) return

            var frozenWidth = 0
            for (index in safeFrozenStart until frozenEnd) {
                val child = row.getChildAt(index)
                val originalWidth = originalWidths.getOrNull(index) ?: child.layoutParams.width
                if (originalWidth >= 0 && child.layoutParams.width != originalWidth) {
                    child.layoutParams.width = originalWidth
                }
                frozenWidth += if (originalWidth >= 0) originalWidth else child.measuredWidth
            }
            val availableWidth = (
                row.width - row.paddingLeft - row.paddingRight - frozenWidth
                ).coerceAtLeast(0)
            val fittedWidth = (availableWidth / scrollableVisibleCount).coerceAtLeast(1)

            var changed = false
            for (index in 0 until row.childCount) {
                if (index in safeFrozenStart until frozenEnd) continue
                val child = row.getChildAt(index)
                if (child.layoutParams.width != fittedWidth) {
                    child.layoutParams.width = fittedWidth
                    changed = true
                }
            }
            if (changed) row.requestLayout()
        }
    }
}
