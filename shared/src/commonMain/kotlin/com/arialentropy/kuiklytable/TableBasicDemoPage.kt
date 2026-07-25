package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.arialentropy.kuiklytable.base.BasePager
import com.arialentropy.kuiklytable.base.bridgeModule
import kotlin.math.max
import kotlin.math.min

/**
 * KuiklyTable 组件展示 Demo
 *
 * 验证 ST-1 + ST-2：列定义/行列渲染/对齐/斑马纹（ST-1），
 * 横纵双向滚动/固定表头/边框/内边距/行高配置（ST-2）。
 * 配置面板用可点击选项（observable + click，参照 Kuikly 官方 AppSettingPage 模式）。
 */
@Page("table_basic", supportInLocal = true)
internal class TableBasicDemoPage : BasePager() {

    data class User(
        val id: Int,
        val name: String,
        val age: Int,
        val email: String,
        val city: String,
        val department: String,
        val position: String,
        val hireDate: String,
        val salary: String,
        val status: String,
    )

    // 20 行数据，足够触发纵向滚动（验证固定表头）
    private val users = (1..20).map { i ->
        User(
            id = i,
            name = "员工$i",
            age = 20 + (i * 3) % 40,
            email = "employee$i.long.mailbox@example-company.internal",
            city = listOf("北京", "上海", "广州", "深圳", "杭州")[i % 5],
            department = listOf("技术部", "产品部", "设计部", "运营部")[i % 4],
            position = listOf("工程师", "产品经理", "设计师", "运营专员")[i % 4],
            hireDate = "202${i % 5}-0${i % 9 + 1}-1${i % 9}",
            salary = "${10 + i}k",
            status = if (i % 3 == 0) "离职" else if (i % 2 == 0) "休假" else "在职",
        )
    }

    // 年龄列：3 列和 8 列模式共用，对齐可配置（响应式）
    private val ageColumn = ColumnModel<User>(
        key = "age",
        title = "年龄",
        accessor = { it.age.toString() },
        width = 60f,
        alignment = ColumnAlignment.End,
    )

    private val nameColumn = ColumnModel<User>(key = "name", title = "姓名", accessor = { it.name }, width = 80f)
    private val emailColumn = ColumnModel<User>(key = "email", title = "邮箱", accessor = { it.email })
    private val wideEmailColumn = ColumnModel<User>(
        key = "email",
        title = "邮箱",
        accessor = { it.email },
        width = 150f,
    )
    private val statusTextColumn = ColumnModel<User>(
        key = "status",
        title = "状态",
        accessor = { it.status },
        width = 90f,
    )
    private val statusRendererColumn = ColumnModel<User>(
        key = "status",
        title = "状态",
        accessor = { it.status },
        width = 90f,
        cellRenderer = {
            user, _ ->
            val tagStyle = this@TableBasicDemoPage.resolveStatusTagStyle(user.status)
            View {
                attr {
                    flex(1f)
                    flexDirectionRow()
                    alignItemsCenter()
                    justifyContentCenter()
                }
                View {
                    attr {
                        height(22f)
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentCenter()
                        backgroundColor(Color(tagStyle.background))
                        borderRadius(11f)
                        paddingLeft(8f)
                        paddingRight(8f)
                    }
                    Text {
                        attr {
                            height(22f)
                            text(user.status)
                            fontSize(12f)
                            lineHeight(22f)
                            color(Color(tagStyle.text))
                            lines(1)
                            textOverFlowTail()
                        }
                    }
                }
            }
        },
        headerRenderer = { column ->
            Text {
                attr {
                    flex(1f)
                    text("状态列")
                    fontSize(14f)
                    fontWeightBold()
                     color(Color(this@TableBasicDemoPage.currentTheme().actionText))
                    lines(1)
                    textOverFlowTail()
                }
            }
        },
    )

    // 3 列模式（fits 页面，无横向滚动）
    private val columns3 = listOf(nameColumn, ageColumn, emailColumn)

    // 5 列模式（总宽超页面 → 横向滚动）
    private val cityColumn = ColumnModel<User>(key = "city", title = "城市", accessor = { it.city }, width = 100f)

    private val avatarColumn = ColumnModel<User>(
        key = "avatar",
        title = "头像",
        accessor = { it.name },
        width = 60f,
        cellRenderer = { user, _ ->
            View {
                attr {
                    flex(1f)
                    alignItemsCenter()
                    justifyContentCenter()
                }
                View {
                    attr {
                        size(32f, 32f)
                        borderRadius(16f)
                         backgroundColor(Color(this@TableBasicDemoPage.currentTheme().actionText))
                        allCenter()
                    }
                    Text {
                        attr {
                            text(user.name.take(1))
                            fontSize(14f)
                             color(Color(this@TableBasicDemoPage.currentTheme().actionTextOnFill))
                            fontWeightBold()
                        }
                    }
                }
            }
        },
    )

    private val notifyColumn = ColumnModel<User>(
        key = "notify",
        title = "通知",
        accessor = { "开" },
        width = 70f,
        cellRenderer = { user, _ ->
            View {
                attr {
                    flex(1f)
                    alignItemsCenter()
                    justifyContentCenter()
                }
                Switch {
                    attr {
                        size(40f, 24f)
                        isOn(this@TableBasicDemoPage.notifyEnabled(user))
                         onColor(Color(this@TableBasicDemoPage.currentTheme().actionText))
                         unOnColor(Color(this@TableBasicDemoPage.currentTheme().gridLine))
                    }
                    event {
                        switchOnChanged { on ->
                            this@TableBasicDemoPage.setNotifyEnabled(user, on)
                        }
                    }
                }
            }
        },
    )

    // ===== 可配置状态（observable，变化触发表格重渲染）=====
    private var wideTable by observable(true)          // 3列 / 5列 / 7列（横向滚动）
    private var rendererExampleColumns by observable(false)
    private var activeColumns: ObservableList<ColumnModel<User>> by observableList()
    private var selectedColumn by observable<ColumnModel<User>>(ageColumn)
    private var zebraOn by observable(true)             // 斑马纹
    private var borderedOn by observable(false)         // 列边框
    private var compactPadding by observable(false)     // 紧凑内边距
    private var fixedRowHeight by observable(false)     // 固定行高
    private var themeMode: DemoThemeMode by observable(DemoThemeMode.Light)
    private var compactHeader by observable(false)
    private var customStatusRendererOn by observable(true)
    private var mobileMode: TableMobileMode by observable(TableMobileMode.Table)
    private var tableState by observable("正常")
    private var overflowTipOn by observable(true)
    private var notifyStateById: Map<Int, Boolean> by observable(users.associate { it.id to (it.name.hashCode() % 2 == 0) })
    private var overflowTipVisible by observable(false)
    private var overflowTipText by observable("")
    private var overflowTipLeft by observable(24f)
    private var overflowTipTop by observable(220f)
    private var overflowTipArrowLeft by observable(40f)
    private var activeExample by observable("宽表滚动")

    init {
        activeColumns.addAll(currentColumns())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(ctx.currentTheme().rowBackground))
            }

            // 页面标题
            Text {
                attr {
                    text("KuiklyTable 组件展示")
                    fontSize(18f)
                    fontWeightSemiBold()
                    color(Color(ctx.currentTheme().cellText))
                    margin(16f)
                    marginBottom(8f)
                }
            }

            // ===== 配置面板 =====
            Scroller {
                attr {
                    marginLeft(16f)
                    marginRight(16f)
                    marginBottom(12f)
                    height(300f)
                }

                ConfigGroup("演示场景", first = true, theme = { ctx.currentTheme() }) {
                    Text {
                        attr {
                            text(ctx.exampleDescription())
                            fontSize(12f)
                            color(Color(ctx.currentTheme().cellTextSecondary))
                            marginBottom(8f)
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(label = { "基础表格" }, active = { ctx.activeExample == "基础表格" }) {
                            ctx.selectExample("基础表格")
                        }
                        ToggleChip(label = { "宽表滚动" }, active = { ctx.activeExample == "宽表滚动" }) {
                            ctx.selectExample("宽表滚动")
                        }
                        ToggleChip(label = { "Renderer 插槽" }, active = { ctx.activeExample == "Renderer 插槽" }) {
                            ctx.selectExample("Renderer 插槽")
                        }
                        ToggleChip(label = { "Mobile List" }, active = { ctx.activeExample == "Mobile List" }) {
                            ctx.selectExample("Mobile List")
                        }
                        ToggleChip(label = { "无数据" }, active = { ctx.activeExample == "无数据" }) {
                            ctx.selectExample("无数据")
                        }
                        ToggleChip(label = { "加载中" }, active = { ctx.activeExample == "加载中" }) {
                            ctx.selectExample("加载中")
                        }
                    }
                }

                ConfigGroup("列与布局", theme = { ctx.currentTheme() }) {
                    Scroller {
                        attr {
                            height(44f)
                            flexDirectionRow()
                        }
                        vfor({ ctx.activeColumns }) { col ->
                            ToggleChip(label = { "列:${col.title}" }, active = { ctx.selectedColumn === col }) {
                                ctx.selectedColumn = col
                            }
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(label = { "左对齐" }, active = { ctx.selectedColumn.alignment is ColumnAlignment.Start }) {
                            ctx.selectedColumn.alignment = ColumnAlignment.Start
                        }
                        ToggleChip(label = { "居中" }, active = { ctx.selectedColumn.alignment is ColumnAlignment.Center }) {
                            ctx.selectedColumn.alignment = ColumnAlignment.Center
                        }
                        ToggleChip(label = { "右对齐" }, active = { ctx.selectedColumn.alignment is ColumnAlignment.End }) {
                            ctx.selectedColumn.alignment = ColumnAlignment.End
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(label = { "内边距:${if (ctx.compactPadding) "紧凑" else "标准"}" }, active = { ctx.compactPadding }) {
                            ctx.compactPadding = !ctx.compactPadding
                        }
                        ToggleChip(label = { "行高:${if (ctx.fixedRowHeight) "固定48" else "自适应"}" }, active = { ctx.fixedRowHeight }) {
                            ctx.fixedRowHeight = !ctx.fixedRowHeight
                        }
                    }
                }

                ConfigGroup("外观", theme = { ctx.currentTheme() }) {
                    Text {
                        attr {
                            text("主题")
                            fontSize(12f)
                            color(Color(ctx.currentTheme().cellTextSecondary))
                            marginBottom(6f)
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); alignItemsCenter() }
                        ToggleChip(label = { "浅色" }, active = { ctx.themeMode is DemoThemeMode.Light }) {
                            ctx.themeMode = DemoThemeMode.Light
                        }
                        ToggleChip(label = { "深色" }, active = { ctx.themeMode is DemoThemeMode.Dark }) {
                            ctx.themeMode = DemoThemeMode.Dark
                        }
                        ToggleChip(label = { "蓝色" }, active = { ctx.themeMode is DemoThemeMode.Blue }) {
                            ctx.themeMode = DemoThemeMode.Blue
                        }
                    }
                    Text {
                        attr {
                            text("表格外观")
                            fontSize(12f)
                            color(Color(ctx.currentTheme().cellTextSecondary))
                            marginTop(4f)
                            marginBottom(6f)
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); alignItemsCenter() }
                        ToggleChip(label = { "斑马纹:${if (ctx.zebraOn) "开" else "关"}" }, active = { ctx.zebraOn }) {
                            ctx.zebraOn = !ctx.zebraOn
                        }
                        ToggleChip(label = { "边框:${if (ctx.borderedOn) "开" else "关"}" }, active = { ctx.borderedOn }) {
                            ctx.borderedOn = !ctx.borderedOn
                        }
                        ToggleChip(label = { "表头:${if (ctx.compactHeader) "紧凑" else "标准"}" }, active = { ctx.compactHeader }) {
                            ctx.compactHeader = !ctx.compactHeader
                        }
                    }
                }

                ConfigGroup("扩展能力", theme = { ctx.currentTheme() }) {
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(
                            label = { "状态渲染:${if (ctx.customStatusRendererOn) "自定义" else "默认"}" },
                            active = { ctx.customStatusRendererOn },
                        ) {
                            ctx.customStatusRendererOn = !ctx.customStatusRendererOn
                            ctx.syncActiveColumns()
                            ctx.selectedColumn = ctx.ageColumn
                        }
                        ToggleChip(label = { "溢出提示:${if (ctx.overflowTipOn) "开" else "关"}" }, active = { ctx.overflowTipOn }) {
                            ctx.overflowTipOn = !ctx.overflowTipOn
                            ctx.hideOverflowTip()
                        }
                    }
                }

            }

            // ===== 表格（左右留白 16dp）=====
            View {
                attr {
                    flex(1f)
                    marginLeft(16f)
                    marginRight(16f)
                    marginBottom(16f)
                }
                TableView<User> {
                    attr {
                        flex(1f) // 让 TableView（ComposeView）撑满父容器，内部 List 的 flex 才能拿到高度
                        columns = ctx.activeColumns
                        data = ctx.currentData()
                        zebraStripe = ctx.zebraOn
                        bordered = ctx.borderedOn
                        cellPaddingH = if (ctx.compactPadding) 8f else 12f
                        cellPaddingV = if (ctx.compactPadding) 6f else 10f
                        rowHeight = if (ctx.fixedRowHeight) 48f else 0f
                        themeColors = ctx.currentTheme()
                        mobileMode = ctx.mobileMode
                        mobilePrimaryColumnKey = "name"
                        mobileStatusColumnKey = "status"
                        mobileStatusTagStyleByText = ctx.statusTagStyleByText(ctx.currentTheme())
                        loading = ctx.tableState == "加载"
                        emptyText = "暂无员工数据"
                        loadingText = "正在加载员工数据"
                        enableOverflowCellClick = ctx.overflowTipOn
                        headerStyle = if (ctx.compactHeader) {
                            TableHeaderStyle(
                                fontSize = 13f,
                                fontWeight = TableHeaderFontWeight.Bold,
                                paddingH = 8f,
                                paddingV = 6f,
                                height = 40f,
                                bottomBorderWidth = 2f,
                            )
                        } else {
                            TableHeaderStyle.Default
                        }
                    }
                    event {
                        rowClick = { user ->
                            ctx.bridgeModule.toast("点击了: ${user.name}")
                        }
                        overflowCellClick = { info ->
                            ctx.showOverflowTip(info)
                        }
                        overflowTipDismiss = {
                            ctx.hideOverflowTip()
                        }
                    }
                }
            }

            View {
                attr {
                    absolutePositionAllZero()
                    zIndex(30)
                    visibility(ctx.overflowTipVisible && ctx.overflowTipOn)
                    touchEnable(ctx.overflowTipVisible && ctx.overflowTipOn)
                }
                event {
                    click { ctx.hideOverflowTip() }
                }
                View {
                    attr {
                        absolutePosition(left = ctx.overflowTipLeft, top = ctx.overflowTipTop)
                        width(ctx.overflowTipWidth())
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(8f)
                        paddingBottom(8f)
                        borderRadius(8f)
                        backgroundColor(Color(0xEE222222))
                    }
                    event {
                        click {
                            // Consume taps inside the title-like tip.
                        }
                    }
                    Text {
                        attr {
                            text(ctx.overflowTipText)
                            fontSize(13f)
                            color(Color.WHITE)
                        }
                    }
                }
                Image {
                    attr {
                        absolutePosition(left = ctx.overflowTipLeft + ctx.overflowTipArrowLeft, top = ctx.overflowTipTop - 6f)
                        zIndex(31)
                        size(16f, 8f)
                        src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAICAYAAADwdn+XAAAANElEQVR42mNgwAOUlJTewTADqQBZM8mGYNNMtCH4NBM0hBjNOA0hRTOGIeRohhtCiWYQBgBrGmjtRbMqEAAAAABJRU5ErkJggg==")
                    }
                }
            }
        }
    }

    private fun currentTheme(): TableThemeColors = when (themeMode) {
        is DemoThemeMode.Dark -> TableThemeColors.Dark
        is DemoThemeMode.Blue -> TableThemeColors.Blue
        is DemoThemeMode.Light -> TableThemeColors.Light
    }

    private fun selectExample(example: String) {
        activeExample = example
        tableState = when (example) {
            "无数据" -> "空"
            "加载中" -> "加载"
            else -> "正常"
        }
        mobileMode = if (example == "Mobile List") TableMobileMode.List else TableMobileMode.Table
        wideTable = example != "基础表格"
        rendererExampleColumns = example == "Renderer 插槽"
        customStatusRendererOn = example == "Renderer 插槽" || example == "Mobile List"
        selectedColumn = ageColumn
        syncActiveColumns()
        hideOverflowTip()
    }

    private fun exampleDescription(): String = when (activeExample) {
        "基础表格" -> "3 列默认文本，验证基础渲染与默认 fallback"
        "宽表滚动" -> "5 列宽表，验证横向滚动与固定表头"
        "Renderer 插槽" -> "7 列示例，验证自定义单元格和交互子组件"
        "Mobile List" -> "显式 List 模式，验证移动端 grouped list 转译"
        "无数据" -> "保留当前展示模式，在内容区显示无数据占位"
        "加载中" -> "保留旧内容并显示加载遮罩，期间禁止交互"
        else -> "选择一个场景查看对应能力"
    }

    private fun currentStatusColumn(): ColumnModel<User> =
        if (customStatusRendererOn) statusRendererColumn else statusTextColumn

    private fun resolveStatusTagStyle(status: String): TableStatusTagStyle =
        statusTagStyleByText(currentTheme())[status]
            ?: TableStatusTagStyle.fromPreset(TableStatusTagPreset.fromText(status), currentTheme())

    private fun statusTagStyleByText(themeColors: TableThemeColors): Map<String, TableStatusTagStyle> = mapOf(
        "在职" to TableStatusTagStyle(themeColors.statusTagInfoBackground, themeColors.statusTagInfoText),
        "休假" to TableStatusTagStyle(themeColors.statusTagNeutralBackground, themeColors.statusTagNeutralText),
        "离职" to TableStatusTagStyle(themeColors.statusTagBackgroundAlt, themeColors.statusTagTextAlt),
    )

    private fun currentColumns(): List<ColumnModel<User>> =
        when {
            rendererExampleColumns -> listOf(
                avatarColumn,
                nameColumn,
                ageColumn,
                wideEmailColumn,
                currentStatusColumn(),
                cityColumn,
                notifyColumn,
            )
            wideTable -> listOf(nameColumn, ageColumn, wideEmailColumn, cityColumn, currentStatusColumn())
            else -> columns3
        }

    private fun currentData(): List<User> =
        if (tableState == "空") emptyList() else users

    private fun notifyEnabled(user: User): Boolean =
        notifyStateById[user.id] ?: (user.name.hashCode() % 2 == 0)

    private fun setNotifyEnabled(user: User, enabled: Boolean) {
        notifyStateById = notifyStateById + (user.id to enabled)
    }

    private fun syncActiveColumns() {
        activeColumns.clear()
        activeColumns.addAll(currentColumns())
    }

    private fun hideOverflowTip() {
        overflowTipVisible = false
        overflowTipText = ""
    }

    private fun showOverflowTip(info: TableOverflowCellInfo<User>) {
        overflowTipText = info.text
        overflowTipLeft = overflowTipLeft(info)
        overflowTipTop = overflowTipTop(info)
        overflowTipArrowLeft = overflowTipArrowLeft(info, overflowTipLeft)
        overflowTipVisible = true
    }

    private fun overflowTipWidth(): Float = min(320f, pagerData.pageViewWidth - TIP_SCREEN_MARGIN * 2f)

    private fun overflowTipLeft(info: TableOverflowCellInfo<User>): Float {
        val width = overflowTipWidth()
        val desiredLeft = TABLE_LEFT + info.estimatedCellX + info.estimatedCellWidth / 2f - width / 2f
        return min(max(desiredLeft, TIP_SCREEN_MARGIN), pagerData.pageViewWidth - width - TIP_SCREEN_MARGIN)
    }

    private fun overflowTipTop(info: TableOverflowCellInfo<User>): Float {
        val cellTop = TABLE_TOP_ESTIMATE + info.estimatedCellY
        return min(cellTop + info.estimatedCellHeight + TIP_GAP, pagerData.pageViewHeight - TIP_HEIGHT_ESTIMATE - TIP_SCREEN_MARGIN)
    }

    private fun overflowTipArrowLeft(info: TableOverflowCellInfo<User>, tipLeft: Float): Float {
        val cellCenter = TABLE_LEFT + info.estimatedCellX + info.estimatedCellWidth / 2f
        return min(max(cellCenter - tipLeft - TIP_ARROW_HALF_WIDTH, 16f), overflowTipWidth() - TIP_ARROW_WIDTH - 8f)
    }

    companion object {
        private const val TABLE_LEFT = 16f
        private const val TABLE_TOP_ESTIMATE = 390f
        private const val TIP_SCREEN_MARGIN = 16f
        private const val TIP_HEIGHT_ESTIMATE = 50f
        private const val TIP_GAP = 6f
        private const val TIP_ARROW_WIDTH = 16f
        private const val TIP_ARROW_HALF_WIDTH = 8f
    }

}

private sealed class DemoThemeMode {
    object Light : DemoThemeMode()
    object Dark : DemoThemeMode()
    object Blue : DemoThemeMode()
}

/**
 * 可点击的配置选项（chip 样式），顶层扩展函数。
 * label / active 传 lambda（在 attr 块内调用），保证响应式更新。
 */
private fun ViewContainer<*, *>.ToggleChip(
    label: () -> String,
    active: () -> Boolean,
    onClick: () -> Unit,
) {
    View {
        attr {
            alignItemsCenter()
            justifyContentCenter()
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(7f)
            paddingBottom(7f)
            marginRight(8f)
            marginBottom(8f)
            borderRadius(16f)
            backgroundColor(Color(if (active()) 0xFFEAF4FF else 0xFFFFFFFF))
            border(Border(1f, BorderStyle.SOLID, Color(if (active()) 0xFF2E77E5 else 0xFFE1E6EF)))
        }
        Text {
            attr {
                text(label())
                fontSize(12f)
                color(Color(if (active()) 0xFF2E77E5 else 0xFF4B5563))
            }
        }
        event {
            click { onClick() }
        }
    }
}

private fun ViewContainer<*, *>.ConfigGroup(
    title: String,
    first: Boolean = false,
    theme: () -> TableThemeColors,
    content: ViewBuilder,
) {
    Text {
        attr {
            text(title)
            fontSize(12f)
            color(Color(theme().cellTextSecondary))
            marginTop(if (first) 0f else 8f)
            marginBottom(4f)
        }
    }
    View {
        attr {
            backgroundColor(Color(theme().cardBackground))
            borderRadius(10f)
            border(Border(1f, BorderStyle.SOLID, Color(theme().cardBorder)))
            paddingTop(8f)
            paddingBottom(8f)
            paddingLeft(8f)
            paddingRight(8f)
            marginBottom(8f)
        }
        content()
    }
}

private fun ViewContainer<*, *>.ConfigRow(
    label: () -> String,
    active: () -> Boolean,
    onClick: () -> Unit,
) {
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(10f)
            paddingBottom(10f)
            marginBottom(8f)
            borderRadius(12f)
            backgroundColor(Color(if (active()) 0xFFEAF4FF else 0xFFFFFFFF))
            border(Border(1f, BorderStyle.SOLID, Color(if (active()) 0xFF2E77E5 else 0xFFE1E6EF)))
        }
        Text {
            attr {
                flex(1f)
                text(label())
                fontSize(13f)
                color(Color(if (active()) 0xFF2E77E5 else 0xFF4B5563))
            }
            event {
                click { onClick() }
            }
        }
        event {
            click { onClick() }
        }
    }
}
