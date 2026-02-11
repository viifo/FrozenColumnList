package com.viifo.frozencolumnlist.demo.ui.fragment

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.data.FrozenHeaderData
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
                // 其他列 (可滚动) 宽度为90dp
                parent.context.dp2px(90)
            }
        }
    }

    override fun createRowContainer(parent: ViewGroup): ViewGroup {
        val paddingVertical = parent.context.dp2px(8)
        return LinearLayoutCompat(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayoutCompat.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, paddingVertical, 0, paddingVertical)
        }
    }

    override fun createFrozenHeader(parent: ViewGroup, data: List<FrozenHeaderData>): List<View> {
        return (0 until data.size).map {
            createHeaderView(
                context = parent.context,
                gravity = Gravity.START,
                text = data[it].name
            )
        }
    }

    override fun createScrollableHeader(parent: ViewGroup, data: List<FrozenHeaderData>): List<View> {
        return (0 until data.size).map {
            createHeaderView(context = parent.context, text = data[it].name)
        }
    }

    override fun createFrozenViews(parent: ViewGroup, size: Int): List<View> {
        return (0 until size).map {
            LinearLayoutCompat(parent.context).apply {
                orientation = LinearLayoutCompat.VERTICAL
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
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
            }
        }
    }

    override fun createScrollableViews(parent: ViewGroup, size: Int): List<View> {
        // 可滚动列 (动态设置，eg.这里设置 9 列)
        return (0 until size).map { index ->
            AppCompatTextView(parent.context).also {
                // 只有一个 view，不需要设置 id
                // it.id = R.id.item_xxx
                it.setTextColor(Color.BLACK)
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                it.gravity = Gravity.END
                it.setPadding(
                    if (index == 0) 0 else parent.context.dp2px(8),
                    0, 0, 0
                )
            }
        }
    }

    override fun bindFrozenViews(views: List<View>, data: StockModel, payloads: List<Any?>) {
        // 固定列数据绑定
        views.getOrNull(0)?.apply {
            findViewById<AppCompatTextView>(R.id.item_tv_name).text = data.name
            findViewById<AppCompatTextView>(R.id.item_tv_code).text = data.code
        }
    }

    override fun bindScrollableViews(views: List<View>, data: StockModel, payloads: List<Any?>) {
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

    private fun createHeaderView(
        context: Context,
        text: String?,
        gravity: Int = Gravity.END
    ): AppCompatTextView {
        return AppCompatTextView(context).also {
            it.text = text
            it.setTextColor(Color.GRAY)
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            it.gravity = gravity
            it.setPadding(0, context.dp2px(12), 0, context.dp2px(12))
        }
    }
}