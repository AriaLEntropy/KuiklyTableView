package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.directives.vforLazy
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
                    // 必须每次都写入，条件跳过会导致 None/0 无法清掉上一帧的边框与圆角
                    borderRadius(tableAttr.cornerRadius.coerceAtLeast(0f))
                    border(
                        tableAttr.borderMode.borderSpec(tableAttr.themeColors)
                            ?: Border(0f, BorderStyle.SOLID, Color(0x00000000)),
                    )
                    backgroundColor(
                        Color(
                            if (ctx.shouldRenderMobileList()) {
                                tableAttr.themeColors.cardBackground
                            } else {
                                tableAttr.themeColors.rowBackground
                            },
                        ),
                    )
                }

                View {
                    attr {
                        absolutePositionAllZero()
                        opacity(if (tableAttr.loading) 0.4f else 1f)
                        touchEnable(!tableAttr.loading)
                    }
                    // 互斥创建：两套布局若同时挂载，后执行的 List 会覆盖 mainBodyList，
                    // 导致 scrollToTop 作用到隐藏列表上。
                    vif({ ctx.viewportWidth > 0f && !ctx.shouldRenderMobileList() }) {
                        ctx.renderTableLayout(this)
                    }
                    vif({ ctx.viewportWidth > 0f && ctx.shouldRenderMobileList() }) {
                        ctx.renderMobileListLayout(this)
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
        target.borderMode = attr.borderMode
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
            ctx.renderTableRowLoop(listView, layout, region)
            // footer 必须是 List 的兄弟节点而非行节点的一部分：Windowed 模式下
            // vforLazy 用挂载行的平均高度估算未挂载区域，把 footer 塞进末行会撑高该行。
            ctx.renderLoadMoreFooter(
                this,
                width = if (region == TableColumnRegion.Fixed) layout.fixedWidth else layout.contentWidth,
                visible = region != TableColumnRegion.Fixed,
            )
        }
    }

    private fun renderTableRowLoop(
        listView: ListView<*, *>,
        layout: TableResolvedColumnLayout<T>,
        region: TableColumnRegion,
    ) {
        val ctx = this
        when (val renderMode = attr.rowRenderMode) {
            is TableRowRenderMode.Standard -> {
                listView.vforIndex({ displayRows }) { row, _, _ ->
                    View { ctx.renderTableRowWrapper(this, row, layout, region) }
                }
            }
            is TableRowRenderMode.Windowed -> {
                require(attr.fixedColumnCount <= 0) {
                    "TableRowRenderMode.Windowed does not support fixed columns"
                }
                listView.vforLazy({ displayRows }, renderMode.maxRenderedRows) { row, _, _ ->
                    View { ctx.renderTableRowWrapper(this, row, layout, region) }
                }
            }
        }
    }

    private fun renderTableRowWrapper(
        container: ViewContainer<*, *>,
        row: TableDisplayRow<T>,
        layout: TableResolvedColumnLayout<T>,
        region: TableColumnRegion,
    ) {
        renderTableRowComponent(container, row, layout, region)
        renderBodyDivider(container)
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
        attr.rowRenderMode is TableRowRenderMode.Windowed -> DEFAULT_ROW_HEIGHT_ESTIMATE
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
                    borderMode = ctx.attr.borderMode
                    cellPaddingH = ctx.attr.cellPaddingH
                    cellPaddingV = ctx.attr.cellPaddingV
                    themeColors = ctx.attr.themeColors
                    enableOverflowCellClick = ctx.attr.enableOverflowCellClick
                }
                event {
                    rowClick = { ctx.event.rowClick?.invoke(it) }
                    cellClick = { ctx.event.cellClick?.invoke(it) }
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
                // 外框与圆角由 Table 根容器统一绘制，List 内容不再套一层卡片。
                backgroundColor(Color(tableAttr.themeColors.cardBackground))
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
            ctx.renderMobileRowLoop(listView)
            ctx.renderLoadMoreFooter(this)
        }
    }

    private fun renderMobileRowLoop(listView: ListView<*, *>) {
        val ctx = this
        when (val renderMode = attr.rowRenderMode) {
            is TableRowRenderMode.Standard -> {
                listView.vforIndex({ displayRows }) { row, index, count ->
                    View { ctx.renderMobileRowComponent(this, row, index == count - 1) }
                }
            }
            is TableRowRenderMode.Windowed -> {
                require(attr.fixedColumnCount <= 0) {
                    "TableRowRenderMode.Windowed does not support fixed columns"
                }
                listView.vforLazy({ displayRows }, renderMode.maxRenderedRows) { row, index, count ->
                    View { ctx.renderMobileRowComponent(this, row, index == count - 1) }
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
                cellClick = { ctx.event.cellClick?.invoke(it) }
                overflowCellClick = { ctx.event.overflowCellClick?.invoke(it) }
            }
        }
        if (!isLast) {
            container.View {
                attr {
                    height(1f)
                    marginLeft(16f)
                    marginRight(16f)
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
        val ctx = this
        val tableAttr = attr
        container.View {
            attr {
                val active = ctx.displayRows.isNotEmpty() && (tableAttr.hasMore || tableAttr.loadingMore)
                width?.let { width(it) }
                height(if (active) LOAD_MORE_FOOTER_HEIGHT else 0f)
                allCenter()
                backgroundColor(Color(tableAttr.themeColors.rowBackground))
                visibility(visible && active)
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
        if (shouldResetLoadMoreTrigger(previousRows, nextRows)) {
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
