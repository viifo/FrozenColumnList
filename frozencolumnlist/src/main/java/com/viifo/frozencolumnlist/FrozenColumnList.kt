package com.viifo.frozencolumnlist

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import androidx.core.content.withStyledAttributes

/**
 * 一个支持冻结列的 RecyclerView 列表
 * 适用于需要固定左侧列，右侧内容可滚动的场景，如股票自选列表等
 */
class FrozenColumnList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    /** 内部持有的 RecyclerView */
    val recyclerView: RecyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    private var layoutManager: FrozenColumnLayoutManager? = null
    private var frozenColumnCount: Int = 1

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
     * 设置 RecyclerView 的 Adapter
     */
    fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        recyclerView.adapter = adapter
    }

    /**
     * 设置同步滑动的监听器（用于同步表头）
     */
    fun setOnHorizontalScrollListener(listener: (offset: Int) -> Unit) {
        layoutManager?.onHorizontalScroll = listener
    }

    /**
     * 初始化 View，包括 RecyclerView 和 LayoutManager
     */
    private fun initView() {
        // 初始化 LayoutManager 并绑定
        layoutManager = FrozenColumnLayoutManager(context, frozenColumnCount)
        recyclerView.layoutManager = layoutManager
        // 处理 ViewPager2 嵌套冲突
        setupTouchConflictResolution()
        // 添加 RecyclerView 到布局
        addView(recyclerView)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.FrozenColumnList) {
            frozenColumnCount = getInt(R.styleable.FrozenColumnList_frozenColumnCount, 1)
        }
    }

    init {
        // 解析 XML 属性
        initAttrs(context, attrs)
        // 初始化 View
        initView()
    }

}