package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import kotlin.math.max

internal class TableListRowView<T> : ComposeView<TableListRowAttr<T>, TableListRowEvent<T>>() {
    override fun createAttr(): TableListRowAttr<T> = TableListRowAttr()
    override fun createEvent(): TableListRowEvent<T> = TableListRowEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val row = ctx.attr.row
            val primaryColumn = ctx.attr.primaryColumn
            if (row != null && primaryColumn != null) {
                attr {
                    paddingLeft(16f); paddingRight(16f); paddingTop(12f); paddingBottom(12f)
                    backgroundColor(Color(ctx.rowBackground(row)))
                }
                event { click { ctx.event.rowClick?.invoke(row.item) } }
                View {
                    attr { flexDirectionRow(); alignItemsFlexEnd() }
                    Text {
                        attr {
                            flex(1f); height(TITLE_LINE_HEIGHT); text(primaryColumn.accessor(row.item))
                            fontSize(16f); fontWeightSemiBold(); lineHeight(TITLE_LINE_HEIGHT)
                            color(Color(ctx.attr.themeColors.cellText)); lines(1); textOverFlowTail()
                        }
                    }
                    ctx.attr.statusColumn?.let { statusColumn ->
                        val statusText = statusColumn.accessor(row.item)
                        val style = ctx.resolveStatusStyle(row.item, statusText)
                        View {
                            attr {
                                marginLeft(8f); height(22f); flexDirectionRow(); alignItemsCenter(); justifyContentCenter()
                                paddingLeft(8f); paddingRight(8f); borderRadius(11f); backgroundColor(Color(style.background))
                            }
                            Text {
                                attr {
                                    height(22f); text(statusText); fontSize(12f); lineHeight(22f)
                                    color(Color(style.text)); lines(1); textOverFlowTail()
                                }
                            }
                        }
                    }
                }
                ctx.attr.columns.forEachIndexed { index, column ->
                    if (column.key != DataTableSelection.COLUMN_KEY &&
                        column !== primaryColumn &&
                        column !== ctx.attr.statusColumn
                    ) {
                        ctx.renderField(this, row, index, column)
                    }
                }
            }
        }
    }

    private fun renderField(
        container: ViewContainer<*, *>,
        row: TableDisplayRow<T>,
        columnIndex: Int,
        column: ColumnModel<T>,
    ) {
        val ctx = this
        val text = column.accessor(row.item)
        val isDefaultText = column.cellRenderer == null
        val isTruncated = isDefaultText && isTextTruncated(text)
        val resolvedColumn = ctx.attr.layout?.dataColumn(columnIndex)
        val overflowInfo = if (isDefaultText && resolvedColumn != null) TableOverflowCellInfo(
            row.displayIndex,
            columnIndex,
            column.key,
            row.item,
            text,
            isTruncated,
            resolvedColumn.x,
            row.displayIndex * DEFAULT_ROW_HEIGHT_ESTIMATE,
            resolvedColumn.width,
            DEFAULT_ROW_HEIGHT_ESTIMATE,
        ) else null
        val cellInfo = TableCellClickInfo(
            rowIndex = row.displayIndex,
            columnIndex = columnIndex,
            columnKey = column.key,
            rowData = row.item,
        )
        val tableClickEnabled = column.enableRowClick || column.enableCellClick ||
            (isTruncated && attr.enableOverflowCellClick)
        container.View {
            attr { flexDirectionRow(); alignItemsCenter(); paddingTop(4f); paddingBottom(4f) }
            if (tableClickEnabled) {
                event {
                    click {
                        ctx.dispatchFieldClick(column, row.item, overflowInfo, cellInfo, isTruncated)
                    }
                }
            }
            Text {
                attr {
                    width(FIELD_LABEL_WIDTH); text(column.title); fontSize(13f)
                    color(Color(ctx.attr.themeColors.cellTextSecondary)); lines(1); textOverFlowTail()
                }
            }
            Text {
                attr {
                    flex(1f); text(text); "title" with text; fontSize(14f); color(Color(ctx.attr.themeColors.cellText)); lines(1); textOverFlowTail()
                    when (column.alignment) {
                        is ColumnAlignment.Center -> textAlignCenter()
                        is ColumnAlignment.End -> textAlignRight()
                        is ColumnAlignment.Start -> textAlignLeft()
                    }
                }
                if (tableClickEnabled) {
                    event {
                        click {
                            ctx.dispatchFieldClick(column, row.item, overflowInfo, cellInfo, isTruncated)
                        }
                    }
                }
            }
        }
    }

    private fun dispatchFieldClick(
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

    private fun isTextTruncated(text: String): Boolean {
        if (!attr.enableOverflowCellClick || text.isEmpty()) return false
        val availableWidth = max(
            attr.viewportWidth - CARD_HORIZONTAL_MARGIN * 2f - CARD_HORIZONTAL_PADDING * 2f - FIELD_LABEL_WIDTH,
            0f,
        )
        return estimatedTextWidth(text) > availableWidth
    }

    private fun resolveStatusStyle(item: T, text: String): TableStatusTagStyle =
        attr.statusTagStyleResolver?.invoke(item, text, attr.themeColors)
            ?: attr.statusTagStyleByText[text]
            ?: attr.statusTagPresetByText[text]?.let { TableStatusTagStyle.fromPreset(it, attr.themeColors) }
            ?: TableStatusTagStyle.fromPreset(TableStatusTagPreset.fromText(text), attr.themeColors)

    private fun estimatedTextWidth(text: String): Float = text.sumOf { ch ->
        (if (ch.code > ASCII_MAX_CODE) DEFAULT_FONT_SIZE else DEFAULT_FONT_SIZE * ASCII_CHAR_WIDTH_RATIO).toDouble()
    }.toFloat()

    private fun rowBackground(row: TableDisplayRow<T>): Long =
        if (attr.selectedRowKeys.any { it == row.key }) {
            attr.themeColors.selectedRowBackground
        } else {
            attr.themeColors.cardBackground
        }

    companion object {
        private const val CARD_HORIZONTAL_MARGIN = 8f
        private const val CARD_HORIZONTAL_PADDING = 16f
        private const val FIELD_LABEL_WIDTH = 86f
        private const val TITLE_LINE_HEIGHT = 22f
        private const val DEFAULT_FONT_SIZE = 14f
        private const val ASCII_CHAR_WIDTH_RATIO = 0.58f
        private const val ASCII_MAX_CODE = 255
        private const val DEFAULT_ROW_HEIGHT_ESTIMATE = 48f
    }
}

internal class TableListRowAttr<T> : ComposeAttr() {
    var row: TableDisplayRow<T>? by observable(null)
    var columns: List<ColumnModel<T>> by observable(emptyList())
    var layout: TableResolvedColumnLayout<T>? by observable(null)
    var primaryColumn: ColumnModel<T>? by observable(null)
    var statusColumn: ColumnModel<T>? by observable(null)
    var viewportWidth: Float by observable(0f)
    var themeColors: TableThemeColors by observable(TableThemeColors())
    var statusTagPresetByText: Map<String, TableStatusTagPreset> by observable(emptyMap())
    var statusTagStyleByText: Map<String, TableStatusTagStyle> by observable(emptyMap())
    var statusTagStyleResolver: ((T, String, TableThemeColors) -> TableStatusTagStyle)? by observable(null)
    var enableOverflowCellClick: Boolean by observable(true)
    var selectedRowKeys: List<Any> by observable(emptyList())
}

internal class TableListRowEvent<T> : ComposeEvent() {
    var rowClick: ((T) -> Unit)? = null
    var cellClick: ((TableCellClickInfo<T>) -> Unit)? = null
    var overflowCellClick: ((TableOverflowCellInfo<T>) -> Unit)? = null
}

internal fun <T> ViewContainer<*, *>.TableListRowView(init: TableListRowView<T>.() -> Unit) {
    addChild(TableListRowView<T>(), init)
}
