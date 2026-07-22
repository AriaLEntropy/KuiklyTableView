package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import kotlin.math.max
import kotlin.math.min

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

    private var overflowPopupVisible by observable(false)
    private var overflowPopupText by observable("")

    override fun didRemoveFromParentView() {
        closeOverflowPopup()
        super.didRemoveFromParentView()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val tableAttr = ctx.attr
        return {
            View {
                attr {
                    flex(1f)
                    positionRelative()
                    backgroundColor(Color(tableAttr.themeColors.rowBackground))
                }

                View {
                    attr {
                        absolutePositionAllZero()
                        opacity(if (tableAttr.loading) 0.4f else 1f)
                        touchEnable(!tableAttr.loading && tableAttr.errorText == null)
                    }

                    View {
                        attr {
                            absolutePositionAllZero()
                            visibility(!ctx.shouldRenderMobileList())
                            touchEnable(!ctx.shouldRenderMobileList())
                        }
                        ctx.renderTableLayout(this)
                    }

                    View {
                        attr {
                            absolutePositionAllZero()
                            visibility(ctx.shouldRenderMobileList())
                            touchEnable(ctx.shouldRenderMobileList())
                        }
                        ctx.renderMobileListLayout(this)
                    }
                }

                ctx.renderStateLayer(this)
                ctx.renderOverflowPopupLayer(this)
            }
        }
    }

    private fun renderTableLayout(container: ViewContainer<*, *>) {
        val ctx = this
        container.Scroller {
            attr {
                flex(1f)
                flexDirectionRow()
            }
            event {
                scroll { ctx.closeOverflowPopup() }
                dragBegin { ctx.closeOverflowPopup() }
            }
            View {
                attr {
                    width(ctx.contentWidth())
                }

                ctx.renderHeaderRow(this)
                ctx.renderHeaderDivider(this)
                ctx.renderBodyRows(this)
            }
        }
    }

    private fun renderHeaderRow(container: ViewContainer<*, *>) {
        val tableAttr = attr
        container.View {
            attr {
                flexDirectionRow()
                backgroundColor(Color(tableAttr.themeColors.headerBackground))
                if (tableAttr.headerStyle.height > 0f) {
                    height(tableAttr.headerStyle.height)
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
                            paddingLeft(tableAttr.headerStyle.paddingH)
                            paddingRight(tableAttr.headerStyle.paddingH)
                            paddingTop(tableAttr.headerStyle.paddingV)
                            paddingBottom(tableAttr.headerStyle.paddingV)
                        }
                        if (column.headerRenderer != null) {
                            column.headerRenderer.invoke(this, column)
                        } else {
                            Text {
                                attr {
                                    flex(1f)
                                    text(column.title)
                                    fontSize(tableAttr.headerStyle.fontSize)
                                    when (tableAttr.headerStyle.fontWeight) {
                                        is TableHeaderFontWeight.Normal -> fontWeightNormal()
                                        is TableHeaderFontWeight.Medium -> fontWeightMedium()
                                        is TableHeaderFontWeight.Semisolid -> fontWeightSemisolid()
                                        is TableHeaderFontWeight.Bold -> fontWeightBold()
                                    }
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

    private fun renderBodyRows(container: ViewContainer<*, *>) {
        val ctx = this
        val tableAttr = attr
        container.List {
            attr {
                flex(1f)
                backgroundColor(Color(tableAttr.themeColors.rowBackground))
            }
            event {
                scroll { ctx.closeOverflowPopup() }
                dragBegin { ctx.closeOverflowPopup() }
            }
            tableAttr.data.forEachIndexed { index, item ->
                ctx.renderTableRow(this, item, index)
                View {
                    attr {
                        height(1f)
                        backgroundColor(Color(tableAttr.themeColors.gridLine))
                    }
                }
            }
        }
    }

    private fun renderTableRow(container: ViewContainer<*, *>, item: T, index: Int) {
        val ctx = this
        val tableAttr = attr
        container.View {
            attr {
                flexDirectionRow()
                backgroundColor(
                    Color(
                        if (tableAttr.zebraStripe && index % 2 == 1) {
                            tableAttr.themeColors.rowBackgroundAlt
                        } else {
                            tableAttr.themeColors.rowBackground
                        }
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
                val cellText = column.accessor(item)
                val isDefaultText = column.cellRenderer == null
                val isTruncatedText = isDefaultText && ctx.isDefaultCellTextTruncated(cellText, column)
                View {
                    attr {
                        if (column.width != null) {
                            width(column.width)
                        } else {
                            flex(column.flex)
                        }
                        flexDirectionRow()
                    }
                    if (isDefaultText) {
                        event {
                            click {
                                if (isTruncatedText) {
                                    ctx.showOverflowPopup(cellText)
                                } else {
                                    ctx.event.rowClick?.invoke(item)
                                }
                            }
                        }
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
                        if (column.cellRenderer != null) {
                            View {
                                attr {
                                    flex(1f)
                                    flexDirectionRow()
                                }
                                column.cellRenderer.invoke(this, item, column)
                            }
                        } else {
                            Text {
                                attr {
                                    flex(1f)
                                    text(cellText)
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
    }

    private fun renderMobileListLayout(container: ViewContainer<*, *>) {
        val ctx = this
        val tableAttr = attr
        container.List {
            attr {
                flex(1f)
                backgroundColor(Color(tableAttr.themeColors.rowBackgroundAlt))
                paddingTop(8f)
                paddingBottom(8f)
            }
            event {
                scroll { ctx.closeOverflowPopup() }
                dragBegin { ctx.closeOverflowPopup() }
            }
            tableAttr.data.forEach { item ->
                ctx.renderMobileCard(this, item)
            }
        }
    }

    private fun renderMobileCard(container: ViewContainer<*, *>, item: T) {
        val ctx = this
        val tableAttr = attr
        val primaryColumn = primaryMobileColumn() ?: return
        val statusColumn = statusMobileColumn()
        container.View {
            attr {
                marginLeft(8f)
                marginRight(8f)
                marginBottom(8f)
                paddingLeft(16f)
                paddingRight(16f)
                paddingTop(12f)
                paddingBottom(12f)
                borderRadius(8f)
                backgroundColor(Color(tableAttr.themeColors.cardBackground))
                border(Border(1f, BorderStyle.SOLID, Color(tableAttr.themeColors.cardBorder)))
            }
            event {
                click {
                    ctx.event.rowClick?.invoke(item)
                }
            }

            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        flex(1f)
                        text(primaryColumn.accessor(item))
                        fontSize(16f)
                        fontWeightSemisolid()
                        color(Color(tableAttr.themeColors.cellText))
                        lines(1)
                        textOverFlowTail()
                    }
                }
                if (statusColumn != null) {
                    View {
                        attr {
                            marginLeft(8f)
                            paddingLeft(8f)
                            paddingRight(8f)
                            paddingTop(3f)
                            paddingBottom(3f)
                            borderRadius(10f)
                            backgroundColor(Color(tableAttr.themeColors.statusTagBackground))
                        }
                        Text {
                            attr {
                                text(statusColumn.accessor(item))
                                fontSize(12f)
                                color(Color(tableAttr.themeColors.statusTagText))
                                lines(1)
                                textOverFlowTail()
                            }
                        }
                    }
                }
            }

            View {
                attr {
                    height(1f)
                    marginTop(10f)
                    marginBottom(6f)
                    backgroundColor(Color(tableAttr.themeColors.gridLine))
                }
            }

            tableAttr.columns.forEach { column ->
                if (column !== primaryColumn && column !== statusColumn) {
                    ctx.renderMobileFieldRow(this, item, column)
                }
            }
        }
    }

    private fun renderMobileFieldRow(
        container: ViewContainer<*, *>,
        item: T,
        column: ColumnModel<T>,
    ) {
        val ctx = this
        val tableAttr = attr
        val fieldText = column.accessor(item)
        val isDefaultText = column.cellRenderer == null
        val isTruncatedText = isDefaultText && ctx.isMobileFieldTextTruncated(fieldText)
        container.View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                paddingTop(4f)
                paddingBottom(4f)
            }
            if (isDefaultText) {
                event {
                    click {
                        if (isTruncatedText) {
                            ctx.showOverflowPopup(fieldText)
                        } else {
                            ctx.event.rowClick?.invoke(item)
                        }
                    }
                }
            }
            Text {
                attr {
                    width(86f)
                    text(column.title)
                    fontSize(13f)
                    color(Color(tableAttr.themeColors.cellTextSecondary))
                    lines(1)
                    textOverFlowTail()
                }
            }
            Text {
                attr {
                    flex(1f)
                    text(fieldText)
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
                    // Consume state-layer taps so Loading/Empty/Error never leak rowClick to rows below.
                }
            }

            ctx.renderLoadingState(this)
            ctx.renderEmptyState(this)
            ctx.renderErrorState(this)
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

    private fun renderEmptyState(container: ViewContainer<*, *>) {
        val ctx = this
        val tableAttr = attr
        container.View {
            attr {
                absolutePositionAllZero()
                allCenter()
                visibility(ctx.shouldShowEmptyState())
            }
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

    private fun renderErrorState(container: ViewContainer<*, *>) {
        val ctx = this
        val tableAttr = attr
        container.View {
            attr {
                absolutePositionAllZero()
                allCenter()
                visibility(ctx.shouldShowErrorState())
            }
            Text {
                attr {
                    text("!")
                    fontSize(28f)
                    fontWeightBold()
                    color(Color(tableAttr.themeColors.errorText))
                    marginBottom(8f)
                }
            }
            Text {
                attr {
                    text(tableAttr.errorText ?: "")
                    fontSize(14f)
                    color(Color(tableAttr.themeColors.stateText))
                    marginBottom(12f)
                }
            }
            View {
                attr {
                    visibility(ctx.event.retry != null)
                    touchEnable(ctx.event.retry != null)
                    paddingLeft(16f)
                    paddingRight(16f)
                    paddingTop(8f)
                    paddingBottom(8f)
                    borderRadius(16f)
                    backgroundColor(Color(tableAttr.themeColors.actionText))
                }
                Text {
                    attr {
                        text(tableAttr.retryText)
                        fontSize(13f)
                        fontWeightMedium()
                        color(Color(tableAttr.themeColors.actionTextOnFill))
                    }
                }
                event {
                    click {
                        ctx.event.retry?.invoke()
                    }
                }
            }
        }
    }

    private fun renderOverflowPopupLayer(container: ViewContainer<*, *>) {
        val ctx = this
        val tableAttr = attr
        container.View {
            attr {
                absolutePositionAllZero()
                zIndex(20)
                allCenter()
                visibility(ctx.shouldShowOverflowPopup())
                touchEnable(ctx.shouldShowOverflowPopup())
            }
            event {
                click { ctx.closeOverflowPopup() }
            }

            View {
                attr {
                    absolutePositionAllZero()
                    zIndex(20)
                    backgroundColor(Color(tableAttr.themeColors.popupScrim))
                    touchEnable(ctx.shouldShowOverflowPopup())
                }
                event {
                    click { ctx.closeOverflowPopup() }
                }
            }

            View {
                attr {
                    zIndex(21)
                    width(ctx.popupWidth())
                    paddingLeft(16f)
                    paddingRight(16f)
                    paddingTop(14f)
                    paddingBottom(14f)
                    borderRadius(8f)
                    backgroundColor(Color(tableAttr.themeColors.popupBackground))
                    border(Border(1f, BorderStyle.SOLID, Color(tableAttr.themeColors.popupBorder)))
                }
                event {
                    click {
                        // Consume taps inside the popup so only outside taps close it.
                    }
                }
                Text {
                    attr {
                        text(ctx.overflowPopupText)
                        fontSize(14f)
                        color(Color(tableAttr.themeColors.cellText))
                    }
                }
            }
        }
    }

    private fun showOverflowPopup(text: String) {
        if (!attr.enableOverflowPopup) {
            return
        }
        overflowPopupText = text
        overflowPopupVisible = true
    }

    private fun closeOverflowPopup() {
        if (overflowPopupVisible) {
            overflowPopupVisible = false
            overflowPopupText = ""
        }
    }

    private fun isDefaultCellTextTruncated(text: String, column: ColumnModel<T>): Boolean {
        if (!attr.enableOverflowPopup || text.isEmpty()) {
            return false
        }
        val availableWidth = max(columnRenderWidth(column) - attr.cellPaddingH * 2f, 0f)
        return estimatedTextWidth(text, DEFAULT_CELL_FONT_SIZE) > availableWidth
    }

    private fun isMobileFieldTextTruncated(text: String): Boolean {
        if (!attr.enableOverflowPopup || text.isEmpty()) {
            return false
        }
        val availableWidth = max(
            pagerData.pageViewWidth -
                MOBILE_CARD_HORIZONTAL_MARGIN * 2f -
                MOBILE_CARD_HORIZONTAL_PADDING * 2f -
                MOBILE_FIELD_LABEL_WIDTH,
            0f,
        )
        return estimatedTextWidth(text, DEFAULT_CELL_FONT_SIZE) > availableWidth
    }

    private fun shouldShowOverflowPopup(): Boolean =
        attr.enableOverflowPopup && overflowPopupVisible

    private fun columnRenderWidth(column: ColumnModel<T>): Float {
        column.width?.let { return it }
        val fixedTotal = attr.columns.sumOf { it.width?.toDouble() ?: 0.0 }.toFloat()
        val flexTotal = attr.columns
            .filter { it.width == null }
            .sumOf { it.flex.toDouble() }
            .toFloat()
        if (flexTotal <= 0f) {
            return MIN_FLEX_COLUMN_WIDTH
        }
        val flexSpace = max(contentWidth() - fixedTotal, attr.columns.count { it.width == null } * MIN_FLEX_COLUMN_WIDTH)
        return flexSpace * column.flex / flexTotal
    }

    private fun estimatedTextWidth(text: String, fontSize: Float): Float =
        text.sumOf { ch ->
            val width = if (ch.code > ASCII_MAX_CODE) fontSize else fontSize * ASCII_CHAR_WIDTH_RATIO
            width.toDouble()
        }.toFloat()

    private fun popupWidth(): Float {
        val pageWidth = pagerData.pageViewWidth
        if (pageWidth <= POPUP_HORIZONTAL_MARGIN * 2f) {
            return POPUP_DEFAULT_WIDTH
        }
        return min(pageWidth - POPUP_HORIZONTAL_MARGIN * 2f, POPUP_MAX_WIDTH)
    }

    /** 表格内容总宽：固定列宽之和 + 弹性列最小宽，与页面宽取较大者（小于页宽时弹性列撑满） */
    private fun contentWidth(): Float {
        val pageWidth = pagerData.pageViewWidth
        val fixedTotal = attr.columns.sumOf { it.width?.toDouble() ?: 0.0 }.toFloat()
        val flexCount = attr.columns.count { it.width == null }
        val naturalWidth = fixedTotal + flexCount * MIN_FLEX_COLUMN_WIDTH
        return if (naturalWidth > pageWidth) naturalWidth else pageWidth
    }

    private fun shouldRenderMobileList(): Boolean =
        when (attr.mobileMode) {
            is TableMobileMode.Auto -> attr.columns.size <= MOBILE_LIST_AUTO_MAX_COLUMNS
            is TableMobileMode.List -> true
            is TableMobileMode.Table -> false
        }

    private fun hasStateLayer(): Boolean =
        attr.loading || attr.errorText != null || attr.data.isEmpty()

    private fun shouldShowEmptyState(): Boolean =
        !attr.loading && attr.errorText == null && attr.data.isEmpty()

    private fun shouldShowErrorState(): Boolean =
        !attr.loading && attr.errorText != null

    private fun stateLayerBackground(): Long =
        if (attr.loading) attr.themeColors.stateOverlayBackground else attr.themeColors.rowBackground

    private fun primaryMobileColumn(): ColumnModel<T>? =
        attr.mobilePrimaryColumnKey?.let { key ->
            attr.columns.firstOrNull { it.key == key }
        } ?: attr.columns.firstOrNull()

    private fun statusMobileColumn(): ColumnModel<T>? =
        attr.mobileStatusColumnKey?.let { key ->
            attr.columns.firstOrNull { it.key == key }
        }

    companion object {
        /** 弹性列最小宽（表格超宽需要横向滚动时，弹性列至少有这么宽） */
        private const val MIN_FLEX_COLUMN_WIDTH = 100f
        private const val MOBILE_LIST_AUTO_MAX_COLUMNS = 3
        private const val DEFAULT_CELL_FONT_SIZE = 14f
        private const val ASCII_CHAR_WIDTH_RATIO = 0.58f
        private const val ASCII_MAX_CODE = 255
        private const val MOBILE_CARD_HORIZONTAL_MARGIN = 8f
        private const val MOBILE_CARD_HORIZONTAL_PADDING = 16f
        private const val MOBILE_FIELD_LABEL_WIDTH = 86f
        private const val POPUP_HORIZONTAL_MARGIN = 24f
        private const val POPUP_DEFAULT_WIDTH = 320f
        private const val POPUP_MAX_WIDTH = 420f
    }
}

/**
 * 移动端展示模式。
 */
sealed class TableMobileMode {
    /** 列数 <= 3 时转译为 Mobile List，否则保留横向表格。 */
    object Auto : TableMobileMode()

    /** 强制保留横向表格。 */
    object Table : TableMobileMode()

    /** 强制转译为 Mobile List 卡片。 */
    object List : TableMobileMode()
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

    /** 表头结构样式；品牌主题不在此处固化。 */
    var headerStyle: TableHeaderStyle by observable(TableHeaderStyle.Default)

    /** 移动端展示模式；Auto 下列数 <= 3 转译为 Mobile List。 */
    var mobileMode: TableMobileMode by observable(TableMobileMode.Auto)

    /** Mobile List 主字段列 key；未配置时使用第一列。 */
    var mobilePrimaryColumnKey: String? by observable(null)

    /** Mobile List 状态标签列 key；未配置时不显示状态标签。 */
    var mobileStatusColumnKey: String? by observable(null)

    /** Loading 状态；为 true 时保留旧内容并降低透明度。 */
    var loading: Boolean by observable(false)

    /** Error 状态文案；非 null 时显示错误层。 */
    var errorText: String? by observable(null)

    /** Empty 状态文案。 */
    var emptyText: String by observable("暂无数据")

    /** Loading 状态文案。 */
    var loadingText: String by observable("加载中…")

    /** Retry 按钮文案。 */
    var retryText: String by observable("重试")

    /** 是否启用默认文本单元格的截断全文浮层；自定义 renderer 不受此开关接管。 */
    var enableOverflowPopup: Boolean by observable(true)
}

/**
 * TableView 事件
 */
class TableEvent<T> : ComposeEvent() {
    /** 行点击回调 */
    var rowClick: ((T) -> Unit)? = null

    /** 错误状态重试回调 */
    var retry: (() -> Unit)? = null
}

/**
 * DSL 入口：在任意 ViewContainer 中使用 TableView
 */
fun <T> ViewContainer<*, *>.TableView(init: TableView<T>.() -> Unit) {
    addChild(TableView<T>(), init)
}
