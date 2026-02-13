package com.viifo.frozencolumnlist

internal object FrozenColumConfig {
    /** 默认冻结(固定)列宽，单位 dp */
    const val DEFAULT_FROZEN_COLUMN_WITH_DP = 120
    /** 默认列宽，单位 dp */
    const val DEFAULT_COLUMN_WITH_DP = 80
    /** 默认最大越界距离，单位 dp */
    const val DEFAULT_MAX_OVER_SCROLL_THRESHOLD_DP = 80
    /** 默认越界回弹动画触发阈值，单位 dp */
    const val DEFAULT_OVER_SCROLL_ANIMATOR_THRESHOLD_DP = 10
    /** 默认越界回弹阻尼系数 */
    const val DEFAULT_OVER_SCROLL_DAMPING = 0.6f
}