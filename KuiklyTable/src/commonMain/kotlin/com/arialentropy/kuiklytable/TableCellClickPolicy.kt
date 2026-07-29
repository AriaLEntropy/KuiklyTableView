package com.arialentropy.kuiklytable

/**
 * 单元格点击优先级：overflow > cellClick > rowClick。
 * 一次点击只返回一条 Table 侧路径；与是否配置 cellRenderer 无关。
 */
internal fun <T> resolveTableCellClickAction(
    enableRowClick: Boolean,
    enableCellClick: Boolean,
    isTruncated: Boolean,
    hasOverflowListener: Boolean,
    hasCellClickListener: Boolean,
    hasRowClickListener: Boolean,
    overflowInfo: TableOverflowCellInfo<T>?,
    cellInfo: TableCellClickInfo<T>?,
    rowData: T,
): TableCellClickAction<T> {
    if (isTruncated && hasOverflowListener && overflowInfo != null) {
        return TableCellClickAction.Overflow(overflowInfo)
    }
    if (enableCellClick && hasCellClickListener && cellInfo != null) {
        return TableCellClickAction.Cell(cellInfo)
    }
    if (enableRowClick && hasRowClickListener) {
        return TableCellClickAction.Row(rowData)
    }
    return TableCellClickAction.None()
}
