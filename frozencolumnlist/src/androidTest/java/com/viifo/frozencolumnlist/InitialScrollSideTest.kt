package com.viifo.frozencolumnlist

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.FrozenTextColumn
import com.viifo.frozencolumnlist.layout.FrozenTextRowLayout
import com.viifo.frozencolumnlist.provider.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InitialScrollSideTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val headers = List(10) { FrozenHeaderData(it + 1, "column $it") }
    private val items = List(30) { FrozenColumnData("row$it", 10) }
    private fun columnWidth(index: Int) = if (index == 9) 120 else 80
    private fun columns() = List(10) { FrozenTextColumn(it + 1, columnWidth(it), "column $it") }
    private fun config(side: FrozenColumnSide = FrozenColumnSide.RIGHT,
        position: FrozenColumnPosition = FrozenColumnPosition.END) = FrozenColumnConfig(
        frozenColumnPosition = position, visibleColumnCount = 4, initialScrollSide = side)

    private fun textProvider() = object : TextColumnProvider<FrozenColumnData>() {
        override fun getHeaderColumns(context: Context, data: List<FrozenHeaderData>) = columns()
        override fun getItemColumns(context: Context, data: FrozenColumnData) = columns()
        override fun getMinimumRowHeight(context: Context, header: Boolean) = 60
    }

    private fun legacyProvider() = object : DefaultColumnProvider<FrozenColumnData>() {
        private fun row() = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            for (index in 0 until 10) addView(TextView(context).apply { text = "column $index" },
                LinearLayout.LayoutParams(columnWidth(index), 60))
        }
        override fun createHeaderRowView(parent: ViewGroup, columnCount: Int) = row()
        override fun createItemRowView(parent: ViewGroup, viewType: Int, columnCount: Int) = row()
        override fun bindHeaderRow(holder: FrozenHeaderViewHolder, data: List<FrozenHeaderData>) = Unit
        override fun bindItemRow(holder: FrozenColumnViewHolder<FrozenColumnData>,
            data: FrozenColumnData, payloads: List<Any?>) = Unit
    }

    private fun fixture(config: FrozenColumnConfig, provider: ColumnProvider<FrozenColumnData> = textProvider(),
        withData: Boolean = true): Pair<FrozenColumnList, FrozenColumnHeader> {
        val list = FrozenColumnList(context)
        val header = FrozenColumnHeader(context)
        list.setColumnConfig(config)
        list.setProvider(provider)
        header.setProvider(provider)
        list.attachHeader(header)
        header.setHeaderData(headers)
        if (withData) list.submitList(items)
        return list to header
    }

    private fun layout(view: View, width: Int = 420, height: Int = 60) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }

    private fun layoutBoth(list: FrozenColumnList, header: FrozenColumnHeader, width: Int = 420) {
        layout(header, width)
        layout(list, width, 300)
        // Traditional rows may request a second measure after fitting ordinary column widths.
        layout(header, width)
        layout(list, width, 300)
    }

    private fun assertAligned(row: ViewGroup, settings: FrozenColumnConfig) {
        val start = settings.resolveFrozenStart(10)
        val end = start + settings.frozenColumnCount
        val index = if (settings.initialScrollSide == FrozenColumnSide.LEFT) {
            if (start == 0) end else 0
        } else {
            if (start == 0) 9 else start - 1
        }
        val frozenWidth = (start until end).sumOf(::columnWidth)
        val expectedEdge = if (settings.initialScrollSide == FrozenColumnSide.LEFT) {
            if (start == 0) frozenWidth else 0
        } else {
            if (start == 0) row.width else row.width - frozenWidth
        }
        if (row is FrozenTextRowLayout) {
            val bounds = Rect()
            assertTrue("column $index should be visible for $settings", row.getColumnBounds(index, bounds))
            assertEquals(expectedEdge, if (settings.initialScrollSide == FrozenColumnSide.LEFT) bounds.left else bounds.right)
            assertEquals(index, row.columnAt(bounds.exactCenterX(), bounds.exactCenterY()))
        } else {
            val column = row.getChildAt(index)
            assertEquals(expectedEdge, (column.x + if (settings.initialScrollSide == FrozenColumnSide.RIGHT) column.width else 0).toInt())
        }
    }

    private fun assertAligned(list: FrozenColumnList, header: FrozenColumnHeader,
        side: FrozenColumnSide = list.columnConfig.initialScrollSide) {
        val settings = list.columnConfig.copy(initialScrollSide = side)
        val manager = list.getFrozenColumnLayoutManager()!!
        assertTrue(manager.maxScrollWidth > 0)
        assertEquals(if (settings.initialScrollSide == FrozenColumnSide.RIGHT) manager.maxScrollWidth else 0,
            manager.horizontalOffset)
        assertTrue(list.childCount > 0)
        for (index in 0 until list.childCount) assertAligned(list.getChildAt(index) as ViewGroup, settings)
        assertAligned(header.headerRowView!!, settings)
    }

    @Test fun frozenCountChangesAlignAdjacentColumnsAndExplicitResetUsesInitialSide() = instrumentation.runOnMainSync {
        for (position in listOf(FrozenColumnPosition.START, FrozenColumnPosition.END)) {
            for (side in FrozenColumnSide.values()) {
                val (list, header) = fixture(config(side, position))
                layoutBoth(list, header)
                assertAligned(list, header)
                val adjacentSide = if (position == FrozenColumnPosition.END) FrozenColumnSide.RIGHT else FrozenColumnSide.LEFT
                for (count in listOf(2, 3, 1, 2, 3, 1)) {
                    list.setColumnConfig(list.columnConfig.copy(frozenColumnCount = count))
                    layoutBoth(list, header)
                    assertAligned(list, header, adjacentSide)
                    list.resetHorizontalOffsets()
                    layoutBoth(list, header)
                    assertAligned(list, header)
                    list.updateHorizontalOffset(50)
                }
            }
        }
    }

    @Test fun switchingDefaultSideReusesRowsAndResetHonorsTheNewSide() = instrumentation.runOnMainSync {
        val (list, header) = fixture(config())
        layoutBoth(list, header)
        val manager = list.layoutManager
        val row = list.getChildAt(0)
        for (side in listOf(FrozenColumnSide.LEFT, FrozenColumnSide.RIGHT, FrozenColumnSide.LEFT)) {
            list.setColumnConfig(list.columnConfig.copy(initialScrollSide = side))
            layoutBoth(list, header)
            assertSame(manager, list.layoutManager)
            assertSame(row, list.getChildAt(0))
            assertAligned(list, header)
            list.updateHorizontalOffset(70)
            list.setColumnConfig(list.columnConfig)
            list.requestLayout()
            layoutBoth(list, header)
            assertEquals(70, list.getFrozenColumnLayoutManager()!!.horizontalOffset)
            list.resetHorizontalOffsets()
            layoutBoth(list, header)
            assertAligned(list, header)
        }
    }

    @Test fun rightDefaultWaitsForDataAndTracksViewportWidth() = instrumentation.runOnMainSync {
        val (list, header) = fixture(config(), withData = false)
        list.resetHorizontalOffsets()
        layoutBoth(list, header)
        list.submitList(items)
        layoutBoth(list, header)
        assertAligned(list, header)
        layoutBoth(list, header, 540)
        assertAligned(list, header)
        list.scrollBy(0, 600)
        assertTrue(list.getChildAdapterPosition(list.getChildAt(0)) > 0)
        assertAligned(list, header)
    }

    @Test fun explicitOffsetBeforeDataIsNotOverwrittenByRightDefault() = instrumentation.runOnMainSync {
        val (list, header) = fixture(config(), withData = false)
        list.updateHorizontalOffset(70)
        layoutBoth(list, header)
        list.submitList(items)
        layoutBoth(list, header)
        assertEquals(70, list.getFrozenColumnLayoutManager()!!.horizontalOffset)
        assertEquals(70, (header.headerRowView as FrozenTextRowLayout).leftOffset)
        list.resetHorizontalOffsets()
        layoutBoth(list, header)
        assertAligned(list, header)
    }

    @Test fun traditionalRowsAndHeadersAlsoAlignAdjacentColumnsAfterFrozenCountChanges() = instrumentation.runOnMainSync {
        for (position in listOf(FrozenColumnPosition.START, FrozenColumnPosition.END)) {
            for (side in FrozenColumnSide.values()) {
                val (list, header) = fixture(config(side, position), legacyProvider())
                layoutBoth(list, header)
                assertAligned(list, header)
                val adjacentSide = if (position == FrozenColumnPosition.END) FrozenColumnSide.RIGHT else FrozenColumnSide.LEFT
                for (count in listOf(2, 3, 1)) {
                    list.setColumnConfig(list.columnConfig.copy(frozenColumnCount = count))
                    layoutBoth(list, header)
                    assertAligned(list, header, adjacentSide)
                    list.resetHorizontalOffsets()
                    layoutBoth(list, header)
                    assertAligned(list, header)
                }
            }
        }
    }

    @Test fun disablingRightDefaultThenCyclingKeepsAdjacentColumnsEvenAfterResize() = instrumentation.runOnMainSync {
        val (list, header) = fixture(config())
        layoutBoth(list, header)
        list.setColumnConfig(list.columnConfig.copy(initialScrollSide = FrozenColumnSide.LEFT))
        layoutBoth(list, header)
        assertAligned(list, header, FrozenColumnSide.LEFT)
        for (count in listOf(2, 3, 1, 2, 3, 1)) {
            list.setColumnConfig(list.columnConfig.copy(frozenColumnCount = count))
            layoutBoth(list, header)
            assertAligned(list, header, FrozenColumnSide.RIGHT)
            assertEquals(FrozenColumnSide.LEFT, list.columnConfig.initialScrollSide)
            layoutBoth(list, header, 540)
            assertAligned(list, header, FrozenColumnSide.RIGHT)
        }
        // A wide viewport may temporarily remove overflow; narrowing it must retain right alignment.
        list.setColumnConfig(list.columnConfig.copy(visibleColumnCount = null))
        layoutBoth(list, header, 1200)
        assertEquals(0, list.getFrozenColumnLayoutManager()!!.maxScrollWidth)
        layoutBoth(list, header)
        assertAligned(list, header, FrozenColumnSide.RIGHT)
    }
}
