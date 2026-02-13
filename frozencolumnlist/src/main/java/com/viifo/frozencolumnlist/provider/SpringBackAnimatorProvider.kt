package com.viifo.frozencolumnlist.provider

/**
 * 越界回弹动画提供器
 */
interface SpringBackAnimatorProvider {

    /**
     * 启动回弹动画
     * @param startOffset 当前的偏移量（通常是负数或大于 maxScrollWidth）
     * @param targetOffset 目标偏移量（通常是 0 或 maxScrollWidth）
     * @param onUpdate 每一帧的回调，必须将计算出的新 offset 传回
     * @param onCancelOrEnd 动画结束或取消回调，参数 isEnd 表示是否是正常结束
     */
    fun startSpringBack(
        startOffset: Int,
        targetOffset: Int,
        onUpdate: (Int) -> Unit,
        onCancelOrEnd: (isEnd: Boolean) -> Unit,
    )

    /**
     * 强制停止当前正在运行的动画
     */
    fun stop()
}