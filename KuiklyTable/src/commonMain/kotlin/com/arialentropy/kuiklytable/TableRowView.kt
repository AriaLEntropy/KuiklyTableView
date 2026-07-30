package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import kotlin.math.max

internal class TableRowView<T> : ComposeView<TableRowAttr<T>, TableRowEvent<T>>() {
    override fun createAttr(): TableRowAttr<T> = TableRowAttr()
    override fun createEvent(): TableRowEvent<T> = TableRowEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            ctx.attr.row?.let { row ->
                attr {
                    flexDirectionRow()
                    alignItemsStretch()
                    width(ctx.attr.columns.sumOf { it.width.toDouble() }.toFloat())
                    backgroundColor(Color(ctx.rowBackground(row)))
                    if (ctx.attr.rowHeight > 0f) height(ctx.attr.rowHeight)
                }
                ctx.attr.columns.forEachIndexed { columnIndex, resolvedColumn ->
                    val isLastColumn = columnIndex == ctx.attr.columns.lastIndex
                    if (resolvedColumn.isGeneratedIndex) {
                        ctx.renderIndexCell(this, row, resolvedColumn, isLastColumn)
                    } else {
                        resolvedColumn.model?.let { column ->
                            ctx.renderDataCell(this, row, resolvedColumn, column, isLastColumn)
                        }
                    }
                }
            }
        }
    }

    private fun renderIndexCell(
        container: ViewContainer<*, *>,
        row: TableDisplayRow<T>,
        resolvedColumn: TableResolvedColumn<T>,
        isLastColumn: Boolean,
    ) {
        val ctx = this
        container.View {
            attr {
                width(resolvedColumn.width)
                flexDirectionRow()
                backgroundColor(Color(ctx.rowBackground(row)))
            }
            View {
                attr { flex(1f); alignItemsCenter(); justifyContentCenter() }
                Text {
                    attr {
                        text((row.displayIndex + 1).toString())
                        fontSize(DEFAULT_CELL_FONT_SIZE)
                        color(Color(ctx.attr.themeColors.cellTextSecondary))
                    }
                }
            }
            View {
                attr {
                    val columnStroke = ctx.attr.lineMode.resolve(ctx.attr.themeColors).column
                    val dividerWidth = columnStroke.columnDividerWidth(isLastColumn)
                    width(dividerWidth)
                    if (dividerWidth > 0f) {
                        height(ctx.columnDividerHeight())
                    }
                    backgroundColor(Color(columnStroke?.color ?: 0x00000000))
                }
            }
        }
    }

    private fun renderDataCell(
        container: ViewContainer<*, *>,
        row: TableDisplayRow<T>,
        resolvedColumn: TableResolvedColumn<T>,
        column: ColumnModel<T>,
        isLastColumn: Boolean,
    ) {
        val ctx = this
        val text = column.accessor(row.item)
        val isDefaultText = column.cellRenderer == null
        val isTruncated = isDefaultText && isTextTruncated(text, resolvedColumn.width)
        val overflowInfo = if (isDefaultText) {
            createOverflowInfo(row, resolvedColumn, column, text, isTruncated)
        } else {
            null
        }
        val cellInfo = TableCellClickInfo(
            rowIndex = row.displayIndex,
            columnIndex = resolvedColumn.dataIndex ?: 0,
            columnKey = column.key,
            rowData = row.item,
        )
        val tableClickEnabled = column.enableRowClick || column.enableCellClick ||
            (isTruncated && attr.enableOverflowCellClick)
        container.View {
            attr {
                width(resolvedColumn.width)
                flexDirectionRow()
                backgroundColor(Color(ctx.rowBackground(row)))
            }
            if (tableClickEnabled) {
                event {
                    click {
                        ctx.dispatchCellClick(column, row.item, overflowInfo, cellInfo, isTruncated)
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                    flexDirectionRow()
                    alignItemsCenter()
                    paddingLeft(ctx.attr.cellPaddingH)
                    paddingRight(ctx.attr.cellPaddingH)
                    paddingTop(ctx.attr.cellPaddingV)
                    paddingBottom(ctx.attr.cellPaddingV)
                    touchEnable(true)
                }
                if (tableClickEnabled) {
                    event {
                        click {
                            ctx.dispatchCellClick(column, row.item, overflowInfo, cellInfo, isTruncated)
                        }
                    }
                }
                if (column.cellRenderer != null) {
                    View {
                        attr { flex(1f); flexDirectionRow(); alignItemsCenter() }
                        column.cellRenderer.invoke(this, row.item, column)
                    }
                } else {
                    Text {
                        attr {
                            flex(1f)
                            text(text)
                            fontSize(DEFAULT_CELL_FONT_SIZE)
                            color(Color(ctx.attr.themeColors.cellText))
                            lines(1)
                            textOverFlowTail()
                            touchEnable(true)
                            when (column.alignment) {
                                is ColumnAlignment.Center -> textAlignCenter()
                                is ColumnAlignment.End -> textAlignRight()
                                is ColumnAlignment.Start -> textAlignLeft()
                            }
                        }
                        if (tableClickEnabled) {
                            event {
                                click {
                                    ctx.dispatchCellClick(column, row.item, overflowInfo, cellInfo, isTruncated)
                                }
                            }
                        }
                    }
                }
            }
            View {
                attr {
                    val columnStroke = ctx.attr.lineMode.resolve(ctx.attr.themeColors).column
                    val dividerWidth = columnStroke.columnDividerWidth(isLastColumn)
                    width(dividerWidth)
                    if (dividerWidth > 0f) {
                        height(ctx.columnDividerHeight())
                    }
                    backgroundColor(Color(columnStroke?.color ?: 0x00000000))
                }
            }
        }
    }

    private fun columnDividerHeight(): Float {
        if (attr.rowHeight > 0f) return attr.rowHeight
        return attr.cellPaddingV * 2f + DEFAULT_CELL_FONT_SIZE + 4f
    }

    private fun dispatchCellClick(
        column: ColumnModel<T>,
        item: T,
        overflowInfo: TableOverflowCellInfo<T>?,
        cellInfo: TableCellClickInfo<T>,
        isTruncated: Boolean,
    ) {
        when (
            val action = resolveTableCellClickAction(
                enableRowClick = column.enableRowClick,
                enableCellClick = column.enableCellClick,
                isTruncated = isTruncated && attr.enableOverflowCellClick,
                hasOverflowListener = event.overflowCellClick != null,
                hasCellClickListener = event.cellClick != null,
                hasRowClickListener = event.rowClick != null,
                overflowInfo = overflowInfo,
                cellInfo = cellInfo,
                rowData = item,
            )
        ) {
            is TableCellClickAction.Overflow -> event.overflowCellClick?.invoke(action.info)
            is TableCellClickAction.Cell -> event.cellClick?.invoke(action.info)
            is TableCellClickAction.Row -> event.rowClick?.invoke(action.rowData)
            is TableCellClickAction.None -> Unit
        }
    }

    private fun isTextTruncated(text: String, columnWidth: Float): Boolean {
        if (!attr.enableOverflowCellClick || text.isEmpty()) return false
        val availableWidth = max(columnWidth - attr.cellPaddingH * 2f, 0f)
        return estimatedTextWidth(text) > availableWidth
    }

    private fun createOverflowInfo(
        row: TableDisplayRow<T>,
        resolvedColumn: TableResolvedColumn<T>,
        column: ColumnModel<T>,
        text: String,
        isOverflow: Boolean,
    ): TableOverflowCellInfo<T> = TableOverflowCellInfo(
        row.displayIndex,
        resolvedColumn.dataIndex ?: 0,
        column.key,
        row.item,
        text,
        isOverflow,
        resolvedColumn.x,
        row.displayIndex * if (attr.rowHeight > 0f) attr.rowHeight else DEFAULT_ROW_HEIGHT_ESTIMATE,
        resolvedColumn.width,
        if (attr.rowHeight > 0f) attr.rowHeight else DEFAULT_ROW_HEIGHT_ESTIMATE,
    )

    private fun estimatedTextWidth(text: String): Float = text.sumOf { ch ->
        (if (ch.code > ASCII_MAX_CODE) DEFAULT_CELL_FONT_SIZE else DEFAULT_CELL_FONT_SIZE * ASCII_CHAR_WIDTH_RATIO).toDouble()
    }.toFloat()

    private fun rowBackground(row: TableDisplayRow<T>): Long {
        if (attr.selectedRowKeys.any { it == row.key }) {
            return attr.themeColors.selectedRowBackground
        }
        return if (attr.zebraStripe && row.displayIndex % 2 == 1) {
            attr.themeColors.rowBackgroundAlt
        } else {
            attr.themeColors.rowBackground
        }
    }

    companion object {
        private const val DEFAULT_CELL_FONT_SIZE = 14f
        private const val ASCII_CHAR_WIDTH_RATIO = 0.58f
        private const val ASCII_MAX_CODE = 255
        private const val DEFAULT_ROW_HEIGHT_ESTIMATE = 48f
    }
}

internal class TableRowAttr<T> : ComposeAttr() {
    var row: TableDisplayRow<T>? by observable(null)
    var columns: List<TableResolvedColumn<T>> by observable(emptyList())
    var rowHeight: Float by observable(0f)
    var zebraStripe: Boolean by observable(true)
    var lineMode: TableLineMode by observable(TableLineMode.Grid)
    var cellPaddingH: Float by observable(12f)
    var cellPaddingV: Float by observable(10f)
    var themeColors: TableThemeColors by observable(TableThemeColors())
    var enableOverflowCellClick: Boolean by observable(true)
    var selectedRowKeys: List<Any> by observable(emptyList())
}

internal class TableRowEvent<T> : ComposeEvent() {
    var rowClick: ((T) -> Unit)? = null
    var cellClick: ((TableCellClickInfo<T>) -> Unit)? = null
    var overflowCellClick: ((TableOverflowCellInfo<T>) -> Unit)? = null
}

internal fun <T> ViewContainer<*, *>.TableRowView(init: TableRowView<T>.() -> Unit) {
    addChild(TableRowView<T>(), init)
}
