package com.viifo.frozencolumnlist

import android.graphics.Rect
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.ui.MainActivity
import com.viifo.frozencolumnlist.layout.FrozenTextRowLayout
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise real button clicks with a window, layout passes, and the Demo's actual provider. */
@RunWith(AndroidJUnit4::class)
class Watchlist4SwitchTest {
    @Test fun disabledInitialRightSwitchStillKeepsColumnsContinuousWhenCyclingFrozenCount() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_end)).perform(click())
            val toggle = onView(allOf(withId(R.id.switch_initial_right), isDisplayed()))
            val countButton = onView(allOf(withId(R.id.btn_load), isDisplayed()))
            toggle.check(matches(isChecked()))
            countButton.check(matches(withText("固定 1 列")))
            assertVisibleColumns(listOf(6, 7, 8, 9))

            toggle.perform(click()).check(matches(not(isChecked())))
            assertVisibleColumns(listOf(0, 1, 2, 9))
            for (count in listOf(2, 3, 1, 2, 3, 1)) {
                countButton.perform(click()).check(matches(withText("固定 $count 列")))
                toggle.check(matches(not(isChecked())))
                assertVisibleColumns(listOf(6, 7, 8, 9))
            }
        }
    }

    private fun assertVisibleColumns(expected: List<Int>) {
        for (id in listOf(R.id.frozen_column_header, R.id.frozen_column_list)) {
            onView(allOf(withId(id), isDisplayed())).check { view, noView ->
                if (noView != null) throw noView
                val container = view as ViewGroup
                val rows = if (view is FrozenColumnList) {
                    assertEquals(FrozenColumnPosition.END, view.columnConfig.frozenColumnPosition)
                    (0 until container.childCount).map { container.getChildAt(it) }
                } else listOf(container.getChildAt(0))
                val textRows = rows.filterIsInstance<FrozenTextRowLayout>()
                assertTrue(textRows.isNotEmpty())
                for (row in textRows) {
                    val bounds = Rect()
                    // Ignore up to two remainder pixels from fitting three ordinary columns.
                    val visible = row.columns.indices.filter { row.getColumnBounds(it, bounds) && bounds.width() > 2 }
                    assertEquals("Visible logical columns in ${resourcesName(id)}", expected, visible)
                    if (expected == listOf(6, 7, 8, 9)) {
                        for (index in 6..8) {
                            assertTrue(row.getColumnBounds(index, bounds))
                            val edge = bounds.right
                            assertTrue(row.getColumnBounds(index + 1, bounds))
                            assertEquals("Columns must touch the frozen range without a gap", edge, bounds.left)
                        }
                        assertEquals(row.width - row.paddingRight, bounds.right)
                    }
                }
            }
        }
    }

    private fun resourcesName(id: Int) = if (id == R.id.frozen_column_header) "header" else "list rows"
}
