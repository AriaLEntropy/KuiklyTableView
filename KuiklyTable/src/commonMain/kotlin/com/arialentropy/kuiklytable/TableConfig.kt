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
     * Stable business identity for a source row.
     *
     * The value must be unique within the current [data] list and stable for the same
     * business row across data updates. When omitted, the source index is used as a
     * fallback key. Duplicate keys are unsupported; the table does not de-duplicate,
     * rewrite, or repair them. The key is used by the table's derived row model and
     * future row-scoped state, but it does not change classic vfor/vforIndex node diffing.
     */
    var rowKey: ((T) -> Any)? by observable(null)
    var zebraStripe: Boolean by observable(true)
    var bordered: Boolean by observable(false)
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
    var listPrimaryColumnKey: String? by observable(null)
    var listStatusColumnKey: String? by observable(null)
    var listStatusTagPresetByText: Map<String, TableStatusTagPreset> by observable(emptyMap())
    var listStatusTagStyleByText: Map<String, TableStatusTagStyle> by observable(emptyMap())
    var listStatusTagStyleResolver: ((T, String, TableThemeColors) -> TableStatusTagStyle)? by observable(null)
    var loading: Boolean by observable(false)
    var emptyText: String by observable("暂无数据")
    var loadingText: String by observable("加载中…")
    /** Custom Empty state content. When null, the built-in empty icon and [emptyText] are used. */
    var emptyRenderer: (ViewContainer<*, *>.() -> Unit)? by observable(null)
    /** Custom Loading state content. When null, the built-in indicator and [loadingText] are used. */
    var loadingRenderer: (ViewContainer<*, *>.() -> Unit)? by observable(null)
    var enableOverflowCellClick: Boolean by observable(true)
}

class TableEvent<T> : ComposeEvent() {
    var rowClick: ((T) -> Unit)? = null
    var overflowCellClick: ((TableOverflowCellInfo<T>) -> Unit)? = null
    var overflowTipDismiss: (() -> Unit)? = null
    var sortChange: ((TableSortState) -> Unit)? = null
}
