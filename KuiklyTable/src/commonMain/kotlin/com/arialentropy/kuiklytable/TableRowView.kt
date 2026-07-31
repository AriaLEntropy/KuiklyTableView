package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.Frame
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
        val cellInfo = TableCellClickInfo(
            rowIndex = row.displayIndex,
            columnIndex = resolvedColumn.dataIndex ?: 0,
            columnKey = column.key,
            rowData = row.item,
        )
        val tableClickEnabled = column.enableRowClick || column.enableCellClick ||
            (isTruncated && attr.enableOverflowCellClick)
        val longPressEnabled = event.cellLongPress != null
        // 点击时再测相对 Table 根的真实矩形（对齐 Web getBoundingClientRect 思路）
        var cellRef: ViewRef<*>? = null
        fun overflowPayload(): TableOverflowCellInfo<T>? {
            if (!isDefaultText) return null
            val measured = cellRef?.view?.let { ctx.measureCellInTable(it) }
            return ctx.createOverflowInfo(row, resolvedColumn, column, text, isTruncated, measured)
        }
        fun fireClick() {
            ctx.dispatchCellClick(column, row.item, overflowPayload(), cellInfo, isTruncated)
        }
        container.View {
            ref { cellRef = it }
            attr {
                width(resolvedColumn.width)
                flexDirectionRow()
                alignItemsStretch()
                backgroundColor(Color(ctx.rowBackground(row)))
                if (longPressEnabled || tableClickEnabled) touchEnable(true)
            }
            event {
                if (tableClickEnabled) {
                    click { fireClick() }
                }
                if (longPressEnabled) {
                    longPress { params ->
                        ctx.dispatchCellLongPress(params.state, row, resolvedColumn, column.key, text)
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                    alignSelfStretch()
                    flexDirectionRow()
                    alignItemsCenter()
                    justifyContentCenter()
                    paddingLeft(ctx.attr.cellPaddingH)
                    paddingRight(ctx.attr.cellPaddingH)
                    paddingTop(ctx.attr.cellPaddingV)
                    paddingBottom(ctx.attr.cellPaddingV)
                    touchEnable(true)
                }
                event {
                    if (tableClickEnabled) {
                        click { fireClick() }
                    }
                    if (longPressEnabled) {
                        longPress { params ->
                            ctx.dispatchCellLongPress(params.state, row, resolvedColumn, column.key, text)
                        }
                    }
                }
                if (column.cellRenderer != null) {
                    View {
                        attr {
                            flex(1f)
                            flexDirectionRow()
                            alignItemsCenter()
                            justifyContentCenter()
                        }
                        column.cellRenderer.invoke(this, row.item, column)
                    }
                } else {
                    Text {
                        attr {
                            flex(1f)
                            text(text)
                            // Web: default text cell adds native HTML title tooltip.
                            "title" with text
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
                        event {
                            if (tableClickEnabled) {
                                click { fireClick() }
                            }
                            if (longPressEnabled) {
                                longPress { params ->
                                    ctx.dispatchCellLongPress(params.state, row, resolvedColumn, column.key, text)
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

    /**
     * 将单元格 [layoutFrame] 转换到 Table 根坐标系。
     * 对齐 TDesign/Ant Design 相对触发节点实测矩形的做法；失败时返回 null 走估算回退。
     * 注：Kuikly [convertFrame] 忽略 transform（固定列补偿场景可能偏差）。
     */
    private fun measureCellInTable(cell: DeclarativeBaseView<*, *>): Frame? {
        val root = attr.tableRoot ?: return null
        val local = cell.frame
        if (local.isDefaultValue() || local.width <= 0f || local.height <= 0f) return null
        val converted = cell.convertFrame(local, root)
        if (converted.width <= 0f || converted.height <= 0f) return null
        return converted
    }

    private fun dispatchCellLongPress(
        state: String,
        row: TableDisplayRow<T>,
        resolvedColumn: TableResolvedColumn<T>,
        columnKey: String,
        text: String,
    ) {
        if (state != "start") return
        event.cellLongPress?.invoke(
            TableCellLongPressInfo(
                rowIndex = row.displayIndex,
                columnIndex = resolvedColumn.dataIndex ?: 0,
                columnKey = columnKey,
                rowData = row.item,
                text = text,
            ),
        )
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
        measuredInTable: Frame? = null,
    ): TableOverflowCellInfo<T> {
        val fallbackHeight = estimatedRowHeight()
        val rowDivider = attr.lineMode.resolve(attr.themeColors).row?.width?.coerceAtLeast(0f) ?: 0f
        val fallbackY = attr.bodyOriginY + row.displayIndex * (fallbackHeight + rowDivider)
        return TableOverflowCellInfo(
            row.displayIndex,
            resolvedColumn.dataIndex ?: 0,
            column.key,
            row.item,
            text,
            isOverflow,
            measuredInTable?.x ?: resolvedColumn.x,
            measuredInTable?.y ?: fallbackY,
            measuredInTable?.width ?: resolvedColumn.width,
            measuredInTable?.height ?: fallbackHeight,
        )
    }

    private fun estimatedRowHeight(): Float =
        if (attr.rowHeight > 0f) attr.rowHeight
        else attr.cellPaddingV * 2f + DEFAULT_CELL_FONT_SIZE + 4f

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
    }
}

internal class TableRowAttr<T> : ComposeAttr() {
    var row: TableDisplayRow<T>? by observable(null)
    var columns: List<TableResolvedColumn<T>> by observable(emptyList())
    var rowHeight: Float by observable(0f)
    var zebraStripe: Boolean by observable(false)
    var lineMode: TableLineMode by observable(TableLineMode.Grid)
    var cellPaddingH: Float by observable(12f)
    var cellPaddingV: Float by observable(10f)
    var themeColors: TableThemeColors by observable(TableThemeColors())
    var enableOverflowCellClick: Boolean by observable(true)
    var selectedRowKeys: List<Any> by observable(emptyList())
    /**
     * 表体相对 Table 根的 Y 起点（通常为表头块高度）。
     * 仅作溢出坐标估算回退；优先路径为单元格实测 [convertFrame]。
     */
    var bodyOriginY: Float by observable(0f)
    /** Table 根容器，用于把单元格矩形 convertFrame 到相对 Table 根；点击时读取，不必 observable。 */
    var tableRoot: ViewContainer<*, *>? = null
}

internal class TableRowEvent<T> : ComposeEvent() {
    var rowClick: ((T) -> Unit)? = null
    var cellClick: ((TableCellClickInfo<T>) -> Unit)? = null
    var cellLongPress: ((TableCellLongPressInfo<T>) -> Unit)? = null
    var overflowCellClick: ((TableOverflowCellInfo<T>) -> Unit)? = null
}

internal fun <T> ViewContainer<*, *>.TableRowView(init: TableRowView<T>.() -> Unit) {
    addChild(TableRowView(), init)
}
