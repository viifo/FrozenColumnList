package com.viifo.frozencolumnlist.layout

import kotlin.math.abs

internal enum class MiddleFrozenGestureDirection {
    NONE,
    HORIZONTAL,
    VERTICAL
}

/** 在 RecyclerView 分发 dx/dy 前，使用同一触摸事件的完整位移锁定手势方向。 */
internal object MiddleFrozenGestureDirectionResolver {

    fun resolve(
        dx: Float,
        dy: Float,
        horizontalThreshold: Int,
        verticalThreshold: Int
    ): MiddleFrozenGestureDirection {
        val absDx = abs(dx)
        val absDy = abs(dy)
        return when {
            absDy > verticalThreshold && absDy >= absDx -> MiddleFrozenGestureDirection.VERTICAL
            absDx > horizontalThreshold && absDx > absDy -> MiddleFrozenGestureDirection.HORIZONTAL
            else -> MiddleFrozenGestureDirection.NONE
        }
    }
}
