package com.arialentropy.kuiklytable

import kotlin.math.max

internal object TableColumnLayoutResolver {
    fun <T> resolve(
        columns: List<ColumnModel<T>>,
        viewportWidth: Float,
        autoIndexColumn: Boolean,
        indexColumnWidth: Float,
        fixedColumnCount: Int,
    ): TableResolvedColumnLayout<T> {
        if (!autoIndexColumn && columns.size == 1) {
            val column = columns.first()
            val fallbackWidth = max(column.width ?: column.minWidth, 0f)
            val width = if (viewportWidth > 0f) viewportWidth else fallbackWidth
            val resolvedColumn = TableResolvedColumn(column, 0, 0, 0f, width, false)
            val fixedCount = fixedColumnCount.coerceIn(0, 1)
            return TableResolvedColumnLayout(
                all = listOf(resolvedColumn),
                fixed = if (fixedCount == 1) listOf(resolvedColumn) else emptyList(),
                scrollable = if (fixedCount == 1) emptyList() else listOf(resolvedColumn),
                contentWidth = width,
                fixedWidth = if (fixedCount == 1) width else 0f,
            )
        }
        val generatedWidth = if (autoIndexColumn) indexColumnWidth else 0f
        val fixedDataWidth = columns.sumOf { it.width?.toDouble() ?: 0.0 }.toFloat()
        val flexColumns = columns.filter { it.width == null }
        val minimumFlexWidth = flexColumns.sumOf { max(it.minWidth, 0f).toDouble() }.toFloat()
        val naturalWidth = generatedWidth + fixedDataWidth + minimumFlexWidth
        val contentWidth = max(viewportWidth, naturalWidth)
        val extraFlexWidth = max(contentWidth - generatedWidth - fixedDataWidth - minimumFlexWidth, 0f)
        val flexWeight = flexColumns.sumOf { max(it.flex, 0f).toDouble() }.toFloat()
        val resolved = mutableListOf<TableResolvedColumn<T>>()
        var x = 0f

        if (autoIndexColumn) {
            resolved += TableResolvedColumn(null, null, 0, x, generatedWidth, true)
            x += generatedWidth
        }

        columns.forEachIndexed { dataIndex, column ->
            val width = column.width ?: (max(column.minWidth, 0f) + if (flexWeight > 0f) {
                extraFlexWidth * max(column.flex, 0f) / flexWeight
            } else {
                0f
            })
            resolved += TableResolvedColumn(column, dataIndex, resolved.size, x, width, false)
            x += width
        }

        val fixedCount = fixedColumnCount.coerceIn(0, resolved.size)
        val fixed = resolved.take(fixedCount)
        return TableResolvedColumnLayout(
            all = resolved,
            fixed = fixed,
            scrollable = resolved.drop(fixedCount),
            contentWidth = x,
            fixedWidth = fixed.sumOf { it.width.toDouble() }.toFloat(),
        )
    }

    fun naturalWidth(
        columns: List<ColumnModel<*>>,
        autoIndexColumn: Boolean,
        indexColumnWidth: Float,
    ): Float = if (!autoIndexColumn && columns.size == 1) {
        max(columns.first().width ?: columns.first().minWidth, 0f)
    } else {
        (if (autoIndexColumn) indexColumnWidth else 0f) +
            columns.sumOf { (it.width ?: max(it.minWidth, 0f)).toDouble() }.toFloat()
    }
}
