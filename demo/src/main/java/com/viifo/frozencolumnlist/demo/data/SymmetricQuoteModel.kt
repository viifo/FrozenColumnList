package com.viifo.frozencolumnlist.demo.data

import com.viifo.frozencolumnlist.data.FrozenColumnData

/** Watchlist3 的中心对称行情行数据。 */
data class SymmetricQuoteModel(
    override val id: String,
    val rowLabel: String,
    val values: List<String>
) : FrozenColumnData(id, values.size)
