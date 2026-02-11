package com.viifo.frozencolumnlist.ext

import android.content.Context

/**
 * dp值转换为px
 */
internal fun Context.dp2px(dp: Int): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}