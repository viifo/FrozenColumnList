package com.viifo.frozencolumnlist.layout

import com.viifo.frozencolumnlist.FrozenColumnSide

/** Geometry shared by the traditional column hierarchy and the grouped text row. */
internal interface FrozenRowViewport {
    val frozenStart: Int
    val frozenEnd: Int
    val maxLeftOffset: Int
    val maxRightOffset: Int
    val lastTouchX: Float
    val lastTouchY: Float
    var leftBackgroundColor: Int
    var rightBackgroundColor: Int
    fun sideAt(x: Float): FrozenColumnSide?
    fun applyHorizontalOffsets(left: Int, right: Int)
}
