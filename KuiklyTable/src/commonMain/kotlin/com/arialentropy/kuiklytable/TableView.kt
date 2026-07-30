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
 * 滚动架构：
 * - `fixedFirstColumn == false`：普通横向滚动
 * - `fixedFirstColumn == true`：表头 Fixed|H-Scroller；表体单个 H-Scroller 包纵向 List（与无固定列同构），
 *   固定列用 +scrollX transform 钉在视口左侧。表头/表体 Scroller 用 pending 互相同步，禁止多行 H-Scroller。
 *   要求固定行高；不与 Windowed 组合。固定列 transform 命令式更新，不用 observable。
 */
class TableView<T> : ComposeView<TableAttr<T>, TableEvent<T>>() {

    private var displayRows: ObservableList<TableDisplayRow<T>> by observableList()
    private var viewportWidth by observable(0f)
    /** 非 observable：避免横滑每帧触发可见行 ReactiveObserver 重跑 attr。 */
    private var horizontalScrollX = 0f
    /** 与 fixedFirstColumn 分离的布局开关，确保 vif 能可靠订阅到变化。 */
    private var pinnedMode by observable(false)
    private var mainBodyList: ListView<*, *>? = null
    private var headerHorizontalScroller: ScrollerView<*, *>? = null
    private var bodyHorizontalScroller: ScrollerView<*, *>? = null
    private val pinnedFixedContentRefs = mutableMapOf<Int, ViewRef<*>>()
    private var pinnedFixedClusterWidth = 0f
    private var lastLoadMoreTriggerRowCount: Int? = null
    private var pendingHorizontalScrollX = Float.NaN

    override fun createAttr(): TableAttr<T> = TableAttr()

    override fun createEvent(): TableEvent<T> = TableEvent()

    fun scrollToTop(animated: Boolean = false) {
        setVerticalContentOffset(0f, animated)
    }

    /** 供 DataTable 等外层在管线 / 配置变化时同步到 Table。 */
    fun updateData(rows: List<T>) {
        attr.data = rows
    }

    fun updateFixedFirstColumn(enabled: Boolean, slots: Int = if (enabled) 1 else 0) {
        attr.fixedFirstColumn = enabled
        attr.fixedColumnSlots = slots.coerceAtLeast(0)
        pinnedMode = enabled
        if (!enabled) {
            horizontalScrollX = 0f
            pendingHorizontalScrollX = Float.NaN
            headerHorizontalScroller = null
            bodyHorizontalScroller = null
            pinnedFixedContentRefs.clear()
        }
    }

    fun updateThemeColors(colors: TableThemeColors) {
        attr.themeColors = colors
    }

    fun updateSelectedRowKeys(keys: List<Any>) {
        attr.selectedRowKeys = keys
    }

    fun updateSortState(state: TableSortState) {
        attr.sortState = state
    }

    override fun created() {
        pinnedMode = attr.fixedFirstColumn
        if (attr.fixedFirstColumn && attr.fixedColumnSlots == 0) {
            attr.fixedColumnSlots = 1
        }
        bindValueChange(
            valueBlock = { attr.fixedFirstColumn to attr.fixedColumnSlots },
            valueChange = { value ->
                val state = value as Pair<*, *>
                pinnedMode = state.first as Boolean
                if (!pinnedMode) {
                    horizontalScrollX = 0f
                    pendingHorizontalScrollX = Float.NaN
                    headerHorizontalScroller = null
                    bodyHorizontalScroller = null
                    pinnedFixedContentRefs.clear()
                }
            },
        )
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
                    borderRadius(tableAttr.cornerRadius.coerceAtLeast(0f))
                    border(
                        tableAttr.lineMode.resolve(tableAttr.themeColors).outer
                            .toOuterBorderOrTransparent(),
                    )
                    backgroundColor(Color(tableAttr.themeColors.rowBackground))
                }

                View {
                    attr {
                        absolutePositionAllZero()
                        opacity(if (tableAttr.loading) 0.4f else 1f)
                        touchEnable(!tableAttr.loading)
                    }
                    // pinnedMode 为开时仍可能因单列/缺 width 而无效；以 resolved fixed 为准
                    vif({
                        ctx.viewportWidth > 0f &&
                            (!ctx.pinnedMode || ctx.resolvedColumnLayout().fixed.isEmpty())
                    }) {
                        ctx.renderPlainTableLayout(this, ctx.resolvedColumnLayout())
                    }
                    vif({
                        ctx.viewportWidth > 0f &&
                            ctx.pinnedMode &&
                            ctx.resolvedColumnLayout().fixed.isNotEmpty()
                    }) {
                        ctx.renderPinnedLeftTableLayout(this, ctx.resolvedColumnLayout())
                    }
                }

                ctx.renderStateLayer(this)
            }
        }
    }

    private fun renderTableLayout(container: ViewContainer<*, *>) {
        val layout = resolvedColumnLayout()
        if (layout.fixed.isEmpty()) {
            if (horizontalScrollX != 0f) {
                horizontalScrollX = 0f
            }
            pinnedFixedContentRefs.clear()
            bodyHorizontalScroller = null
            renderPlainTableLayout(container, layout)
        } else {
            renderPinnedLeftTableLayout(container, layout)
        }
    }

    /** 无固定列：横向 Scroller 包表头 + 单 List。 */
    private fun renderPlainTableLayout(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
    ) {
        val ctx = this
        val tableAttr = attr
        container.Scroller {
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
                    ctx.renderHeaderRow(this, layout)
                    ctx.renderHeaderDivider(this)
                }
                ctx.renderBodyRows(this, layout)
            }
        }
    }

    /**
     * 左侧固定列：表头 Fixed|H-Scroller；表体单个 H-Scroller + 纵向 List。
     * 固定列集群用 +scrollX 补偿钉住；表体横滑走原生 Scroller，避免 pan 卡顿。
     */
    private fun renderPinnedLeftTableLayout(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
    ) {
        val ctx = this
        val tableAttr = attr
        val scrollableWidth = max(layout.contentWidth - layout.fixedWidth, 0f)
        if (tableAttr.rowHeight <= 0f) {
            println("[KuiklyTable] fixedFirstColumn expects rowHeight>0; falling back to ${DEFAULT_ROW_HEIGHT_ESTIMATE}dp")
        }
        pinnedFixedClusterWidth = layout.fixedWidth + (frozenDividerStroke()?.width ?: 0f)
        container.View {
            attr {
                flex(1f)
                flexDirectionColumn()
            }
            if (tableAttr.fixedHeader) {
                ctx.renderPinnedHeaderRow(this, layout, scrollableWidth)
                ctx.renderHeaderDivider(this)
            }
            ctx.renderPinnedBodyScroller(this, layout, scrollableWidth)
        }
    }

    private fun renderPinnedHeaderRow(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
        scrollableWidth: Float,
    ) {
        val ctx = this
        container.View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                width(ctx.effectiveViewportWidth())
                height(ctx.attr.headerStyle.height.coerceAtLeast(44f))
                backgroundColor(Color(ctx.attr.themeColors.headerBackground))
            }
            View {
                attr {
                    width(layout.fixedWidth)
                    height(ctx.attr.headerStyle.height.coerceAtLeast(44f))
                    backgroundColor(Color(ctx.attr.themeColors.headerBackground))
                    overflow(true)
                    zIndex(2)
                }
                TableHeaderRowView<T> {
                    attr { ctx.applyHeaderRowAttr(this, layout.fixed) }
                    event { columnClick = { ctx.toggleSort(it) } }
                }
            }
            ctx.renderFrozenColumnDivider(this)
            Scroller {
                ref {
                    @Suppress("UNCHECKED_CAST")
                    ctx.headerHorizontalScroller = (it as ViewRef<ScrollerView<*, *>>).view
                }
                attr {
                    flex(1f)
                    flexDirectionRow()
                    height(ctx.attr.headerStyle.height.coerceAtLeast(44f))
                    bouncesEnable(false)
                    showScrollerIndicator(true)
                }
                event {
                    scroll(sync = true) { params ->
                        ctx.event.overflowTipDismiss?.invoke()
                        ctx.onHeaderHorizontalScroll(params.offsetX)
                    }
                    dragBegin { ctx.event.overflowTipDismiss?.invoke() }
                }
                View {
                    attr {
                        width(scrollableWidth)
                        flexDirectionRow()
                    }
                    TableHeaderRowView<T> {
                        attr { ctx.applyHeaderRowAttr(this, layout.scrollable) }
                        event { columnClick = { ctx.toggleSort(it) } }
                    }
                }
            }
        }
    }

    private fun renderPinnedBodyScroller(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
        scrollableWidth: Float,
    ) {
        val ctx = this
        container.Scroller {
            ref {
                @Suppress("UNCHECKED_CAST")
                ctx.bodyHorizontalScroller = (it as ViewRef<ScrollerView<*, *>>).view
            }
            attr {
                flex(1f)
                flexDirectionRow()
                bouncesEnable(false)
                showScrollerIndicator(false)
            }
            event {
                scroll(sync = true) { params ->
                    ctx.event.overflowTipDismiss?.invoke()
                    ctx.onBodyHorizontalScroll(params.offsetX)
                }
                dragBegin { ctx.event.overflowTipDismiss?.invoke() }
            }
            View {
                attr {
                    width(layout.contentWidth)
                    flex(1f)
                    flexDirectionColumn()
                }
                ctx.renderPinnedBodyList(this, layout, scrollableWidth)
            }
        }
    }

    private fun renderPinnedBodyList(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
        scrollableWidth: Float,
    ) {
        val ctx = this
        val tableAttr = attr
        container.List {
            val listView = this
            ctx.mainBodyList = listView
            attr {
                flex(1f)
                width(layout.contentWidth)
                backgroundColor(Color(tableAttr.themeColors.rowBackground))
            }
            event {
                scroll { params ->
                    ctx.event.overflowTipDismiss?.invoke()
                    ctx.maybeTriggerLoadMore(params)
                }
                dragBegin { ctx.event.overflowTipDismiss?.invoke() }
            }
            if (!tableAttr.fixedHeader) {
                ctx.renderPinnedInListHeaderRow(this, layout, scrollableWidth)
                ctx.renderHeaderDivider(this)
            }
            vif({ ctx.displayRows.isEmpty() }) {
                ctx.renderEmptyPlaceholder(this, width = layout.contentWidth)
            }
            when (val renderMode = tableAttr.rowRenderMode) {
                is TableRowRenderMode.Standard -> {
                    listView.vforIndex({ ctx.displayRows }) { row, _, _ ->
                        View {
                            ctx.renderPinnedBodyRow(this, row, layout, scrollableWidth)
                        }
                    }
                }
                is TableRowRenderMode.Windowed -> {
                    require(!tableAttr.fixedFirstColumn) {
                        "TableRowRenderMode.Windowed does not support fixed columns"
                    }
                    listView.vforLazy({ ctx.displayRows }, renderMode.maxRenderedRows) { row, _, _ ->
                        View {
                            ctx.renderPinnedBodyRow(this, row, layout, scrollableWidth)
                        }
                    }
                }
            }
            ctx.renderLoadMoreFooter(this, width = layout.contentWidth)
        }
    }

    /** 表头随内容纵滚时：与表体同行结构，固定区走 +scrollX，不再嵌套独立 H-Scroller。 */
    private fun renderPinnedInListHeaderRow(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
        scrollableWidth: Float,
    ) {
        val ctx = this
        container.View {
            attr {
                flexDirectionRow()
                width(layout.contentWidth)
                height(ctx.attr.headerStyle.height.coerceAtLeast(44f))
                backgroundColor(Color(ctx.attr.themeColors.headerBackground))
            }
            View {
                ref {
                    ctx.registerPinnedFixedContent(it)
                }
                attr {
                    flexDirectionRow()
                    height(ctx.attr.headerStyle.height.coerceAtLeast(44f))
                    zIndex(2)
                    transform(
                        translate = Translate(
                            percentageX = ctx.pinnedFixedTranslatePercentage(ctx.horizontalScrollX),
                            percentageY = 0f,
                        ),
                    )
                }
                View {
                    attr {
                        width(layout.fixedWidth)
                        height(ctx.attr.headerStyle.height.coerceAtLeast(44f))
                        backgroundColor(Color(ctx.attr.themeColors.headerBackground))
                        overflow(true)
                    }
                    TableHeaderRowView<T> {
                        attr { ctx.applyHeaderRowAttr(this, layout.fixed) }
                        event { columnClick = { ctx.toggleSort(it) } }
                    }
                }
                ctx.renderFrozenColumnDivider(this)
            }
            View {
                attr {
                    width(scrollableWidth)
                    height(ctx.attr.headerStyle.height.coerceAtLeast(44f))
                    flexDirectionRow()
                }
                TableHeaderRowView<T> {
                    attr { ctx.applyHeaderRowAttr(this, layout.scrollable) }
                    event { columnClick = { ctx.toggleSort(it) } }
                }
            }
        }
    }

    private fun renderPinnedBodyRow(
        container: ViewContainer<*, *>,
        row: TableDisplayRow<T>,
        layout: TableResolvedColumnLayout<T>,
        scrollableWidth: Float,
    ) {
        val ctx = this
        container.View {
            attr {
                flexDirectionRow()
                width(layout.contentWidth)
                height(ctx.effectiveRowHeight())
            }
            // 整表横滚后，固定列集群 +scrollX 补偿钉在视口左侧
            View {
                ref {
                    ctx.registerPinnedFixedContent(it)
                }
                attr {
                    flexDirectionRow()
                    height(ctx.effectiveRowHeight())
                    zIndex(2)
                    transform(
                        translate = Translate(
                            percentageX = ctx.pinnedFixedTranslatePercentage(ctx.horizontalScrollX),
                            percentageY = 0f,
                        ),
                    )
                }
                View {
                    attr {
                        width(layout.fixedWidth)
                        height(ctx.effectiveRowHeight())
                        backgroundColor(Color(ctx.rowBackground(row)))
                        overflow(true)
                    }
                    ctx.renderTableRowComponent(this, row, layout, TableColumnRegion.Fixed)
                }
                ctx.renderFrozenColumnDivider(this)
            }
            View {
                attr {
                    width(scrollableWidth)
                    height(ctx.effectiveRowHeight())
                    flexDirectionRow()
                    backgroundColor(Color(ctx.attr.themeColors.rowBackground))
                }
                ctx.renderTableRowComponent(this, row, layout, TableColumnRegion.Scrollable)
            }
        }
        ctx.renderBodyDivider(container)
    }

    private fun renderFrozenColumnDivider(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr {
                val stroke = ctx.frozenDividerStroke()
                width(stroke?.width?.coerceAtLeast(0f) ?: 0f)
                alignSelfStretch()
                backgroundColor(Color(stroke?.color ?: 0x00000000))
            }
        }
    }

    /**
     * 固定列右侧分隔：None 不画；Custom 跟 column；Grid/Horizontal 用 frozenDivider 色。
     */
    private fun frozenDividerStroke(): TableStroke? = when (val mode = attr.lineMode) {
        is TableLineMode.None -> null
        is TableLineMode.Custom -> mode.style.column?.let { column ->
            TableStroke(
                color = attr.themeColors.frozenDividerColor ?: column.color,
                width = column.width,
            )
        }
        else -> TableStroke(attr.themeColors.effectiveFrozenDividerColor(), 1f)
    }

    private fun renderHeaderRow(
        container: ViewContainer<*, *>,
        layout: TableResolvedColumnLayout<T>,
        region: TableColumnRegion = TableColumnRegion.All,
    ) {
        val ctx = this
        val visibleColumns = layout.columnsFor(region)
        container.TableHeaderRowView<T> {
            attr { ctx.applyHeaderRowAttr(this, visibleColumns) }
            event { columnClick = { ctx.toggleSort(it) } }
        }
    }

    private fun applyHeaderRowAttr(
        target: TableHeaderRowAttr<T>,
        columns: List<TableResolvedColumn<T>>,
    ) {
        target.columns = columns
        target.sortState = attr.sortState
        target.indexColumnTitle = attr.indexColumnTitle
        target.lineMode = attr.lineMode
        target.themeColors = attr.themeColors
        target.headerStyle = attr.headerStyle
    }

    private fun renderHeaderDivider(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr {
                val stroke = ctx.attr.lineMode.resolve(ctx.attr.themeColors).header
                height(stroke?.width?.coerceAtLeast(0f) ?: 0f)
                backgroundColor(Color(stroke?.color ?: 0x00000000))
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
            ctx.mainBodyList = listView
            attr {
                flex(1f)
                backgroundColor(Color(tableAttr.themeColors.rowBackground))
            }
            event {
                scroll { params ->
                    ctx.event.overflowTipDismiss?.invoke()
                    ctx.maybeTriggerLoadMore(params)
                }
                dragBegin { ctx.event.overflowTipDismiss?.invoke() }
            }
            if (!tableAttr.fixedHeader) {
                ctx.renderHeaderRow(this, layout, region)
                ctx.renderHeaderDivider(this)
            }
            vif({ ctx.displayRows.isEmpty() }) {
                ctx.renderEmptyPlaceholder(this, width = layout.contentWidth)
            }
            ctx.renderTableRowLoop(listView, layout, region)
            ctx.renderLoadMoreFooter(this, width = layout.contentWidth)
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
                require(!attr.fixedFirstColumn) {
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
        val ctx = this
        container.View {
            attr {
                val stroke = ctx.attr.lineMode.resolve(ctx.attr.themeColors).row
                height(stroke?.width?.coerceAtLeast(0f) ?: 0f)
                backgroundColor(Color(stroke?.color ?: 0x00000000))
            }
        }
    }

    private fun effectiveRowHeight(): Float = when {
        attr.rowHeight > 0f -> attr.rowHeight
        attr.rowRenderMode is TableRowRenderMode.Windowed -> DEFAULT_ROW_HEIGHT_ESTIMATE
        attr.fixedFirstColumn -> DEFAULT_ROW_HEIGHT_ESTIMATE
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
        container.TableRowView<T> {
            attr {
                this.row = row
                columns = visibleColumns
                rowHeight = ctx.effectiveRowHeight()
                zebraStripe = ctx.attr.zebraStripe
                lineMode = ctx.attr.lineMode
                cellPaddingH = ctx.attr.cellPaddingH
                cellPaddingV = ctx.attr.cellPaddingV
                themeColors = ctx.attr.themeColors
                enableOverflowCellClick = ctx.attr.enableOverflowCellClick
                selectedRowKeys = ctx.attr.selectedRowKeys
            }
            event {
                rowClick = { ctx.event.rowClick?.invoke(it) }
                cellClick = { ctx.event.cellClick?.invoke(it) }
                overflowCellClick = { ctx.event.overflowCellClick?.invoke(it) }
            }
        }
    }

    private fun renderStateLayer(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr {
                absolutePositionAllZero()
                zIndex(10)
                visibility(ctx.hasStateLayer())
                touchEnable(ctx.hasStateLayer())
                backgroundColor(Color(ctx.stateLayerBackground()))
            }
            event {
                click { }
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
        fixedColumnSlots = attr.fixedColumnSlots,
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

    private fun hasStateLayer(): Boolean = attr.loading

    private fun stateLayerBackground(): Long = attr.themeColors.stateOverlayBackground

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
        effectiveRowHeight() > 0f -> effectiveRowHeight() + BODY_DIVIDER_HEIGHT
        else -> DEFAULT_ROW_HEIGHT_ESTIMATE + BODY_DIVIDER_HEIGHT
    }

    private fun setVerticalContentOffset(offsetY: Float, animated: Boolean) {
        mainBodyList?.setContentOffset(0f, max(offsetY, 0f), animated)
    }

    private fun registerPinnedFixedContent(ref: ViewRef<*>) {
        pinnedFixedContentRefs[ref.nativeRef] = ref
        ref.view?.let { applyPinnedFixedTransform(it, horizontalScrollX) }
    }

    private fun pinnedFixedTranslatePercentage(offsetX: Float): Float {
        val width = pinnedFixedClusterWidth
        return if (width > 0f) offsetX / width else 0f
    }

    private fun applyPinnedFixedTransform(view: DeclarativeBaseView<*, *>, offsetX: Float) {
        view.getViewAttr().transform(
            translate = Translate(
                percentageX = pinnedFixedTranslatePercentage(offsetX),
                percentageY = 0f,
            ),
        )
    }

    private fun syncPinnedFixedTransforms(offsetX: Float) {
        val iterator = pinnedFixedContentRefs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val view = entry.value.view
            if (view == null) {
                iterator.remove()
                continue
            }
            applyPinnedFixedTransform(view, offsetX)
        }
    }

    private fun commitHorizontalScrollX(offsetX: Float): Boolean {
        val next = max(offsetX, 0f)
        if (kotlin.math.abs(horizontalScrollX - next) <= SCROLL_SYNC_TOLERANCE) return false
        horizontalScrollX = next
        syncPinnedFixedTransforms(next)
        return true
    }

    private fun consumePendingHorizontalScroll(offsetX: Float): Boolean {
        val pendingOffset = pendingHorizontalScrollX
        if (pendingOffset.isNaN()) return false
        val matches = kotlin.math.abs(pendingOffset - offsetX) <= SCROLL_SYNC_TOLERANCE
        if (matches) {
            pendingHorizontalScrollX = Float.NaN
        }
        return matches
    }

    private fun onHeaderHorizontalScroll(offsetX: Float) {
        if (consumePendingHorizontalScroll(offsetX)) return
        if (!commitHorizontalScrollX(offsetX)) return
        pendingHorizontalScrollX = horizontalScrollX
        bodyHorizontalScroller?.setContentOffset(horizontalScrollX, 0f, false)
    }

    private fun onBodyHorizontalScroll(offsetX: Float) {
        if (consumePendingHorizontalScroll(offsetX)) return
        if (!commitHorizontalScrollX(offsetX)) return
        pendingHorizontalScrollX = horizontalScrollX
        headerHorizontalScroller?.setContentOffset(horizontalScrollX, 0f, false)
    }

    private fun syncLoadMoreTriggerState(previousRows: List<TableDisplayRow<T>>, nextRows: List<TableDisplayRow<T>>) {
        if (shouldResetLoadMoreTrigger(previousRows, nextRows)) {
            lastLoadMoreTriggerRowCount = null
        }
    }

    companion object {
        private const val DEFAULT_ROW_HEIGHT_ESTIMATE = 48f
        private const val BODY_DIVIDER_HEIGHT = 1f
        private const val LOAD_MORE_FOOTER_HEIGHT = 44f
        private const val SCROLL_SYNC_TOLERANCE = 0.5f
    }
}

fun <T> ViewContainer<*, *>.TableView(init: TableView<T>.() -> Unit) {
    addChild(TableView<T>(), init)
}
