package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList

class TableAttr<T> : ComposeAttr() {
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
    var zebraStripe: Boolean by observable(true)
    /**
     * Table 根容器外框。
     *
     * 默认 [TableBorderMode.Default]：1dp，颜色取 [themeColors.gridLine]。
     * [TableBorderMode.None] 关闭外框和列间竖线；[TableBorderMode.Custom] 指定颜色与宽度。
     */
    var borderMode: TableBorderMode by observable(TableBorderMode.Default)
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
    var fixedHeader: Boolean by observable(true)
    var fixedColumnCount: Int by observable(0)
    var displayMode: TableDisplayMode by observable(TableDisplayMode.Table)
    /**
     * Row DSL rendering strategy. Configure during Table creation; changing this value after
     * the Table is mounted does not rebuild the rendering branch.
     */
    var rowRenderMode: TableRowRenderMode = TableRowRenderMode.Standard
    var listPrimaryColumnKey: String? by observable(null)
    var listStatusColumnKey: String? by observable(null)
    var listStatusTagPresetByText: Map<String, TableStatusTagPreset> by observable(emptyMap())
    var listStatusTagStyleByText: Map<String, TableStatusTagStyle> by observable(emptyMap())
    var listStatusTagStyleResolver: ((T, String, TableThemeColors) -> TableStatusTagStyle)? by observable(null)
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
}

class TableEvent<T> : ComposeEvent() {
    var rowClick: ((T) -> Unit)? = null
    var overflowCellClick: ((TableOverflowCellInfo<T>) -> Unit)? = null
    var overflowTipDismiss: (() -> Unit)? = null
    var sortChange: ((TableSortState) -> Unit)? = null
    var loadMore: (() -> Unit)? = null
}
