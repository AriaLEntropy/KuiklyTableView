package com.arialentropy.kuiklytable

/**
 * DataTable 派生结果：筛选 → 排序 → 分页。
 */
data class DataTablePageResult<T>(
    val pageItems: List<T>,
    val pageKeys: List<Any>,
    val filteredTotal: Int,
    val pageCount: Int,
    val pageIndex: Int,
    val pageSize: Int,
)

/**
 * KuiklyDataTable 数据管线。不修改外部源 [data]。
 *
 * 顺序：筛选 → 单列排序（复用 TableDataPipeline）→ 客户端分页。
 */
object DataTablePipeline {
    fun <T> buildPage(
        data: List<T>,
        rowKey: ((T) -> Any)?,
        columns: List<ColumnModel<T>>,
        sortState: TableSortState,
        filterPredicate: ((T) -> Boolean)?,
        enablePagination: Boolean,
        pageIndex: Int,
        pageSize: Int,
    ): DataTablePageResult<T> {
        val filtered = if (filterPredicate == null) data else data.filter(filterPredicate)
        val sortedRows = TableDataPipeline.buildDisplayRows(
            data = filtered,
            rowKey = rowKey,
            columns = columns,
            sortState = sortState,
        )
        val filteredTotal = sortedRows.size
        if (!enablePagination) {
            return DataTablePageResult(
                pageItems = sortedRows.map { it.item },
                pageKeys = sortedRows.map { it.key },
                filteredTotal = filteredTotal,
                pageCount = 1,
                pageIndex = 0,
                pageSize = filteredTotal.coerceAtLeast(1),
            )
        }
        val size = pageSize.coerceAtLeast(1)
        val pageCount = if (filteredTotal == 0) 1 else (filteredTotal + size - 1) / size
        val safeIndex = pageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        val from = (safeIndex * size).coerceAtMost(filteredTotal)
        val to = (from + size).coerceAtMost(filteredTotal)
        val pageRows = if (from >= to) emptyList() else sortedRows.subList(from, to)
        return DataTablePageResult(
            pageItems = pageRows.map { it.item },
            pageKeys = pageRows.map { it.key },
            filteredTotal = filteredTotal,
            pageCount = pageCount,
            pageIndex = safeIndex,
            pageSize = size,
        )
    }
}
