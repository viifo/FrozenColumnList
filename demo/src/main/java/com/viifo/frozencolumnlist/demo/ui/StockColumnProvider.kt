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
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.ext.dp2px
import com.viifo.frozencolumnlist.layout.GenericStockAdapter
import com.viifo.frozencolumnlist.provider.DefaultColumnProvider

/**
 * 自定义股票持仓列表提供者
 */
class StockColumnProvider : DefaultColumnProvider<StockModel>() {

    @SuppressLint("SetTextI18n")
    override fun createEmptyView(context: Context): View {
        return LinearLayoutCompat(context).apply {
            orientation = LinearLayoutCompat.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addView(
                AppCompatImageView(context).apply {
                    id = R.id.empty_item_icon
                    setImageResource(R.drawable.ic_empty)
                },
                context.dp2px(30),
                context.dp2px(30),
            )
            addView(
                AppCompatTextView(context).apply {
                    id = R.id.empty_item_desc
                    text = "没有自选股数据 (EmptyView)"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTextColor(context.getColor(R.color.gray))
                    setPadding(0, context.dp2px(15), 0, context.dp2px(15))
                },
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT
            )
        }
    }

    @SuppressLint("SetTextI18n")
    override fun createFooterView(context: Context): View {
        return AppCompatTextView(context).apply {
            text = "＋添加自选股 (FooterView)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(context.getColor(R.color.purple_200))
            gravity = Gravity.CENTER
            layoutParams = ViewGroup.MarginLayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = context.dp2px(12)
                it.bottomMargin = context.dp2px(12)
            }
        }
    }

//    override fun getColumnWidths(parent: ViewGroup, size: Int): List<Int> {
//        return (0 until size).map {
//            if (it == 0) {
//                // 第一列 (固定) 宽度为120dp
//                parent.context.dp2px(120)
//            } else {
//                // 其他列 (可滚动) 宽度为80dp
//                parent.context.dp2px(80)
//            }
//        }
//    }

    override fun createRowFrozenViews(parent: ViewGroup, viewType: Int, size: Int): List<View> {
        val paddingVertical = parent.context.dp2px(8)
        return (0 until size).map {
            LinearLayoutCompat(parent.context).apply {
                orientation = LinearLayoutCompat.VERTICAL
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.WHITE)
                addView(AppCompatTextView(parent.context).also {
                    it.id = R.id.item_tv_name
                    it.setTextColor(Color.BLACK)
                    it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                })
                addView(AppCompatTextView(parent.context).also {
                    it.id = R.id.item_tv_code
                    it.setTextColor(Color.GRAY)
                    it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                })
                setPadding(
                    parent.context.dp2px(12),
                    paddingVertical,
                    parent.context.dp2px(8),
                    paddingVertical
                )
            }
        }
    }

    override fun createRowScrollableViews(parent: ViewGroup, viewType: Int, size: Int): List<View> {
        // 可滚动列 (动态设置，eg.这里设置 9 列)
        val typedArray = parent.context.resources.obtainTypedArray(R.array.column_ids)
        val list = (0 until size).map { index ->
            AppCompatTextView(parent.context).also {
                it.id = typedArray.getResourceId(index, View.NO_ID)
                it.setTextColor(Color.BLACK)
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                it.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                it.setPadding(
                    0,
                    0,
                    parent.context.dp2px(if (index == size - 1) 14 else 10),
                    0
                )
                it.setBackgroundColor(Color.WHITE)
            }
        }
        typedArray.recycle()
        return list
    }

    override fun bindRowFrozenViews(
        holder: GenericStockAdapter.GenericViewHolder<StockModel>,
        data: StockModel,
        payloads: List<Any?>
    ) {
        // 固定列数据绑定
        holder.setText(R.id.item_tv_name, data.name)
        holder.setText(R.id.item_tv_code, data.code)
    }

    override fun bindRowScrollableViews(
        holder: GenericStockAdapter.GenericViewHolder<StockModel>,
        data: StockModel,
        payloads: List<Any?>
    ) {
        // 可滚动列数据绑定
        holder.setText(R.id.item_tv_price, data.price)
        holder.setText(R.id.item_tv_change, data.changePercent)
        holder.getView<AppCompatTextView>(R.id.item_tv_change).apply {
            text = data.changePercent
            // 简单的涨跌颜色逻辑
            setTextColor(if (data.changePercent.contains("+")) Color.RED else Color.GREEN)
        }
        holder.setText(R.id.item_tv_change_amount, data.changeAmount)
        holder.setText(R.id.item_tv_prev_close, data.preClose)
        holder.setText(R.id.item_tv_volume, data.volume)
        holder.setText(R.id.item_tv_amplitude, data.amplitude)
        holder.setText(R.id.item_tv_turnover, data.turnover)
        holder.setText(R.id.item_tv_market_cap, data.marketCap)
        holder.setText(R.id.item_tv_circulating_cap, data.circulatingCap)
    }

}