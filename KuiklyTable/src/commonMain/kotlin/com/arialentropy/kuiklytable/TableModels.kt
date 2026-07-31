package com.arialentropy.kuiklytable

internal data class TableDisplayRow<T>(
    val item: T,
    val key: Any,
    val sourceIndex: Int,
    val displayIndex: Int,
)

internal data class TableResolvedColumn<T>(
    val model: ColumnModel<T>?,
    val dataIndex: Int?,
    val renderedIndex: Int,
    val x: Float,
    val width: Float,
    val isGeneratedIndex: Boolean,
)

internal data class TableResolvedColumnLayout<T>(
    val all: List<TableResolvedColumn<T>>,
    val fixed: List<TableResolvedColumn<T>>,
    val scrollable: List<TableResolvedColumn<T>>,
    val contentWidth: Float,
    val fixedWidth: Float,
) {
    fun columnsFor(region: TableColumnRegion): List<TableResolvedColumn<T>> = when (region) {
        TableColumnRegion.All -> all
        TableColumnRegion.Scrollable -> scrollable
        TableColumnRegion.Fixed -> fixed
    }

    fun dataColumn(dataIndex: Int): TableResolvedColumn<T> =
        all.first { it.dataIndex == dataIndex }
}

internal enum class TableColumnRegion {
    All,
    Scrollable,
    Fixed,
}

class TableOverflowCellInfo<T>(
    val rowIndex: Int,
    val columnIndex: Int,
    val columnKey: String,
    val rowData: T,
    val text: String,
    val isOverflow: Boolean,
    /**
     * 单元格相对 **Table 根容器** 的左上角 X（dp）。
     * 优先为点击时 `convertFrame` 实测；仅当测量失败时回退列布局估算。
     * 业务浮层应锚在 Table 外包的 `positionRelative` 容器内，直接使用本字段。
     */
    val estimatedCellX: Float,
    /**
     * 单元格相对 **Table 根容器** 的顶边 Y（dp）。
     * 优先为点击时实测；回退时才用表头块高度 + 行累计估算。
     */
    val estimatedCellY: Float,
    val estimatedCellWidth: Float,
    val estimatedCellHeight: Float,
)

class TableCellClickInfo<T>(
    val rowIndex: Int,
    val columnIndex: Int,
    val columnKey: String,
    val rowData: T,
)

/**
 * 单元格长按信息。 [text] 为列 [ColumnModel.accessor] 的完整文案（截断前）。
 * 剪贴板等副作用由业务在 [TableEvent.cellLongPress] 中自行处理。
 */
class TableCellLongPressInfo<T>(
    val rowIndex: Int,
    val columnIndex: Int,
    val columnKey: String,
    val rowData: T,
    val text: String,
)

/**
 * Table 侧单元格点击分发结果。一次点击只走一条路径。
 */
sealed class TableCellClickAction<T> {
    class Overflow<T>(val info: TableOverflowCellInfo<T>) : TableCellClickAction<T>()
    class Cell<T>(val info: TableCellClickInfo<T>) : TableCellClickAction<T>()
    class Row<T>(val rowData: T) : TableCellClickAction<T>()
    class None<T> : TableCellClickAction<T>()
}

sealed class TableSortDirection {
    object None : TableSortDirection()
    object Ascending : TableSortDirection()
    object Descending : TableSortDirection()
}

data class TableSortState(
    val columnKey: String? = null,
    val direction: TableSortDirection = TableSortDirection.None,
)
