package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * KuiklyDataTable：在复用 [TableView] 的基础上提供行选择、筛选与客户端分页。
 *
 * 管线：源 data -> filterPredicate -> 单列排序 -> 分页 -> TableView。
 * 选中身份是 rowKey；全选作用域为当前页。
 */
class DataTableView<T> : ComposeView<DataTableAttr<T>, DataTableEvent<T>>() {
    private var tableRef: ViewRef<TableView<T>>? = null
    /** 管线输出的当前页行；用 observable + bindValueChange 保证 pageSize 等变化会推到 TableView。 */
    private var pageItems: List<T> by observable(emptyList())
    /** 最近一次同步的管线结果。渲染路径（分页栏 / 表头复选框）只读它，不在 attr/vif 内重复跑完整管线。 */
    private var syncedPage: DataTablePageResult<T>? by observable(null)
    /** Theme / layout switches remount renderer closures that capture structure-dependent content. */
    private var themeRenderBranch by observable(false)
    private var lastThemeIdentity: Int? = null
    private var lastFixedHeader: Boolean? = null

    override fun createAttr(): DataTableAttr<T> = DataTableAttr()
    override fun createEvent(): DataTableEvent<T> = DataTableEvent()

    fun scrollToTop(animated: Boolean = false) {
        tableRef?.view?.scrollToTop(animated)
    }

    override fun created() {
        bindValueChange(
            valueBlock = {
                // 返回值必须带上 fixedHeader / fixedFirstColumn / theme 等，否则只改布局时 page 相同、valueChange 不触发
                DataTableSyncState(
                    fixedHeader = attr.fixedHeader,
                    fixedFirstColumn = attr.fixedFirstColumn,
                    fixedColumnSlots = if (attr.fixedFirstColumn && attr.enableRowSelection) 2 else if (attr.fixedFirstColumn) 1 else 0,
                    themeIdentity = themeIdentityOf(attr.themeColors),
                    selectedKeys = attr.selectedKeys,
                    enableRowSelection = attr.enableRowSelection,
                    sortState = attr.sortState,
                    page = currentPage(),
                )
            },
            valueChange = { value ->
                @Suppress("UNCHECKED_CAST")
                val state = value as DataTableSyncState<T>
                pageItems = state.page.pageItems
                syncedPage = state.page
                val themeChanged = lastThemeIdentity != null && lastThemeIdentity != state.themeIdentity
                val headerChanged = lastFixedHeader != null && lastFixedHeader != state.fixedHeader
                if (themeChanged || headerChanged) {
                    // 固定表头改动会换布局树，需重建 TableView
                    themeRenderBranch = !themeRenderBranch
                }
                lastThemeIdentity = state.themeIdentity
                lastFixedHeader = state.fixedHeader
                val table = tableRef?.view ?: return@bindValueChange
                table.updateData(state.page.pageItems)
                table.updateFixedFirstColumn(state.fixedFirstColumn, state.fixedColumnSlots)
                table.updateThemeColors(attr.themeColors)
                table.updateSortState(state.sortState)
                table.updateSelectedRowKeys(
                    if (state.enableRowSelection) state.selectedKeys else emptyList(),
                )
            },
        )
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                flexDirectionColumn()
            }
            View {
                attr {
                    flex(1f)
                }
                vif({ ctx.themeRenderBranch }) {
                    ctx.renderTableContent(this)
                }
                vif({ !ctx.themeRenderBranch }) {
                    ctx.renderTableContent(this)
                }
            }
            vif({ ctx.attr.enablePagination }) {
                ctx.renderPaginationBar(this)
            }
        }
    }

    private fun renderTableContent(container: ViewContainer<*, *>) {
        val ctx = this
        vif({ ctx.attr.enableRowSelection }) {
            ctx.renderInnerTable(container, withSelection = true)
        }
        vif({ !ctx.attr.enableRowSelection }) {
            ctx.renderInnerTable(container, withSelection = false)
        }
    }

    /** 渲染路径读取入口：优先读已同步的 page；首次同步前兜底现场计算。 */
    private fun readablePage(): DataTablePageResult<T> = syncedPage ?: currentPage()

    private fun currentPage(): DataTablePageResult<T> {
        val page = DataTablePipeline.buildPage(
            data = attr.data,
            rowKey = attr.rowKey,
            columns = businessColumns(attr),
            sortState = attr.sortState,
            filterPredicate = attr.filterPredicate,
            enablePagination = attr.enablePagination,
            pageIndex = attr.pageIndex,
            pageSize = attr.pageSize,
        )
        if (attr.enablePagination && page.pageIndex != attr.pageIndex) {
            attr.pageIndex = page.pageIndex
        }
        return page
    }

    private fun renderInnerTable(container: ViewContainer<*, *>, withSelection: Boolean) {
        val ctx = this
        container.TableView<T> {
            ref {
                @Suppress("UNCHECKED_CAST")
                ctx.tableRef = it as ViewRef<TableView<T>>
            }
            attr {
                flex(1f)
                applyFrom(ctx.attr)
                fixedColumnSlots = if (ctx.attr.fixedFirstColumn && withSelection) 2 else if (ctx.attr.fixedFirstColumn) 1 else 0
                data = ctx.pageItems
                val nextColumns = mutableListOf<ColumnModel<T>>()
                if (withSelection) {
                    nextColumns.add(ctx.selectionColumn())
                }
                nextColumns.addAll(businessColumns(ctx.attr))
                columns.clear()
                nextColumns.forEach { columns.add(it) }
                selectedRowKeys = if (withSelection) ctx.attr.selectedKeys else emptyList()
            }
            event {
                rowClick = ctx.event.rowClick
                cellClick = { info ->
                    if (withSelection && info.columnKey == DataTableSelection.COLUMN_KEY) {
                        ctx.toggleRow(info.rowData)
                    } else {
                        ctx.event.cellClick?.invoke(info)
                    }
                }
                overflowCellClick = ctx.event.overflowCellClick
                overflowTipDismiss = ctx.event.overflowTipDismiss
                sortChange = { state ->
                    ctx.attr.sortState = state
                    ctx.event.sortChange?.invoke(state)
                }
                loadMore = ctx.event.loadMore
            }
        }
    }

    private fun renderPaginationBar(container: ViewContainer<*, *>) {
        val ctx = this
        val theme = ctx.attr.themeColors
        container.View {
            attr {
                flexDirectionRow()
                flexWrap(FlexWrap.WRAP)
                alignItemsCenter()
                paddingTop(8f)
                paddingBottom(4f)
            }
            Text {
                attr {
                    val page = ctx.readablePage()
                    text("共 ${page.filteredTotal} 条 · ${page.pageIndex + 1}/${page.pageCount} 页")
                    fontSize(12f)
                    color(Color(theme.cellTextSecondary))
                    marginRight(12f)
                    marginBottom(8f)
                }
            }
            PaginationChip(label = { "上一页" }, enabled = { ctx.readablePage().pageIndex > 0 }, theme = theme) {
                ctx.goToPage(ctx.attr.pageIndex - 1)
            }
            PaginationChip(
                label = { "下一页" },
                enabled = {
                    val page = ctx.readablePage()
                    page.pageIndex < page.pageCount - 1
                },
                theme = theme,
            ) {
                ctx.goToPage(ctx.attr.pageIndex + 1)
            }
            listOf(5, 10, 20).forEach { size ->
                PaginationChip(
                    label = { "$size/页" },
                    enabled = { true },
                    active = { ctx.attr.pageSize == size },
                    theme = theme,
                ) {
                    ctx.event.pageSizeChange?.invoke(size)
                    ctx.attr.pageSize = size
                    ctx.goToPage(0)
                }
            }
        }
    }

    private fun goToPage(index: Int) {
        val page = DataTablePipeline.buildPage(
            data = attr.data,
            rowKey = attr.rowKey,
            columns = businessColumns(attr),
            sortState = attr.sortState,
            filterPredicate = attr.filterPredicate,
            enablePagination = attr.enablePagination,
            pageIndex = index,
            pageSize = attr.pageSize,
        )
        attr.pageIndex = page.pageIndex
        event.pageChange?.invoke(page.pageIndex)
        tableRef?.view?.scrollToTop(animated = true)
    }

    private fun toggleRow(item: T) {
        val key = rowKeyOf(item)
        val next = DataTableSelection.toggleKey(attr.selectedKeys, key)
        attr.selectedKeys = next
        event.selectionChange?.invoke(next)
    }

    private fun toggleHeaderSelectAll() {
        val keys = readablePage().pageKeys
        val next = DataTableSelection.toggleSelectAll(keys, attr.selectedKeys)
        attr.selectedKeys = next
        event.selectionChange?.invoke(next)
    }

    private fun rowKeyOf(item: T): Any {
        val keyFn = attr.rowKey
        if (keyFn != null) return keyFn(item)
        val index = attr.data.indexOfFirst { it === item }
        return if (index >= 0) index else item.hashCode()
    }

    private fun headerSelectAllState(): DataTableSelectAllState =
        DataTableSelection.selectAllState(readablePage().pageKeys, attr.selectedKeys)

    private fun selectionColumn(): ColumnModel<T> {
        val ctx = this
        val width = attr.selectionColumnWidth
        return ColumnModel(
            key = DataTableSelection.COLUMN_KEY,
            title = "",
            accessor = { "" },
            width = width,
            enableRowClick = false,
            enableCellClick = true,
            headerRenderer = { _ ->
                View {
                    attr {
                        flex(1f)
                        allCenter()
                    }
                    event {
                        click { ctx.toggleHeaderSelectAll() }
                    }
                    vif({ ctx.headerSelectAllState() == DataTableSelectAllState.All }) {
                        SelectionCheckMark(
                            state = DataTableSelectAllState.All,
                            themeColors = ctx.attr.themeColors,
                        )
                    }
                    vif({ ctx.headerSelectAllState() == DataTableSelectAllState.Partial }) {
                        SelectionCheckMark(
                            state = DataTableSelectAllState.Partial,
                            themeColors = ctx.attr.themeColors,
                        )
                    }
                    vif({ ctx.headerSelectAllState() == DataTableSelectAllState.None }) {
                        SelectionCheckMark(
                            state = DataTableSelectAllState.None,
                            themeColors = ctx.attr.themeColors,
                        )
                    }
                }
            },
            cellRenderer = { item, _ ->
                val key = ctx.rowKeyOf(item)
                View {
                    attr {
                        flex(1f)
                        allCenter()
                    }
                    vif({ DataTableSelection.isSelected(ctx.attr.selectedKeys, key) }) {
                        SelectionCheckMark(
                            state = DataTableSelectAllState.All,
                            themeColors = ctx.attr.themeColors,
                        )
                    }
                    vif({ !DataTableSelection.isSelected(ctx.attr.selectedKeys, key) }) {
                        SelectionCheckMark(
                            state = DataTableSelectAllState.None,
                            themeColors = ctx.attr.themeColors,
                        )
                    }
                }
            },
        )
    }

    companion object {
        private fun <T> businessColumns(attr: DataTableAttr<T>): List<ColumnModel<T>> =
            attr.columns.filter { it.key != DataTableSelection.COLUMN_KEY }
    }
}

class DataTableAttr<T> : TableAttr<T>() {
    /** 开启后注入选择列并支持行高亮；关闭后无选择列、无选中高亮。 */
    var enableRowSelection: Boolean by observable(false)
    /** 受控选中 rowKey 列表；排序/分页不改写身份，只改变展示顺序与可见集。 */
    var selectedKeys: List<Any> by observable(emptyList())
    var selectionColumnWidth: Float by observable(DataTableSelection.DEFAULT_COLUMN_WIDTH)
    var filterPredicate: ((T) -> Boolean)? by observable(null)
    var enablePagination: Boolean by observable(false)
    var pageIndex: Int by observable(0)
    var pageSize: Int by observable(10)
}

class DataTableEvent<T> : TableEvent<T>() {
    var selectionChange: ((List<Any>) -> Unit)? = null
    var pageChange: ((Int) -> Unit)? = null
    var pageSizeChange: ((Int) -> Unit)? = null
}

fun <T> ViewContainer<*, *>.DataTableView(init: DataTableView<T>.() -> Unit) {
    addChild(DataTableView(), init)
}

/** bindValueChange 票据：固定列/表头/主题变化时即使分页结果相同也要触发同步。 */
private data class DataTableSyncState<T>(
    val fixedHeader: Boolean,
    val fixedFirstColumn: Boolean,
    val fixedColumnSlots: Int,
    val themeIdentity: Int,
    val selectedKeys: List<Any>,
    val enableRowSelection: Boolean,
    val sortState: TableSortState,
    val page: DataTablePageResult<T>,
)

/** KMP 可用的主题指纹；避免 JVM-only 的 System.identityHashCode。 */
private fun themeIdentityOf(theme: TableThemeColors): Int {
    var h = theme.headerBackground.hashCode()
    h = 31 * h + theme.headerText.hashCode()
    h = 31 * h + theme.cellText.hashCode()
    h = 31 * h + theme.gridLine.hashCode()
    h = 31 * h + theme.rowBackground.hashCode()
    h = 31 * h + theme.rowBackgroundAlt.hashCode()
    h = 31 * h + theme.selectedRowBackground.hashCode()
    h = 31 * h + theme.actionText.hashCode()
    h = 31 * h + (theme.dividerColor?.hashCode() ?: 0)
    return h
}

private fun <T> TableAttr<T>.applyFrom(source: DataTableAttr<T>) {
    tableWidth = source.tableWidth
    rowKey = source.rowKey
    zebraStripe = source.zebraStripe
    lineMode = source.lineMode
    cornerRadius = source.cornerRadius
    cellPaddingH = source.cellPaddingH
    cellPaddingV = source.cellPaddingV
    rowHeight = source.rowHeight
    themeColors = source.themeColors
    headerStyle = source.headerStyle
    sortState = source.sortState
    autoIndexColumn = source.autoIndexColumn
    indexColumnTitle = source.indexColumnTitle
    indexColumnWidth = source.indexColumnWidth
    fixedHeader = source.fixedHeader
    fixedFirstColumn = source.fixedFirstColumn
    fixedColumnSlots = if (source.fixedFirstColumn && source.enableRowSelection) 2 else if (source.fixedFirstColumn) 1 else 0
    displayMode = source.displayMode
    rowRenderMode = source.rowRenderMode
    listPrimaryColumnKey = source.listPrimaryColumnKey
    listStatusColumnKey = source.listStatusColumnKey
    listStatusTagPresetByText = source.listStatusTagPresetByText
    listStatusTagStyleByText = source.listStatusTagStyleByText
    listStatusTagStyleResolver = source.listStatusTagStyleResolver
    loading = source.loading
    emptyText = source.emptyText
    loadingText = source.loadingText
    emptyRenderer = source.emptyRenderer
    loadingRenderer = source.loadingRenderer
    hasMore = source.hasMore
    loadingMore = source.loadingMore
    loadMoreThresholdRows = source.loadMoreThresholdRows
    enableOverflowCellClick = source.enableOverflowCellClick
}

private fun ViewContainer<*, *>.PaginationChip(
    label: () -> String,
    enabled: () -> Boolean,
    theme: TableThemeColors,
    active: () -> Boolean = { false },
    onClick: () -> Unit,
) {
    View {
        attr {
            height(44f)
            alignItemsCenter()
            justifyContentCenter()
            paddingLeft(12f)
            paddingRight(12f)
            marginRight(8f)
            marginBottom(8f)
            borderRadius(16f)
            opacity(if (enabled()) 1f else 0.4f)
            backgroundColor(
                Color(
                    when {
                        active() -> theme.rowBackgroundAlt
                        else -> theme.cardBackground
                    },
                ),
            )
            border(
                Border(
                    1f,
                    BorderStyle.SOLID,
                    Color(if (active()) theme.actionText else theme.cardBorder),
                ),
            )
        }
        Text {
            attr {
                text(label())
                fontSize(12f)
                color(Color(if (active()) theme.actionText else theme.cellText))
            }
        }
        event {
            click {
                if (enabled()) onClick()
            }
        }
    }
}

private fun ViewContainer<*, *>.SelectionCheckMark(
    state: DataTableSelectAllState,
    themeColors: TableThemeColors,
) {
    addChild(SelectionCheckMarkView(), {
        attr {
            this.state = state
            this.themeColors = themeColors
        }
    })
}

private class SelectionCheckMarkView : ComposeView<SelectionCheckMarkAttr, ComposeEvent>() {
    override fun createAttr(): SelectionCheckMarkAttr = SelectionCheckMarkAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            val state = ctx.attr.state
            val themeColors = ctx.attr.themeColors
            val checked = state == DataTableSelectAllState.All
            val partial = state == DataTableSelectAllState.Partial
            val borderColor = if (checked || partial) {
                themeColors.actionText
            } else {
                themeColors.gridLine
            }
            val fill = when {
                checked -> themeColors.actionText
                partial -> themeColors.statusTagInfoBackground
                else -> themeColors.selectionControlBackground
            }
            View {
                attr {
                    width(18f)
                    height(18f)
                    borderRadius(3f)
                    border(Border(1.5f, BorderStyle.SOLID, Color(borderColor)))
                    backgroundColor(Color(fill))
                    allCenter()
                }
                Text {
                    attr {
                        text(
                            when (state) {
                                DataTableSelectAllState.All -> "✓"
                                DataTableSelectAllState.Partial -> "−"
                                DataTableSelectAllState.None -> ""
                            },
                        )
                        fontSize(if (partial) 14f else 12f)
                        color(Color(if (checked) themeColors.actionTextOnFill else themeColors.actionText))
                        textAlignCenter()
                    }
                }
            }
        }
    }
}

private class SelectionCheckMarkAttr : ComposeAttr() {
    var state: DataTableSelectAllState by observable(DataTableSelectAllState.None)
    var themeColors: TableThemeColors by observable(TableThemeColors.Light)
}
