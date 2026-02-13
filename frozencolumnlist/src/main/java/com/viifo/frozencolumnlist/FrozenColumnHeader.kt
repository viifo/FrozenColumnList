package com.viifo.frozencolumnlist

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.GravityCompat
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.ext.dp2px
import com.viifo.frozencolumnlist.provider.ColumnProvider

/**
 * 冻结(固定)列表头视图
 */
class FrozenColumnHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    /** 表头点击事件 */
    var onHeaderClickListener: ((view: View, header: FrozenHeaderData?) -> Unit)? = null
    /** 表头数据 */
    val headerData: MutableList<FrozenHeaderData> = mutableListOf()

    /** 视图对齐方式 */
    var itemGravity: Int = GravityCompat.START or Gravity.TOP
        set(value) {
            field = value
            gravity = value
        }

    /** 视图是否充满父容器高度 */
    var itemFullHeight: Boolean = true

    /** 视图宽度, 默认值为 120dp */
    var itemWidth: Int = LayoutParams.WRAP_CONTENT

    /** 冻结(固定)列数量 */
    private var frozenColumnCount: Int = 1

    /**
     * 刷新表头，通常在表头数据发生变化但是表头数量没有发生变化时使用
     * 如果表头数量发生变化，请使用 [setHeaderData] 方法, 并使用 [FrozenColumnList.rebuildList] 方法刷新列表
     *
     * @param headerData 表头数据
     */
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("NotifyDataSetChanged")
    fun refreshHeader(headerData: List<FrozenHeaderData>) {
        if (childCount <= 0) return
        // TODO ... 刷新表头
    }

    /**
     * 设置表头数据
     * @param headerData 表头数据
     * @param provider 列视图提供器
     */
    fun setHeaderData(
        data: List<FrozenHeaderData>,
        provider: ColumnProvider<out FrozenColumnData>
    ) {
        headerData.clear()
        headerData.addAll(data)
        frozenColumnCount = provider.getFrozenColumnCount()
        // 初始化表头
        val frozenHeaders = provider.createFrozenHeader(
            this,
            headerData.take(frozenColumnCount)
        )
        val scrollHeaders = provider.createScrollableHeader(
            this,
            headerData.takeLast(headerData.size - frozenColumnCount)
        )
        val viewWidths = provider.getColumnWidths(this, headerData.size)
        initHeader(frozenHeaders, scrollHeaders, viewWidths)
    }

    /**
     * 初始化表头视图
     * @param frozenHeaders 固定列头视图列表
     * @param scrollHeaders 滚动列头视图列表
     * @param viewWidths 头视图宽度列表
     */
    private fun initHeader(
        frozenHeaders: List<View>?,
        scrollHeaders: List<View>?,
        viewWidths: List<Int>?
    ) {
        removeAllViews()
        frozenHeaders?.forEachIndexed { index, view ->
            view.setOnClickListener {
                onHeaderClickListener?.invoke(
                    it, headerData.getOrNull(index)
                )
            }
            addView(
                view,
                viewWidths?.getOrNull(index) ?: itemWidth,
                if (itemFullHeight) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
            )
        }
        val frozenHeaderSize = frozenHeaders?.size ?: 0
        scrollHeaders?.forEachIndexed { index, view ->
            view.setOnClickListener {
                onHeaderClickListener?.invoke(
                    it, headerData.getOrNull(frozenHeaderSize + index)
                )
            }
            addView(
                view,
                viewWidths?.getOrNull(frozenHeaderSize + index) ?: itemWidth,
                if (itemFullHeight) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
            )
        }
    }

    /**
     * 初始化属性
     * @param context 上下文
     * @param attrs 属性集
     */
    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.FrozenColumnHeader) {
            itemGravity = getInt(R.styleable.FrozenColumnHeader_itemGravity, GravityCompat.START or Gravity.TOP)
            itemFullHeight = getBoolean(R.styleable.FrozenColumnHeader_itemFullHeight, true)
            itemWidth = getDimensionPixelSize(
                R.styleable.FrozenColumnHeader_itemWidth,
                context.dp2px(FrozenColumConfig.DEFAULT_COLUMN_WITH_DP)
            )
        }
    }

    init {
        orientation = HORIZONTAL
        // 初始化属性
        initAttrs(context, attrs)
    }

}