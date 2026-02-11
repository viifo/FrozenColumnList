package com.viifo.frozencolumnlist.demo.data

import com.viifo.frozencolumnlist.data.FrozenColumnData

data class StockModel(
    val code: String,       // 股票代码 (固定列使用)
    val name: String,       // 股票名称 (固定列使用)
    val price: String,      // 最新价
    val changePercent: String, // 涨跌幅
    val changeAmount: String,  // 涨跌额
    val preClose: String,      // 昨收价
    val volume: String,     // 成交额
    val amplitude: String,  // 振幅
    val turnover: String,    // 换手率
    val marketCap: String,      // 总市值
    val circulatingCap: String, // 流通市值
) : FrozenColumnData(code)