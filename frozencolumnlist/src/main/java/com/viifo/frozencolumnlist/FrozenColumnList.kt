package com.viifo.frozencolumnlist

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
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
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 越界回弹阻尼系数，默认为 0.6f */
    var overScrollDamping = 0f
        get() = layoutManager
            ?.overScrollDamping
            ?: FrozenColumConfig.DEFAULT_OVER_SCROLL_DAMPING
        set(value) {
            field = value
            layoutManager?.overScrollDamping = value
        }

    /** 越界回弹动画触发阈值（像素, 默认 10dp） */
    var overScrollAnimatorThreshold = 0
        get() = layoutManager
            ?.overScrollAnimatorThreshold
            ?: context.dp2px(FrozenColumConfig.DEFAULT_OVER_SCROLL_ANIMATOR_THRESHOLD_DP)
        set(value) {
            field = value
            layoutManager?.overScrollAnimatorThreshold = value
        }

    /** 最大越界距离（像素, 默认 80dp） */
    var maxOverScrollThreshold: Int = 0
        get() = layoutManager
            ?.maxOverScrollThreshold
            ?: context.dp2px(FrozenColumConfig.DEFAULT_MAX_OVER_SCROLL_THRESHOLD_DP)
        set(value) {
            field = value
            layoutManager?.maxOverScrollThreshold = value
        }

    /** 内部持有的 RecyclerView */
    val recyclerView: RecyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
    private var provider: ColumnProvider<out FrozenColumnData>? = null
    private var adapter: GenericStockAdapter<out FrozenColumnData>? = null
    private var layoutManager: FrozenColumnLayoutManager? = null

    /**
     * 设置 Adapter 并绑定数据
     * @param provider 列视图提供器
     */
    fun <T: FrozenColumnData> setProvider(provider: ColumnProvider<T>) {
        this@FrozenColumnList.provider = provider
        layoutManager?.frozenColumnCount = provider.getFrozenColumnCount()
        recyclerView.adapter = GenericStockAdapter(provider).also { adapter = it }
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
     * 重建 List， 通常在表头数量发生变化时使用
     * @param list 数据列表
     */
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("NotifyDataSetChanged")
    fun <T: FrozenColumnData> rebuildList(list: List<T>) {
        (adapter as? ListAdapter<T, *>)?.apply {
            // TODO ... 刷新数据
            // 清理缓存池并更新数据，强制 Adapter 重新走 onCreateViewHolder
            recyclerView.recycledViewPool.clear()
            notifyDataSetChanged()
        }
    }

    /**
     * 绑定表头视图 (同步滚动表头)
     * @param header 表头视图
     */
    fun attachHeader(header: FrozenColumnHeader?) {
        if (header == null) return
        layoutManager?.addHorizontalScrollListener {
            layoutManager?.syncColumns(header)
        }
        header.onHorizontalScrollListener = { event ->
            // 将 Header 的触摸事件转发给 RecyclerView 处理
            recyclerView.dispatchTouchEvent(event)
        }
    }


    /**
     * 添加 ItemDecoration
     */
    fun addItemDecoration(decoration: RecyclerView.ItemDecoration) {
        recyclerView.addItemDecoration(decoration)
    }

    /**
     * 添加水平滚动监听
     */
    fun addHorizontalScrollListener(listener: (Int) -> Unit) {
        layoutManager?.addHorizontalScrollListener(listener)
    }

    /**
     * 移除水平滚动监听
     */
    fun removeHorizontalScrollListener(listener: (Int) -> Unit) {
        layoutManager?.removeHorizontalScrollListener(listener)
    }

    /**
     * 设置越界回弹动画提供器
     * @param animatorProvider 越界回弹动画提供器
     */
    fun setSpringBackAnimatorProvider(
        animatorProvider: SpringBackAnimatorProvider?
    ) {
        layoutManager?.springBackAnimatorProvider = animatorProvider
    }

    /**
     * 获取布局管理器
     */
    fun getLayoutManager(): FrozenColumnLayoutManager? {
        return layoutManager ?: (recyclerView.layoutManager as? FrozenColumnLayoutManager)
    }

    /**
     * 获取适配器
     */
    fun getAdapter(): GenericStockAdapter<out FrozenColumnData>? {
        return adapter ?: (recyclerView.adapter as? GenericStockAdapter<out FrozenColumnData>)
    }

    /**
     * 设置 ItemAnimator
     */
    fun setItemAnimator(animator: RecyclerView.ItemAnimator?) {
        recyclerView.itemAnimator = animator
    }

    /**
     * 获取 ItemAnimator
     */
    fun getItemAnimator(): RecyclerView.ItemAnimator? {
        return recyclerView.itemAnimator
    }

    /**
     * 处理 ViewPager2 嵌套冲突
     */
    private fun setupTouchConflictResolution() {
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
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
        recyclerView.layoutManager = FrozenColumnLayoutManager(context).also { layoutManager = it }
        recyclerView.setHasFixedSize(true)
        recyclerView.overScrollMode = OVER_SCROLL_NEVER
        addView(recyclerView)
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
        layoutManager?.removeAllHorizontalScrollListener()
        super.onDetachedFromWindow()
    }

}