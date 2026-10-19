package com.viifo.frozencolumnlist

import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.FrozenTextColumn
import com.viifo.frozencolumnlist.layout.FrozenRowTextView
import com.viifo.frozencolumnlist.layout.FrozenTextRowLayout
import com.viifo.frozencolumnlist.provider.TextColumnProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FrozenTextRowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val config = FrozenColumnConfig(frozenColumnPosition = FrozenColumnPosition.MIDDLE,
        middleColumnStart = 7, visibleColumnCount = 5)
    private fun columns() = List(15) { FrozenTextColumn(it + 1, if (it == 7) 100 else 80, "value $it") }
    private fun layout(row: View, width: Int = 420) {
        row.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(60, View.MeasureSpec.EXACTLY))
        row.layout(0, 0, width, 60)
    }
    private fun row() = FrozenTextRowLayout(context, 15).apply {
        configure(config)
        submitColumns(columns())
        layout(this)
        applyHorizontalOffsets(maxLeftOffset, 0)
    }

    @Test fun fifteenColumnsCreateThreeContentViewsAndKeepNativeFrozenStyling() = instrumentation.runOnMainSync {
        val row = row()
        assertEquals(3, row.childCount)
        assertEquals(2, (0 until row.childCount).count { row.getChildAt(it) is FrozenRowTextView })
        val fixed = row.getFrozenColumnView(7)!!
        assertEquals("value 7", fixed.text.toString())
        fixed.elevation = 8f
        assertEquals(8f, fixed.elevation)
        assertEquals(100, fixed.width)
        assertEquals(7, row.columnAt(160f, 20f))
        assertEquals(8, row.columnAt(260f, 20f))
    }

    @Test fun visibleBoundsAndTouchesAgreeAfterIndependentScroll() = instrumentation.runOnMainSync {
        val row = row()
        row.applyHorizontalOffsets(370, 35)
        val bounds = Rect()
        assertTrue(row.getColumnBounds(4, bounds))
        assertEquals(Rect(0, 0, 30, 60), bounds)
        assertFalse(row.getColumnBounds(0, bounds))
        assertTrue(bounds.isEmpty)
        assertTrue(row.getColumnBoundsById(9, bounds))
        assertEquals(Rect(260, 0, 305, 60), bounds)
        var clicked: Int? = null
        row.setOnClickListener { clicked = row.columnAt(row.lastTouchX, row.lastTouchY) }
        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 270f, 20f, 0)
        val up = MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, 270f, 20f, 0)
        row.dispatchTouchEvent(down)
        row.dispatchTouchEvent(up)
        down.recycle(); up.recycle()
        // Unattached Views may post performClick; the same activation path is also used by TalkBack.
        row.activateColumn(8)
        assertEquals(8, clicked)
    }

    @Test fun updatesRetainThreeViewsAndFrozenIdentity() = instrumentation.runOnMainSync {
        val row = row()
        val fixed = row.getFrozenColumnView(7)
        val first = row.getChildAt(0)
        row.updateColumn(2, row.columns[2].copy(text = "+3.1%", textColor = Color.RED))
        row.updateColumn(7, row.columns[7].copy(text = "updated"))
        layout(row)
        assertEquals(3, row.childCount)
        assertSame(fixed, row.getFrozenColumnView(7))
        assertSame(first, row.getChildAt(0))
        assertEquals("updated", fixed!!.text.toString())
        assertEquals(370 + 30, row.leftOffset)
    }

    @Test fun switchingFrozenRangeRebuildsOnlyRequiredNativeViews() = instrumentation.runOnMainSync {
        val row = row()
        row.configure(config.copy(frozenColumnCount = 2, visibleColumnCount = 6))
        layout(row, 500)
        assertEquals(4, row.childCount)
        assertNotNull(row.getFrozenColumnView(8))
        assertEquals(100, row.getFrozenColumnView(7)!!.width)
        assertEquals(80, row.getFrozenColumnView(8)!!.width)
        row.configure(FrozenColumnConfig(frozenColumnPosition = FrozenColumnPosition.END, visibleColumnCount = 4))
        layout(row, 420)
        assertEquals(2, row.childCount)
        assertEquals(420, row.getFrozenColumnView(14)!!.right)
        row.configure(FrozenColumnConfig(visibleColumnCount = 4))
        layout(row, 420)
        assertEquals(2, row.childCount)
        assertEquals(0, row.getFrozenColumnView(0)!!.left)
    }

    @Test fun headerReusesRowAndKeepsLogicalClickAndRefreshIndices() = instrumentation.runOnMainSync {
        val provider = object : TextColumnProvider<FrozenColumnData>() {
            override fun getHeaderColumns(context: Context, data: List<FrozenHeaderData>) =
                columns().mapIndexed { index, column -> column.copy(text = data[index].name.orEmpty()) }
            override fun getItemColumns(context: Context, data: FrozenColumnData) = columns()
        }
        val header = FrozenColumnHeader(context)
        header.setColumnConfig(config)
        header.setProvider(provider)
        val data = List(15) { FrozenHeaderData(it + 1, "header $it") }
        header.setHeaderData(data)
        layout(header)
        val row = header.getChildAt(0) as FrozenTextRowLayout
        header.setHeaderData(data)
        assertSame(row, header.getChildAt(0))
        row.applyHorizontalOffsets(row.maxLeftOffset, 0)
        var clicked = -1
        header.onHeaderItemClickListener = { _, index -> clicked = index }
        row.activateColumn(9)
        assertEquals(9, clicked)
        header.refreshHeader(9, FrozenHeaderData(10, "changed"))
        assertEquals("changed", row.columns[9].text)
        header.setColumnConfig(config.copy(frozenColumnCount = 2, visibleColumnCount = 6))
        layout(header, 500)
        assertEquals(4, row.childCount)
    }

    @Test fun fixedColumnAccessibilityClickUsesLogicalIndex() = instrumentation.runOnMainSync {
        val row = row()
        var clicked = -1
        row.setOnClickListener { clicked = row.columnAt(row.lastTouchX, row.lastTouchY) ?: -1 }
        row.getFrozenColumnView(7)!!.performClick()
        assertEquals(7, clicked)
    }

    @Test fun partiallyVisibleTextCannotPaintOverNativeFrozenBackground() = instrumentation.runOnMainSync {
        for (position in FrozenColumnPosition.values()) {
            val row = row()
            val settings = FrozenColumnConfig(frozenColumnPosition = position)
            row.configure(settings)
            val fixedIndex = settings.resolveFrozenStart(15)
            row.submitColumns(columns().mapIndexed { index, column ->
                column.copy(backgroundColor = if (index == fixedIndex) Color.BLUE else Color.RED)
            })
            layout(row)
            row.applyHorizontalOffsets(35, 35)
            val bitmap = Bitmap.createBitmap(420, 60, Bitmap.Config.ARGB_8888)
            row.draw(Canvas(bitmap))
            assertEquals(Color.BLUE, bitmap.getPixel(row.frozenStart + 2, 2))
            assertEquals(Color.BLUE, bitmap.getPixel(row.frozenEnd - 2, 2))
            bitmap.recycle()
        }
    }

    private fun provider() = object : TextColumnProvider<FrozenColumnData>() {
        override fun getHeaderColumns(context: Context, data: List<FrozenHeaderData>) = columns()
        override fun getItemColumns(context: Context, data: FrozenColumnData) = columns().map {
            it.copy(text = "${data.id}:${it.id}")
        }
        override fun getMinimumRowHeight(context: Context, header: Boolean) = 60
        override fun getSideBackgroundColor(context: Context, data: FrozenColumnData,
            side: FrozenColumnSide, selected: Boolean): Int = if (selected) Color.RED else Color.WHITE
    }

    private fun layoutList(list: FrozenColumnList) {
        list.measure(View.MeasureSpec.makeMeasureSpec(420, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY))
        list.layout(0, 0, 420, 300)
    }

    @Test fun recyclerRestoresOffsetsAndColumnClicksAfterVerticalReuse() = instrumentation.runOnMainSync {
        val list = FrozenColumnList(context)
        val header = FrozenColumnHeader(context)
        val provider = provider()
        list.setColumnConfig(config)
        list.setProvider(provider)
        header.setColumnConfig(config)
        header.setProvider(provider)
        header.setHeaderData(List(15) { FrozenHeaderData(it, "header $it") })
        layout(header)
        list.attachHeader(header)
        list.submitList(List(50) { FrozenColumnData("row$it", 15) })
        layoutList(list)
        list.updateHorizontalOffsets(370, 35)
        list.scrollBy(0, 900)
        val row = list.getChildAt(0) as FrozenTextRowLayout
        val headerRow = header.getChildAt(0) as FrozenTextRowLayout
        assertEquals(3, row.childCount)
        assertEquals(370, row.leftOffset)
        assertEquals(35, row.rightOffset)
        assertEquals(headerRow.rightOffset, row.rightOffset)
        val position = list.getChildAdapterPosition(row)
        assertTrue(position > 0)
        var clickedPosition = -1
        var clickedColumn = -1
        var clickedId = -1
        list.setOnColumnClickListener { _, item, column, id ->
            clickedPosition = item; clickedColumn = column; clickedId = id
        }
        row.activateColumn(8)
        assertEquals(position, clickedPosition)
        assertEquals(8, clickedColumn)
        assertEquals(9, clickedId)
        list.setSideSelected(position, FrozenColumnSide.LEFT, true)
        layoutList(list)
        assertEquals(Color.RED, (list.findViewHolderForAdapterPosition(position)!!.itemView as FrozenTextRowLayout).leftBackgroundColor)
    }

    @Test fun selectionFollowsDataIdWhenListOrderChanges() {
        lateinit var list: FrozenColumnList
        val items = List(5) { FrozenColumnData("row$it", 15) }
        instrumentation.runOnMainSync {
            list = FrozenColumnList(context)
            list.setColumnConfig(config)
            list.setProvider(provider())
            list.submitList(items)
            layoutList(list)
            list.setSideSelected(0, FrozenColumnSide.RIGHT, true)
        }
        val committed = CountDownLatch(1)
        instrumentation.runOnMainSync { list.submitList(items.reversed(), Runnable { committed.countDown() }) }
        assertTrue(committed.await(5, TimeUnit.SECONDS))
        instrumentation.runOnMainSync {
            layoutList(list)
            assertTrue(list.getFrozenColumnAdapter()!!.isSideSelected(4, FrozenColumnSide.RIGHT))
            assertFalse(list.getFrozenColumnAdapter()!!.isSideSelected(0, FrozenColumnSide.RIGHT))
        }
    }

    @Test fun leadingAndTrailingManagersSynchronizeGroupedHeaderAndRows() = instrumentation.runOnMainSync {
        for (position in listOf(FrozenColumnPosition.START, FrozenColumnPosition.END)) {
            val list = FrozenColumnList(context)
            val header = FrozenColumnHeader(context)
            val provider = provider()
            list.setColumnConfig(FrozenColumnConfig(frozenColumnPosition = position, visibleColumnCount = 4))
            list.setProvider(provider)
            header.setProvider(provider)
            list.attachHeader(header)
            header.setHeaderData(List(15) { FrozenHeaderData(it, "header $it") })
            layout(header)
            list.submitList(List(5) { FrozenColumnData("row$it", 15) })
            layoutList(list)
            list.updateHorizontalOffset(80)
            val row = list.getChildAt(0) as FrozenTextRowLayout
            val headerRow = header.getChildAt(0) as FrozenTextRowLayout
            assertEquals(2, row.childCount)
            assertEquals(80, row.leftOffset)
            assertEquals(80, headerRow.rightOffset)
            val bounds = Rect()
            val headerBounds = Rect()
            val index = if (position == FrozenColumnPosition.START) 2 else 1
            assertTrue(row.getColumnBounds(index, bounds))
            assertTrue(headerRow.getColumnBounds(index, headerBounds))
            assertEquals(bounds.left, headerBounds.left)
            assertEquals(bounds.right, headerBounds.right)
        }
    }
}
