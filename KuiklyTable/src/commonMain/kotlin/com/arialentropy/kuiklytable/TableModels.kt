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
    val estimatedCellX: Float,
    val estimatedCellY: Float,
    val estimatedCellWidth: Float,
    val estimatedCellHeight: Float,
)

sealed class TableDisplayMode {
    object Table : TableDisplayMode()
    object List : TableDisplayMode()
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
