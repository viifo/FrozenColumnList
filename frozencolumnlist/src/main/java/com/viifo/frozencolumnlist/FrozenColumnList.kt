package com.viifo.frozencolumnlist

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.IdRes
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

    var provider: ColumnProvider<out FrozenColumnData>? = null
        private set

    private var genericStockAdapter: GenericStockAdapter<out FrozenColumnData>? = null
    private var frozenColumnLayoutManager: FrozenColumnLayoutManager? = null

    /** 触发阈值（像素） */
    private val touchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    /** 是否已处理触摸冲突解决策略 */
    private var setupTouchConflictResolution = false
    /** 最大越界距离（像素, 默认 80dp） */
    private var prevMaxOverScrollThreshold = context.dp2px(80)

    /** 记录触摸事件的初始坐标 */
    private var startX = 0f
    private var startY = 0f

    /**
     * 为 FrozenColumnList 设置 ColumnProvider
     * @param provider 列视图提供器
     */
    fun <T: FrozenColumnData> setProvider(provider: ColumnProvider<T>) {
        this@FrozenColumnList.provider = provider
        frozenColumnLayoutManager?.frozenColumnCount = provider.getFrozenColumnCount()
        adapter = provider.getAdapter().also {
            genericStockAdapter = it
            it.setupEmptyView(context, provider.createEmptyView(context))
            it.setupFooterView(context, provider.createFooterView(context))
        }
    }

    /**
     * 设置 FrozenColumnList 的数据
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
     * 添加子项点击事件监听的 View ID 集合
     * @param viewIds 子项点击事件监听的 View ID 集合
     */
    fun addChildClickViewIds(@IdRes vararg viewIds: Int) {
        genericStockAdapter?.addChildClickViewIds(*viewIds)
    }

    /**
     * 设置 item 子 view 点击事件监听
     * @param listener 子 view项点击事件监听回调
     */
    fun setOnItemChildClickListener(listener: ((View, position: Int, itemViewType: Int) -> Unit)? = null) {
        genericStockAdapter?.onItemChildClickListener = listener
    }

    /**
     * 设置 Item 点击事件监听
     * @param listener Item 点击事件监听回调
     */
    fun setOnItemClickListener(listener: ((View, position: Int, itemViewType: Int) -> Unit)? = null) {
        genericStockAdapter?.onItemClickListener = listener
    }

    /**
     * 设置 EmptyView 子 view 点击事件监听
     * @param listener 子 view项点击事件监听回调
     */
    fun setOnEmptyViewChildClickListener(listener: ((View) -> Unit)? = null) {
        genericStockAdapter?.onEmptyViewChildClickListener = listener
    }

    /**
     * 设置 EmptyView 点击事件监听
     * @param listener Item 点击事件监听回调
     */
    fun setOnEmptyViewClickListener(listener: ((View) -> Unit)? = null) {
        genericStockAdapter?.onEmptyViewClickListener = listener
    }

    /**
     * 设置 FooterView 子 view 点击事件监听
     * @param listener 子 view项点击事件监听回调
     */
    fun setOnFooterViewChildClickListener(listener: ((View) -> Unit)? = null) {
        genericStockAdapter?.onFooterViewChildClickListener = listener
    }

    /**
     * 设置 FooterView 点击事件监听
     * @param listener Item 点击事件监听回调
     */
    fun setOnFooterViewClickListener(listener: ((View) -> Unit)? = null) {
        genericStockAdapter?.onFooterViewClickListener = listener
    }

    /**
     * 获取指定位置的 Item 数据
     * @param position Item 位置
     * @return Item 数据
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: FrozenColumnData> getItem(position: Int): T? {
        return (adapter as? ListAdapter<T, *>)?.currentList?.getOrNull(position)
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
        return genericStockAdapter ?: (adapter as? GenericStockAdapter<out FrozenColumnData>)
    }

    /**
     * 获取当前绑定的数据列表
     * @return 当前绑定的数据列表
     */
    fun getData(): List<FrozenColumnData>? {
        return getFrozenColumnAdapter()?.currentList
    }

    /**
     * 处理 ViewPager2 嵌套冲突
     */
    fun setupTouchConflictResolution(value: Boolean) {
        if (value) {
            getFrozenColumnLayoutManager()?.let {
                prevMaxOverScrollThreshold = it.maxOverScrollThreshold
                it.maxOverScrollThreshold = 0
            }
        } else {
            getFrozenColumnLayoutManager()?.maxOverScrollThreshold = prevMaxOverScrollThreshold
        }
        setupTouchConflictResolution = value
    }

    /**
     * 初始化 View
     */
    private fun initView() {
        layoutManager = FrozenColumnLayoutManager(context).also { frozenColumnLayoutManager = it }
        // setHasFixedSize(true)
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

    /**
     * 处理 ViewPager2 嵌套冲突
     */
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (!setupTouchConflictResolution){
            return super.dispatchTouchEvent(e)
        }
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = e.x
                startY = e.y
                // 告知父容器：先不要拦截，我可能需要滑动
                parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.x - startX
                val dy = e.y - startY
                if (abs(dx) > abs(dy) && abs(dx) > touchSlop) {
                    // 横向滑动逻辑
                    if (!canScrollHorizontally(if (dx > 0) -1 else 1)) {
                        // 已经无法再滑动了
                        parent.requestDisallowInterceptTouchEvent(false)
                    } else {
                        // 还能滑动，继续霸占事件
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                } else if (abs(dy) > touchSlop) {
                    // 纵向滑动逻辑
                    if (!canScrollVertically(if (dy > 0) -1 else 1)) {
                        // 已经无法再滑动了
                        parent.requestDisallowInterceptTouchEvent(false)
                    } else {
                        // 还能滑动，继续霸占事件
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.dispatchTouchEvent(e)
    }

}