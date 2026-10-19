package com.viifo.frozencolumnlist.data

import android.graphics.Color
import android.view.Gravity
import androidx.annotation.DrawableRes

/** A logical column, not a View. Dimensions are pixels except text sizes (sp). IDs must be unique. */
data class FrozenTextColumn(
    val id: Int,
    val width: Int,
    val text: String = "",
    val secondaryText: String? = null,
    val textColor: Int = Color.BLACK,
    val secondaryTextColor: Int = Color.GRAY,
    val textSizeSp: Float = 14f,
    val secondaryTextSizeSp: Float = 12f,
    val bold: Boolean = false,
    val gravity: Int = Gravity.CENTER,
    val paddingStart: Int = 0,
    val paddingEnd: Int = 0,
    val backgroundColor: Int = Color.TRANSPARENT,
    @param:DrawableRes val endDrawable: Int = 0,
    val drawablePadding: Int = 0,
    val contentDescription: String? = null
) {
    init {
        require(width > 0) { "width must be positive pixels" }
        require(textSizeSp > 0 && secondaryTextSizeSp > 0) { "text sizes must be positive" }
        require(paddingStart >= 0 && paddingEnd >= 0 && drawablePadding >= 0)
    }
}
