package com.viifo.frozencolumnlist.demo.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.SortDirection
import com.viifo.frozencolumnlist.decoration.BoundDividerDecoration
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.databinding.FragmentWatchlist2Binding
import com.viifo.frozencolumnlist.demo.ui.StockColumnProvider
import com.viifo.frozencolumnlist.demo.ui.StockItemAnimator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Watchlist2Fragment: Fragment() {

    private var mBinding: FragmentWatchlist2Binding? = null

    private var stockList: List<StockModel> = listOf()
    private var currentPage = 0
    private var watchlistPageSize = 5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentWatchlist2Binding.inflate(
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
        mBinding?.refreshLayout?.setEnableLoadMore(false)
        mBinding?.refreshLayout?.setEnableAutoLoadMore(false)
        mBinding?.refreshLayout?.setEnableNestedScroll(false)

        // 初始化 FrozenColumnList
        // mBinding?.frozenColumnList?.setupViewPager2TouchConflictResolution(true)
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
        // 设置 EmptyView 点击事件
        mBinding?.frozenColumnList?.addChildClickViewIds(R.id.empty_item_icon, R.id.empty_item_desc)
        mBinding?.frozenColumnList?.setOnEmptyViewChildClickListener { view ->
            Toast.makeText(context, "点击了 EmptyView，id = ${view.id}", Toast.LENGTH_SHORT).show()
        }
        // 设置 Footer View 点击事件
        mBinding?.frozenColumnList?.setOnFooterViewClickListener { view ->
            Toast.makeText(context, "点击了 Footer View", Toast.LENGTH_SHORT).show()
        }
        // 设置 Item 点击事件
        mBinding?.frozenColumnList?.setOnItemClickListener { view, position, itemViewType ->
            val item = mBinding?.frozenColumnList?.getItem<StockModel>(position)
            Toast.makeText(context, "点击了 item ${item?.name}", Toast.LENGTH_SHORT).show()
        }
        // 初始化 FrozenColumnHeader
        mBinding?.frozenColumnHeader?.setProvider(provider)
        mBinding?.frozenColumnHeader?.onHeaderItemClickListener = { _, index ->
            // 点击表头排序
            mBinding?.frozenColumnHeader?.headerData?.let { list ->
                val item = list.getOrNull(index)?.takeIf { it.sort != null } ?: return@let
                Toast.makeText(
                    requireContext(),
                    "点击了表头 ${item.name}，当前排序方向 ${item.sort}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        mBinding?.btnLoad?.setOnClickListener {
            if (stockList.isEmpty()) {
                // 更新列表
                stockList = mockStockData().toMutableList()
                mBinding?.frozenColumnList?.submitList(stockList)
                mBinding?.btnLoad?.text = "清空数据"
            } else {
                stockList = emptyList()
                mBinding?.frozenColumnList?.submitList(stockList)
                mBinding?.btnLoad?.text = "加载数据"
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
    }

    /**
     * 刷新数据
     */
    private fun refreshData() {
        lifecycleScope.launch {
            // 刷新数据时，禁用水平滚动
            mBinding?.frozenColumnList?.canScrollHorizontally = false
            delay(2000L)
            currentPage = 0

            stockList = mockStockData().toMutableList()
            mBinding?.frozenColumnList?.submitList(stockList)

            mBinding?.btnLoad?.text = "清空数据"
            mBinding?.refreshLayout?.finishRefresh()
            mBinding?.refreshLayout?.finishLoadMore()
            mBinding?.frozenColumnList?.canScrollHorizontally = true
        }
    }

    /**
     * 加载更多数据
     */
    private fun loadMoreData() {
        lifecycleScope.launch {
            // 加载更多数据时，禁用水平滚动
            mBinding?.frozenColumnList?.canScrollHorizontally = false
            delay(2000L)
            currentPage += 1

            val list = mockStockData(currentPage * watchlistPageSize).toMutableList()
            list.addAll(0, stockList)
            stockList = list
            mBinding?.frozenColumnList?.submitList(stockList)

            mBinding?.btnLoad?.text = "清空数据"
            mBinding?.refreshLayout?.finishRefresh()
            mBinding?.refreshLayout?.finishLoadMore()
            mBinding?.frozenColumnList?.canScrollHorizontally = true
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

}