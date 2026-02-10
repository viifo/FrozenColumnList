package com.viifo.frozencolumnlist.demo.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.demo.data.StockModel
import com.viifo.frozencolumnlist.demo.databinding.ItemStockBinding

class StockAdapter() : RecyclerView.Adapter<StockAdapter.ViewHolder>() {

    private var stocks: List<StockModel>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stock = stocks?.getOrNull(position) ?: return
        holder.binding.tvName.text = stock.name
        holder.binding.tvCode.text = stock.code
        holder.binding.tvPrice.text = stock.price
        holder.binding.tvChange.text = stock.changePercent
        holder.binding.tvChangeAmount.text = stock.changeAmount
        holder.binding.tvPrevClose.text = stock.preClose
        holder.binding.tvVolume.text = stock.volume
        holder.binding.tvTurnover.text = stock.turnover
        holder.binding.tvMarketCap.text = stock.marketCap
        holder.binding.tvCirculatingCap.text = stock.circulatingCap
        // 简单的涨跌颜色逻辑
        val color = if (stock.changePercent.contains("+")) Color.RED else Color.GREEN
        holder.binding.tvChange.setTextColor(color)
    }

    override fun getItemCount() = stocks?.size ?: 0

    @SuppressLint("NotifyDataSetChanged")
    fun setData(stocks: List<StockModel>) {
        this.stocks = stocks
        notifyDataSetChanged()
    }

    class ViewHolder(
        val binding: ItemStockBinding
    ) : RecyclerView.ViewHolder(binding.root)

}