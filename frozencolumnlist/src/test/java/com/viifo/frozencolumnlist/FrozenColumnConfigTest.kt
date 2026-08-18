package com.viifo.frozencolumnlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FrozenColumnConfigTest {

    @Test
    fun startPosition_alwaysStartsAtZero() {
        val config = FrozenColumnConfig(
            frozenColumnCount = 3,
            frozenColumnPosition = FrozenColumnPosition.START
        )

        assertEquals(0, config.resolveFrozenStart(columnCount = 15))
    }

    @Test
    fun middlePosition_defaultsToCenteredRange() {
        val config = FrozenColumnConfig(
            frozenColumnCount = 3,
            frozenColumnPosition = FrozenColumnPosition.MIDDLE
        )

        assertEquals(6, config.resolveFrozenStart(columnCount = 15))
    }

    @Test
    fun middlePosition_usesExplicitStart() {
        val config = FrozenColumnConfig(
            frozenColumnCount = 2,
            frozenColumnPosition = FrozenColumnPosition.MIDDLE,
            middleColumnStart = 9
        )

        assertEquals(9, config.resolveFrozenStart(columnCount = 15))
    }

    @Test
    fun invalidFrozenRange_failsEarly() {
        val config = FrozenColumnConfig(
            frozenColumnCount = 3,
            frozenColumnPosition = FrozenColumnPosition.MIDDLE,
            middleColumnStart = 13
        )

        assertThrows(IllegalArgumentException::class.java) {
            config.resolveFrozenStart(columnCount = 15)
        }
    }

    @Test
    fun visibleColumnCount_acceptsSymmetricFiveColumnViewport() {
        val config = FrozenColumnConfig(
            frozenColumnCount = 1,
            frozenColumnPosition = FrozenColumnPosition.MIDDLE,
            visibleColumnCount = 5
        )

        assertEquals(7, config.resolveFrozenStart(columnCount = 15))
    }

    @Test
    fun visibleColumnCount_acceptsFourColumnStartViewport() {
        val config = FrozenColumnConfig(
            frozenColumnCount = 1,
            frozenColumnPosition = FrozenColumnPosition.START,
            visibleColumnCount = 4
        )

        assertEquals(0, config.resolveFrozenStart(columnCount = 10))
    }

    @Test
    fun visibleColumnCount_rejectsAsymmetricMiddleViewport() {
        assertThrows(IllegalArgumentException::class.java) {
            FrozenColumnConfig(
                frozenColumnCount = 1,
                frozenColumnPosition = FrozenColumnPosition.MIDDLE,
                visibleColumnCount = 4
            )
        }
    }

    @Test
    fun visibleColumnCount_cannotExceedDataColumns() {
        val config = FrozenColumnConfig(
            frozenColumnCount = 1,
            frozenColumnPosition = FrozenColumnPosition.MIDDLE,
            visibleColumnCount = 17
        )

        assertThrows(IllegalArgumentException::class.java) {
            config.resolveFrozenStart(columnCount = 15)
        }
    }
}
