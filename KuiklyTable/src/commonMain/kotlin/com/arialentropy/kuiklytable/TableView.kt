package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*

/**
 * KuiklyTable 主组件
 *
 * 使用 ComposeView 路线，在 commonMain 内用基础组件组合 Table。
 *
 * 滚动架构（ST-2）：
 * - 单个横向 Scroller 包住「表头行 + 纵向 List」，横向滚动时表头与数据行天然同步（无需手写同步逻辑）
 * - 表头作为纵向 List 的兄弟节点，纵向滚动时天然固定（固定表头）
 * - 纵向 List 复用 KuiklyUI 原生列表（RecyclerView/UICollectionView 回收）
 */
class TableView<T> : ComposeView<TableAttr<T>, TableEvent<T>>() {

    override fun createAttr(): TableAttr<T> = TableAttr()

    override fun createEvent(): TableEvent<T> = TableEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        val tableAttr = ctx.attr
        return {
            // 横向滚动容器：表头和数据行都在其中，横向滚动天然同步（无需手写同步逻辑）
            Scroller {
                attr {
                    flex(1f)
                    flexDirectionRow()
                }
                // 表格内容（宽度 = 表格总宽，高度随 Scroller 拉伸）
                View {
                    attr {
                        width(ctx.contentWidth())
                    }
                    // === 表头行 ===
            View {
                attr {
                    flexDirectionRow()
                    backgroundColor(Color(tableAttr.themeColors.headerBackground))
                    if (tableAttr.rowHeight > 0f) {
                        height(tableAttr.rowHeight)
                        alignItemsCenter()
                    }
                }
                vforIndex({ tableAttr.columns }) { column, index, count ->
                    View {
                        attr {
                            if (column.width != null) {
                                width(column.width)
                            } else {
                                flex(column.flex)
                            }
                            flexDirectionRow()
                        }
                        View {
                            attr {
                                flex(1f)
                                flexDirectionRow()
                                paddingLeft(tableAttr.cellPaddingH)
                                paddingRight(tableAttr.cellPaddingH)
                                paddingTop(tableAttr.cellPaddingV)
                                paddingBottom(tableAttr.cellPaddingV)
                            }
                            Text {
                                attr {
                                    flex(1f)
                                    text(column.title)
                                    fontSize(14f)
                                    fontWeightMedium()
                                    color(Color(tableAttr.themeColors.headerText))
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
                        View {
                            attr {
                                width(if (tableAttr.bordered && index < count - 1) 1f else 0f)
                                backgroundColor(Color(tableAttr.themeColors.gridLine))
                            }
                        }
                    }
                }
            }

            // 表头下分割线
            View {
                attr {
                    height(1f)
                    backgroundColor(Color(tableAttr.themeColors.gridLine))
                }
            }

            // === 数据行（纵向 List，原生回收；作为 body 直接子级，flex 才能撑满剩余高度） ===
            List {
                attr {
                    flex(1f)
                    backgroundColor(Color(tableAttr.themeColors.rowBackground))
                }
                tableAttr.data.forEachIndexed { index, item ->
                    // 行
                    View {
                        attr {
                            flexDirectionRow()
                            backgroundColor(
                                Color(
                                    if (tableAttr.zebraStripe && index % 2 == 1)
                                        tableAttr.themeColors.rowBackgroundAlt
                                    else
                                        tableAttr.themeColors.rowBackground
                                )
                            )
                            if (tableAttr.rowHeight > 0f) {
                                height(tableAttr.rowHeight)
                                alignItemsCenter()
                            }
                        }
                        event {
                            click {
                                ctx.event.rowClick?.invoke(item)
                            }
                        }
                        vforIndex({ tableAttr.columns }) { column, colIndex, count ->
                            View {
                                attr {
                                    if (column.width != null) {
                                        width(column.width)
                                    } else {
                                        flex(column.flex)
                                    }
                                    flexDirectionRow()
                                }
                                View {
                                    attr {
                                        flex(1f)
                                        flexDirectionRow()
                                        paddingLeft(tableAttr.cellPaddingH)
                                        paddingRight(tableAttr.cellPaddingH)
                                        paddingTop(tableAttr.cellPaddingV)
                                        paddingBottom(tableAttr.cellPaddingV)
                                    }
                                    Text {
                                        attr {
                                            flex(1f)
                                            text(column.accessor(item))
                                            fontSize(14f)
                                            color(Color(tableAttr.themeColors.cellText))
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
                                View {
                                    attr {
                                        width(if (tableAttr.bordered && colIndex < count - 1) 1f else 0f)
                                        backgroundColor(Color(tableAttr.themeColors.gridLine))
                                    }
                                }
                            }
                        }
                    }
                    // 行分割线
                    View {
                        attr {
                            height(1f)
                            backgroundColor(Color(tableAttr.themeColors.gridLine))
                        }
                    }
                }
            }
                }
            }
        }
    }

    /** 表格内容总宽：固定列宽之和 + 弹性列最小宽，与页面宽取较大者（小于页宽时弹性列撑满） */
    private fun contentWidth(): Float {
        val pageWidth = pagerData.pageViewWidth
        val fixedTotal = attr.columns.sumOf { it.width?.toDouble() ?: 0.0 }.toFloat()
        val flexCount = attr.columns.count { it.width == null }
        val naturalWidth = fixedTotal + flexCount * MIN_FLEX_COLUMN_WIDTH
        return if (naturalWidth > pageWidth) naturalWidth else pageWidth
    }

    companion object {
        /** 弹性列最小宽（表格超宽需要横向滚动时，弹性列至少有这么宽） */
        private const val MIN_FLEX_COLUMN_WIDTH = 100f
    }
}

/**
 * TableView 属性
 */
class TableAttr<T> : ComposeAttr() {
    /** 列定义列表 */
    var columns: ObservableList<ColumnModel<T>> by observableList()

    /** 数据列表 */
    var data: List<T> by observable(emptyList())

    /** 是否启用斑马纹 */
    var zebraStripe: Boolean by observable(true)

    /** 是否显示列边框（竖向分隔线）；水平分隔线始终显示 */
    var bordered: Boolean by observable(false)

    /** 单元格水平内边距（dp） */
    var cellPaddingH: Float by observable(12f)

    /** 单元格垂直内边距（dp） */
    var cellPaddingV: Float by observable(10f)

    /** 行高（dp）；> 0 时固定行高并垂直居中，0 表示由内边距+内容自适应 */
    var rowHeight: Float by observable(0f)

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
