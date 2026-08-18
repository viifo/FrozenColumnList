# FrozenColumnList

[![](https://jitpack.io/v/viifo/FrozenColumnList.svg)](https://jitpack.io/#viifo/FrozenColumnList)

[中文](README.md) | [English](README_EN.md)

面向行情、自选股等多列数据场景的高性能列表。统一的 `FrozenColumnList` 支持：

- 前 n 列固定，其余列横向滚动；
- 中间 n 列固定，左右两侧独立滚动或镜像同步滚动；
- 左右区域分别设置普通背景、选中背景；
- 固定列直接通过 XML 或代码设置背景、圆角、字体、elevation、阴影等样式；
- 与 `SmartRefreshLayout`、EmptyView、FooterView、DiffUtil 和 ViewPager2 嵌套场景配合使用。

```kotlin
implementation("com.github.viifo:FrozenColumnList:2.0.0")
```

## 2.0 升级提示

> 2.0 是不兼容旧版 Provider API 的大版本升级。

旧版逐列创建/绑定方法已删除，包括 `createFrozenHeader`、`createScrollableHeader`、
`createItemRowFrozenViews`、`createItemRowScrollableViews` 及其逐列 bind 方法。
`MiddleFrozenColumnList`、`MiddleFrozenColumnHeader` 和 `MiddleFrozenColumnProvider`
也已合并到统一实现。

2.0 改为一次创建或 inflate 完整行：

| 旧版 | 2.0 |
| --- | --- |
| 每列分别创建或 inflate，15 列会重复解析 15 次布局 | 每个 ViewHolder 只 inflate 一次完整行 XML |
| 固定列数量由 Provider 决定 | 由 `FrozenColumnConfig` 统一配置 |
| 中间固定使用独立列表组件 | 统一使用 `FrozenColumnList` / `FrozenColumnHeader` |
| 框架分别回调每一列 bind | 自定义整行 ViewHolder 集中缓存和绑定 |

升级后需要重写 `ColumnProvider`。XML 行根的直接子 View 必须与数据列按顺序一一对应。
列宽默认应直接在完整行 XML 或 `createItemRowView()` 中设置。中间固定模式如果配置了
`visibleColumnCount`，固定列继续采用 XML 宽度，普通列则由组件等宽计算。

## 固定列配置

```kotlin
val config = FrozenColumnConfig(
    frozenColumnCount = 1,
    frozenColumnPosition = FrozenColumnPosition.MIDDLE,
    middleColumnStart = 7, // null 时自动居中
    visibleColumnCount = 5, // 中间固定列 + 左右各完整显示 2 列
    scrollMode = FrozenColumnScrollMode.INDEPENDENT,
    horizontalScrollThreshold = resources.getDimensionPixelSize(R.dimen.horizontal_threshold)
)

stockList.setColumnConfig(config)
stockList.setProvider(provider)

stockHeader.setProvider(provider)
stockList.attachHeader(stockHeader) // 同时把 config 传给表头
stockHeader.setHeaderData(headers)
```

`FrozenColumnPosition`：

- `START`：固定前 n 列，已实现；
- `MIDDLE`：固定从 `middleColumnStart` 开始的 n 列，已实现；
- `END`：API 已预留，当前调用会明确抛出未实现异常。

中间固定模式下，`SYNCHRONIZED` 是视觉镜像同步：拖动左侧时，右侧向相反方向移动相同
进度，使两侧同时靠近或远离中间固定列。

`visibleColumnCount` 必须大于固定列数量；在 MIDDLE 模式下，减去固定列数量后必须是偶数。设置为 5、
固定 1 列时，组件保留固定列 XML 宽度，并将剩余可用宽度四等分，因此不同屏幕宽度下都能
完整显示“左 2 + 固定 1 + 右 2”。START 模式设置为 4、固定 1 列时，则完整显示
“固定 1 + 普通 3”。未设置时，所有列继续使用 XML 中声明的宽度。

## 完整行 XML

中间固定模式的完整行根必须使用 `MiddleFrozenRowLayout`。每个单元格仍然可以独立设置样式：

```xml
<com.viifo.frozencolumnlist.layout.MiddleFrozenRowLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <TextView
        android:id="@+id/left_amount"
        android:layout_width="86dp"
        android:layout_height="48dp" />

    <TextView
        android:id="@+id/strike_price"
        android:layout_width="96dp"
        android:layout_height="48dp"
        android:background="@color/white"
        android:elevation="8dp"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/right_amount"
        android:layout_width="86dp"
        android:layout_height="48dp" />
</com.viifo.frozencolumnlist.layout.MiddleFrozenRowLayout>
```

`START` 模式可使用任意 `ViewGroup` 作为行根，同样要求所有列是直接子 View。

## Provider 与整行 ViewHolder

简单场景可以继承 `DefaultColumnProvider`：

```kotlin
class QuoteProvider : DefaultColumnProvider<Quote>() {

    override fun createHeaderRowView(parent: ViewGroup, columnCount: Int): ViewGroup {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.header_quote_row, parent, false) as ViewGroup
    }

    override fun createItemRowView(
        parent: ViewGroup,
        viewType: Int,
        columnCount: Int
    ): ViewGroup {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quote_row, parent, false) as ViewGroup
    }

    override fun bindHeaderRow(
        holder: FrozenHeaderViewHolder,
        data: List<FrozenHeaderData>
    ) {
        data.forEachIndexed { index, header ->
            holder.getColumnView<TextView>(index).text = header.name
        }
    }

    override fun bindItemRow(
        holder: FrozenColumnViewHolder<Quote>,
        data: Quote,
        payloads: List<Any?>
    ) {
        holder.getView<TextView>(R.id.strike_price).text = data.strikePrice
        holder.getColumnView<TextView>(0).text = data.values[0]
    }
}
```

复杂场景可直接实现 `ColumnProvider`，在 `createItemViewHolder()` 中返回自己的
`FrozenColumnViewHolder`。这样可以在构造时只执行一次 `findViewById`：

```kotlin
private class QuoteHolder(
    override val rowView: ViewGroup
) : FrozenColumnViewHolder<Quote> {
    private val strikePrice = rowView.findViewById<TextView>(R.id.strike_price)

    override fun bind(data: Quote, payloads: List<Any?>) {
        strikePrice.text = data.strikePrice
    }
}
```

表头对应接口为 `FrozenHeaderViewHolder`。完整的 15 列 XML 示例见
`demo/src/main/res/layout/item_symmetric_quote_row.xml` 和
`demo/src/main/res/layout/header_symmetric_quote_row.xml`。

## 左右区域背景与选中

```kotlin
override fun getSideBackgroundColor(
    context: Context,
    data: Quote,
    side: FrozenColumnSide,
    selected: Boolean
): Int {
    if (selected) {
        return if (side == FrozenColumnSide.LEFT) leftSelected else rightSelected
    }
    return if (side == FrozenColumnSide.LEFT) leftNormal else rightNormal
}

stockList.setOnSideClickListener { _, position, side ->
    // 单击回调仍然保留；同时设置双击监听时，会在双击判定结束后触发
}

stockList.setOnSideDoubleClickListener { _, position, side ->
    stockList.toggleSideSelected(position, side)
}
```

左右状态按数据 `id` 保存，不依赖当前 ViewHolder 位置，回收复用后会重新绑定正确背景。

## SmartRefreshLayout

```xml
<com.scwang.smart.refresh.layout.SmartRefreshLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.viifo.frozencolumnlist.FrozenColumnList
        android:id="@+id/stock_list"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:fclHorizontalScrollThreshold="24dp" />
</com.scwang.smart.refresh.layout.SmartRefreshLayout>
```

中间固定模式会在手势超过阈值后才锁定水平方向，降低短促快速垂直滑动被误判为横向滑动的
概率。Demo 同时关闭了 `SmartRefreshLayout` 的 nested scroll，以避免刷新容器争抢横向手势。

## 已完成的性能与稳定性优化

- 每个表头或 ViewHolder 只创建/inflate 一次完整行，避免按列重复解析 XML；
- 使用 `ListAdapter + DiffUtil`，只更新发生变化的数据；
- 可通过自定义 ViewHolder 缓存子 View，避免 bind 阶段重复查找；
- 水平偏移由 LayoutManager 保存，新出现或复用的行会立即应用当前偏移；
- 同步时校验实际 `translationX` 和 `clipBounds`，不只依赖行内 offset 标记；
- 横向滚动裁剪复用 `Rect`，不在逐帧逐列路径创建临时对象；
- 左右选中背景使用 payload 局部刷新；
- 水平/垂直方向锁定带可配置阈值，减少快速垂直滑动误触；
- 配置结构发生变化时清空不兼容的回收池，避免复用错误行根；
- Provider 返回的列数、行根类型和 ViewHolder 归属会尽早校验，错误配置直接给出异常。

## 后续优化评估

| 优化项 | 收益 | 风险/成本 | 建议 |
| --- | --- | --- | --- |
| 自定义 ViewHolder 缓存全部列引用 | 中高，减少 bind 查找 | 低 | 业务 Provider 优先采用 |
| 为大列表提供稳定 ID | 中，降低更新闪烁 | ID 冲突会造成错误复用 | 仅在业务 ID 全局唯一时启用 |
| 合并高频行情更新并使用 payload | 高，降低主线程 bind 次数 | 需业务侧聚合更新 | 高频数据场景优先 |
| 关闭不必要的 change animation | 中，避免动画改写位移并减少过绘 | 会失去默认更新动画 | 大数据或高频刷新推荐 |
| 缓存每种 viewType 的列宽/滚动范围 | 中，减少 layout 计算 | 动态宽度时需可靠失效 | 可作为下一阶段 |
| 使用基准测试和 Macrobenchmark | 不直接提速，但可防回退 | 需要测试设备和基准模块 | 发布 2.0 前建议 |
| 支持末尾 n 列固定 | 功能扩展 | 手势、裁剪和回弹边界需新增测试 | 后续独立实现 |

## 常用 API

- `setColumnConfig(config)`
- `setProvider(provider)`
- `attachHeader(header)`
- `submitList(list)`
- `resetHorizontalOffsets()`
- `updateHorizontalOffset(offset)` / `updateHorizontalOffsets(left, right)`
- `setOnItemClickListener` / `setOnItemChildClickListener`
- `setOnSideClickListener` / `setOnSideDoubleClickListener`
- `setSideSelected` / `toggleSideSelected` / `clearSideSelection`
- `setSpringBackAnimatorProvider(provider)`
- `setupViewPager2TouchConflictResolution(enabled)`

## License

```
Copyright 2021 viifo

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
