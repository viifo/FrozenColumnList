package com.viifo.frozencolumnlist.layout

import com.viifo.frozencolumnlist.FrozenColumnConfig
import com.viifo.frozencolumnlist.FrozenColumnPosition

/** Pixel geometry independent of Android Views; drawing and hit testing use the same coordinates. */
internal class TextColumnGeometry(
    widths: IntArray,
    config: FrozenColumnConfig,
    viewportWidth: Int,
    val paddingLeft: Int = 0,
    paddingRight: Int = 0
) {
    val count = widths.size
    val frozenIndex = config.resolveFrozenStart(count)
    val frozenEndIndex = frozenIndex + config.frozenColumnCount
    val viewportEnd = (viewportWidth - paddingRight).coerceAtLeast(paddingLeft)
    private val edges = IntArray(count + 1)
    val frozenWidth: Int
    val frozenStart: Int
    val frozenEnd: Int
    val maxLeftOffset: Int
    val maxRightOffset: Int

    init {
        require(widths.all { it > 0 }) { "Text column widths must be positive pixels" }
        frozenWidth = (frozenIndex until frozenEndIndex).sumOf { widths[it] }
        val fitted = config.visibleColumnCount?.let {
            ((viewportEnd - paddingLeft - frozenWidth).coerceAtLeast(0) /
                (it - config.frozenColumnCount)).coerceAtLeast(1)
        }
        for (i in widths.indices) {
            val width = if (i in frozenIndex until frozenEndIndex) widths[i] else fitted ?: widths[i]
            edges[i + 1] = edges[i] + width
        }
        frozenStart = when (config.frozenColumnPosition) {
            FrozenColumnPosition.START -> paddingLeft
            FrozenColumnPosition.END -> viewportEnd - frozenWidth
            FrozenColumnPosition.MIDDLE -> config.frozenViewportStart
                ?: (paddingLeft + (viewportEnd - paddingLeft - frozenWidth) / 2)
        }.coerceIn(paddingLeft, (viewportEnd - frozenWidth).coerceAtLeast(paddingLeft))
        frozenEnd = frozenStart + frozenWidth
        maxLeftOffset = (edges[frozenIndex] - (frozenStart - paddingLeft)).coerceAtLeast(0)
        maxRightOffset = (edges[count] - edges[frozenEndIndex] -
            (viewportEnd - frozenEnd).coerceAtLeast(0)).coerceAtLeast(0)
    }

    fun width(index: Int): Int = edges[index + 1] - edges[index]

    fun left(index: Int, leftOffset: Int, rightOffset: Int): Int = when {
        index < frozenIndex -> paddingLeft + edges[index] - leftOffset
        index < frozenEndIndex -> frozenStart + edges[index] - edges[frozenIndex]
        else -> frozenEnd + edges[index] - edges[frozenEndIndex] - rightOffset
    }

    fun regionStart(index: Int): Int = when {
        index < frozenIndex -> paddingLeft
        index < frozenEndIndex -> frozenStart
        else -> frozenEnd
    }

    fun regionEnd(index: Int): Int = when {
        index < frozenIndex -> frozenStart
        index < frozenEndIndex -> minOf(frozenEnd, viewportEnd)
        else -> viewportEnd
    }

    fun columnAt(x: Float, leftOffset: Int, rightOffset: Int): Int? {
        if (x < paddingLeft || x >= viewportEnd) return null
        val start: Int
        val end: Int
        val contentX: Float
        when {
            x < frozenStart -> {
                start = 0; end = frozenIndex
                contentX = x - paddingLeft + leftOffset
            }
            x < frozenEnd -> {
                start = frozenIndex; end = frozenEndIndex
                contentX = x - frozenStart + edges[frozenIndex]
            }
            else -> {
                start = frozenEndIndex; end = count
                contentX = x - frozenEnd + edges[frozenEndIndex] + rightOffset
            }
        }
        var low = start
        var high = end
        while (low < high) {
            val mid = (low + high) ushr 1
            if (edges[mid + 1] <= contentX) low = mid + 1 else high = mid
        }
        return low.takeIf { it < end && contentX >= edges[it] }
    }
}
