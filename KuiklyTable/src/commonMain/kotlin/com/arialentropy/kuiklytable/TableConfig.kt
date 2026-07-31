package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList

open class TableAttr<T> : ComposeAttr() {
    var columns: ObservableList<ColumnModel<T>> by observableList()
    var data: List<T> by observable(emptyList())
    /**
     * Table 根容器宽度。为 null 时沿父容器横向撑满，相当于 100% 宽度；配置数值时使用显式宽度。
     */
    var tableWidth: Float? by observable(null)
    /**
     * 源数据行的稳定业务标识。
     *
     * 该值必须在当前 [data] 列表内唯一，并且同一业务行在数据更新前后保持稳定。
     * 未配置时使用源数据索引作为 fallback key。重复 key 不受支持，组件不会自动去重、
     * 改写或修复。该 key 用于表格内部派生行身份和后续行级状态关联，
     * 但不改变经典 vfor/vforIndex 的底层节点 diff 行为。
     */
    var rowKey: ((T) -> Any)? by observable(null)
    /** 隔行变色（斑马纹）。默认关闭；开启后偶数行用 [TableThemeColors.rowBackgroundAlt]。 */
    var zebraStripe: Boolean by observable(false)
    /**
     * 表格线模式。默认 [TableLineMode.Grid]：外框 + 表头/行/列线。
     * [TableLineMode.None] 关闭全部线；[TableLineMode.Horizontal] 仅横线族；
     * [TableLineMode.Custom] 按 [TableLineStyle] 逐项配置。
     * 与 [cornerRadius] 独立：无线时圆角仍可裁切内容。
     */
    var lineMode: TableLineMode by observable(TableLineMode.Grid)
    /**
     * Table 根容器圆角，单位为 dp。
     *
     * 默认 [TableCornerRadius.Default]（8dp）；设为 [TableCornerRadius.None]（0）关闭圆角。
     */
    var cornerRadius: Float by observable(TableCornerRadius.Default)
    var cellPaddingH: Float by observable(12f)
    var cellPaddingV: Float by observable(10f)
    var rowHeight: Float by observable(0f)
    var themeColors: TableThemeColors by observable(TableThemeColors())
    var headerStyle: TableHeaderStyle by observable(TableHeaderStyle.Default)
    var sortState: TableSortState by observable(TableSortState())
    var autoIndexColumn: Boolean by observable(false)
    var indexColumnTitle: String by observable("序号")
    var indexColumnWidth: Float by observable(56f)
    /**
     * 纵滚时表头始终钉在表体上方，不参与纵向滚动。
     *
     * 保留该字段以兼容旧调用；写入 `false` 无效，布局仍按固定表头处理。
     */
    var fixedHeader: Boolean by observable(true)
    /**
     * 是否固定第一列。开启时使用单纵向 List + 行内固定区。
     *
     * 约束：
     * - 固定列必须配置显式正数 [ColumnModel.width]（DataTable 多选时选择列与第一业务列均需有 width）
     * - 仅一列时忽略本开关，保持普通横向滚动
     * - 建议显式 `rowHeight > 0`；不与 [TableRowRenderMode.Windowed] 组合
     */
    var fixedFirstColumn: Boolean by observable(false)
    /** Internal fixed slot count; DataTable uses 2 when selection is enabled. */
    internal var fixedColumnSlots: Int by observable(0)
    /**
     * Row DSL rendering strategy. Configure during Table creation; changing this value after
     * the Table is mounted does not rebuild the rendering branch.
     */
    var rowRenderMode: TableRowRenderMode = TableRowRenderMode.Standard
    var loading: Boolean by observable(false)
    var emptyText: String by observable("暂无数据")
    var loadingText: String by observable("加载中…")
    /** 自定义 Empty 状态内容；为 null 时使用内置空态图形和 [emptyText]。 */
    var emptyRenderer: (ViewContainer<*, *>.() -> Unit)? by observable(null)
    /** 自定义 Loading 状态内容；为 null 时使用内置加载指示和 [loadingText]。 */
    var loadingRenderer: (ViewContainer<*, *>.() -> Unit)? by observable(null)
    var hasMore: Boolean by observable(false)
    var loadingMore: Boolean by observable(false)
    var loadMoreThresholdRows: Int by observable(3)
    var enableOverflowCellClick: Boolean by observable(true)
    /**
     * 当前选中行的 rowKey 集合。非空时对应行使用 [TableThemeColors.selectedRowBackground] 高亮。
     * 由 KuiklyDataTable 写入；基础 Table 场景保持空列表即可。
     */
    var selectedRowKeys: List<Any> by observable(emptyList())
}

open class TableEvent<T> : ComposeEvent() {
    var rowClick: ((T) -> Unit)? = null
    var cellClick: ((TableCellClickInfo<T>) -> Unit)? = null
    /** 单元格长按；参数含完整 [TableCellLongPressInfo.text]。组件不内置复制。 */
    var cellLongPress: ((TableCellLongPressInfo<T>) -> Unit)? = null
    var overflowCellClick: ((TableOverflowCellInfo<T>) -> Unit)? = null
    var overflowTipDismiss: (() -> Unit)? = null
    /**
     * 排序状态变化（仅 columnKey + 方向）。
     * 比较规则不在此事件中传递，请写在对应列的 [ColumnModel.sortComparator]。
     */
    var sortChange: ((TableSortState) -> Unit)? = null
    var loadMore: (() -> Unit)? = null
}
