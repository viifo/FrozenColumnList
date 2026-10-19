package com.viifo.frozencolumnlist.layout

import com.viifo.frozencolumnlist.FrozenColumnConfig
import com.viifo.frozencolumnlist.FrozenColumnPosition
import org.junit.Assert.*
import org.junit.Test

class TextColumnGeometryTest {
    private val widths = IntArray(15) { if (it == 7) 100 else 80 }
    private val middle = FrozenColumnConfig(frozenColumnPosition = FrozenColumnPosition.MIDDLE,
        middleColumnStart = 7, visibleColumnCount = 5)

    @Test fun middleHasIndependentRangesAndExactBoundaryHits() {
        val g = TextColumnGeometry(widths, middle, 420)
        assertEquals(160, g.frozenStart)
        assertEquals(260, g.frozenEnd)
        assertEquals(400, g.maxLeftOffset)
        assertEquals(400, g.maxRightOffset)
        assertEquals(5, g.columnAt(0f, 400, 0))
        assertEquals(6, g.columnAt(159.9f, 400, 0))
        assertEquals(7, g.columnAt(160f, 400, 0))
        assertEquals(8, g.columnAt(260f, 400, 0))
        assertEquals(9, g.columnAt(419.9f, 400, 0))
        assertNull(g.columnAt(420f, 400, 0))
    }

    @Test fun partiallyVisibleColumnsUseScrolledCoordinates() {
        val g = TextColumnGeometry(widths, middle, 420)
        assertEquals(-50, g.left(4, 370, 35))
        assertEquals(4, g.columnAt(0f, 370, 35))
        assertEquals(5, g.columnAt(30f, 370, 35))
        assertEquals(8, g.columnAt(260f, 370, 35))
        assertEquals(9, g.columnAt(305f, 370, 35))
    }

    @Test fun fittingPreservesNativeFrozenWidthAndAccountsForPadding() {
        val g = TextColumnGeometry(widths, middle, 460, 10, 30)
        assertEquals(100, g.width(7))
        assertEquals(80, g.width(0))
        assertEquals(170, g.frozenStart)
        assertEquals(430, g.viewportEnd)
        assertNull(g.columnAt(9.9f, 400, 0))
        assertNull(g.columnAt(430f, 400, 0))
    }

    @Test fun multipleFrozenColumnsAndRuntimeVisibleCountUseNaturalWidths() {
        val two = middle.copy(frozenColumnCount = 2, visibleColumnCount = 6)
        val g = TextColumnGeometry(widths, two, 500)
        assertEquals(180, g.frozenWidth)
        assertEquals(80, g.width(6))
        assertEquals(7, g.columnAt(160f, 400, 0))
        assertEquals(8, g.columnAt(260f, 400, 0))
        assertEquals(9, g.columnAt(340f, 400, 0))
        val unfitted = TextColumnGeometry(widths, two.copy(visibleColumnCount = null), 700)
        assertEquals(80, unfitted.width(0))
    }

    @Test fun startSupportsOverscrollWithoutHittingFrozenColumnThroughTheClip() {
        val g = TextColumnGeometry(intArrayOf(120, 80, 80, 80, 80), FrozenColumnConfig(), 360)
        assertEquals(80, g.maxRightOffset)
        assertEquals(0, g.columnAt(119f, 0, 40))
        assertEquals(1, g.columnAt(120f, 0, 40))
        assertEquals(2, g.columnAt(160f, 0, 40))
        assertNull(g.columnAt(120f, 0, -30))
        assertEquals(1, g.columnAt(150f, 0, -30))
    }

    @Test fun endAnchorsFrozenColumnsAndLeavesOverscrollGap() {
        val g = TextColumnGeometry(intArrayOf(80, 80, 80, 80, 120),
            FrozenColumnConfig(frozenColumnPosition = FrozenColumnPosition.END), 360)
        assertEquals(240, g.frozenStart)
        assertEquals(80, g.maxLeftOffset)
        assertEquals(4, g.columnAt(240f, 80, 0))
        assertEquals(3, g.columnAt(239f, 80, 0))
        assertNull(g.columnAt(0f, -20, 0))
        assertNull(g.columnAt(230f, 100, 0))
    }

    @Test fun noScrollableColumnsAndNarrowViewportRemainValid() {
        val g = TextColumnGeometry(intArrayOf(120), FrozenColumnConfig(), 80)
        assertEquals(0, g.maxLeftOffset)
        assertEquals(0, g.maxRightOffset)
        assertEquals(0, g.columnAt(79f, 0, 0))
        assertNull(g.columnAt(80f, 0, 0))
    }

    @Test fun asymmetricMiddleAndExplicitPositionHaveSeparateLimits() {
        val g = TextColumnGeometry(IntArray(10) { 80 },
            FrozenColumnConfig(frozenColumnPosition = FrozenColumnPosition.MIDDLE,
                middleColumnStart = 3, frozenViewportStart = 120), 400)
        assertEquals(120, g.maxLeftOffset)
        assertEquals(280, g.maxRightOffset)
        assertEquals(2, g.columnAt(119f, 120, 0))
        assertEquals(3, g.columnAt(120f, 120, 0))
        assertEquals(4, g.columnAt(200f, 120, 0))
    }
}
