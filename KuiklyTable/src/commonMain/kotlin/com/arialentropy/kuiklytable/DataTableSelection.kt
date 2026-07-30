package com.arialentropy.kuiklytable

/**
 * 表头全选控件状态：相对当前展示行（排序/过滤后）计算。
 */
enum class DataTableSelectAllState {
    None,
    Partial,
    All,
}

/**
 * KuiklyDataTable 行选择纯逻辑。选中身份是 rowKey，与 display 位置无关。
 */
object DataTableSelection {
    const val COLUMN_KEY = "__datatable_selection__"
    const val DEFAULT_COLUMN_WIDTH = 48f

    fun selectAllState(
        visibleKeys: List<Any>,
        selectedKeys: Collection<Any>,
    ): DataTableSelectAllState {
        if (visibleKeys.isEmpty()) return DataTableSelectAllState.None
        val selected = selectedKeys.toSet()
        var selectedVisible = 0
        for (key in visibleKeys) {
            if (key in selected) selectedVisible += 1
        }
        return when (selectedVisible) {
            0 -> DataTableSelectAllState.None
            visibleKeys.size -> DataTableSelectAllState.All
            else -> DataTableSelectAllState.Partial
        }
    }

    fun toggleKey(
        selectedKeys: Collection<Any>,
        key: Any,
    ): List<Any> {
        val next = selectedKeys.toMutableList()
        val index = next.indexOfFirst { it == key }
        if (index >= 0) {
            next.removeAt(index)
        } else {
            next.add(key)
        }
        return next
    }

    /**
     * 半选或未选 → 全选当前展示行；已全选 → 清空当前展示行的选中（保留不在可见集中的 key）。
     */
    fun toggleSelectAll(
        visibleKeys: List<Any>,
        selectedKeys: Collection<Any>,
    ): List<Any> {
        val selected = selectedKeys.toMutableList()
        val visibleSet = visibleKeys.toSet()
        return when (selectAllState(visibleKeys, selected)) {
            DataTableSelectAllState.All -> selected.filterNot { it in visibleSet }
            DataTableSelectAllState.None,
            DataTableSelectAllState.Partial,
            -> {
                val merged = selected.filterNot { it in visibleSet }.toMutableList()
                for (key in visibleKeys) {
                    if (key !in merged) merged.add(key)
                }
                merged
            }
        }
    }

    fun <T> visibleKeys(
        data: List<T>,
        rowKey: ((T) -> Any)?,
        columns: List<ColumnModel<T>>,
        sortState: TableSortState,
    ): List<Any> = TableDataPipeline.buildDisplayRows(data, rowKey, columns, sortState).map { it.key }

    fun isSelected(selectedKeys: Collection<Any>, key: Any): Boolean =
        selectedKeys.any { it == key }
}
