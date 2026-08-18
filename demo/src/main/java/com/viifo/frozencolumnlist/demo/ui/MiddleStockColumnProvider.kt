package com.viifo.frozencolumnlist.demo.ui

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.viifo.frozencolumnlist.FrozenColumnSide
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.SymmetricQuoteModel
import com.viifo.frozencolumnlist.provider.DefaultColumnProvider
import com.viifo.frozencolumnlist.provider.FrozenColumnViewHolder
import com.viifo.frozencolumnlist.provider.FrozenHeaderViewHolder

/** 左右区域的灰色背景分界配置，null 表示该侧不启用灰色区域。 */
data class SideBackgroundBoundaryConfig(
    /** 左侧从该数据 ID 开始（包含该行）向下使用灰色交替背景。 */
    val leftGrayFromId: Int? = null,
    /** 右侧截至该数据 ID（包含该行）向上使用灰色交替背景。 */
    val rightGrayThroughId: Int? = null
)

/** Watchlist3 示例：表头和列表项都只 inflate 一次完整的 15 列 XML。 */
class MiddleStockColumnProvider(
    private val backgroundBoundary: SideBackgroundBoundaryConfig = SideBackgroundBoundaryConfig()
) : DefaultColumnProvider<SymmetricQuoteModel>() {

    override fun createHeaderRowView(parent: ViewGroup, columnCount: Int): ViewGroup {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.header_symmetric_quote_row, parent, false) as ViewGroup
    }

    override fun bindHeaderRow(holder: FrozenHeaderViewHolder, data: List<FrozenHeaderData>) {
        data.forEachIndexed { index, item ->
            holder.getColumnView<AppCompatTextView>(index).text = item.name
        }
    }

    override fun createItemRowView(
        parent: ViewGroup,
        viewType: Int,
        columnCount: Int
    ): ViewGroup {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_symmetric_quote_row, parent, false) as ViewGroup
    }

    override fun bindItemRow(
        holder: FrozenColumnViewHolder<SymmetricQuoteModel>,
        data: SymmetricQuoteModel,
        payloads: List<Any?>
    ) {
        data.values.forEachIndexed { index, value ->
            holder.getColumnView<AppCompatTextView>(index).apply {
                text = value
                setTextColor(
                    if (index in CHANGE_COLUMN_INDICES) {
                        if (value.startsWith("+")) Color.RED else Color.rgb(0, 145, 90)
                    } else {
                        Color.BLACK
                    }
                )
            }
        }
    }

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
