package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*

/**
 * KuiklyTable 主组件
 *
 * 使用 ComposeView 路线，在 commonMain 内用基础组件组合 Table。
 */
class TableView<T> : ComposeView<TableAttr<T>, TableEvent<T>>() {

    override fun createAttr(): TableAttr<T> = TableAttr()

    override fun createEvent(): TableEvent<T> = TableEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 表格根容器
            View {
                attr {
                    flex(1f)
                    backgroundColor(Color(ctx.attr.themeColors.rowBackground))
                }

                // === 表头行 ===
                View {
                    attr {
                        flexDirectionRow()
                        backgroundColor(Color(ctx.attr.themeColors.headerBackground))
                    }
                    ctx.attr.columns.forEach { column ->
                        View {
                            attr {
                                if (column.width != null) {
                                    width(column.width)
                                } else {
                                    flex(column.flex)
                                }
                                flexDirectionRow()
                                paddingLeft(12f)
                                paddingRight(12f)
                                paddingTop(10f)
                                paddingBottom(10f)
                            }
                            Text {
                                attr {
                                    flex(1f)
                                    text(column.title)
                                    fontSize(14f)
                                    fontWeightMedium()
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
                }

                // 表头下分割线
                View {
                    attr {
                        height(1f)
                        backgroundColor(Color(ctx.attr.themeColors.gridLine))
                    }
                }

                // === 数据行 ===
                ctx.attr.data.forEachIndexed { index, item ->
                    // 行
                    View {
                        attr {
                            flexDirectionRow()
                            backgroundColor(
                                Color(
                                    if (ctx.attr.zebraStripe && index % 2 == 1)
                                        ctx.attr.themeColors.rowBackgroundAlt
                                    else
                                        ctx.attr.themeColors.rowBackground
                                )
                            )
                        }
                        event {
                            click {
                                ctx.event.rowClick?.invoke(item)
                            }
                        }

                        ctx.attr.columns.forEach { column ->
                            View {
                                attr {
                                    if (column.width != null) {
                                        width(column.width)
                                    } else {
                                        flex(column.flex)
                                    }
                                    flexDirectionRow()
                                    paddingLeft(12f)
                                    paddingRight(12f)
                                    paddingTop(10f)
                                    paddingBottom(10f)
                                }
                                Text {
                                    attr {
                                        flex(1f)
                                        text(column.accessor(item))
                                        fontSize(14f)
                                        color(Color(ctx.attr.themeColors.cellText))
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
                    }

                    // 行分割线
                    View {
                        attr {
                            height(1f)
                            backgroundColor(Color(ctx.attr.themeColors.gridLine))
                        }
                    }
                }
            }
        }
    }
}

/**
 * TableView 属性
 */
class TableAttr<T> : ComposeAttr() {
    /** 列定义列表 */
    var columns: List<ColumnModel<T>> by observable(emptyList())

    /** 数据列表 */
    var data: List<T> by observable(emptyList())

    /** 是否启用斑马纹 */
    var zebraStripe: Boolean by observable(true)

    /** 主题色 */
    var themeColors: TableThemeColors by observable(TableThemeColors())
}

/**
 * TableView 事件
 */
class TableEvent<T> : ComposeEvent() {
    /** 行点击回调 */
    var rowClick: ((T) -> Unit)? = null
}

/**
 * DSL 入口：在任意 ViewContainer 中使用 TableView
 */
fun <T> ViewContainer<*, *>.TableView(init: TableView<T>.() -> Unit) {
    addChild(TableView<T>(), init)
}
