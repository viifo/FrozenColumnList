package com.viifo.frozencolumnlist.provider

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.R
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.SortDirection
import com.viifo.frozencolumnlist.ext.dp2px

/**
 * 默认列数据提供者
 */
abstract class  DefaultColumnProvider<T : FrozenColumnData> : ColumnProvider<T> {

    override fun getFrozenColumnCount(): Int = 1

    override fun getColumnWidths(parent: ViewGroup, size: Int): List<Int> = emptyList()

    override fun createFrozenHeader(
        parent: ViewGroup,
        size: Int,
        onClick: ((View, Int) -> Unit)?
    ): List<View> {
        return (0 until size).map { index ->
            AppCompatTextView(parent.context).also {
                it.setTextColor(Color.GRAY)
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                it.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                it.setPadding(
                    parent.context.dp2px(12),
                    0,
                    parent.context.dp2px(8),
                    0
                )
                it.compoundDrawablePadding = parent.context.dp2px(2)
                it.setOnClickListener { view -> onClick?.invoke(view, index) }
            }
        }
    }

    override fun createScrollableHeader(
        parent: ViewGroup,
        size: Int,
        onClick: ((View, Int) -> Unit)?
    ): List<View> {
        return (0 until size).map { index ->
            AppCompatTextView(parent.context).also {
                it.setTextColor(Color.GRAY)
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                it.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                it.setPadding(
                    parent.context.dp2px(12),
                    0,
                    parent.context.dp2px(if (index == size - 1) 12 else 8),
                    0
                )
                it.compoundDrawablePadding = parent.context.dp2px(2)
                it.setOnClickListener { view -> onClick?.invoke(view, index) }
            }
        }
    }

    override fun bindFrozenHeaderView(view: View, data: FrozenHeaderData?) {
        (view as? AppCompatTextView)?.text = data?.name
    }

    override fun bindScrollableHeaderView(view: View, data: FrozenHeaderData?) {
        (view as? AppCompatTextView)?.let { textView ->
            textView.text = data?.name
            textView.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                when (data?.sort) {
                    SortDirection.None -> R.drawable.frozen_column_list_ic_sort_none
                    SortDirection.Asc -> R.drawable.frozen_column_list_ic_sort_asc
                    SortDirection.Desc -> R.drawable.frozen_column_list_ic_sort_desc
                    else -> 0
                },
                0
            )
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

}