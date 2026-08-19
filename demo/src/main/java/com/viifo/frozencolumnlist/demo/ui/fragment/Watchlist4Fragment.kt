package com.viifo.frozencolumnlist.demo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.viifo.frozencolumnlist.FrozenColumnConfig
import com.viifo.frozencolumnlist.FrozenColumnPosition
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.data.SortDirection
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.databinding.FragmentWatchlist2Binding
import com.viifo.frozencolumnlist.demo.ui.StockColumnProvider

/** 演示将末尾 n 列固定在列表右侧。 */
class Watchlist4Fragment : Fragment() {

    private var binding: FragmentWatchlist2Binding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWatchlist2Binding.inflate(inflater, container, false)
        .also { binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return
        val provider = StockColumnProvider(reverseColumns = true)
        var frozenColumnCount = 2
        var config = FrozenColumnConfig(
            frozenColumnCount = 2,
            frozenColumnPosition = FrozenColumnPosition.END,
            visibleColumnCount = 4
        )

        binding.tvTabWatchlist.text = "末尾列固定"
        binding.btnLoad.text = "固定 2 列"
        binding.refreshLayout.setEnableRefresh(false)
        binding.refreshLayout.setEnableLoadMore(false)
        binding.refreshLayout.setEnableNestedScroll(false)

        binding.frozenColumnList.setColumnConfig(config)
        binding.frozenColumnList.setProvider(provider)
        binding.frozenColumnHeader.setColumnConfig(config)
        binding.frozenColumnHeader.setProvider(provider)
        binding.frozenColumnHeader.setHeaderData(mockHeaderData())
        binding.frozenColumnList.attachHeader(binding.frozenColumnHeader)
        binding.btnLoad.setOnClickListener {
            frozenColumnCount = frozenColumnCount % 3 + 1
            config = config.copy(frozenColumnCount = frozenColumnCount)
            binding.frozenColumnList.setColumnConfig(config)
            binding.frozenColumnList.resetHorizontalOffsets()
            binding.btnLoad.text = "固定 $frozenColumnCount 列"
        }
        binding.frozenColumnList.setOnItemClickListener { _, position, _ ->
            val item = binding.frozenColumnList.getItem<StockModel>(position)
            Toast.makeText(requireContext(), "点击了 ${item?.name}", Toast.LENGTH_SHORT).show()
        }
        binding.frozenColumnHeader.onHeaderItemClickListener = { _, position ->
            Toast.makeText(
                requireContext(),
                "点击表头：${binding.frozenColumnHeader.headerData[position].name}",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.frozenColumnList.submitList(mockStockData())
    }

    private fun mockHeaderData(): List<FrozenHeaderData> = listOf(
        FrozenHeaderData(1, getString(R.string.stock_name)),
        FrozenHeaderData(2, getString(R.string.stock_price), SortDirection.None),
        FrozenHeaderData(3, getString(R.string.stock_change), SortDirection.None),
        FrozenHeaderData(4, getString(R.string.stock_change_amount), SortDirection.None),
        FrozenHeaderData(5, getString(R.string.stock_close_price), SortDirection.None),
        FrozenHeaderData(6, getString(R.string.stock_volume), SortDirection.None),
        FrozenHeaderData(7, getString(R.string.stock_amplitude)),
        FrozenHeaderData(8, getString(R.string.stock_turnover)),
        FrozenHeaderData(9, getString(R.string.stock_market_cap)),
        FrozenHeaderData(10, getString(R.string.stock_circulating_cap))
    ).asReversed()

    private fun mockStockData(): List<StockModel> = List(30) { index ->
        val up = index % 3 != 0
        val sign = if (up) "+" else "-"
        StockModel(
            code = String.format("%05d.HK", index),
            name = "股票 $index",
            price = "${380 + index}.20",
            changePercent = "$sign${1 + index % 8}.${index % 10}%",
            changeAmount = "$sign${index % 6}.${10 + index % 80}",
            preClose = "${360 + index}.50",
            volume = "${10 + index}亿",
            amplitude = "${2 + index % 5}.2%",
            turnover = "${index % 4}.45%",
            marketCap = "${1000 + index * 17}亿",
            circulatingCap = "${800 + index * 13}亿"
        )
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
