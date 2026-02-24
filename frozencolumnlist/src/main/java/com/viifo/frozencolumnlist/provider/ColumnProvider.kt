package com.viifo.frozencolumnlist.provider

import android.view.View
import android.view.ViewGroup
import com.viifo.frozencolumnlist.data.FrozenColumnData
import com.viifo.frozencolumnlist.data.FrozenHeaderData
import com.viifo.frozencolumnlist.layout.GenericStockAdapter
import com.viifo.frozencolumnlist.layout.GenericStockAdapter.GenericViewHolder

/**
 * 列视图提供器接口
 * 为了性能考虑，应该使用代码创建 View 而不是 XML 布局,
 * 获取 item view 列表的好处在于可以自定义每一个 item 的布局
 *
 * @param T 列数据模型类型
 */
interface ColumnProvider<T : FrozenColumnData> {

    /**
     * 获取 Adapter, 默认使用 [GenericStockAdapter]
     * @return Adapter
     */
    fun getAdapter(): GenericStockAdapter<T> = GenericStockAdapter(this)

    /**
     * 获取冻结(固定)列数量
     * @return 冻结(固定)列数量
     */
     fun getFrozenColumnCount(): Int

    /**
     * 获取各列宽度（像素）, 如果返回空列表, 则使用默认宽度
     * @param parent 固定列 View 容器
     * @param size 数据项数量
     * @return 各列宽度列表
     */
     fun getColumnWidths(parent: ViewGroup, size: Int): List<Int>

    /**
     * 创建固定列的表头子 View 列表
     * @param parent 表头容器
     * @param size 数量
     * @param onClick 点击回调, 为空时表示不处理点击事件
     * @return 固定列的表头子 View 列表
     */
    fun createFrozenHeader(
        parent: ViewGroup,
        size: Int,
        onClick: ((View, Int) -> Unit)?
    ): List<View>

    /**
     * 创建可滚动列表头子 View 列表
     * @param parent 表头容器
     * @param size 数量
     * onClick: ((View, Int) -> Unit)?
     * @return 可滚动列表头子 View 列表
     */
    fun createScrollableHeader(
        parent: ViewGroup,
        size: Int,
        onClick: ((View, Int) -> Unit)?
    ): List<View>

    /**
     * 绑定固定列表头数据到对应的表头 View
     * @param view 固定列的表头 View 列表
     * @param data 固定列表头对应的数据列表
     */
    fun bindFrozenHeaderView(view: View, data: FrozenHeaderData?)

    /**
     * 绑定可滚动列表头数据到对应的表头 View
     * @param view 可滚动列的表头 View 列表
     * @param data 可滚动列表头对应的数据列表
     */
    fun bindScrollableHeaderView(view: View, data: FrozenHeaderData?)

    /**
     * 创建每行的 View 容器
     * @param parent 外部行容器父 ViewGroup
     * @param viewType View 类型
     * @return 每行的 View 容器
     */
    fun createRowContainer(parent: ViewGroup, viewType: Int): ViewGroup

    /**
     * 创建固定列的子 View 列表
     * @param parent 固定列子 View 容器
     * @param viewType View 类型
     * @param size 数量 (固定项数量配置)
     * @return 固定列的子 View 列表
     */
    fun createRowFrozenViews(parent: ViewGroup, viewType: Int, size: Int): List<View>

    /**
     * 创建可滚动列的子 View 列表
     * @param parent 可滚动列子 View 容器
     * @param viewType View 类型
     * @param size 数量 (表头总数量 - 固定项数量配置得出)
     * @return 可滚动列的子 View 列表
     */
    fun createRowScrollableViews(parent: ViewGroup, viewType: Int, size: Int): List<View>

    /**
     * 绑定固定列数据到对应的 View
     * @param holder ViewHolder
     * @param data 固定列对应的数据
     * @param payloads 局部刷新数据, 为空时表示全量刷新
     */
    fun bindRowFrozenViews(
        holder: GenericViewHolder<T>,
        data: T,
        payloads: List<Any?>
    )

    /**
     * 绑定可滚动列数据到对应的 View
     * @param holder ViewHolder
     * @param data 可滚动列对应的数据
     * @param payloads 局部刷新数据, 为空时表示全量刷新
     */
    fun bindRowScrollableViews(
        holder: GenericViewHolder<T>,
        data: T,
        payloads: List<Any?>
    )

}