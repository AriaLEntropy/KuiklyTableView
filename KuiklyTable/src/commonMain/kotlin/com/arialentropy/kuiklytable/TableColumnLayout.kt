package com.arialentropy.kuiklytable

import kotlin.math.max

internal object TableColumnLayoutResolver {
    fun <T> resolve(
        columns: List<ColumnModel<T>>,
        viewportWidth: Float,
        autoIndexColumn: Boolean,
        indexColumnWidth: Float,
        fixedColumnSlots: Int,
    ): TableResolvedColumnLayout<T> {
        val effectiveSlots = effectiveFixedColumnSlots(
            columns = columns,
            autoIndexColumn = autoIndexColumn,
            requestedSlots = fixedColumnSlots,
        )
        if (!autoIndexColumn && columns.size == 1) {
            val column = columns.first()
            val fallbackWidth = max(column.width ?: column.minWidth, 0f)
            val width = if (viewportWidth > 0f) viewportWidth else fallbackWidth
            val resolvedColumn = TableResolvedColumn(column, 0, 0, 0f, width, false)
            // 单列无法形成「固定 + 可滚」分区，忽略固定列
            return TableResolvedColumnLayout(
                all = listOf(resolvedColumn),
                fixed = emptyList(),
                scrollable = listOf(resolvedColumn),
                contentWidth = width,
                fixedWidth = 0f,
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

        val fixedCount = effectiveSlots.coerceIn(0, resolved.size)
        val fixed = resolved.take(fixedCount)
        return TableResolvedColumnLayout(
            all = resolved,
            fixed = fixed,
            scrollable = resolved.drop(fixedCount),
            contentWidth = x,
            fixedWidth = fixed.sumOf { it.width.toDouble() }.toFloat(),
        )
    }

    /**
     * 计算实际生效的固定列槽位数。
     *
     * - 请求 ≤0：不固定
     * - 渲染列总数 ≤1：单列无法固定，返回 0
     * - 请求槽位覆盖全部渲染列：没有可滚区，返回 0
     * - 将被固定的业务列必须带显式 [ColumnModel.width]（生成序号列用 indexColumnWidth）；否则返回 0
     */
    fun effectiveFixedColumnSlots(
        columns: List<ColumnModel<*>>,
        autoIndexColumn: Boolean,
        requestedSlots: Int,
    ): Int {
        if (requestedSlots <= 0) return 0
        val renderColumnCount = (if (autoIndexColumn) 1 else 0) + columns.size
        if (renderColumnCount <= 1) return 0
        val slots = requestedSlots.coerceAtMost(renderColumnCount - 1)
        if (slots <= 0) return 0

        var remaining = slots
        if (autoIndexColumn) {
            remaining -= 1
        }
        if (remaining > 0) {
            val targets = columns.take(remaining)
            if (targets.any { it.width == null || it.width!! <= 0f }) {
                println(
                    "[KuiklyTable] fixedFirstColumn requires explicit positive width on fixed columns; " +
                        "falling back to plain horizontal scroll",
                )
                return 0
            }
        }
        return slots
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
