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
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.SortDirection
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.ext.dp2px
import com.viifo.frozencolumnlist.provider.DefaultColumnProvider
import com.viifo.frozencolumnlist.provider.FrozenColumnViewHolder
import com.viifo.frozencolumnlist.provider.FrozenHeaderViewHolder

/** 前置固定列 Demo；每行在 createItemRowView 中一次性创建。 */
class StockColumnProvider : DefaultColumnProvider<StockModel>() {

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

    override fun createHeaderRowView(parent: ViewGroup, columnCount: Int): ViewGroup {
        return LinearLayoutCompat(parent.context).apply {
            orientation = LinearLayoutCompat.HORIZONTAL
            repeat(columnCount) { index ->
                addView(
                    createHeaderCell(index == 0),
                    if (index == 0) context.dp2px(120) else context.dp2px(80),
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    }

    override fun bindHeaderRow(holder: FrozenHeaderViewHolder, data: List<FrozenHeaderData>) {
        data.forEachIndexed { index, item -> bindHeaderCell(holder.getColumnView(index), item) }
    }

    override fun bindHeaderColumn(
        holder: FrozenHeaderViewHolder,
        position: Int,
        data: FrozenHeaderData
    ) = bindHeaderCell(holder.getColumnView(position), data)

    override fun createItemRowView(
        parent: ViewGroup,
        viewType: Int,
        columnCount: Int
    ): ViewGroup {
        return LinearLayoutCompat(parent.context).apply {
            orientation = LinearLayoutCompat.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(-1, -2)
            repeat(columnCount) { index ->
                val cell = if (index == 0) createNameCell(this) else createValueCell(this, index, columnCount)
                addView(cell, if (index == 0) context.dp2px(120) else context.dp2px(80), -1)
            }
        }
    }

    override fun bindItemRow(
        holder: FrozenColumnViewHolder<StockModel>,
        data: StockModel,
        payloads: List<Any?>
    ) {
        holder.getView<AppCompatTextView>(R.id.item_tv_name).text = data.name
        holder.getView<AppCompatTextView>(R.id.item_tv_code).text = data.code
        val values = listOf(
            data.price, data.changePercent, data.changeAmount, data.preClose, data.volume,
            data.amplitude, data.turnover, data.marketCap, data.circulatingCap
        )
        values.take(data.columnCount - 1).forEachIndexed { index, value ->
            holder.getView<AppCompatTextView>(columnIds[index]).apply {
                text = value
                if (id == R.id.item_tv_change) {
                    setTextColor(if (value.startsWith("+")) Color.RED else Color.rgb(0, 145, 90))
                }
            }
        }
    }

    private fun bindHeaderCell(view: AppCompatTextView, data: FrozenHeaderData) {
        view.text = data.name
        view.setCompoundDrawablesWithIntrinsicBounds(
            0, 0,
            when (data.sort) {
                SortDirection.None -> com.viifo.frozencolumnlist.R.drawable.frozen_column_list_ic_sort_none
                SortDirection.Asc -> com.viifo.frozencolumnlist.R.drawable.frozen_column_list_ic_sort_asc
                SortDirection.Desc -> com.viifo.frozencolumnlist.R.drawable.frozen_column_list_ic_sort_desc
                null -> 0
            }, 0
        )
    }

    private fun createNameCell(parent: ViewGroup): View = LinearLayoutCompat(parent.context).apply {
        orientation = LinearLayoutCompat.VERTICAL
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setPadding(parent.context.dp2px(12), parent.context.dp2px(8), parent.context.dp2px(8), parent.context.dp2px(8))
        setBackgroundColor(Color.WHITE)
        addView(text(parent, R.id.item_tv_name, Gravity.START, Color.BLACK, 14f))
        addView(text(parent, R.id.item_tv_code, Gravity.START, Color.GRAY, 12f))
    }

    private fun createValueCell(parent: ViewGroup, index: Int, count: Int): View {
        return text(
            parent,
            columnIds[index - 1],
            Gravity.END or Gravity.CENTER_VERTICAL,
            Color.BLACK,
            14f
        ).apply {
            setPadding(0, 0, parent.context.dp2px(if (index == count - 1) 14 else 10), 0)
            setBackgroundColor(Color.WHITE)
        }
    }

    private fun text(parent: ViewGroup, idValue: Int, gravityValue: Int, color: Int, size: Float) =
        AppCompatTextView(parent.context).apply {
            id = idValue
            gravity = gravityValue
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        }

    private fun ViewGroup.createHeaderCell(start: Boolean): AppCompatTextView {
        return AppCompatTextView(context).apply {
            gravity = (if (start) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL
            setTextColor(Color.GRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(context.dp2px(10), 0, context.dp2px(10), 0)
            setBackgroundColor(Color.WHITE)
        }
    }
}
