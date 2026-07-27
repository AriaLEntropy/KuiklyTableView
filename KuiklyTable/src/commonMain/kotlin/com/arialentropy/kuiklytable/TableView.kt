package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import kotlin.math.max

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

    private var displayRows: ObservableList<TableDisplayRow<T>> by observableList()
    private var viewportWidth by observable(0f)
    private var syncingVerticalScroll = false
    private var syncedVerticalOffset = Float.NaN
    private var mainBodyList: ListView<*, *>? = null
    private var fixedBodyList: ListView<*, *>? = null

    override fun createAttr(): TableAttr<T> = TableAttr()

    override fun createEvent(): TableEvent<T> = TableEvent()

    override fun created() {
        bindValueChange(
            valueBlock = {
                TableDataPipeline.buildDisplayRows(attr.data, attr.rowKey, attr.columns, attr.sortState)
            },
            valueChange = { value ->
                @Suppress("UNCHECKED_CAST")
                syncDisplayRows(value as List<TableDisplayRow<T>>)
            },
        )
    }

    override fun layoutFrameDidChanged(frame: Frame) {
        super.layoutFrameDidChanged(frame)
        if (frame.width > 0f && frame.width != viewportWidth) {
            viewportWidth = frame.width
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val tableAttr = ctx.attr
        return {
            View {
                attr {
                    flex(1f)
                    positionRelative()
                    overflow(true)
                    backgroundColor(Color(tableAttr.themeColors.rowBackground))
                }

                View {
                    attr {
                        absolutePositionAllZero()
                        opacity(if (tableAttr.loading) 0.4f else 1f)
                        touchEnable(!tableAttr.loading)
                    }

                    View {
                        attr {
                            absolutePositionAllZero()
                            visibility(!ctx.shouldRenderMobileList())
                            touchEnable(!ctx.shouldRenderMobileList())
                        }
                        vif({ ctx.viewportWidth > 0f }) {
                            ctx.renderTableLayout(this)
                        }
                    }

                    View {
                        attr {
                            absolutePositionAllZero()
                            visibility(ctx.shouldRenderMobileList())
                            touchEnable(ctx.shouldRenderMobileList())
                        }
                        vif({ ctx.viewportWidth > 0f }) {
                            ctx.renderMobileListLayout(this)
                        }
                    }
                }

                ctx.renderStateLayer(this)
            }
        }
    }

    private fun renderTableLayout(container: ViewContainer<*, *>) {
        val ctx = this
        val tableAttr = attr
        val layout = resolvedColumnLayout()
        container.View {
            attr {
                flex(1f)
                positionRelative()
                if (tableAttr.bordered) {
                    border(Border(1f, BorderStyle.SOLID, Color(tableAttr.themeColors.gridLine)))
                }
            }
            Scroller {
                attr {
                    flex(1f)
                    flexDirectionRow()
                }
                event {
                    scroll(sync = true) { ctx.event.overflowTipDismiss?.invoke() }
                    dragBegin { ctx.event.overflowTipDismiss?.invoke() }
                }
                View {
                    attr { width(layout.contentWidth) }
                    if (tableAttr.fixedHeader) {
                        ctx.renderHeaderRow(this, layout, TableColumnRegion.Scrollable)
                        ctx.renderHeaderDivider(this)
                    }
                    ctx.renderBodyRows(this, layout, TableColumnRegion.Scrollable)
                }
            }
            if (layout.fixed.isNotEmpty()) {
                View {
                    attr {
                        absolutePosition(top = 0f, left = 0f, bottom = 0f)
                        width(layout.fixedWidth)
                        zIndex(tableAttr.fixedColumnCount + 10)
                        backgroundColor(Color(tableAttr.themeColors.rowBackground))
                    }
                    if (tableAttr.fixedHeader) {
                        ctx.renderHeaderRow(this, layout, TableColumnRegion.Fixed)
                        ctx.renderHeaderDivider(this)
                    }
                    ctx.renderBodyRows(this, layout, TableColumnRegion.Fixed)
                }
            }
        }
    }

    private fun renderHeaderRow(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
        region: TableColumnRegion = TableColumnRegion.All,
    ) {
        val ctx = this
        val tableAttr = attr
        val visibleColumns = layout.columnsFor(region)
        if (region == TableColumnRegion.Scrollable && layout.fixed.isNotEmpty()) {
            container.View {
                attr { flexDirectionRow() }
                View { attr { width(layout.fixedWidth) } }
                TableHeaderRowView<T> {
                    attr { ctx.applyHeaderRowAttr(this, visibleColumns) }
                    event { columnClick = { ctx.toggleSort(it) } }
                }
            }
        } else {
            container.TableHeaderRowView<T> {
                attr { ctx.applyHeaderRowAttr(this, visibleColumns) }
                event { columnClick = { ctx.toggleSort(it) } }
            }
        }
    }

    private fun applyHeaderRowAttr(
        target: TableHeaderRowAttr<T>,
        columns: List<TableResolvedColumn<T>>,
    ) {
        target.columns = columns
        target.sortState = attr.sortState
        target.indexColumnTitle = attr.indexColumnTitle
        target.bordered = attr.bordered
        target.themeColors = attr.themeColors
        target.headerStyle = attr.headerStyle
    }

    private fun renderHeaderDivider(container: ViewContainer<*, *>) {
        val tableAttr = attr
        container.View {
            attr {
                height(tableAttr.headerStyle.bottomBorderWidth)
                backgroundColor(Color(tableAttr.themeColors.gridLine))
            }
        }
    }

    private fun renderBodyRows(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
        region: TableColumnRegion = TableColumnRegion.All,
    ) {
        val ctx = this
        val tableAttr = attr
        container.List {
            val listView = this
            if (region == TableColumnRegion.Scrollable) ctx.mainBodyList = listView
            if (region == TableColumnRegion.Fixed) ctx.fixedBodyList = listView
            attr {
                flex(1f)
                backgroundColor(Color(tableAttr.themeColors.rowBackground))
            }
            event {
                scroll { params ->
                    ctx.event.overflowTipDismiss?.invoke()
                    if (region != TableColumnRegion.All &&
                        !ctx.syncingVerticalScroll &&
                        (ctx.syncedVerticalOffset.isNaN() || kotlin.math.abs(ctx.syncedVerticalOffset - params.offsetY) > 0.5f)
                    ) {
                        ctx.syncingVerticalScroll = true
                        ctx.syncedVerticalOffset = params.offsetY
                        val target = if (region == TableColumnRegion.Fixed) ctx.mainBodyList else ctx.fixedBodyList
                        target?.setContentOffset(0f, params.offsetY)
                        ctx.syncingVerticalScroll = false
                    }
                }
                dragBegin { ctx.event.overflowTipDismiss?.invoke() }
            }
            if (!tableAttr.fixedHeader) {
                ctx.renderHeaderRow(this, layout, region)
                ctx.renderHeaderDivider(this)
            }
            vif({ ctx.displayRows.isEmpty() }) {
                ctx.renderEmptyPlaceholder(
                    this,
                    width = if (region == TableColumnRegion.Fixed) layout.fixedWidth else layout.contentWidth,
                )
            }
            vif({ ctx.displayRows.isNotEmpty() }) {
                listView.vforIndex({ ctx.displayRows }) { row, _, _ ->
                    View {
                        ctx.renderTableRowComponent(this, row, layout, region)
                        ctx.renderBodyDivider(this)
                    }
                }
            }
        }
    }

    private fun renderBodyDivider(container: ViewContainer<*, *>) {
        val tableAttr = attr
        container.View {
            attr {
                height(1f)
                backgroundColor(Color(tableAttr.themeColors.gridLine))
            }
        }
    }

    private fun effectiveRowHeight(): Float = when {
        attr.rowHeight > 0f -> attr.rowHeight
        attr.fixedColumnCount > 0 -> DEFAULT_ROW_HEIGHT_ESTIMATE
        else -> 0f
    }

    private fun syncDisplayRows(rows: List<TableDisplayRow<T>>) {
        if (displayRows.size != rows.size || displayRows.indices.any { currentIndex ->
                val current = displayRows[currentIndex]
                val next = rows[currentIndex]
                current.key != next.key || current.item !== next.item || current.displayIndex != next.displayIndex
            }
        ) {
            displayRows = ObservableList(rows.toMutableList())
        }
    }

    private fun renderTableRowComponent(
        container: ViewContainer<*, *>,
        row: TableDisplayRow<T>,
        layout: TableResolvedColumnLayout<T>,
        region: TableColumnRegion = TableColumnRegion.All,
    ) {
        val ctx = this
        val visibleColumns = layout.columnsFor(region)
        container.View {
            attr {
                flexDirectionRow()
            }
            if (region == TableColumnRegion.Scrollable && layout.fixed.isNotEmpty()) {
                View { attr { width(layout.fixedWidth) } }
            }
            TableRowView<T> {
                attr {
                    this.row = row
                    columns = visibleColumns
                    rowHeight = ctx.effectiveRowHeight()
                    zebraStripe = ctx.attr.zebraStripe
                    bordered = ctx.attr.bordered
                    cellPaddingH = ctx.attr.cellPaddingH
                    cellPaddingV = ctx.attr.cellPaddingV
                    themeColors = ctx.attr.themeColors
                    enableOverflowCellClick = ctx.attr.enableOverflowCellClick
                }
                event {
                    rowClick = { ctx.event.rowClick?.invoke(it) }
                    overflowCellClick = { ctx.event.overflowCellClick?.invoke(it) }
                }
            }
        }
    }

    private fun renderMobileListLayout(container: ViewContainer<*, *>) {
        val ctx = this
        val tableAttr = attr
        container.List {
            attr {
                flex(1f)
                backgroundColor(Color(tableAttr.themeColors.rowBackgroundAlt))
            }
            event {
                scroll { ctx.event.overflowTipDismiss?.invoke() }
                dragBegin { ctx.event.overflowTipDismiss?.invoke() }
            }
            vif({ ctx.displayRows.isEmpty() }) {
                ctx.renderEmptyPlaceholder(this)
            }
            vif({ ctx.displayRows.isNotEmpty() }) {
                View {
                    attr {
                        marginTop(8f)
                        marginLeft(8f)
                        marginRight(8f)
                        marginBottom(8f)
                        borderRadius(8f)
                        backgroundColor(Color(tableAttr.themeColors.cardBackground))
                    }
                    vforIndex({ ctx.displayRows }) { row, index, count ->
                        View {
                            ctx.renderMobileRowComponent(this, row, index == count - 1)
                        }
                    }
                }
            }
        }
    }

    private fun renderMobileRowComponent(container: ViewContainer<*, *>, row: TableDisplayRow<T>, isLast: Boolean) {
        val ctx = this
        val tableAttr = attr
        val primaryColumn = primaryMobileColumn() ?: return
        val statusColumn = statusMobileColumn()
        container.TableListRowView<T> {
            attr {
                this.row = row
                columns = tableAttr.columns
                layout = ctx.resolvedColumnLayout()
                this.primaryColumn = primaryColumn
                this.statusColumn = statusColumn
                viewportWidth = ctx.effectiveViewportWidth()
                themeColors = tableAttr.themeColors
                statusTagPresetByText = tableAttr.listStatusTagPresetByText
                statusTagStyleByText = tableAttr.listStatusTagStyleByText
                statusTagStyleResolver = tableAttr.listStatusTagStyleResolver
                enableOverflowCellClick = tableAttr.enableOverflowCellClick
            }
            event {
                rowClick = { ctx.event.rowClick?.invoke(it) }
                overflowCellClick = { ctx.event.overflowCellClick?.invoke(it) }
            }
        }
        if (!isLast) {
            container.View {
                attr {
                    height(1f)
                    marginLeft(16f)
                    backgroundColor(Color(tableAttr.themeColors.gridLine))
                }
            }
        }
    }

    private fun renderStateLayer(container: ViewContainer<*, *>) {
        val ctx = this
        val tableAttr = attr
        container.View {
            attr {
                absolutePositionAllZero()
                zIndex(10)
                visibility(ctx.hasStateLayer())
                touchEnable(ctx.hasStateLayer())
                backgroundColor(Color(ctx.stateLayerBackground()))
            }
            event {
                click {
                    // Consume state-layer taps so Loading/Empty never leak rowClick to rows below.
                }
            }

            ctx.renderLoadingState(this)
        }
    }

    private fun renderLoadingState(container: ViewContainer<*, *>) {
        val tableAttr = attr
        container.View {
            attr {
                absolutePositionAllZero()
                allCenter()
                visibility(tableAttr.loading)
            }
            if (tableAttr.loadingRenderer != null) {
                tableAttr.loadingRenderer?.invoke(this)
            } else {
                ActivityIndicator {
                    attr {
                        isGrayStyle(true)
                        marginBottom(10f)
                    }
                }
                Text {
                    attr {
                        text(tableAttr.loadingText)
                        fontSize(14f)
                        color(Color(tableAttr.themeColors.stateText))
                    }
                }
            }
        }
    }

    private fun renderEmptyPlaceholder(container: ViewContainer<*, *>, width: Float? = null) {
        val tableAttr = attr
        container.View {
            attr {
                width?.let { width(it) }
                height(180f)
                allCenter()
                backgroundColor(Color(tableAttr.themeColors.rowBackground))
            }
            if (tableAttr.emptyRenderer != null) {
                tableAttr.emptyRenderer?.invoke(this)
            } else {
                Text {
                    attr {
                        text("—")
                        fontSize(36f)
                        color(Color(tableAttr.themeColors.cellTextSecondary))
                        marginBottom(8f)
                    }
                }
                Text {
                    attr {
                        text(tableAttr.emptyText)
                        fontSize(14f)
                        color(Color(tableAttr.themeColors.stateText))
                    }
                }
            }
        }
    }

    private fun effectiveViewportWidth(): Float = if (viewportWidth > 0f) viewportWidth else {
        TableColumnLayoutResolver.naturalWidth(attr.columns, attr.autoIndexColumn, attr.indexColumnWidth)
    }

    private fun resolvedColumnLayout(): TableResolvedColumnLayout<T> = TableColumnLayoutResolver.resolve(
        columns = attr.columns,
        viewportWidth = effectiveViewportWidth(),
        autoIndexColumn = attr.autoIndexColumn,
        indexColumnWidth = attr.indexColumnWidth,
        fixedColumnCount = attr.fixedColumnCount,
    )

    private fun toggleSort(column: ColumnModel<T>) {
        val current = attr.sortState
        val direction = if (current.columnKey != column.key) {
            TableSortDirection.Ascending
        } else {
            when (current.direction) {
                is TableSortDirection.None -> TableSortDirection.Ascending
                is TableSortDirection.Ascending -> TableSortDirection.Descending
                is TableSortDirection.Descending -> TableSortDirection.None
            }
        }
        val next = TableSortState(if (direction is TableSortDirection.None) null else column.key, direction)
        attr.sortState = next
        event.sortChange?.invoke(next)
    }

    private fun shouldRenderMobileList(): Boolean =
        when (attr.displayMode) {
            is TableDisplayMode.List -> true
            is TableDisplayMode.Table -> false
        }

    private fun hasStateLayer(): Boolean =
        attr.loading

    private fun stateLayerBackground(): Long =
        attr.themeColors.stateOverlayBackground

    private fun primaryMobileColumn(): ColumnModel<T>? =
        attr.listPrimaryColumnKey?.let { key ->
            attr.columns.firstOrNull { it.key == key }
        } ?: attr.columns.firstOrNull()

    private fun statusMobileColumn(): ColumnModel<T>? =
        attr.listStatusColumnKey?.let { key ->
            attr.columns.firstOrNull { it.key == key }
        }

    companion object {
        private const val DEFAULT_ROW_HEIGHT_ESTIMATE = 48f
    }
}

/**
 * DSL 入口：在任意 ViewContainer 中使用 TableView
 */
fun <T> ViewContainer<*, *>.TableView(init: TableView<T>.() -> Unit) {
    addChild(TableView<T>(), init)
}
