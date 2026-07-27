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
    private var pendingScrollableOffset = Float.NaN
    private var pendingFixedOffset = Float.NaN
    private var mainBodyList: ListView<*, *>? = null
    private var fixedBodyList: ListView<*, *>? = null
    private var lastLoadMoreTriggerRowCount: Int? = null

    override fun createAttr(): TableAttr<T> = TableAttr()

    override fun createEvent(): TableEvent<T> = TableEvent()

    fun scrollToTop(animated: Boolean = false) {
        setVerticalContentOffset(0f, animated)
    }

    fun scrollToRow(index: Int, animated: Boolean = false) {
        if (index < 0 || displayRows.isEmpty()) return
        val targetIndex = index.coerceAtMost(displayRows.lastIndex)
        setVerticalContentOffset(targetIndex * estimatedRowScrollHeight(), animated)
    }

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
                    tableAttr.tableWidth?.let { width(it) } ?: alignSelfStretch()
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
            if (region == TableColumnRegion.All) ctx.mainBodyList = listView
            attr {
                flex(1f)
                backgroundColor(Color(tableAttr.themeColors.rowBackground))
            }
            event {
                scroll { params ->
                    ctx.event.overflowTipDismiss?.invoke()
                    if (region != TableColumnRegion.Fixed) ctx.maybeTriggerLoadMore(params)
                    if (region != TableColumnRegion.All && !ctx.consumePendingSync(region, params.offsetY)) {
                        val targetRegion = if (region == TableColumnRegion.Fixed) {
                            TableColumnRegion.Scrollable
                        } else {
                            TableColumnRegion.Fixed
                        }
                        ctx.setPendingSync(targetRegion, params.offsetY)
                        val target = if (targetRegion == TableColumnRegion.Fixed) {
                            ctx.fixedBodyList
                        } else {
                            ctx.mainBodyList
                        }
                        target?.setContentOffset(0f, params.offsetY)
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
                        if (row.displayIndex == ctx.displayRows.lastIndex) {
                            ctx.renderLoadMoreFooter(
                                this,
                                width = if (region == TableColumnRegion.Fixed) layout.fixedWidth else layout.contentWidth,
                                visible = region != TableColumnRegion.Fixed,
                            )
                        }
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
        val previousRows = displayRows.toList()
        if (displayRows.size != rows.size || displayRows.indices.any { currentIndex ->
                val current = displayRows[currentIndex]
                val next = rows[currentIndex]
                current.key != next.key || current.item !== next.item || current.displayIndex != next.displayIndex
            }
        ) {
            displayRows = ObservableList(rows.toMutableList())
            syncLoadMoreTriggerState(previousRows, rows)
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
            val listView = this
            ctx.mainBodyList = listView
            attr {
                flex(1f)
                backgroundColor(Color(tableAttr.themeColors.rowBackgroundAlt))
            }
            event {
                scroll { params ->
                    ctx.event.overflowTipDismiss?.invoke()
                    ctx.maybeTriggerLoadMore(params)
                }
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
                            if (index == count - 1) {
                                ctx.renderLoadMoreFooter(this)
                            }
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
                    // 消费状态层点击，避免 Loading/Empty 点击透传到底部行。
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

    private fun maybeTriggerLoadMore(params: ScrollParams) {
        if (!attr.hasMore || attr.loadingMore || displayRows.isEmpty() || event.loadMore == null) {
            if (!attr.hasMore) lastLoadMoreTriggerRowCount = null
            return
        }
        if (lastLoadMoreTriggerRowCount == displayRows.size) return
        val remaining = params.contentHeight - params.offsetY - params.viewHeight
        val threshold = estimatedRowScrollHeight() * max(attr.loadMoreThresholdRows, 0)
        if (remaining <= threshold) {
            lastLoadMoreTriggerRowCount = displayRows.size
            event.loadMore?.invoke()
        }
    }

    private fun renderLoadMoreFooter(
        container: ViewContainer<*, *>,
        width: Float? = null,
        visible: Boolean = true,
    ) {
        val tableAttr = attr
        container.View {
            attr {
                width?.let { width(it) }
                height(if (tableAttr.hasMore || tableAttr.loadingMore) LOAD_MORE_FOOTER_HEIGHT else 0f)
                allCenter()
                backgroundColor(Color(tableAttr.themeColors.rowBackground))
                visibility(visible && (tableAttr.hasMore || tableAttr.loadingMore))
            }
            if (tableAttr.loadingMore) {
                ActivityIndicator {
                    attr {
                        isGrayStyle(true)
                        marginRight(6f)
                    }
                }
            }
            Text {
                attr {
                    text(if (tableAttr.loadingMore) "加载更多中…" else "继续向下滚动加载更多")
                    fontSize(12f)
                    color(Color(tableAttr.themeColors.cellTextSecondary))
                }
            }
        }
    }

    private fun estimatedRowScrollHeight(): Float = when {
        attr.displayMode is TableDisplayMode.List -> LIST_ROW_HEIGHT_ESTIMATE
        effectiveRowHeight() > 0f -> effectiveRowHeight() + BODY_DIVIDER_HEIGHT
        else -> DEFAULT_ROW_HEIGHT_ESTIMATE + BODY_DIVIDER_HEIGHT
    }

    private fun setVerticalContentOffset(offsetY: Float, animated: Boolean) {
        val targetOffset = max(offsetY, 0f)
        setPendingSync(TableColumnRegion.Scrollable, targetOffset)
        mainBodyList?.setContentOffset(0f, targetOffset, animated)
        setPendingSync(TableColumnRegion.Fixed, targetOffset)
        fixedBodyList?.setContentOffset(0f, targetOffset, animated)
    }

    private fun consumePendingSync(region: TableColumnRegion, offsetY: Float): Boolean {
        val pendingOffset = when (region) {
            TableColumnRegion.Fixed -> pendingFixedOffset
            TableColumnRegion.Scrollable -> pendingScrollableOffset
            TableColumnRegion.All -> Float.NaN
        }
        if (pendingOffset.isNaN()) return false
        val matches = kotlin.math.abs(pendingOffset - offsetY) <= SCROLL_SYNC_TOLERANCE
        if (matches) {
            setPendingSync(region, Float.NaN)
        }
        return matches
    }

    private fun setPendingSync(region: TableColumnRegion, offsetY: Float) {
        when (region) {
            TableColumnRegion.Fixed -> pendingFixedOffset = offsetY
            TableColumnRegion.Scrollable -> pendingScrollableOffset = offsetY
            TableColumnRegion.All -> Unit
        }
    }

    private fun syncLoadMoreTriggerState(previousRows: List<TableDisplayRow<T>>, nextRows: List<TableDisplayRow<T>>) {
        if (previousRows.size != nextRows.size || previousRows.map { it.key } != nextRows.map { it.key }) {
            lastLoadMoreTriggerRowCount = null
        }
    }

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
        private const val BODY_DIVIDER_HEIGHT = 1f
        private const val LOAD_MORE_FOOTER_HEIGHT = 44f
        private const val LIST_ROW_HEIGHT_ESTIMATE = 74f
        private const val SCROLL_SYNC_TOLERANCE = 1f
    }
}

/**
 * DSL 入口：在任意 ViewContainer 中使用 TableView
 */
fun <T> ViewContainer<*, *>.TableView(init: TableView<T>.() -> Unit) {
    addChild(TableView<T>(), init)
}
