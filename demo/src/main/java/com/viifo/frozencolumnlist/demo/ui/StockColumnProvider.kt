package com.viifo.frozencolumnlist.demo.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setPadding
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.SortDirection
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.ext.dp2px
import com.viifo.frozencolumnlist.provider.TextColumnProvider
import com.viifo.frozencolumnlist.data.FrozenTextColumn

/** 行情列 Demo；可按正常或反向顺序创建完整行。 */
class StockColumnProvider(
    private val reverseColumns: Boolean = false
) : TextColumnProvider<StockModel>() {

    private val columnIds = listOf(
        R.id.item_tv_price,
        R.id.item_tv_change,
        R.id.item_tv_change_amount,
        R.id.item_tv_prev_close,
        R.id.item_tv_volume,
        R.id.item_tv_amplitude,
        R.id.item_tv_turnover,
        R.id.item_tv_market_cap,
        R.id.item_tv_circulating_cap
    )

    @SuppressLint("SetTextI18n")
    override fun createEmptyView(context: Context): View {
        return LinearLayoutCompat(context).apply {
            orientation = LinearLayoutCompat.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            addView(AppCompatImageView(context).apply {
                id = R.id.empty_item_icon
                setImageResource(R.drawable.ic_empty)
            }, context.dp2px(30), context.dp2px(30))
            addView(AppCompatTextView(context).apply {
                id = R.id.empty_item_desc
                text = "没有自选股数据 (EmptyView)"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(context.getColor(R.color.gray))
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(context.dp2px(15))
            }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun createFooterView(context: Context): View {
        return AppCompatTextView(context).apply {
            text = "＋添加自选股 (FooterView)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(context.getColor(R.color.purple_200))
            gravity = Gravity.CENTER
            setPadding(context.dp2px(12))
        }
    }

    override fun getHeaderColumns(context: Context, data: List<FrozenHeaderData>): List<FrozenTextColumn> =
        data.mapIndexed { position, header ->
            val originalIndex = if (reverseColumns) data.lastIndex - position else position
            FrozenTextColumn(
                id = header.id ?: position,
                width = context.dp2px(if (originalIndex == 0) 120 else 80),
                text = header.name.orEmpty(),
                textColor = Color.GRAY,
                textSizeSp = 12f,
                gravity = (if (originalIndex == 0 && !reverseColumns) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL,
                paddingStart = context.dp2px(10),
                paddingEnd = context.dp2px(10),
                backgroundColor = Color.WHITE,
                endDrawable = when (header.sort) {
                    SortDirection.None -> com.viifo.frozencolumnlist.R.drawable.frozen_column_list_ic_sort_none
                    SortDirection.Asc -> com.viifo.frozencolumnlist.R.drawable.frozen_column_list_ic_sort_asc
                    SortDirection.Desc -> com.viifo.frozencolumnlist.R.drawable.frozen_column_list_ic_sort_desc
                    null -> 0
                }
            )
        }

    override fun getItemColumns(context: Context, data: StockModel): List<FrozenTextColumn> {
        val values = listOf(data.price, data.changePercent, data.changeAmount, data.preClose,
            data.volume, data.amplitude, data.turnover, data.marketCap, data.circulatingCap)
        val columns = listOf(
            FrozenTextColumn(
                id = R.id.item_tv_name, width = context.dp2px(120), text = data.name,
                secondaryText = data.code,
                gravity = (if (reverseColumns) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL,
                paddingStart = context.dp2px(12), paddingEnd = context.dp2px(8),
                backgroundColor = Color.WHITE
            )
        ) + values.take(data.columnCount - 1).mapIndexed { index, value ->
            FrozenTextColumn(
                id = columnIds[index], width = context.dp2px(80), text = value,
                textColor = if (index == 1) {
                    if (value.startsWith("+")) Color.RED else Color.rgb(0, 145, 90)
                } else Color.BLACK,
                gravity = Gravity.END or Gravity.CENTER_VERTICAL,
                paddingEnd = context.dp2px(if (index == data.columnCount - 2) 14 else 10),
                backgroundColor = Color.WHITE
            )
        }
        return if (reverseColumns) columns.reversed() else columns
    }

    override fun getChangePayload(oldItem: StockModel, newItem: StockModel): Any = Unit
}
