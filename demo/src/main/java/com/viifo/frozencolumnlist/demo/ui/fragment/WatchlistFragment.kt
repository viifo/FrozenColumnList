package com.viifo.frozencolumnlist.demo.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.decoration.BoundDividerDecoration
import com.viifo.frozencolumnlist.demo.R
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.databinding.FragementWatchlistBinding

/**
 * 自选列表Fragment
 */
class WatchlistFragment: Fragment() {

    private var mBinding: FragementWatchlistBinding? = null

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
        mBinding?.frozenColumnList?.addItemDecoration(
            BoundDividerDecoration(
                context = context,
                dividerColor = context?.getColor(R.color.divider_2) ?: Color.GRAY
            )
        )
        mBinding?.frozenColumnHeader?.onHeaderClickListener = { _, header ->
            Toast.makeText(requireContext(), "点击了${header?.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initData() {
        mBinding?.frozenColumnList?.submitList(mockStockData())
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
                name = context?.getString(R.string.stock_price)
            ),
            FrozenHeaderData(
                id = 3,
                name = context?.getString(R.string.stock_change)
            ),
            FrozenHeaderData(
                id = 4,
                name = context?.getString(R.string.stock_change_amount)
            ),
            FrozenHeaderData(
                id = 5,
                name = context?.getString(R.string.stock_close_price)
            ),
            FrozenHeaderData(
                id = 6,
                name = context?.getString(R.string.stock_volume)
            ),
            FrozenHeaderData(
                id = 7,
                name = context?.getString(R.string.stock_amplitude)
            ),
            FrozenHeaderData(
                id = 8,
                name = context?.getString(R.string.stock_turnover)
            ),
            FrozenHeaderData(
                id = 9,
                name = context?.getString(R.string.stock_market_cap)
            ),
            FrozenHeaderData(
                id = 10,
                name = context?.getString(R.string.stock_circulating_cap)
            ),
        )
    }

}