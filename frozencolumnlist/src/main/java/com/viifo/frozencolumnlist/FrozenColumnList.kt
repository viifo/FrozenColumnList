package com.viifo.frozencolumnlist

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.ext.dp2px
import com.viifo.frozencolumnlist.layout.FrozenColumnLayoutManager
import com.viifo.frozencolumnlist.layout.GenericStockAdapter
import com.viifo.frozencolumnlist.provider.ColumnProvider
import com.viifo.frozencolumnlist.provider.SpringBackAnimatorProvider
import kotlin.math.abs

/**
 * 一个支持冻结(固定)列的 RecyclerView 列表
 * 适用于需要固定左侧列，右侧内容可滚动的场景，如股票自选列表等
 */
class FrozenColumnList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    /** 越界回弹阻尼系数，默认为 0.6f */
    var overScrollDamping = 0.0f
        get() = frozenColumnLayoutManager
            ?.overScrollDamping
            ?: FrozenColumConfig.DEFAULT_OVER_SCROLL_DAMPING
        set(value) {
            field = value
            frozenColumnLayoutManager?.overScrollDamping = value
        }

    /** 越界回弹动画触发阈值（像素, 默认 10dp） */
    var overScrollAnimatorThreshold = 0
        get() = frozenColumnLayoutManager
            ?.overScrollAnimatorThreshold
            ?: context.dp2px(FrozenColumConfig.DEFAULT_OVER_SCROLL_ANIMATOR_THRESHOLD_DP)
        set(value) {
            field = value
            frozenColumnLayoutManager?.overScrollAnimatorThreshold = value
        }

    /** 最大越界距离（像素, 默认 80dp） */
    var maxOverScrollThreshold: Int = 0
        get() = frozenColumnLayoutManager
            ?.maxOverScrollThreshold
            ?: context.dp2px(FrozenColumConfig.DEFAULT_MAX_OVER_SCROLL_THRESHOLD_DP)
        set(value) {
            field = value
            frozenColumnLayoutManager?.maxOverScrollThreshold = value
        }

    /** 是否允许水平滚动，默认为 true */
    var canScrollHorizontally = true
        get() = frozenColumnLayoutManager?.canScrollHorizontally ?: true
        set(value) {
            field = value
            frozenColumnLayoutManager?.canScrollHorizontally = value
        }

    private var provider: ColumnProvider<out FrozenColumnData>? = null
    private var genericStockAdapter: GenericStockAdapter<out FrozenColumnData>? = null
    private var frozenColumnLayoutManager: FrozenColumnLayoutManager? = null

    /**
     * 设置 Adapter 并绑定数据
     * @param provider 列视图提供器
     */
    fun <T: FrozenColumnData> setProvider(provider: ColumnProvider<T>) {
        this@FrozenColumnList.provider = provider
        frozenColumnLayoutManager?.frozenColumnCount = provider.getFrozenColumnCount()
        adapter = GenericStockAdapter(provider).also { genericStockAdapter = it }
    }

    /**
     * 设置 Adapter 绑定的数据
     * @param list 数据列表
     * @param commitCallback 提交完成回调
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: FrozenColumnData> submitList(list: List<T>, commitCallback: Runnable? = null) {
        (adapter as? ListAdapter<T, *>)?.submitList(list, commitCallback)
    }

    /**
     * 绑定表头视图 (同步滚动表头)
     * @param header 表头视图
     */
    fun attachHeader(header: FrozenColumnHeader?) {
        if (header == null) return
        frozenColumnLayoutManager?.addHorizontalScrollListener {
            frozenColumnLayoutManager?.syncColumns(header)
        }
        header.onHorizontalScrollListener = { event ->
            // 将 Header 的触摸事件转发给 RecyclerView 处理
            dispatchTouchEvent(event)
        }
    }

    /**
     * 手动同步表头滚动偏移量
     */
    fun syncHeaderOffset(header: FrozenColumnHeader?) {
        header?.let {
            frozenColumnLayoutManager?.syncColumns(header)
        }
    }

    /**
     * 更新水平滚动偏移量
     * @param newOffset 新的滚动偏移量
     */
    fun updateHorizontalOffset(newOffset: Int) {
        frozenColumnLayoutManager?.updateHorizontalOffset(newOffset)
    }

    /**
     * 添加水平滚动监听
     */
    fun addHorizontalScrollListener(listener: (Int) -> Unit) {
        frozenColumnLayoutManager?.addHorizontalScrollListener(listener)
    }

    /**
     * 移除水平滚动监听
     */
    fun removeHorizontalScrollListener(listener: (Int) -> Unit) {
        frozenColumnLayoutManager?.removeHorizontalScrollListener(listener)
    }

    /**
     * 设置越界回弹动画提供器
     * @param animatorProvider 越界回弹动画提供器
     */
    fun setSpringBackAnimatorProvider(
        animatorProvider: SpringBackAnimatorProvider?
    ) {
        frozenColumnLayoutManager?.springBackAnimatorProvider = animatorProvider
    }

    /**
     * 获取布局管理器
     */
    fun getFrozenColumnLayoutManager(): FrozenColumnLayoutManager? {
        return frozenColumnLayoutManager ?: (layoutManager as? FrozenColumnLayoutManager)
    }

    /**
     * 获取适配器
     */
    fun getFrozenColumnAdapter(): GenericStockAdapter<out FrozenColumnData>? {
        return getFrozenColumnAdapter() ?: (adapter as? GenericStockAdapter<out FrozenColumnData>)
    }

    /**
     * 处理 ViewPager2 嵌套冲突
     */
    private fun setupTouchConflictResolution() {
        addOnItemTouchListener(object : SimpleOnItemTouchListener() {
            private var startX = 0f
            private var startY = 0f

            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = e.x
                        startY = e.y
                        // 告知父容器：先不要拦截，我可能需要滑动
                        rv.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(e.x - startX)
                        val dy = abs(e.y - startY)

                        // 如果横滑趋势明显，锁定在当前 View 响应，不让 ViewPager2 翻页
                        if (dx > dy && dx > 10f) {
                            rv.parent.requestDisallowInterceptTouchEvent(true)
                        } else if (dy > dx) {
                            // 纵滑时交给原生处理
                            rv.parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
                return false
            }
        })
    }

    /**
     * 初始化 View
     */
    private fun initView() {
        // 处理 ViewPager2 嵌套冲突
        // setupTouchConflictResolution()
        // 初始化 recyclerView 并添加到根布局
        layoutManager = FrozenColumnLayoutManager(context).also { frozenColumnLayoutManager = it }
        setHasFixedSize(true)
        overScrollMode = OVER_SCROLL_NEVER
    }

    /**
     * 初始化属性
     * @param context 上下文
     * @param attrs 属性集
     */
    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.FrozenColumnList) {
            overScrollDamping = getFloat(
                R.styleable.FrozenColumnList_overScrollDamping,
                FrozenColumConfig.DEFAULT_OVER_SCROLL_DAMPING
            )
            overScrollAnimatorThreshold = getDimensionPixelSize(
                R.styleable.FrozenColumnList_overScrollAnimatorThreshold,
                context.dp2px(FrozenColumConfig.DEFAULT_OVER_SCROLL_ANIMATOR_THRESHOLD_DP)
            )
            maxOverScrollThreshold = getDimensionPixelSize(
                R.styleable.FrozenColumnList_maxOverScrollThreshold,
                context.dp2px(FrozenColumConfig.DEFAULT_COLUMN_WITH_DP)
            )
        }
    }

    init {
        // 初始化 View
        initView()
        // 初始化属性
        initAttrs(context, attrs)
    }

    override fun onDetachedFromWindow() {
        frozenColumnLayoutManager?.removeAllHorizontalScrollListener()
        super.onDetachedFromWindow()
    }

}