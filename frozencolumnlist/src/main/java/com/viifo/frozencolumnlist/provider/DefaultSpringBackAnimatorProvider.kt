package com.viifo.frozencolumnlist.provider

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd

/**
  * 默认的弹簧回退动画提供器
 */
class DefaultSpringBackAnimatorProvider : SpringBackAnimatorProvider {

    private var animator: ValueAnimator? = null

    override fun startSpringBack(
        startOffset: Int,
        targetOffset: Int,
        onUpdate: (Int) -> Unit,
        onCancelOrEnd: (Boolean) -> Unit
    ) {
        animator = ValueAnimator.ofInt(startOffset, targetOffset).apply {
            duration = 300L
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { onUpdate(it.animatedValue as Int) }
            doOnEnd { onCancelOrEnd(true) }
            doOnCancel { onCancelOrEnd(false) }
            start()
        }
    }

    override fun stop() {
        animator?.cancel()
        animator = null
    }

}