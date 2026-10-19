package com.viifo.frozencolumnlist.demo.ui

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import com.viifo.frozencolumnlist.FrozenColumnSide
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.demo.data.SymmetricQuoteModel
import com.viifo.frozencolumnlist.provider.TextColumnProvider
import com.viifo.frozencolumnlist.data.FrozenTextColumn
import com.viifo.frozencolumnlist.demo.ext.dp2px

/** 左右区域的灰色背景分界配置，null 表示该侧不启用灰色区域。 */
data class SideBackgroundBoundaryConfig(
    /** 左侧从该数据 ID 开始（包含该行）向下使用灰色交替背景。 */
    val leftGrayFromId: Int? = null,
    /** 右侧截至该数据 ID（包含该行）向上使用灰色交替背景。 */
    val rightGrayThroughId: Int? = null
)

/** Watchlist3 示例：15 个逻辑列分为左侧文本 View、固定 TextView、右侧文本 View。 */
class MiddleStockColumnProvider(
    private val backgroundBoundary: SideBackgroundBoundaryConfig = SideBackgroundBoundaryConfig()
) : TextColumnProvider<SymmetricQuoteModel>() {

    override fun getHeaderColumns(context: Context, data: List<FrozenHeaderData>): List<FrozenTextColumn> =
        data.mapIndexed { index, item ->
            column(context, index, item.name.orEmpty(), true).copy(id = item.id ?: index)
        }

    override fun getItemColumns(context: Context, data: SymmetricQuoteModel): List<FrozenTextColumn> =
        data.values.mapIndexed { index, value -> column(context, index, value, false) }

    override fun getChangePayload(oldItem: SymmetricQuoteModel, newItem: SymmetricQuoteModel): Any =
        newItem.values.indices.filter { oldItem.values.getOrNull(it) != newItem.values[it] }

    override fun styleFrozenColumnView(view: TextView, columnIndex: Int, header: Boolean) {
        view.setBackgroundColor(Color.WHITE)
        view.elevation = view.context.dp2px(8).toFloat()
        view.setTypeface(view.typeface, android.graphics.Typeface.BOLD)
    }

    private fun column(context: Context, index: Int, text: String, header: Boolean) = FrozenTextColumn(
        id = index + 1,
        width = context.dp2px(if (index == 7) 96 else 86),
        text = text,
        textColor = if (header) Color.rgb(102, 102, 102) else if (index in CHANGE_COLUMN_INDICES) {
            if (text.startsWith("+")) Color.RED else Color.rgb(0, 145, 90)
        } else Color.BLACK,
        textSizeSp = if (header) 12f else 13f,
        bold = index == 7,
        paddingStart = context.dp2px(6),
        paddingEnd = context.dp2px(6)
    )

    override fun getSideBackgroundColor(
        context: Context,
        data: SymmetricQuoteModel,
        side: FrozenColumnSide,
        selected: Boolean
    ): Int {
        if (selected) {
            return if (side == FrozenColumnSide.LEFT) {
                Color.rgb(255, 222, 222)
            } else {
                Color.rgb(218, 233, 255)
            }
        }
        val rowId = data.id.toIntOrNull()
        val evenRow = rowId?.rem(2) == 0
        val usesGrayBackground = when (side) {
            FrozenColumnSide.LEFT -> backgroundBoundary.leftGrayFromId?.let { boundary ->
                rowId != null && rowId >= boundary
            } ?: false
            FrozenColumnSide.RIGHT -> backgroundBoundary.rightGrayThroughId?.let { boundary ->
                rowId != null && rowId <= boundary
            } ?: false
        }
        if (usesGrayBackground) {
            return if (evenRow) GRAY_BACKGROUND_DARK else GRAY_BACKGROUND_LIGHT
        }
        return when (side) {
            FrozenColumnSide.LEFT -> if (evenRow) Color.rgb(255, 248, 248) else Color.WHITE
            FrozenColumnSide.RIGHT -> if (evenRow) Color.rgb(246, 249, 255) else Color.WHITE
        }
    }

    private companion object {
        val CHANGE_COLUMN_INDICES = setOf(2, 3, 11, 12)
        val GRAY_BACKGROUND_DARK = Color.rgb(214, 214, 214)
        val GRAY_BACKGROUND_LIGHT = Color.rgb(238, 238, 238)
    }
}
