package com.viifo.frozencolumnlist.demo.ui

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.ext.dp2px
import com.viifo.frozencolumnlist.provider.ColumnProvider

/**
 * 自定义股票列数据提供者
 */
class StockColumnProvider : ColumnProvider<StockModel> {

    override fun getColumnWidths(parent: ViewGroup, size: Int): List<Int> {
        return (0 until size).map {
            if (it == 0) {
                // 第一列 (固定) 宽度为120dp
                parent.context.dp2px(120)
            } else {
                // 其他列 (可滚动) 宽度为80dp
                parent.context.dp2px(80)
            }
        }
    }

    override fun createRowContainer(parent: ViewGroup): ViewGroup {
        return LinearLayoutCompat(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayoutCompat.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    override fun createRowFrozenViews(parent: ViewGroup, size: Int): List<View> {
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

    override fun createRowScrollableViews(parent: ViewGroup, size: Int): List<View> {
        // 可滚动列 (动态设置，eg.这里设置 9 列)
        return (0 until size).map { index ->
            AppCompatTextView(parent.context).also {
                // 只有一个 view，不需要设置 id
                // it.id = R.id.item_xxx
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
    }

    override fun bindRowFrozenViews(views: List<View>, data: StockModel, payloads: List<Any?>) {
        // 固定列数据绑定
        views.getOrNull(0)?.apply {
            findViewById<AppCompatTextView>(R.id.item_tv_name).text = data.name
            findViewById<AppCompatTextView>(R.id.item_tv_code).text = data.code
        }
    }

    override fun bindRowScrollableViews(views: List<View>, data: StockModel, payloads: List<Any?>) {
        // 可滚动列数据绑定
        (views.getOrNull(0) as? AppCompatTextView)?.text = data.price
        (views.getOrNull(1) as? AppCompatTextView)?.let {
            it.text = data.changePercent
            // 简单的涨跌颜色逻辑
            it.setTextColor(if (data.changePercent.contains("+")) Color.RED else Color.GREEN)
        }
        (views.getOrNull(2) as? AppCompatTextView)?.text = data.changeAmount
        (views.getOrNull(3) as? AppCompatTextView)?.text = data.preClose
        (views.getOrNull(4) as? AppCompatTextView)?.text = data.volume
        (views.getOrNull(5) as? AppCompatTextView)?.text = data.amplitude
        (views.getOrNull(6) as? AppCompatTextView)?.text = data.amplitude
        (views.getOrNull(7) as? AppCompatTextView)?.text = data.turnover
        (views.getOrNull(8) as? AppCompatTextView)?.text = data.marketCap
    }

}