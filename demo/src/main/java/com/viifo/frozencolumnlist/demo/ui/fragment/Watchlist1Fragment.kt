package com.viifo.frozencolumnlist.demo.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.SortDirection
import com.viifo.frozencolumnlist.decoration.BoundDividerDecoration
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.databinding.FragmentWatchlist1Binding
import com.viifo.frozencolumnlist.demo.ui.StockColumnProvider
import com.viifo.frozencolumnlist.demo.ui.StockItemAnimator
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

/**
 * 自选列表Fragment
 */
class Watchlist1Fragment: Fragment() {

    private var mBinding: FragmentWatchlist1Binding? = null
    private var stockList: List<StockModel> = listOf()
    private var isWatchlist = true
    private var currentPage = 0
    private var watchlistPageSize = 200
    private var positionsPageSize = 30
    private var pushJob: Job? = null
    private var animatorJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentWatchlist1Binding.inflate(
            inflater,
            container,
            false
        ).also { mBinding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {
        // 初始化刷新布局
        mBinding?.refreshLayout?.setRefreshHeader(ClassicsHeader(requireContext()))
        mBinding?.refreshLayout?.setRefreshFooter(ClassicsFooter(requireContext()))
        mBinding?.refreshLayout?.setOnRefreshListener { refreshData() }
        mBinding?.refreshLayout?.setOnLoadMoreListener { loadMoreData() }
        mBinding?.refreshLayout?.setEnableAutoLoadMore(false)
        mBinding?.refreshLayout?.setDisableContentWhenRefresh(true) // 禁用刷新时内容区域滚动
        mBinding?.refreshLayout?.setDisableContentWhenLoading(true) // 禁用加载时内容区域滚动
        // 解决refreshLayout 与 frozenColumnList 的滑动冲突
        mBinding?.refreshLayout?.setEnableNestedScroll(false)
//        // 或使用下面注释的代码， 解决refreshLayout 与 frozenColumnList 的滑动冲突
//        // 但是如果没有关闭嵌套滚动，会导致在 frozenColumnList 水平滚动时，仍然可以触发 refreshLayout 的刷新或加载更多
//        mBinding?.refreshLayout?.setScrollBoundaryDecider(object : ScrollBoundaryDecider {
//            override fun canRefresh(content: View?): Boolean {
//                // 无法再往下拉时，允许触发刷新
//                return mBinding?.frozenColumnList?.canScrollVertically(-1) == false
//            }
//            override fun canLoadMore(content: View?): Boolean {
//                // 无法再往上拉时，允许触发加载
//                return mBinding?.frozenColumnList?.canScrollVertically(1) == false
//            }
//        })

        // 初始化 FrozenColumnList
        // mBinding?.frozenColumnList?.setupTouchConflictResolution(true)
        val provider = StockColumnProvider()
        mBinding?.frozenColumnList?.setProvider(provider)
        mBinding?.frozenColumnList?.attachHeader(mBinding?.frozenColumnHeader)
        mBinding?.frozenColumnList?.setItemAnimator(StockItemAnimator(requireContext()))
        mBinding?.frozenColumnList?.addItemDecoration(
            BoundDividerDecoration(
                context = context,
                dividerColor = context?.getColor(R.color.divider_2) ?: Color.GRAY
            )
        )
        mBinding?.frozenColumnList?.setOnItemClickListener { view, position, itemViewType ->
            val item = mBinding?.frozenColumnList?.getItem<StockModel>(position)
            Toast.makeText(context, "点击了 item ${item?.name}", Toast.LENGTH_SHORT).show()
        }
        // 初始化 FrozenColumnHeader
        mBinding?.frozenColumnHeader?.setProvider(provider)
        mBinding?.frozenColumnHeader?.onHeaderClickListener = { _, index ->
            // 点击表头排序
            mBinding?.frozenColumnHeader?.headerData?.let { list ->
                val item = list.getOrNull(index)?.takeIf { it.sort != null } ?: return@let
                // 先恢复上一个 header 状态
                val prevItemIndex = list.indexOfFirst { it.sort != null && it.sort != SortDirection.None }
                if (prevItemIndex > -1) {
                    // 刷新上一个 header 状态
                    mBinding?.frozenColumnHeader?.refreshHeader(
                        prevItemIndex,
                        list[prevItemIndex].copy(sort = SortDirection.None)
                    )
                }
                // 刷新当前点击 header 状态
                val current = item.copy(
                    sort = switchSortDirection(item.sort ?: SortDirection.None)
                )
                mBinding?.frozenColumnHeader?.refreshHeader(index, current)
                sortStockData(current)
            }
        }

        mBinding?.tvTabWatchlist?.setOnClickListener { view ->
            isWatchlist = true
            mBinding?.tvTabWatchlist?.setTextColor(view.context.getColor(R.color.black))
            mBinding?.tvTabPositions?.setTextColor(view.context.getColor(R.color.gray))
            mBinding?.tvTabWatchlist?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            mBinding?.tvTabPositions?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            stopMockPush()
            initWatchList()
        }
        mBinding?.tvTabPositions?.setOnClickListener { view ->
            isWatchlist = false
            mBinding?.tvTabWatchlist?.setTextColor(view.context.getColor(R.color.gray))
            mBinding?.tvTabPositions?.setTextColor(view.context.getColor(R.color.black))
            mBinding?.tvTabWatchlist?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            mBinding?.tvTabPositions?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            stopMockPush()
            initPositionsList()
        }
        mBinding?.btnPush?.setOnClickListener {
            if (pushJob?.isActive == true) { // 关闭推送
                stopMockPush()
            } else { // 开启推送
                startMockPush()
            }
        }
    }

    private fun initData() {
        initWatchList()
    }

    private fun initWatchList() {
        currentPage = 0
        // 先将水平滚动偏移量设置为 0，切换列表后需要显示第一列
        mBinding?.frozenColumnList?.updateHorizontalOffset(0)
        // 更新表头
        mBinding?.frozenColumnHeader?.setHeaderData(mockStockHeaderData())
        // 更新列表
        stockList = mockStockData().toMutableList()
        mBinding?.frozenColumnList?.submitList(stockList)
    }

    private fun initPositionsList() {
        currentPage = 0
        // 先将水平滚动偏移量设置为 0，切换列表后需要显示第一列
        mBinding?.frozenColumnList?.updateHorizontalOffset(0)
        // 更新表头
        mBinding?.frozenColumnHeader?.setHeaderData(mockPositionHeaderData())
        // 更新列表
        stockList = mockPositionsData().toMutableList()
        mBinding?.frozenColumnList?.submitList(stockList)
    }

    /**
     * 刷新数据
     */
    private fun refreshData() {
        lifecycleScope.launch {
            // 刷新数据时，禁用水平滚动
            // mBinding?.frozenColumnList?.canScrollHorizontally = false
            delay(2000L)
            currentPage = 0
            if (isWatchlist) {
                stockList = mockStockData().toMutableList()
                mBinding?.frozenColumnList?.submitList(stockList)
            } else {
                stockList = mockPositionsData().toMutableList()
                mBinding?.frozenColumnList?.submitList(stockList)
            }
            mBinding?.refreshLayout?.finishRefresh()
            mBinding?.refreshLayout?.finishLoadMore()
            // mBinding?.frozenColumnList?.canScrollHorizontally = true
        }
    }

    /**
     * 加载更多数据
     */
    private fun loadMoreData() {
        lifecycleScope.launch {
            // 加载更多数据时，禁用水平滚动
            // mBinding?.frozenColumnList?.canScrollHorizontally = false
            delay(2000L)
            currentPage += 1
            if (isWatchlist) {
                val list = mockStockData(currentPage * watchlistPageSize).toMutableList()
                list.addAll(0, stockList)
                stockList = list
                mBinding?.frozenColumnList?.submitList(stockList)
            } else {
                val list = mockPositionsData(currentPage * positionsPageSize).toMutableList()
                list.addAll(0, stockList)
                stockList = list
                mBinding?.frozenColumnList?.submitList(stockList)
            }
            mBinding?.refreshLayout?.finishRefresh()
            mBinding?.refreshLayout?.finishLoadMore()
            // mBinding?.frozenColumnList?.canScrollHorizontally = true
        }
    }

    /**
     * 对股票数据进行排序
     */
    private fun sortStockData(header: FrozenHeaderData) {
        if (header.sort == null) return
        val selector: (StockModel) -> Float = {
            when (header.id) {
                2 -> it.price.toFloat()
                3 -> it.changePercent.drop(1).dropLast(1).toFloat()
                4 -> it.changeAmount.toFloat()
                5 -> it.preClose.toFloat()
                6 -> it.volume.dropLast(1).toFloat()
                else -> 0f
            }
        }

        val list = if (header.sort == SortDirection.Desc) {
            stockList.sortedByDescending(selector)
        } else {
            stockList.sortedBy(selector)
        }
        stockList = list.toMutableList()
        // 关闭动画，避免刷新时触发涨跌更新动画
        showUpdateAnimation(false)
        mBinding?.frozenColumnList?.submitList(list) {
            animatorJob?.cancel()
            animatorJob = viewLifecycleOwnerLiveData.value?.lifecycleScope?.launch {
                delay(800)
                // 刷新完成后，恢复之前的动画状态
                showUpdateAnimation(pushJob?.isActive == true)
            }
        }
    }

    /**
     * 切换三状态排序方向
     */
    private fun switchSortDirection(sourceDir: SortDirection): SortDirection {
        return when (sourceDir) {
            SortDirection.None -> SortDirection.Asc    // 默认 → 升序
            SortDirection.Asc -> SortDirection.Desc    // 升序 → 降序
            SortDirection.Desc -> SortDirection.None   // 降序 → 默认
        }
    }

    /**
     * 显示或隐藏股票列表项的更新动画
     */
    private fun showUpdateAnimation(show: Boolean) {
        (mBinding
            ?.frozenColumnList
            ?.itemAnimator as? StockItemAnimator)
            ?.isShowUpdateAnimation = show
    }

    /**
     * 启动模拟实时推送数据
     */
    fun startMockPush() {
        mBinding?.btnPush?.text = "关闭推送"
        pushJob?.cancel()
        showUpdateAnimation(true)
        pushJob = lifecycleScope.launch {
            mockStockSocketFlow().collect { list ->
                mBinding?.frozenColumnList?.submitList(list)
            }
        }
    }

    /**
     * 停止模拟实时推送数据
     */
    fun stopMockPush() {
        mBinding?.btnPush?.text = "开启推送"
        pushJob?.cancel()
        showUpdateAnimation(false)
    }

    /**
     * 模拟实时推送的数据流
     */
    private fun mockStockSocketFlow() = flow {
        val layoutManager = mBinding?.frozenColumnList?.getFrozenColumnLayoutManager()
        while (currentCoroutineContext().isActive) {
            delay(2000)
            // 只有在静止状态（IDLE）下才处理逻辑
            // 如果是在 FLING（惯性滚动）或 DRAGGING（拖拽中），直接跳过本次循环
            if (mBinding?.frozenColumnList?.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                continue
            }
            val list = stockList.toMutableList()
            if (list.isEmpty() || layoutManager == null) continue
            // 实时获取屏幕可见的索引范围
            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            val lastVisible = layoutManager.findLastVisibleItemPosition()
            if (firstVisible == RecyclerView.NO_POSITION
                || lastVisible == RecyclerView.NO_POSITION) continue

            // 随机选择一个股票进行更新
            val updateIndex = (firstVisible..lastVisible).random()
            if (updateIndex < 0 || updateIndex >= list.size) continue

            val basePrice = (300..500).random().toDouble()
            val changeAmt = (Random.nextDouble() * 10).let { if (Random.nextBoolean()) it else -it }
            val preClose = basePrice - changeAmt
            val changePct = (changeAmt / preClose) * 100

            // 更新指定索引的股票数据
            list[updateIndex] = list[updateIndex].copy(
                price = String.format(Locale.getDefault(), "%.2f", basePrice),
                changePercent = String.format(Locale.getDefault(),"%+.2f%%", changePct),
                changeAmount  = String.format(Locale.getDefault(),"%+.2f", changeAmt)
            )
            stockList = list
            emit(list)
        }
    }

    /**
     * 模拟股票数据
     */
    private fun mockStockData(startPosition: Int = 0): List<StockModel> {
        return List(watchlistPageSize) { i ->
            val isUp = (0..1).random() == 1
            val prefix = if (isUp) "+" else "-"
            StockModel(
                columnCount = 10,
                code = String.format("%05d.HK", i + startPosition),
                name = "自选股票 ${i + startPosition}",
                price = "${380 + i}.20",
                changePercent = if (i % 2 == 0) "+2.45%" else "-1.12%",
                changeAmount = "$prefix${(0..5).random()}.${(10..99).random()}",
                preClose = "199.50",
                volume = "${10 + i}亿",
                amplitude = "3.2%",
                turnover = "0.45%",
                marketCap = "${(100..5000).random()}亿",
                circulatingCap = "${(50..4000).random()}亿",
            )
        }
    }

    private fun mockStockHeaderData(): List<FrozenHeaderData> {
        return listOf(
            FrozenHeaderData(
                id = 1,
                name = context?.getString(R.string.stock_name)
            ),
            FrozenHeaderData(
                id = 2,
                name = context?.getString(R.string.stock_price),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 3,
                name = context?.getString(R.string.stock_change),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 4,
                name = context?.getString(R.string.stock_change_amount),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 5,
                name = context?.getString(R.string.stock_close_price),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 6,
                name = context?.getString(R.string.stock_volume),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 7,
                name = context?.getString(R.string.stock_amplitude),
                sort = null, // 此字段不排序
            ),
            FrozenHeaderData(
                id = 8,
                name = context?.getString(R.string.stock_turnover),
                sort = null, // 此字段不排序
            ),
            FrozenHeaderData(
                id = 9,
                name = context?.getString(R.string.stock_market_cap),
                sort = null, // 此字段不排序
            ),
            FrozenHeaderData(
                id = 10,
                name = context?.getString(R.string.stock_circulating_cap),
                sort = null, // 此字段不排序
            ),
        )
    }

    /**
     * 模拟持仓数据
     */
    private fun mockPositionsData(startPosition: Int = 0): List<StockModel> {
        return List(positionsPageSize) { i ->
            val isUp = (0..1).random() == 1
            val prefix = if (isUp) "+" else "-"
            StockModel(
                columnCount = 6,
                code = String.format("%05d.HK", i + startPosition),
                name = "持仓股票 ${i + startPosition}",
                price = "${380 + i}.20",
                changePercent = if (i % 2 == 0) "+2.45%" else "-1.12%",
                changeAmount = "$prefix${(0..5).random()}.${(10..99).random()}",
                preClose = "199.50",
                volume = "${10 + i}亿",
                amplitude = "3.2%",
                turnover = "0.45%",
                marketCap = "${(100..5000).random()}亿",
                circulatingCap = "${(50..4000).random()}亿",
            )
        }
    }

    /**
     * 模拟持仓表头数据(与自选表头数量不一致)
     */
    private fun mockPositionHeaderData(): List<FrozenHeaderData> {
        return listOf(
            FrozenHeaderData(
                id = 1,
                name = context?.getString(R.string.stock_name)
            ),
            FrozenHeaderData(
                id = 2,
                name = context?.getString(R.string.stock_price),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 3,
                name = context?.getString(R.string.stock_change),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 4,
                name = context?.getString(R.string.stock_change_amount),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 5,
                name = context?.getString(R.string.stock_close_price),
                sort = SortDirection.None
            ),
            FrozenHeaderData(
                id = 6,
                name = context?.getString(R.string.stock_volume),
                sort = SortDirection.None
            ),
        )
    }

}