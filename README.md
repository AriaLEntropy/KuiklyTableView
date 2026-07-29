# KuiklyTable

基于 [KuiklyUI](https://github.com/Tencent-TDS/KuiklyUI) 的跨端表格组件，支持 Android、iOS、鸿蒙。

用 `TableView` + `ColumnModel` 定义列和数据，支持横纵滚动、主题、自定义单元格、排序和空态/加载态。

## 效果预览

静态对照：

| 基础样式 | 主题三套 | 自定义 Renderer |
| :---: | :---: | :---: |
| <img src="assets/table_showcase_basic.png" alt="基础表格样式" width="280"> | <img src="assets/table_showcase_theme.png" alt="Light / Dark / Blue 主题" width="280"> | <img src="assets/table_showcase_renderer.png" alt="自定义单元格渲染" width="280"> |

| 状态反馈 | Table / List 模式 |
| :---: | :---: |
| <img src="assets/table_showcase_state.png" alt="无数据状态" width="280"> | <img src="assets/table_showcase_mode.png" alt="Table 与 List 展示模式" width="280"> |

交互录屏：

| 双向滚动 | 排序三态 | 大数据虚拟滚动 |
| :---: | :---: | :---: |
| <img src="assets/table_showcase_scroll_demo.gif" alt="横纵双向滚动" width="280"> | <img src="assets/table_showcase_sort_demo.gif" alt="表头三态排序" width="280"> | <img src="assets/table_showcase_large_demo.gif" alt="Windowed 大数据虚拟滚动" width="280"> |

## 功能特性

| 类别 | 能力 |
| --- | --- |
| 基础结构 | `TableView` / `ColumnModel`，固定列宽与弹性 `minWidth + flex` |
| 滚动 | 横纵双向滚动，表头与内容横向同步；可选固定表头 |
| 样式 | 默认 1dp 主题色外框与 8dp 圆角；斑马纹、对齐、行高、内边距可配 |
| 主题 | Light / Dark 预设，`TableThemeColors` 语义色覆盖 |
| 自定义渲染 | `cellRenderer` / `headerRenderer`；未配置回退默认文本 |
| 交互 | 表头三态排序、行点击、截断单元格溢出点击、回顶 |
| 状态 | 加载中 / 空数据，支持自定义 Empty / Loading 内容 |
| 展示模式 | 显式 `Table` 或 `List`（grouped） |
| 数据加载 | `loadMore` 触底回调（分页与请求由业务层负责） |
| 大数据 | 显式 `Standard` / `Windowed` 行渲染策略；窗口模式限制已挂载 DSL 行节点 |

## 接入方式

将本仓库的 `KuiklyTable` 模块引入你的 Kuikly 工程：

```kotlin
// settings.gradle.kts
include(":KuiklyTable")
project(":KuiklyTable").projectDir = file("path/to/KuiklyTable")
```

```kotlin
// 业务模块 build.gradle.kts
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":KuiklyTable"))
            }
        }
    }
}
```

## 基础用法

```kotlin
data class User(
    val id: String,
    val name: String,
    val age: Int,
    val email: String,
)

TableView<User> {
    attr {
        columns.addAll(
            listOf(
                ColumnModel(key = "name", title = "姓名", accessor = { it.name }, width = 80f),
                ColumnModel(
                    key = "age",
                    title = "年龄",
                    accessor = { it.age.toString() },
                    width = 60f,
                    alignment = ColumnAlignment.End,
                    sortable = true,
                    sortComparator = compareBy { it.age },
                ),
                ColumnModel(
                    key = "email",
                    title = "邮箱",
                    accessor = { it.email },
                    minWidth = 140f,
                    flex = 2f,
                ),
            )
        )
        data = users
        rowKey = { it.id }
        zebraStripe = true
        fixedHeader = true
        // borderMode 默认 Default（1dp 主题色）；cornerRadius 默认 8
        // borderMode = TableBorderMode.None
        // cornerRadius = TableCornerRadius.None
    }
    event {
        rowClick = { user -> /* 行点击 */ }
        sortChange = { state -> /* 排序变化 */ }
    }
}
```

### 自定义单元格

未配置 `cellRenderer` 时用默认文本，点击走 `rowClick` 或截断溢出事件。配置后由业务自己画单元格内容；Table 不再在外层隐式触发 `rowClick`，按钮等控件自己处理点击和按压。

```kotlin
ColumnModel<User>(
    key = "status",
    title = "状态",
    accessor = { it.status },
    width = 90f,
    cellRenderer = { user, _ ->
        // 使用方自行绘制彩色标签等业务内容
        View {
            attr {
                flex(1f)
                allCenter()
            }
            Text {
                attr {
                    text(user.status)
                    fontSize(12f)
                }
            }
        }
    },
)
```

### 主题、外框与圆角

默认外框为 1dp 主题色（`TableBorderMode.Default`），默认圆角 8dp（`TableCornerRadius.Default`）。设为 `None` / `0` 可关闭。

```kotlin
attr {
    themeColors = TableThemeColors.Dark
    borderMode = TableBorderMode.Custom(color = 0xFF4F8FFF, width = 1f)
    cornerRadius = TableCornerRadius.Large // 0 / 8 / 12，或任意 dp
    cellPaddingH = 12f
    cellPaddingV = 10f
}
```

### 空态 / 加载中 / 加载更多

```kotlin
attr {
    loading = isLoading
    emptyText = "暂无数据"
    // emptyRenderer = { /* 自定义空态 */ }
    // loadingRenderer = { /* 自定义加载态 */ }
    hasMore = pageHasMore
    loadingMore = isLoadingMore
}
event {
    loadMore = { fetchNextPage() }
}
```

### 回顶

```kotlin
val tableRef = TableView<User> { /* ... */ }

tableRef.scrollToTop(animated = true)
```

### List 模式

```kotlin
attr {
    displayMode = TableDisplayMode.List
    listPrimaryColumnKey = "name"
    listStatusColumnKey = "status"
}
```

### 大数据窗口渲染

默认 `Standard` 会为每行创建 DSL 节点。数据量大时改用 `Windowed`，按窗口挂载行节点（基于 Kuikly `vforLazy`）：

<p align="left">
  <img src="assets/table_showcase_large_demo.gif" alt="Windowed 大数据虚拟滚动" width="280">
</p>

```kotlin
attr {
    data = users
    rowHeight = 48f
    fixedColumnCount = 0
    rowRenderMode = TableRowRenderMode.Windowed(maxRenderedRows = 160)
}
```

`Windowed` 只限制挂载的行节点，完整 `data` 仍在内存里，排序仍作用于全量数据。创建时选定模式后不要在同一 Table 上切换；暂不支持动态行高和固定列组合。

`maxRenderedRows` 建议按可见行数 × 3 估算。设太小，快速滚动可能出现短暂空白。默认 60。

## API 参考

### TableView

```kotlin
fun <T> ViewContainer<*, *>.TableView(init: TableView<T>.() -> Unit)
```

| 方法 | 说明 |
| --- | --- |
| `scrollToTop(animated: Boolean = false)` | 滚动到顶部 |

### TableAttr

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `columns` | `ObservableList<ColumnModel<T>>` | `[]` | 列定义 |
| `data` | `List<T>` | `[]` | 数据源（组件不修改外部列表） |
| `rowKey` | `((T) -> Any)?` | `null` | 稳定行标识；未配置时用源索引 |
| `tableWidth` | `Float?` | `null` | `null` 表示沿父容器撑满 |
| `zebraStripe` | `Boolean` | `true` | 斑马纹 |
| `borderMode` | `TableBorderMode` | `Default` | 默认 1dp 主题色外框；`None` 关闭；`Custom` 自定义 |
| `cornerRadius` | `Float` | `8f` | 根容器圆角（dp）；`0` 无圆角。可用 `TableCornerRadius` |
| `cellPaddingH` | `Float` | `12f` | 水平内边距 |
| `cellPaddingV` | `Float` | `10f` | 垂直内边距 |
| `rowHeight` | `Float` | `0f` | `0` 表示内容自适应 |
| `fixedHeader` | `Boolean` | `true` | 固定表头 |
| `fixedColumnCount` | `Int` | `0` | 左侧固定列数量（实验性） |
| `themeColors` | `TableThemeColors` | Light | 语义色 |
| `displayMode` | `TableDisplayMode` | `Table` | `Table` / `List` |
| `rowRenderMode` | `TableRowRenderMode` | `Standard` | 初始化期行渲染策略；`Windowed(n)` 限制挂载行数 |
| `listPrimaryColumnKey` | `String?` | `null` | List 模式主字段列 |
| `listStatusColumnKey` | `String?` | `null` | List 模式状态列 |
| `loading` | `Boolean` | `false` | 加载中 |
| `emptyText` / `loadingText` | `String` | 见默认文案 | 默认状态文案 |
| `emptyRenderer` / `loadingRenderer` | DSL? | `null` | 自定义状态内容 |
| `hasMore` / `loadingMore` | `Boolean` | `false` | 加载更多状态 |
| `loadMoreThresholdRows` | `Int` | `3` | 距底部约 N 行触发 |
| `enableOverflowCellClick` | `Boolean` | `true` | 截断文本点击 |

### TableEvent

| 事件 | 说明 |
| --- | --- |
| `rowClick` | 行点击 |
| `sortChange` | 排序状态变化 |
| `overflowCellClick` | 截断单元格点击 |
| `overflowTipDismiss` | 溢出提示关闭 |
| `loadMore` | 触底加载更多 |

### ColumnModel

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `key` | `String` | — | 列唯一标识 |
| `title` | `String` | — | 表头文案 |
| `accessor` | `(T) -> String` | — | 默认文本取值 |
| `width` | `Float?` | `null` | 固定列宽；`null` 时用弹性宽度 |
| `minWidth` | `Float` | `100f` | 最小宽度（弹性列） |
| `flex` | `Float` | `1f` | 剩余空间权重 |
| `alignment` | `ColumnAlignment` | `Start` | `Start` / `Center` / `End` |
| `sortable` | `Boolean` | `false` | 是否可排序 |
| `sortComparator` | `Comparator<T>?` | `null` | 自定义比较器 |
| `cellRenderer` | DSL? | `null` | 自定义单元格 |
| `headerRenderer` | DSL? | `null` | 自定义表头 |

## 示例工程

1. Android Studio 打开本仓库
2. 运行 `androidApp`
3. 默认进入 `table_basic` Showcase

```bash
./gradlew :androidApp:compileDebugKotlin
```

## 测试

```bash
./gradlew :KuiklyTable:allTests
```

`commonTest` 覆盖：

- `TableDataPipelineTest` — 排序管线
- `TableColumnLayoutResolverTest` — 列宽分配
- `TableBorderTest` — 边框规格
- `TableRowRenderModeTest` — Standard / Windowed 配置
- `TableLoadMoreTriggerPolicyTest` — 加载更多触发去重

## 相关资源

- [Kuikly 官方文档](https://kuikly.tds.qq.com/)
- [KuiklyUI](https://github.com/Tencent-TDS/KuiklyUI)
- [Kuikly-contrib](https://github.com/Kuikly-contrib)

## License

[MIT](LICENSE)
