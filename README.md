# FrozenColumnList

[![](https://jitpack.io/v/viifo/FrozenColumnList.svg)](https://jitpack.io/#viifo/FrozenColumnList)

[中文](https://github.com/viifo/FrozenColumnList/blob/master/README.md)  | [English](https://github.com/viifo/FrozenColumnList/blob/master/README_EN.md)

高效实现股票自选列表, 左侧列固定，右侧列可滑动查看更多列。



## Demo
下载 [APK-Demo](https://github.com/viifo/FrozenColumnList/releases)



## 预览

|                      滑动效果                       |                     水平越界回弹                      |
|:-----------------------------------------------:|:-----------------------------------------------:|
| <img src="./screenshots/p1.gif" height="500" /> | <img src="./screenshots/p2.gif" height="500" /> |
|                   **搭配刷新控件1**                   |                   **搭配刷新控件2**                   |
| <img src="./screenshots/p3.gif" height="500" /> | <img src="./screenshots/p4.gif" height="500" /> |
|                  **Item 更新动画**                  |                **联动 ViewPager**                 |
| <img src="./screenshots/p5.gif" height="500" /> | <img src="./screenshots/p6.gif" height="500" /> |
|                 **Empty view**                  |                 **Footer view**                 |
| <img src="./screenshots/p7.jpg" height="500" /> | <img src="./screenshots/p8.jpg" height="500" /> |



## 简单用例

1. 在根目录下的 build.gradle 文件中的 repositories 下添加

   ```groovy
   dependencyResolutionManagement {
     	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           ...
           maven { setUrl("https://jitpack.io") }
       }
   }
   ```

2. 添加依赖

   ```groovy
   dependencies {
       implementation("com.github.viifo:FrozenColumnList:1.0.0")
   }
   ```

3. 在 XML 中添加

   ```xml
   <!-- 表头 -->
   <com.viifo.frozencolumnlist.FrozenColumnHeader
       android:id="@+id/list_header"
       android:layout_width="match_parent"
       android:layout_height="35dp"
       android:background="@color/white"
       app:fchItemGravity="center_vertical"/>
   
   <!-- 股票列表 -->
   <com.viifo.frozencolumnlist.FrozenColumnList
       android:id="@+id/stock_list"
       android:layout_width="match_parent"
       android:layout_height="match_parent"/>
   ```

4. 自定义 [ColumnProvider](https://github.com/viifo/FrozenColumnList/blob/master/frozencolumnlist/src/main/java/com/viifo/frozencolumnlist/provider/ColumnProvider.kt)，告诉 `FrozenColumnList` 列表如何配置表头和 item，示例请参考 [StockColumnProvider](https://github.com/viifo/FrozenColumnList/blob/master/demo/src/main/java/com/viifo/frozencolumnlist/demo/ui/StockColumnProvider.kt) 

   ```kotlin
   class CustomProvider : DefaultColumnProvider<StockModel> {
   
       override fun createItemRowFrozenViews(
           parent: ViewGroup,
           viewType: Int,
           size: Int
       ): List<View> {
           // 创建固定列的子 View 列表, 请使用代码创建视图树以提升性能
           val paddingVertical = parent.context.dp2px(8)
           return (0 until size).map {
               LinearLayoutCompat(parent.context).apply {
                   orientation = LinearLayoutCompat.VERTICAL
                   gravity = Gravity.START or Gravity.CENTER_VERTICAL
                   setBackgroundColor(Color.WHITE)
                   addView(AppCompatTextView(parent.context).also {
                       it.id = R.id.item_tv_name
                       it.setTextColor(Color.BLACK)
                       it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                   })
                   addView(AppCompatTextView(parent.context).also {
                       it.id = R.id.item_tv_code
                       it.setTextColor(Color.GRAY)
                       it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                   })
                   setPadding(
                       parent.context.dp2px(12),
                       paddingVertical,
                       parent.context.dp2px(8),
                       paddingVertical
                   )
               }
           }
       }
   
       override fun createItemRowScrollableViews(
           parent: ViewGroup,
           viewType: Int,
           size: Int
       ): List<View> {
           // 创建可滚动列的子 View 列表, 请使用代码创建视图树以提升性能
           return (0 until size).map { index ->
               AppCompatTextView(parent.context).also {
                   it.id = your_view_id
                   it.setTextColor(Color.BLACK)
                   it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                   it.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                   it.setPadding(
                       0,
                       0,
                       parent.context.dp2px(if (index == size - 1) 14 else 10),
                       0
                   )
                   it.setBackgroundColor(Color.WHITE)
               }
           }
       }
   
       override fun bindItemRowFrozenViews(
           holder: GenericStockAdapter.GenericViewHolder<StockModel>,
           data: StockModel,
           payloads: List<Any?>
       ) {
           // 绑定固定列数据到对应的 View
           holder.setText(your_view_id, data.name)
           // ...
       }
   
       override fun bindItemRowScrollableViews(
           holder: GenericStockAdapter.GenericViewHolder<StockModel>,
           data: StockModel,
           payloads: List<Any?>
       ) {
           // 绑定可滚动列数据到对应的 View
           TODO("Not yet implemented")
       }
   }
   ```

5. 设置 `FrozenColumnHeader`

   ```kotlin
   // 为 FrozenColumnHeader 设置 ColumnProvider
   frozenColumnHeader?.setProvider(customProvider)
   // 表头点击事件监听
   frozenColumnHeader?.onHeaderClickListener = { view, index ->
       // eg. 如进行表头排序
   }
   ```

6. 设置 `FrozenColumnList`，其中的 [StockItemAnimator](https://github.com/viifo/FrozenColumnList/blob/master/demo/src/main/java/com/viifo/frozencolumnlist/demo/ui/StockItemAnimator.kt) 用于根据股票的涨跌状态添加 Item 更新闪烁效果

   ```kotlin
   // 为 FrozenColumnList 设置 ColumnProvider
   frozenColumnList.setProvider(provider)
   // 绑定表头视图 (同步滚动表头)
   frozenColumnList.attachHeader(frozenColumnHeader)
   // 设置 Item 更新动画，根据股票的涨跌状态添加闪烁效果
   frozenColumnList.setItemAnimator(StockItemAnimator(requireContext()))
   // 添加 Item 分割线装饰器
   frozenColumnList.addItemDecoration(
       BoundDividerDecoration(
           context = context,
           dividerColor = context?.getColor(R.color.divider_2) ?: Color.GRAY
       )
   )
   // 设置 Item 点击事件监听
   frozenColumnList.setOnItemClickListener { view, position, itemViewType ->
       // ....
   }
   ```

7. 设置列表数据

   ```kotlin
   // 更新表头数据
   frozenColumnHeader?.setHeaderData(headerList)
   // 更新列表数据
   frozenColumnList.submitList(stockList)
   ```



## 属性表

### FrozenColumnList Attrs

|              name              |  format   |                         description                          |
| :----------------------------: | :-------: | :----------------------------------------------------------: |
|       fclItemFrozenWidth       | dimension | item 冻结(固定)视图宽度，默认值为 120dp，优先使用 ColumnProvider 中的宽度 |
|          fclItemWidth          | dimension | item 非冻结(可滚动)视图宽度，默认值为 80dp，优先使用 ColumnProvider 中的宽度 |
|      fclOverScrollDamping      |   float   |               越界回弹阻尼系数，默认值为 0.6f                |
| fclOverScrollAnimatorThreshold | dimension |             越界回弹动画触发阈值，默认值为 10dp              |
|    fclMaxOverScrollDistance    | dimension |               最大越界回弹距离，默认值为 80dp                |



### FrozenColumnList Method

|                  name                  |           format           |                         description                          |
| :------------------------------------: | :------------------------: | :----------------------------------------------------------: |
|         canScrollHorizontally          |          boolean           |                是否允许水平滚动，默认为 true                 |
|              getProvider               |       ColumnProvider       |                  获取设置的 ColumnProvider                   |
|              setProvider               |       ColumnProvider       |               设置列视图提供器 ColumnProvider                |
|              attachHeader              |     FrozenColumnHeader     |                 绑定表头视图 (同步滚动表头)                  |
|               submitList               | List<out FrozenColumnData> |                         设置列表数据                         |
|          addChildClickViewIds          |         vararg Int         |        添加 Item 子 View 点击事件监听的 View ID 集合         |
|      setOnItemChildClickListener       |          callback          |                设置 item 子 view 点击事件监听                |
|         setOnItemClickListener         |          callback          |                    设置 Item 点击事件监听                    |
|    setOnEmptyViewChildClickListener    |          callback          |             设置 EmptyView 子 view 点击事件监听              |
|      setOnEmptyViewClickListener       |          callback          |                 设置 EmptyView 点击事件监听                  |
|   setOnFooterViewChildClickListener    |          callback          |             设置 FooterView 子 view 点击事件监听             |
|      setOnFooterViewClickListener      |          callback          |                 设置 FooterView 点击事件监听                 |
|            syncHeaderOffset            |     FrozenColumnHeader     |                    手动同步表头滚动偏移量                    |
|         updateHorizontalOffset         |            Int             |                      更新水平滚动偏移量                      |
|      addHorizontalScrollListener       |          callback          |                       添加水平滚动监听                       |
|     removeHorizontalScrollListener     |          callback          |                       移除水平滚动监听                       |
|     setSpringBackAnimatorProvider      | SpringBackAnimatorProvider |                    设置越界回弹动画提供器                    |
|      getFrozenColumnLayoutManager      | FrozenColumnLayoutManager  |                        获取布局管理器                        |
|         getFrozenColumnAdapter         |    GenericStockAdapter     |                        获取列表适配器                        |
|                getData                 | List<out FrozenColumnData> |                    获取当前绑定的数据列表                    |
|                getItem                 |    out FrozenColumnData    |                   获取指定位置的 Item 数据                   |
| setupViewPager2TouchConflictResolution |          boolean           | 是否启用 ViewPager2 嵌套冲突解决方案， 默认 false，启用后将关闭水平越界回弹 |



### ColumnProvider Method

|             name             |                format                 |                         description                          |
|:----------------------------:| :-----------------------------------: | :----------------------------------------------------------: |
|          getAdapter          |          GenericStockAdapter          | 指定 Adapter, 默认使用 [GenericStockAdapter](https://github.com/viifo/FrozenColumnList/blob/master/frozencolumnlist/src/main/java/com/viifo/frozencolumnlist/layout/GenericStockAdapter.kt) |
|       createEmptyView        |                 View                  |                          创建空视图                          |
|       createFooterView       |                 View                  |                         创建底部视图                         |
|       getColumnWidths        |               List<Int>               |        获取各列宽度（包括固定列和可滚动列，单位像素）        |
|      createFrozenHeader      |              List<View>               |                 创建固定列的表头子 View 列表                 |
|    createScrollableHeader    |              List<View>               |                 创建可滚动列表头子 View 列表                 |
|     bindFrozenHeaderView     |       (View, FrozenHeaderData)        |             绑定固定列表头数据到对应的表头 View              |
|   bindScrollableHeaderView   |       (View, FrozenHeaderData)        |            绑定可滚动列表头数据到对应的表头 View             |
|    createItemRowContainer    |               ViewGroup               |               创建每个 item 对应的行 View 容器               |
|   createItemRowFrozenViews   |              List<View>               |           创建每个 item 对应的固定列的子 View 列表           |
| createItemRowScrollableViews |              List<View>               |          创建每个 item 对应的可滚动列的子 View 列表          |
|    bindItemRowFrozenViews    | (GenericViewHolder<T>, T,List<Any?> ) |         绑定每个 item 对应的固定列数据到对应的 View          |
|  bindItemRowScrollableViews  | (GenericViewHolder<T>, T,List<Any?> ) |        绑定每个 item 对应的可滚动列数据到对应的 View         |



### FrozenColumnHeader Attrs

|        name        |  format   |                         description                          |
| :----------------: | :-------: | :----------------------------------------------------------: |
| fchItemFullHeight  |  boolean  |             item 视图是否占满高度, 默认值为 true             |
| fchItemFrozenWidth | dimension | item 冻结(固定)视图宽度，默认值为 120dp，优先使用 ColumnProvider 中的宽度 |
|    fchItemWidth    | dimension | item 非冻结(可滚动)视图宽度，默认值为 80dp，优先使用 ColumnProvider 中的宽度 |
|   fchItemGravity   |   enum    |           item 视图对齐方式, 默认值为 start \| top           |



### FrozenColumnHeader Method

|            name            |         format         |           description           |
| :------------------------: | :--------------------: | :-----------------------------: |
|        setProvider         |     ColumnProvider     | 设置列视图提供器 ColumnProvider |
|       setHeaderData        | List<FrozenHeaderData> |          设置表头数据           |
|       refreshHeader        | (Int,FrozenHeaderData) |        刷新单个表头数据         |
| onHorizontalScrollListener |        callback        |  水平滑动时回调，用于同步列表   |
|   onHeaderClickListener    |        callback        |       表头 Item 点击事件        |
|         headerData         | List<FrozenHeaderData> |            表头数据             |



## 混淆

不需要添加混淆过滤代码，并且已经混淆测试通过。



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

