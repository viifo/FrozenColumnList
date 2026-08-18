package com.viifo.frozencolumnlist.layout

import com.viifo.frozencolumnlist.FrozenColumnSide

internal data class MiddleFrozenOffsets(val left: Int, val right: Int)

/** 不依赖 Android View 的偏移计算，便于锁定默认位置和镜像同步规则。 */
internal object MiddleFrozenOffsetCalculator {

    fun default(maxLeft: Int): MiddleFrozenOffsets {
        return MiddleFrozenOffsets(left = maxLeft.coerceAtLeast(0), right = 0)
    }

    fun synchronized(maxLeft: Int, maxRight: Int, progress: Int): MiddleFrozenOffsets {
        val safeMaxLeft = maxLeft.coerceAtLeast(0)
        val safeProgress = progress.coerceIn(0, minOf(safeMaxLeft, maxRight.coerceAtLeast(0)))
        return MiddleFrozenOffsets(
            left = safeMaxLeft - safeProgress,
            right = safeProgress
        )
    }

    fun progressDelta(side: FrozenColumnSide, dx: Int): Int {
        return if (side == FrozenColumnSide.LEFT) -dx else dx
    }
}
