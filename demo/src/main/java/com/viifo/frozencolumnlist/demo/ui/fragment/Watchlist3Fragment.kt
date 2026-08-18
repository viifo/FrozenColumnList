package com.viifo.frozencolumnlist.demo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.viifo.frozencolumnlist.FrozenColumnConfig
import com.viifo.frozencolumnlist.FrozenColumnPosition
import com.viifo.frozencolumnlist.FrozenColumnScrollMode
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.demo.data.SymmetricQuoteModel
import com.viifo.frozencolumnlist.demo.databinding.FragmentWatchlist3Binding
import com.viifo.frozencolumnlist.demo.ui.MiddleStockColumnProvider
import com.viifo.frozencolumnlist.demo.ui.SideBackgroundBoundaryConfig
import com.viifo.frozencolumnlist.demo.ext.dp2px
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Watchlist3Fragment: Fragment() {

    private var mBinding: FragmentWatchlist3Binding? = null
    private var stockList: List<SymmetricQuoteModel> = emptyList()
    private var nextStartPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentWatchlist3Binding.inflate(
            inflater,
            container,
            false
        ).also { mBinding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        refreshImmediately()
    }

    private fun initView() {
        val binding = mBinding ?: return
        val provider = MiddleStockColumnProvider(
            backgroundBoundary = SideBackgroundBoundaryConfig(
                leftGrayFromId = 12,
                rightGrayThroughId = 17
            )
        )
        var columnConfig = FrozenColumnConfig(
            frozenColumnCount = 1,
            frozenColumnPosition = FrozenColumnPosition.MIDDLE,
            middleColumnStart = 7,
            visibleColumnCount = 5,
            scrollMode = FrozenColumnScrollMode.INDEPENDENT,
            horizontalScrollThreshold = requireContext().dp2px(24)
        )

        binding.middleFrozenHeader.setColumnConfig(columnConfig)
        binding.middleFrozenHeader.setProvider(provider)
        binding.middleFrozenHeader.setHeaderData(mockHeaderData())

        binding.middleFrozenList.setColumnConfig(columnConfig)
        binding.middleFrozenList.setProvider(provider)
        binding.middleFrozenList.attachHeader(binding.middleFrozenHeader)
        binding.middleFrozenList.setOnSideClickListener { _, position, side ->
            val item = binding.middleFrozenList.getItem<SymmetricQuoteModel>(position)
            Toast.makeText(
                requireContext(),
                "${item?.rowLabel}：${side.name} 单击",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.middleFrozenList.setOnSideDoubleClickListener { _, position, side ->
            val selected = binding.middleFrozenList.toggleSideSelected(position, side)
            val item = binding.middleFrozenList.getItem<SymmetricQuoteModel>(position)
            Toast.makeText(
                requireContext(),
                "${item?.rowLabel}：${side.name} 双击${if (selected) "选中" else "取消选中"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnScrollMode.setOnClickListener {
            val synchronized = columnConfig.scrollMode == FrozenColumnScrollMode.INDEPENDENT
            columnConfig = columnConfig.copy(scrollMode = if (synchronized) {
                FrozenColumnScrollMode.SYNCHRONIZED
            } else {
                FrozenColumnScrollMode.INDEPENDENT
            })
            binding.middleFrozenList.setColumnConfig(columnConfig)
            binding.btnScrollMode.text = if (synchronized) "同步滚动" else "独立滚动"
            binding.middleFrozenList.resetHorizontalOffsets()
        }

        binding.middleFrozenHeader.onHeaderItemClickListener = { _, position ->
            Toast.makeText(
                requireContext(),
                "点击表头：${binding.middleFrozenHeader.headerData[position].name}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // SmartRefreshLayout 仍然直接持有 RecyclerView；关闭嵌套滚动可避免横向手势误触发刷新。
        binding.refreshLayout.setRefreshHeader(ClassicsHeader(requireContext()))
        binding.refreshLayout.setRefreshFooter(ClassicsFooter(requireContext()))
        binding.refreshLayout.setEnableNestedScroll(false)
        binding.refreshLayout.setEnableAutoLoadMore(false)
        binding.refreshLayout.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(600)
                refreshImmediately()
                binding.refreshLayout.finishRefresh()
            }
        }
        binding.refreshLayout.setOnLoadMoreListener {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(600)
                val more = mockStockData(nextStartPosition, 15)
                nextStartPosition += more.size
                stockList = stockList + more
                binding.middleFrozenList.submitList(stockList)
                binding.refreshLayout.finishLoadMore()
            }
        }
    }

    private fun refreshImmediately() {
        nextStartPosition = 0
        stockList = mockStockData(nextStartPosition, 30)
        nextStartPosition += stockList.size
        mBinding?.middleFrozenList?.apply {
            resetHorizontalOffsets()
            clearSideSelection()
            submitList(stockList)
        }
    }

    private fun mockHeaderData(): List<FrozenHeaderData> {
        return listOf(
            FrozenHeaderData(1, "成交额"),
            FrozenHeaderData(2, "成交量"),
            FrozenHeaderData(3, "涨跌额"),
            FrozenHeaderData(4, "涨跌幅"),
            FrozenHeaderData(5, "最新价"),
            FrozenHeaderData(6, "卖盘"),
            FrozenHeaderData(7, "买盘"),
            FrozenHeaderData(8, "行权价"),
            FrozenHeaderData(9, "买盘"),
            FrozenHeaderData(10, "卖盘"),
            FrozenHeaderData(11, "最新价"),
            FrozenHeaderData(12, "涨跌幅"),
            FrozenHeaderData(13, "涨跌额"),
            FrozenHeaderData(14, "成交量"),
            FrozenHeaderData(15, "成交额")
        )
    }

    private fun mockStockData(startPosition: Int, size: Int): List<SymmetricQuoteModel> {
        return List(size) { offset ->
            val index = startPosition + offset
            val up = index % 3 != 0
            val sign = if (up) "+" else "-"
            val amount = "${10 + index % 90}.${index % 10}亿"
            val volume = "${100 + index % 900}万"
            val changeAmount = "$sign${index % 6}.${10 + index % 80}"
            val changePercent = "$sign${1 + index % 8}.${index % 10}%"
            val latestPrice = "${100 + index % 300}.${index % 100}"
            val sellPrice = "${101 + index % 300}.${20 + index % 70}"
            val buyPrice = "${99 + index % 300}.${10 + index % 80}"
            val strikePrice = "${100 + (index % 20) * 5}.00"
            SymmetricQuoteModel(
                id = index.toString(),
                rowLabel = "期权 $index",
                values = listOf(
                    amount,
                    volume,
                    changeAmount,
                    changePercent,
                    latestPrice,
                    sellPrice,
                    buyPrice,
                    strikePrice,
                    buyPrice,
                    sellPrice,
                    latestPrice,
                    changePercent,
                    changeAmount,
                    volume,
                    amount
                )
            )
        }
    }

    override fun onDestroyView() {
        mBinding = null
        super.onDestroyView()
    }
}
