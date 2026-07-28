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
                    alignItemsCenter()
                    backgroundColor(Color(ctx.rowBackground(row.displayIndex)))
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
                backgroundColor(Color(ctx.rowBackground(row.displayIndex)))
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
                    width(if (ctx.attr.borderMode.hasVisibleLines() && !isLastColumn) 1f else 0f)
                    backgroundColor(Color(ctx.attr.themeColors.gridLine))
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
        val info = if (isDefaultText) createOverflowInfo(row, resolvedColumn, column, text, isTruncated) else null
        container.View {
            attr {
                width(resolvedColumn.width)
                flexDirectionRow()
                backgroundColor(Color(ctx.rowBackground(row.displayIndex)))
            }
            event {
                click {
                    if (isDefaultText) ctx.handleCellClick(row.item, info, isTruncated)
                    else ctx.event.rowClick?.invoke(row.item)
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
                if (isDefaultText) event { click { ctx.handleCellClick(row.item, info, isTruncated) } }
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
                        event { click { ctx.handleCellClick(row.item, info, isTruncated) } }
                    }
                }
            }
            View {
                attr {
                    width(if (ctx.attr.borderMode.hasVisibleLines() && !isLastColumn) 1f else 0f)
                    backgroundColor(Color(ctx.attr.themeColors.gridLine))
                }
            }
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

    private fun handleCellClick(item: T, info: TableOverflowCellInfo<T>?, isTruncated: Boolean) {
        if (isTruncated && info != null && event.overflowCellClick != null) event.overflowCellClick?.invoke(info)
        else event.rowClick?.invoke(item)
    }

    private fun estimatedTextWidth(text: String): Float = text.sumOf { ch ->
        (if (ch.code > ASCII_MAX_CODE) DEFAULT_CELL_FONT_SIZE else DEFAULT_CELL_FONT_SIZE * ASCII_CHAR_WIDTH_RATIO).toDouble()
    }.toFloat()

    private fun rowBackground(index: Int): Long =
        if (attr.zebraStripe && index % 2 == 1) attr.themeColors.rowBackgroundAlt else attr.themeColors.rowBackground

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
    var borderMode: TableBorderMode by observable(TableBorderMode.Default)
    var cellPaddingH: Float by observable(12f)
    var cellPaddingV: Float by observable(10f)
    var themeColors: TableThemeColors by observable(TableThemeColors())
    var enableOverflowCellClick: Boolean by observable(true)
}

internal class TableRowEvent<T> : ComposeEvent() {
    var rowClick: ((T) -> Unit)? = null
    var overflowCellClick: ((TableOverflowCellInfo<T>) -> Unit)? = null
}

internal fun <T> ViewContainer<*, *>.TableRowView(init: TableRowView<T>.() -> Unit) {
    addChild(TableRowView<T>(), init)
}
