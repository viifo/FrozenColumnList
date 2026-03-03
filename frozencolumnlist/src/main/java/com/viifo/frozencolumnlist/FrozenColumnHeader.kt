package com.viifo.frozencolumnlist

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.GravityCompat
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.ext.dp2px
import com.viifo.frozencolumnlist.provider.ColumnProvider
import kotlin.math.abs

/**
 * 冻结(固定)列表头视图
 */
class FrozenColumnHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    /** 水平滑动时回调，用于同步列表 */
    var onHorizontalScrollListener: ((MotionEvent) -> Unit)? = null
    /** 表头点击事件 */
    var onHeaderItemClickListener: ((view: View, position: Int) -> Unit)? = null
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

    /** 视图宽度, 默认值为 80dp */
    var itemWidth: Int = LayoutParams.WRAP_CONTENT

    /** 冻结(固定)视图宽度, 默认值为 120dp */
    var itemFrozenWidth: Int = LayoutParams.WRAP_CONTENT

    /** 冻结(固定)列数量 */
    private var frozenColumnCount: Int = 1

    /** 列视图提供器 */
    private var provider: ColumnProvider<out FrozenColumnData>? = null

    private var startX = 0f
    private var startY = 0f
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private var isDragging = false

    /**
     * 拦截触摸事件，判断是否需要拦截滑动事件
     */
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                startX = event.x
                startY = event.y
                onHorizontalScrollListener?.invoke(event)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - startX)
                val dy = abs(event.y - startY)
                // 如果水平位移超过阈值，判定为滑动
                if (dx > touchSlop && dx > dy) {
                    isDragging = true
                    // 拦截事件，从此开始进入自身的 onTouchEvent
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    /**
     * 处理触摸事件，同步列表滚动
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        onHorizontalScrollListener?.invoke(event) // 回调水平滚动事件
        return super.onTouchEvent(event)
    }

    /**
     * 设置列视图提供器
     * @param provider 列视图提供器
     */
    fun setProvider(provider: ColumnProvider<out FrozenColumnData>) {
        this.provider = provider
    }

    /**
     * 刷新单个表头数据，
     * @param position 需要刷新的表头位置
     * @param item 新的表头数据
     */
    fun refreshHeader(position: Int, item: FrozenHeaderData) {
        if (childCount <= 0 || position < 0 || position >= headerData.size) return
        this.headerData[position] = item
        provider?.bindScrollableHeaderView(getChildAt(position), item)
    }

    /**
     * 设置表头数据
     * @param headerData 表头数据
     */
    fun setHeaderData(data: List<FrozenHeaderData>) {
        val provider = this.provider ?: throw IllegalArgumentException("ColumnProvider is not set")
        headerData.clear()
        headerData.addAll(data)
        frozenColumnCount = provider.getFrozenColumnCount()
        // 初始化表头
        val frozenHeaders = provider.createFrozenHeader(
            parent = this,
            size = frozenColumnCount,
            onClick = { view, position ->
                onHeaderItemClickListener?.invoke(view, position)
            }
        )
        val scrollHeaders = provider.createScrollableHeader(
            parent = this,
            size = headerData.size - frozenColumnCount,
            onClick = { view, position ->
                onHeaderItemClickListener?.invoke(view, frozenColumnCount + position)
            }
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
            provider?.bindFrozenHeaderView(view, headerData.getOrNull(index))
            addView(
                view,
                viewWidths?.getOrNull(index) ?: itemFrozenWidth,
                if (itemFullHeight) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
            )
        }
        scrollHeaders?.forEachIndexed { index, view ->
            provider?.bindScrollableHeaderView(view, headerData.getOrNull(frozenColumnCount + index))
            addView(
                view,
                viewWidths?.getOrNull(frozenColumnCount + index) ?: itemWidth,
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
            itemGravity = getInt(R.styleable.FrozenColumnHeader_fchItemGravity, GravityCompat.START or Gravity.TOP)
            itemFullHeight = getBoolean(R.styleable.FrozenColumnHeader_fchItemFullHeight, true)
            itemWidth = getDimensionPixelSize(
                R.styleable.FrozenColumnHeader_fchItemWidth,
                context.dp2px(FrozenColumConfig.DEFAULT_COLUMN_WITH_DP)
            )
            itemFrozenWidth = getDimensionPixelSize(
                R.styleable.FrozenColumnHeader_fchItemFrozenWidth,
                context.dp2px(FrozenColumConfig.DEFAULT_FROZEN_COLUMN_WITH_DP)
            )
        }
    }

    init {
        orientation = HORIZONTAL
        // 初始化属性
        initAttrs(context, attrs)
    }

}