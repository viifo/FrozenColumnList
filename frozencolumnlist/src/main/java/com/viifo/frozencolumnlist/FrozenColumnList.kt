package com.viifo.frozencolumnlist

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.provider.ColumnProvider
import kotlin.math.abs
import kotlin.math.max

/**
 * 一个支持冻结列的 RecyclerView 列表
 * 适用于需要固定左侧列，右侧内容可滚动的场景，如股票自选列表等
 */
class FrozenColumnList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    /** 表头点击事件 */
    var onHeaderClickListener: ((view: View, header: FrozenHeaderData?) -> Unit)? = null

    /** 冻结列数量 */
    private var mFrozenColumnCount: Int = 1

    /** 内部持有的表头容器 */
    private val mHeaderContainer by lazy { LinearLayoutCompat(context) }
    /** 表头数据 */
    val mHeaderData: MutableList<FrozenHeaderData> = mutableListOf()

    /** 内部持有的 RecyclerView */
    private val mRecyclerView: RecyclerView = RecyclerView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
    private var mProvider: ColumnProvider<out FrozenColumnData>? = null
    private var mAdapter: GenericStockAdapter<out FrozenColumnData>? = null
    private var mLayoutManager: FrozenColumnLayoutManager? = null

    /**
     * 处理 ViewPager2 嵌套冲突
     */
    private fun setupTouchConflictResolution() {
        mRecyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
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
     * 设置 Adapter 并绑定数据
     * @param provider 列视图提供器
     */
    fun <T: FrozenColumnData> setProvider(provider: ColumnProvider<T>) {
        mProvider = provider
        mRecyclerView.adapter = GenericStockAdapter(provider).also { mAdapter = it }
    }

    /**
     * 设置 Adapter 绑定的数据
     * @param list 数据列表
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: FrozenColumnData> submitList(list: List<T>) {
        (mAdapter as? ListAdapter<T, *>)?.submitList(list)
    }

    /**
     * 刷新 Adapter 绑定的数据 (包括表头)
     * @param list 数据列表
     */
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("NotifyDataSetChanged")
    fun <T: FrozenColumnData> refreshList(list: List<T>) {
        (mAdapter as? ListAdapter<T, *>)?.apply {
            // TODO ... 刷新数据
            notifyDataSetChanged()
        }
    }

    /**
     * 设置同步滑动的监听器（用于同步表头）
     */
    fun setOnHorizontalScrollListener(listener: (offset: Int) -> Unit) {
        mLayoutManager?.onHorizontalScroll = listener
    }

    /**
     * 设置表头数据
     */
    fun setHeaderData(headerData: List<FrozenHeaderData>) {
        if (mProvider == null) {
            throw IllegalArgumentException(
                "The Provider is not set. Please call the setProvider() method to set the Provider first."
            )
        }
        mHeaderData.clear()
        mHeaderData.addAll(headerData)

        // 更新 Adapter 固定列和可滚动列数据
        mAdapter?.mFrozenColumnCount = mFrozenColumnCount
        mAdapter?.mScrollableColumnCount = (headerData.size - mFrozenColumnCount).coerceAtLeast(0)

        // 初始化表头
        val frozenHeaders = mProvider?.createFrozenHeader(
            mHeaderContainer,
            headerData.take(mFrozenColumnCount)
        )
        val scrollHeaders = mProvider?.createScrollableHeader(
            mHeaderContainer,
            headerData.takeLast(headerData.size - mFrozenColumnCount)
        )
        val viewWidths = mProvider?.getColumnWidths(
            mHeaderContainer,
            headerData.size
        )
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
        mHeaderContainer.removeAllViews()
        frozenHeaders?.forEachIndexed { index, view ->
            view.setOnClickListener {
                onHeaderClickListener?.invoke(
                    it,
                    mHeaderData.getOrNull(index)
                )
            }
            mHeaderContainer.addView(
                view,
                viewWidths?.getOrNull(index) ?: LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        }
        val frozenHeaderSize = frozenHeaders?.size ?: 0
        scrollHeaders?.forEachIndexed { index, view ->
            view.setOnClickListener {
                onHeaderClickListener?.invoke(
                    it,
                    mHeaderData.getOrNull(frozenHeaderSize + index)
                )
            }
            mHeaderContainer.addView(
                view,
                viewWidths?.get(frozenHeaderSize + index) ?: LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        }
    }

    /**
     * 初始化 View，包括 RecyclerView 和 LayoutManager
     */
    private fun initView() {
        // 处理 ViewPager2 嵌套冲突
        // setupTouchConflictResolution()
        // 添加表头容器到布局
        addView(mHeaderContainer)
        // 初始化 LayoutManager 并绑定
        mLayoutManager = FrozenColumnLayoutManager(context, mFrozenColumnCount)
        mLayoutManager?.onHorizontalScroll = { _ ->
            // 同步滚动表头
            mLayoutManager?.syncColumns(mHeaderContainer)
        }
        mRecyclerView.layoutManager = mLayoutManager
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.overScrollMode = OVER_SCROLL_NEVER
        // 添加 RecyclerView 到布局
        addView(mRecyclerView)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.FrozenColumnList) {
            mFrozenColumnCount = getInt(R.styleable.FrozenColumnList_frozenColumnCount, 1)
        }
    }

    init {
        orientation = VERTICAL
        // 解析 XML 属性
        initAttrs(context, attrs)
        // 初始化 View
        initView()
    }

}