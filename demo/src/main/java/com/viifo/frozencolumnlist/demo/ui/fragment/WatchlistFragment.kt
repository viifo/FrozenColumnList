package com.viifo.frozencolumnlist.demo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.databinding.FragementWatchlistBinding
import com.viifo.frozencolumnlist.demo.ui.adapter.StockAdapter

class WatchlistFragment: Fragment() {

    private var mBinding: FragementWatchlistBinding? = null
    private var mAdapter: StockAdapter? = null

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
        mAdapter = StockAdapter()
        mBinding?.frozenColumnList?.setAdapter(mAdapter)
    }

    private fun initData() {
        mAdapter?.setData(mockStockData())
    }

    /**
     * 模拟股票数据
     */
    private fun mockStockData(): List<StockModel> {
        return List(50) { i ->
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

}