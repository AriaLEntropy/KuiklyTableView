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

/**
 * Table 基础展示 Demo
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
            View {
                attr {
                    flex(1f)
                    flexDirectionRow()
                    alignItemsCenter()
                    justifyContentCenter()
                    backgroundColor(Color(if (user.status == "在职") 0xFFE1F5EA else 0xFFFFEED6))
                    borderRadius(4f)
                    paddingLeft(8f)
                    paddingRight(8f)
                    paddingTop(3f)
                    paddingBottom(3f)
                }
                Text {
                    attr {
                        text(user.status)
                        fontSize(12f)
                        color(Color(if (user.status == "在职") 0xFF16794A else 0xFF9A5B00))
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
                    color(Color(0xFF1565C0))
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

    // ===== 可配置状态（observable，变化触发表格重渲染）=====
    private var wideTable by observable(true)          // 3列 / 5列（横向滚动）
    private var activeColumns: ObservableList<ColumnModel<User>> by observableList()
    private var selectedColumn by observable<ColumnModel<User>>(ageColumn)
    private var zebraOn by observable(true)             // 斑马纹
    private var borderedOn by observable(false)         // 列边框
    private var compactPadding by observable(false)     // 紧凑内边距
    private var fixedRowHeight by observable(false)     // 固定行高
    private var themeMode by observable("浅色")
    private var compactHeader by observable(false)
    private var customStatusRendererOn by observable(true)
    private var mobileMode: TableMobileMode by observable(TableMobileMode.Table)
    private var tableState by observable("正常")
    private var overflowPopupOn by observable(true)

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
                    text("Table 基础展示")
                    fontSize(18f)
                    fontWeightSemisolid()
                    color(Color(ctx.currentTheme().cellText))
                    margin(16f)
                    marginBottom(8f)
                }
            }

            // ===== 配置面板 =====
            View {
                attr {
                    marginLeft(16f)
                    marginRight(16f)
                    marginBottom(12f)
                }

                // 第一行：列数（3列 / 5列，5列触发横向滚动）
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    ToggleChip(label = { "3 列" }, active = { !ctx.wideTable }) {
                        ctx.wideTable = false
                        ctx.activeColumns.clear()
                        ctx.activeColumns.addAll(ctx.columns3)
                        ctx.selectedColumn = ctx.ageColumn
                    }
                    ToggleChip(label = { "5 列（横向滚动）" }, active = { ctx.wideTable }) {
                        ctx.wideTable = true
                        ctx.activeColumns.clear()
                        ctx.activeColumns.addAll(ctx.currentColumns())
                        ctx.selectedColumn = ctx.ageColumn
                    }
                }

                // 第二行：ST-5 截断全文浮层，独立热区避免和表格区域命中冲突
                ConfigRow(
                    label = { "全文浮层:${if (ctx.overflowPopupOn) "开" else "关"}" },
                    active = { ctx.overflowPopupOn },
                ) {
                    ctx.overflowPopupOn = !ctx.overflowPopupOn
                }

                // 第三行：配置列（动态反映当前列）
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    vfor({ ctx.activeColumns }) { col ->
                        ToggleChip(
                            label = { "列:${col.title}" },
                            active = { ctx.selectedColumn === col },
                        ) { ctx.selectedColumn = col }
                    }
                }

                // 第四行：对齐方式（作用于选中列）
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
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

                // 第五行：样式配置
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    ToggleChip(label = { "斑马纹:${if (ctx.zebraOn) "开" else "关"}" }, active = { ctx.zebraOn }) {
                        ctx.zebraOn = !ctx.zebraOn
                    }
                    ToggleChip(label = { "边框:${if (ctx.borderedOn) "开" else "关"}" }, active = { ctx.borderedOn }) {
                        ctx.borderedOn = !ctx.borderedOn
                    }
                    ToggleChip(label = { "内边距:${if (ctx.compactPadding) "紧凑" else "标准"}" }, active = { ctx.compactPadding }) {
                        ctx.compactPadding = !ctx.compactPadding
                    }
                    ToggleChip(label = { "行高:${if (ctx.fixedRowHeight) "固定48" else "自适应"}" }, active = { ctx.fixedRowHeight }) {
                        ctx.fixedRowHeight = !ctx.fixedRowHeight
                    }
                }

                // 第六行：主题与自定义渲染
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    ToggleChip(label = { "主题:${ctx.themeMode}" }, active = { ctx.themeMode == "浅色" }) {
                        ctx.themeMode = when (ctx.themeMode) {
                            "浅色" -> "深色"
                            "深色" -> "蓝色"
                            else -> "浅色"
                        }
                    }
                    ToggleChip(
                        label = { "状态渲染:${if (ctx.customStatusRendererOn) "自定义" else "默认"}" },
                        active = { ctx.customStatusRendererOn },
                    ) {
                        ctx.customStatusRendererOn = !ctx.customStatusRendererOn
                        ctx.syncActiveColumns()
                        ctx.selectedColumn = ctx.ageColumn
                    }
                    ToggleChip(label = { "表头:${if (ctx.compactHeader) "紧凑" else "标准"}" }, active = { ctx.compactHeader }) {
                        ctx.compactHeader = !ctx.compactHeader
                    }
                }

                // 第七行：ST-4 MobileMode，Auto 用列数验证默认转译规则
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    ToggleChip(label = { "模式:Auto" }, active = { ctx.mobileMode is TableMobileMode.Auto }) {
                        ctx.mobileMode = TableMobileMode.Auto
                    }
                    ToggleChip(label = { "模式:Table" }, active = { ctx.mobileMode is TableMobileMode.Table }) {
                        ctx.mobileMode = TableMobileMode.Table
                    }
                    ToggleChip(label = { "模式:List" }, active = { ctx.mobileMode is TableMobileMode.List }) {
                        ctx.mobileMode = TableMobileMode.List
                    }
                }

                // 第八行：ST-4 状态层，切换项均改变表格实际状态
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    ToggleChip(label = { "状态:正常" }, active = { ctx.tableState == "正常" }) {
                        ctx.tableState = "正常"
                    }
                    ToggleChip(label = { "状态:空" }, active = { ctx.tableState == "空" }) {
                        ctx.tableState = "空"
                    }
                    ToggleChip(label = { "状态:加载" }, active = { ctx.tableState == "加载" }) {
                        ctx.tableState = "加载"
                    }
                    ToggleChip(label = { "状态:错误" }, active = { ctx.tableState == "错误" }) {
                        ctx.tableState = "错误"
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
                        loading = ctx.tableState == "加载"
                        errorText = if (ctx.tableState == "错误") "加载失败，请稍后重试" else null
                        emptyText = "暂无员工数据"
                        loadingText = "正在加载员工数据"
                        retryText = "恢复正常"
                        enableOverflowPopup = ctx.overflowPopupOn
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
                        retry = {
                            ctx.tableState = "正常"
                            ctx.bridgeModule.toast("已恢复正常数据")
                        }
                    }
                }
            }
        }
    }

    private fun currentTheme(): TableThemeColors = when (themeMode) {
        "深色" -> TableThemeColors.Dark
        "蓝色" -> TableThemeColors(
            headerBackground = 0xFF0D47A1,
            headerText = 0xFFFFFFFF,
            cellText = 0xFF12304A,
            gridLine = 0xFF90CAF9,
            rowBackground = 0xFFEAF4FF,
            rowBackgroundAlt = 0xFFDCEEFF,
        )
        else -> TableThemeColors.Light
    }

    private fun currentStatusColumn(): ColumnModel<User> =
        if (customStatusRendererOn) statusRendererColumn else statusTextColumn

    private fun currentColumns(): List<ColumnModel<User>> =
        if (wideTable) listOf(nameColumn, ageColumn, wideEmailColumn, cityColumn, currentStatusColumn()) else columns3

    private fun currentData(): List<User> =
        if (tableState == "空") emptyList() else users

    private fun syncActiveColumns() {
        activeColumns.clear()
        activeColumns.addAll(currentColumns())
    }
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
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(8f)
            paddingBottom(8f)
            marginRight(8f)
            marginBottom(8f)
            borderRadius(14f)
            backgroundColor(Color(if (active()) 0xFF4F8FFF else 0xFFEEEEEE))
        }
        Text {
            attr {
                text(label())
                fontSize(12f)
                color(Color(if (active()) 0xFFFFFFFF else 0xFF666666))
            }
        }
        event {
            click { onClick() }
        }
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
            backgroundColor(Color(if (active()) 0xFFEAF4FF else 0xFFF4F4F4))
        }
        Text {
            attr {
                flex(1f)
                text(label())
                fontSize(13f)
                color(Color(if (active()) 0xFF0D47A1 else 0xFF666666))
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
