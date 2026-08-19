package com.viifo.frozencolumnlist

/** 固定列所在区域。 */
enum class FrozenColumnPosition { START, MIDDLE, END }

/** 中间固定模式下左右区域的滚动方式。 */
enum class FrozenColumnScrollMode { INDEPENDENT, SYNCHRONIZED }

/** 中间固定列左右两侧的区域。 */
enum class FrozenColumnSide { LEFT, RIGHT }

/**
 * FrozenColumnList / FrozenColumnHeader 的统一配置。
 *
 * @param frozenColumnCount 固定列数量 n，必须大于 0
 * @param frozenColumnPosition 固定列位于前 n 列、中间 n 列或末尾 n 列
 * @param middleColumnStart 中间固定列起始索引；null 时按总列数居中计算
 * @param scrollMode 中间固定模式下左右区域独立或镜像同步滚动
 * @param frozenViewportStart 中间固定区域的屏幕左坐标；null 时水平居中
 * @param visibleColumnCount 一屏完整显示的列数（包含固定列）；设置后普通列等宽适配可用空间。
 * 中间固定模式要求固定列两侧显示相同数量的普通列
 * @param horizontalScrollThreshold 水平手势触发阈值（像素）；null 时使用系统 touchSlop
 */
data class FrozenColumnConfig(
    val frozenColumnCount: Int = 1,
    val frozenColumnPosition: FrozenColumnPosition = FrozenColumnPosition.START,
    val middleColumnStart: Int? = null,
    val scrollMode: FrozenColumnScrollMode = FrozenColumnScrollMode.INDEPENDENT,
    val frozenViewportStart: Int? = null,
    val visibleColumnCount: Int? = null,
    val horizontalScrollThreshold: Int? = null
) {
    init {
        require(frozenColumnCount > 0) { "frozenColumnCount must be greater than 0" }
        middleColumnStart?.let { require(it >= 0) { "middleColumnStart must not be negative" } }
        visibleColumnCount?.let { visibleCount ->
            require(visibleCount > frozenColumnCount) {
                "visibleColumnCount must be greater than frozenColumnCount"
            }
            require(
                frozenColumnPosition != FrozenColumnPosition.MIDDLE ||
                    (visibleCount - frozenColumnCount) % 2 == 0
            ) {
                "visibleColumnCount must leave the same number of columns on both sides in MIDDLE mode"
            }
        }
        horizontalScrollThreshold?.let {
            require(it >= 0) { "horizontalScrollThreshold must not be negative" }
        }
    }

    internal fun resolveFrozenStart(columnCount: Int): Int {
        require(frozenColumnCount <= columnCount) {
            "frozenColumnCount ($frozenColumnCount) exceeds columnCount ($columnCount)"
        }
        visibleColumnCount?.let { visibleCount ->
            require(visibleCount <= columnCount) {
                "visibleColumnCount ($visibleCount) exceeds columnCount ($columnCount)"
            }
        }
        return when (frozenColumnPosition) {
            FrozenColumnPosition.START -> 0
            FrozenColumnPosition.MIDDLE -> {
                val start = middleColumnStart ?: ((columnCount - frozenColumnCount) / 2)
                require(start + frozenColumnCount <= columnCount) {
                    "Middle frozen range [$start, ${start + frozenColumnCount}) exceeds $columnCount columns"
                }
                start
            }
            FrozenColumnPosition.END -> columnCount - frozenColumnCount
        }
    }

    companion object {
        /** 默认最大越界距离，单位 dp。 */
        const val DEFAULT_MAX_OVER_SCROLL_DISTANCE_DP = 80
        /** 默认越界回弹动画触发阈值，单位 dp。 */
        const val DEFAULT_OVER_SCROLL_ANIMATOR_THRESHOLD_DP = 10
        /** 默认越界回弹阻尼系数。 */
        const val DEFAULT_OVER_SCROLL_DAMPING = 0.6f
    }
}
