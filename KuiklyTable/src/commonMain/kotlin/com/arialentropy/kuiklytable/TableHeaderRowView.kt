package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

internal class TableHeaderRowView<T> : ComposeView<TableHeaderRowAttr<T>, TableHeaderRowEvent<T>>() {
    override fun createAttr(): TableHeaderRowAttr<T> = TableHeaderRowAttr()
    override fun createEvent(): TableHeaderRowEvent<T> = TableHeaderRowEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                width(ctx.attr.columns.sumOf { it.width.toDouble() }.toFloat())
                height(ctx.attr.headerStyle.height.coerceAtLeast(DEFAULT_HEADER_HEIGHT))
                backgroundColor(Color(ctx.attr.themeColors.headerBackground))
            }
            ctx.attr.columns.forEachIndexed { index, resolvedColumn ->
                val isLastColumn = index == ctx.attr.columns.lastIndex
                View {
                    attr {
                        width(resolvedColumn.width)
                        height(ctx.attr.headerStyle.height.coerceAtLeast(DEFAULT_HEADER_HEIGHT))
                        flexDirectionRow()
                        alignItemsCenter()
                        touchEnable(resolvedColumn.model?.sortable == true)
                        backgroundColor(Color(ctx.attr.themeColors.headerBackground))
                    }
                    resolvedColumn.model?.let { column ->
                        event { click { if (column.sortable) ctx.event.columnClick?.invoke(column) } }
                    }
                    View {
                        attr {
                            flex(1f)
                            flexDirectionRow()
                            paddingLeft(ctx.attr.headerStyle.paddingH)
                            paddingRight(ctx.attr.headerStyle.paddingH)
                            paddingTop(ctx.attr.headerStyle.paddingV)
                            paddingBottom(ctx.attr.headerStyle.paddingV)
                        }
                        val column = resolvedColumn.model
                        if (resolvedColumn.isGeneratedIndex) {
                            Text {
                                attr {
                                    flex(1f)
                                    text(ctx.attr.indexColumnTitle)
                                    fontSize(ctx.attr.headerStyle.fontSize)
                                    color(Color(ctx.attr.themeColors.headerText))
                                    lines(1)
                                    textAlignCenter()
                                }
                            }
                        } else if (column?.headerRenderer != null) {
                            column.headerRenderer.invoke(this, column)
                        } else if (column != null) {
                            Text {
                                attr {
                                    flex(1f)
                                    text(ctx.headerText(column))
                                    fontSize(ctx.attr.headerStyle.fontSize)
                                    when (ctx.attr.headerStyle.fontWeight) {
                                        is TableHeaderFontWeight.Normal -> fontWeightNormal()
                                        is TableHeaderFontWeight.Medium -> fontWeightMedium()
                                        is TableHeaderFontWeight.Semisolid -> fontWeightSemiBold()
                                        is TableHeaderFontWeight.Bold -> fontWeightBold()
                                    }
                                    color(Color(ctx.attr.themeColors.headerText))
                                    lines(1)
                                    textOverFlowTail()
                                    when (column.alignment) {
                                        is ColumnAlignment.Center -> textAlignCenter()
                                        is ColumnAlignment.End -> textAlignRight()
                                        is ColumnAlignment.Start -> textAlignLeft()
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
                                height(ctx.attr.headerStyle.height.coerceAtLeast(DEFAULT_HEADER_HEIGHT))
                            }
                            backgroundColor(Color(columnStroke?.color ?: 0x00000000))
                        }
                    }
                }
            }
        }
    }

    private fun headerText(column: ColumnModel<T>): String =
        if (!column.sortable) column.title else "${column.title} ${sortIndicator(column)}"

    private fun sortIndicator(column: ColumnModel<T>): String =
        if (attr.sortState.columnKey != column.key) "↕" else when (attr.sortState.direction) {
            is TableSortDirection.Ascending -> "↑"
            is TableSortDirection.Descending -> "↓"
            is TableSortDirection.None -> "↕"
    }

    companion object {
        private const val DEFAULT_HEADER_HEIGHT = 44f
    }
}

internal class TableHeaderRowAttr<T> : ComposeAttr() {
    var columns: List<TableResolvedColumn<T>> by observable(emptyList())
    var sortState: TableSortState by observable(TableSortState())
    var indexColumnTitle: String by observable("序号")
    var lineMode: TableLineMode by observable(TableLineMode.Grid)
    var themeColors: TableThemeColors by observable(TableThemeColors())
    var headerStyle: TableHeaderStyle by observable(TableHeaderStyle.Default)
}

internal class TableHeaderRowEvent<T> : ComposeEvent() {
    var columnClick: ((ColumnModel<T>) -> Unit)? = null
}

internal fun <T> ViewContainer<*, *>.TableHeaderRowView(init: TableHeaderRowView<T>.() -> Unit) {
    addChild(TableHeaderRowView<T>(), init)
}
