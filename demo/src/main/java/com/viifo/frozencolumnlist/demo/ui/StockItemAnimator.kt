package com.viifo.frozencolumnlist.demo.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.viifo.frozencolumnlist.demo.R

/**
 * 股票列表项动画器
 * 用于处理股票列表项的更新动画，并根据股票的涨跌状态添加闪烁效果
 */
class StockItemAnimator(context: Context) : DefaultItemAnimator() {

    var isShowUpdateAnimation = false

    private val backgrounds = listOf(
        context.getDrawable(R.drawable.bg_gray_equal),
        context.getDrawable(R.drawable.bg_red_up),
        context.getDrawable(R.drawable.bg_green_down)
    )

    override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean = true

    /**
     * 处理股票列表项的更新动画
     */
    override fun animateChange(
        oldHolder: RecyclerView.ViewHolder,
        newHolder: RecyclerView.ViewHolder,
        preInfo: ItemHolderInfo,
        postInfo: ItemHolderInfo
    ): Boolean {
        if (isShowUpdateAnimation && oldHolder === newHolder) {
            val itemView = newHolder.itemView as? ViewGroup
                ?: return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
            val changePercent = itemView
                .findViewById<AppCompatTextView>(R.id.item_tv_change)
                ?.text?.toString()
                .orEmpty()

            // 根据涨跌状态设置背景色
            itemView.background = when {
                changePercent.contains("+") -> backgrounds.getOrNull(1)
                changePercent.contains("-") -> backgrounds.getOrNull(2)
                else -> backgrounds.getOrNull(0)
            }
            // 隐藏所有子视图
            itemView.children.forEach { it.background.alpha = 0 }
            // 设置动画
            ValueAnimator.ofInt(0, 255).apply {
                duration = 800L
                addUpdateListener { animator ->
                    val alpha = animator.animatedValue as Int
                    itemView.children.forEach { it.background.alpha = alpha }
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        itemView.children.forEach { it.background.alpha = 255 }
                        itemView.setBackgroundColor(Color.TRANSPARENT)
                        dispatchChangeFinished(newHolder, false)
                    }
                })
                start()
            }
            return false
        }
        return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
    }

    override fun animateMove(
        holder: RecyclerView.ViewHolder?,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ): Boolean {
        // 不处理移动动画
        return false
    }

}