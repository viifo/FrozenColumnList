package com.viifo.frozencolumnlist.demo.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.SortDirection
import com.viifo.frozencolumnlist.decoration.BoundDividerDecoration
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.databinding.FragementWatchlistBinding
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
class WatchlistFragment: Fragment() {

    private var mBinding: FragementWatchlistBinding? = null
    private var stockList: MutableList<StockModel> = mutableListOf()
    private var pushJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragementWatchlistBinding.inflate(
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
        val provider = StockColumnProvider()
        mBinding?.frozenColumnList?.setProvider(provider)
        mBinding?.frozenColumnHeader?.setHeaderData(mockStockHeaderData(), provider)
        mBinding?.frozenColumnList?.attachHeader(mBinding?.frozenColumnHeader)
        mBinding?.frozenColumnList?.setItemAnimator(StockItemAnimator(requireContext()))
        mBinding?.frozenColumnList?.addItemDecoration(
            BoundDividerDecoration(
                context = context,
                dividerColor = context?.getColor(R.color.divider_2) ?: Color.GRAY
            )
        )
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

        mBinding?.btnPush?.setOnClickListener {
            if (pushJob?.isActive == true) { // 关闭推送
                stopMockPush()
                mBinding?.btnPush?.text = "开启推送"
            } else { // 开启推送
                startMockPush()
                mBinding?.btnPush?.text = "关闭推送"
            }
        }
    }

    private fun initData() {
        stockList.clear()
        stockList.addAll(mockStockData())
        mBinding?.frozenColumnList?.submitList(stockList)
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
        (mBinding?.frozenColumnList?.getItemAnimator() as? StockItemAnimator)?.isShowUpdateAnimator = false
        mBinding?.frozenColumnList?.submitList(list)
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
     * 启动模拟实时推送数据
     */
    fun startMockPush() {
        pushJob?.cancel()
        pushJob = lifecycleScope.launch {
            mockStockSocketFlow().collect { list ->
                (mBinding?.frozenColumnList?.getItemAnimator() as? StockItemAnimator)?.isShowUpdateAnimator = true
                mBinding?.frozenColumnList?.submitList(list)
            }
        }
    }

    /**
     * 停止模拟实时推送数据
     */
    fun stopMockPush() {
        pushJob?.cancel()
    }

    /**
     * 模拟实时推送的数据流
     */
    private fun mockStockSocketFlow() = flow {
        val layoutManager = mBinding?.frozenColumnList?.getLayoutManager()
        while (currentCoroutineContext().isActive) {
            delay(2000)
            // 只有在静止状态（IDLE）下才处理逻辑
            // 如果是在 FLING（惯性滚动）或 DRAGGING（拖拽中），直接跳过本次循环
            if (mBinding?.frozenColumnList?.recyclerView?.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
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
    private fun mockStockData(): List<StockModel> {
        return List(200) { i ->
            val isUp = (0..1).random() == 1
            val prefix = if (isUp) "+" else "-"
            StockModel(
                code = "00700.HK",
                name = "腾讯控股 $i",
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