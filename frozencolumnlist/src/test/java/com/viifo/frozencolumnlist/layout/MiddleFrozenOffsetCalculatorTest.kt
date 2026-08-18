package com.viifo.frozencolumnlist.layout

import com.viifo.frozencolumnlist.FrozenColumnSide
import org.junit.Assert.assertEquals
import org.junit.Test

class MiddleFrozenOffsetCalculatorTest {

    @Test
    fun defaultOffsetShowsLastLeftColumnAndFirstRightColumn() {
        assertEquals(
            MiddleFrozenOffsets(left = 480, right = 0),
            MiddleFrozenOffsetCalculator.default(maxLeft = 480)
        )
    }

    @Test
    fun synchronizedProgressMovesOffsetsInOppositeDirections() {
        assertEquals(
            MiddleFrozenOffsets(left = 360, right = 120),
            MiddleFrozenOffsetCalculator.synchronized(
                maxLeft = 480,
                maxRight = 480,
                progress = 120
            )
        )
    }

    @Test
    fun eitherSideGestureAdvancesTheSameMirroredProgress() {
        assertEquals(30, MiddleFrozenOffsetCalculator.progressDelta(FrozenColumnSide.LEFT, -30))
        assertEquals(30, MiddleFrozenOffsetCalculator.progressDelta(FrozenColumnSide.RIGHT, 30))
    }

    @Test
    fun fastVerticalGestureDoesNotLockHorizontalScrolling() {
        assertEquals(
            MiddleFrozenGestureDirection.VERTICAL,
            MiddleFrozenGestureDirectionResolver.resolve(
                dx = 28f,
                dy = 120f,
                horizontalThreshold = 24,
                verticalThreshold = 8
            )
        )
    }

    @Test
    fun horizontalGestureMustExceedConfiguredThreshold() {
        assertEquals(
            MiddleFrozenGestureDirection.NONE,
            MiddleFrozenGestureDirectionResolver.resolve(
                dx = 20f,
                dy = 4f,
                horizontalThreshold = 24,
                verticalThreshold = 8
            )
        )
        assertEquals(
            MiddleFrozenGestureDirection.HORIZONTAL,
            MiddleFrozenGestureDirectionResolver.resolve(
                dx = 30f,
                dy = 4f,
                horizontalThreshold = 24,
                verticalThreshold = 8
            )
        )
    }
}
