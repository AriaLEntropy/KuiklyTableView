package com.arialentropy.kuiklytable

internal object TableDataPipeline {
    fun <T> buildDisplayRows(
        data: List<T>,
        rowKey: ((T) -> Any)?,
        columns: List<ColumnModel<T>>,
        sortState: TableSortState,
    ): List<TableDisplayRow<T>> {
        val sourceRows = data.mapIndexed { sourceIndex, item -> sourceIndex to item }
        val sortedRows = applySorting(sourceRows, columns, sortState)
        return sortedRows.mapIndexed { displayIndex, (sourceIndex, item) ->
            TableDisplayRow(
                item = item,
                key = rowKey?.invoke(item) ?: sourceIndex,
                sourceIndex = sourceIndex,
                displayIndex = displayIndex,
            )
        }
    }

    private fun <T> applySorting(
        rows: List<Pair<Int, T>>,
        columns: List<ColumnModel<T>>,
        sortState: TableSortState,
    ): List<Pair<Int, T>> {
        if (sortState.direction is TableSortDirection.None) return rows
        val column = columns.firstOrNull { it.key == sortState.columnKey && it.sortable } ?: return rows
        val comparator = column.sortComparator ?: Comparator { left: T, right: T ->
            column.accessor(left).compareTo(column.accessor(right))
        }
        return when (sortState.direction) {
            is TableSortDirection.Ascending -> rows.sortedWith(
                Comparator { left, right -> comparator.compare(left.second, right.second) },
            )
            is TableSortDirection.Descending -> rows.sortedWith(
                Comparator { left, right -> comparator.compare(right.second, left.second) },
            )
            is TableSortDirection.None -> rows
        }
    }
}
