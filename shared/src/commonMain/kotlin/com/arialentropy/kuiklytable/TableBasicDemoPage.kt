package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.compose.Button
import com.arialentropy.kuiklytable.base.BasePager
import com.arialentropy.kuiklytable.base.bridgeModule
import com.arialentropy.kuiklytable.base.setTimeout
import kotlin.math.max
import kotlin.math.min

/**
 * KuiklyTable 组件展示 Demo
 *
 * 验证 ST-1 + ST-2：列定义/行列渲染/对齐/斑马纹（ST-1），
 * 横纵双向滚动/边框/内边距/行高配置（ST-2）；表头纵滚固定，不提供随内容滚动开关。
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

    // 50 行数据，足够触发纵向滚动与加载更多
    private val users = (1..50).map(::createUser)

    private fun createUser(i: Int): User =
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

    // 年龄：展示用 toString，排序必须按 Int，否则 "9" > "10" 这种字符串序会错
    private val ageColumn = ColumnModel<User>(
        key = "age",
        title = "年龄",
        accessor = { it.age.toString() },
        width = 80f,
        sortable = true,
        sortComparator = compareBy { it.age },
    )

    // 姓名：展示为「员工N」；不配 comparator 会按字符串排成 员工1、员工10、员工2…
    // Demo 用与编号同源的 id 做规则，证明「规则在列上、与 sortChange 事件解耦」
    private val nameColumn = ColumnModel<User>(
        key = "name",
        title = "姓名",
        accessor = { it.name },
        width = 80f,
        sortable = true,
        sortComparator = compareBy { it.id },
    )
    private val emailColumn = ColumnModel<User>(
        key = "email",
        title = "邮箱",
        accessor = { it.email },
        minWidth = 140f,
        flex = 2f,
    )
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

    /** Demo 业务列：彩色标签由使用方 cellRenderer 传入，不是 Table 内置组件。 */
    private val statusTagColumn = ColumnModel<User>(
        key = "statusTag",
        title = "状态",
        accessor = { it.status },
        width = 90f,
        cellRenderer = { user, _ ->
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
    )

    // 3 列模式（fits 页面，无横向滚动）
    private val columns3 = listOf(nameColumn, ageColumn, emailColumn)

    private val basicColumns = listOf(
        ColumnModel<User>(
            key = "basicName",
            title = "姓名",
            accessor = { it.name },
            width = 80f,
        ),
        ColumnModel<User>(
            key = "basicAge",
            title = "年龄",
            accessor = { it.age.toString() },
            width = 80f,
        ),
        ColumnModel<User>(
            key = "basicEmail",
            title = "邮箱",
            accessor = { it.email },
            minWidth = 140f,
            flex = 2f,
        ),
    )

    /**
     * 基础章列宽对照样例：姓名/年龄设 width（固定）；邮箱 width=null + minWidth/flex（弹性）。
     * 哪一列走弹性由接入方决定，此处仅作混用示例。
     */
    private val flexibleWidthColumns = listOf(
        ColumnModel<User>(
            key = "flexName",
            title = "姓名",
            accessor = { it.name },
            width = 72f,
        ),
        ColumnModel<User>(
            key = "flexAge",
            title = "年龄",
            accessor = { it.age.toString() },
            width = 56f,
        ),
        ColumnModel<User>(
            key = "flexEmail",
            title = "邮箱",
            accessor = { it.email },
            minWidth = 120f,
            flex = 1f,
        ),
    )

    /** 基础章列宽对照样例：三列均显式 width，验证「无弹性列 → 剩余 viewport 留空」。 */
    private val fixedWidthColumns = listOf(
        ColumnModel<User>(
            key = "fixedName",
            title = "姓名",
            accessor = { it.name },
            width = 72f,
        ),
        ColumnModel<User>(
            key = "fixedAge",
            title = "年龄",
            accessor = { it.age.toString() },
            width = 56f,
        ),
        ColumnModel<User>(
            key = "fixedEmail",
            title = "邮箱",
            accessor = { it.email },
            width = 140f,
        ),
    )

    private val alignmentColumn = ColumnModel<User>(
        key = "alignment",
        title = "姓名",
        accessor = { it.name },
        width = 180f,
    )

    private val sortingColumns = listOf(
        ColumnModel<User>(
            key = "plainName",
            title = "姓名（不可排序）",
            accessor = { it.name },
            minWidth = 140f,
            sortable = false,
        ),
        ColumnModel<User>(
            key = "sortableAge",
            title = "年龄（数字）",
            accessor = { it.age.toString() },
            minWidth = 140f,
            sortable = true,
            // 规则挂在列上：按 Int，不是按 accessor 字符串 "20"
            sortComparator = compareBy { it.age },
        ),
    )

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
                        size(28f, 28f)
                        borderRadius(14f)
                        backgroundColor(Color(this@TableBasicDemoPage.currentTheme().actionText))
                        allCenter()
                    }
                    Text {
                        attr {
                            text(user.name.take(1))
                            fontSize(12f)
                            color(Color(this@TableBasicDemoPage.currentTheme().actionTextOnFill))
                            fontWeightBold()
                        }
                    }
                }
            }
        },
    )

    /** Demo 业务操作：点击、按压和禁用反馈均由 renderer 内的 Button 负责。 */
    private val actionColumn = ColumnModel<User>(
        key = "action",
        title = "操作",
        accessor = { "查看" },
        width = 80f,
        enableRowClick = false,
        enableCellClick = false,
        cellRenderer = { user, _ ->
            val disabled = user.status == "离职"
            val theme = this@TableBasicDemoPage.currentTheme()
            View {
                attr {
                    flex(1f)
                    alignItemsCenter()
                    justifyContentCenter()
                }
                // 不用原生 Button：其最小高度偏大，demo 操作列改用轻量 View
                View {
                    attr {
                        width(48f)
                        height(22f)
                        borderRadius(4f)
                        allCenter()
                        backgroundColor(Color(if (disabled) theme.statusTagNeutralBackground else theme.actionText))
                    }
                    Text {
                        attr {
                            text(if (disabled) "不可用" else "查看")
                            fontSize(11f)
                            color(Color(if (disabled) theme.statusTagNeutralText else theme.actionTextOnFill))
                        }
                    }
                    if (!disabled) {
                        event {
                            click { this@TableBasicDemoPage.bridgeModule.toast("按钮操作: ${user.name}") }
                        }
                    }
                }
            }
        },
    )

    // ===== 可配置状态（observable，变化触发表格重渲染）=====
    private var wideTable by observable(true)          // 3列 / 5列 / 7列（横向滚动）
    private var activeColumns: ObservableList<ColumnModel<User>> by observableList()
    private var selectedColumn by observable<ColumnModel<User>>(ageColumn)
    private var tableLineMode: TableLineMode by observable(TableLineMode.Grid)
    private var customLineColor: Long by observable(0xFF2E77E5)
    private var tableCornerRadius: Float by observable(TableCornerRadius.Default)
    private var cornerRadiusInputRef: ViewRef<InputView>? = null
    private var compactPadding by observable(false)     // 紧凑内边距
    private var fixedRowHeight by observable(false)     // 固定行高
    private var scrollFixedFirstColumn by observable(false)
    /** 列宽对照：false = 固定 width；true = minWidth + flex（默认） */
    private var columnWidthFlexible by observable(true)
    private var sortState by observable(TableSortState())
    private var sortingDemoSortState by observable(TableSortState())
    private var themeMode: DemoThemeMode by observable(DemoThemeMode.Light)
    private var compactHeader by observable(false)
    private var tableState by observable("正常")
    private var customStateRendererOn by observable(false)
    private var demoEnableRowClick by observable(true)
    private var demoEnableCellClick by observable(false)
    private var overflowTipVisible by observable(false)
    private var overflowTipText by observable("")
    private var overflowTipLeft by observable(0f)
    private var overflowTipTop by observable(0f)
    private var overflowTipArrowLeft by observable(0f)
    private var overflowTipBubbleWidth by observable(200f)
    private var scrollDemoTableRef: ViewRef<TableView<User>>? = null
    private var scrollDemoLimit by observable(20)
    private var scrollDemoLoadingMore by observable(false)
    private var activeExample by observable("双向滚动")
    private var activeSection by observable("基础")

    init {
        activeColumns.addAll(currentColumns())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color(ctx.currentTheme().rowBackground)) }
            ctx.renderShowcaseHeader(this)
            ctx.renderSectionNavigation(this)
            View {
                attr { flex(1f); positionRelative() }
                vif({ ctx.activeSection == "基础" }) { ctx.renderBasicSection(this) }
                vif({ ctx.activeSection == "滚动" }) { ctx.renderScrollSection(this) }
                vif({ ctx.activeSection == "主题" }) { ctx.renderThemeSection(this) }
                vif({ ctx.activeSection == "自定义" }) { ctx.renderRendererSection(this) }
                vif({ ctx.activeSection == "状态" }) { ctx.renderStateSection(this) }
            }
        }
    }

    private fun legacyBody(): ViewBuilder {
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

                ConfigGroup("核心验收", first = true, theme = { ctx.currentTheme() }) {
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
                        ToggleChip(label = { "基础样式" }, active = { ctx.activeExample == "基础样式" }, theme = { ctx.currentTheme() }, emphasis = true) {
                            ctx.selectExample("基础样式")
                        }
                        ToggleChip(label = { "双向滚动" }, active = { ctx.activeExample == "双向滚动" }, theme = { ctx.currentTheme() }, emphasis = true) {
                            ctx.selectExample("双向滚动")
                        }
                        ToggleChip(label = { "主题定制" }, active = { ctx.activeExample == "主题定制" }, theme = { ctx.currentTheme() }, emphasis = true) {
                            ctx.selectExample("主题定制")
                        }
                        ToggleChip(label = { "自定义渲染" }, active = { ctx.activeExample == "自定义渲染" }, theme = { ctx.currentTheme() }, emphasis = true) {
                            ctx.selectExample("自定义渲染")
                        }
                    }
                    Text {
                        attr {
                            text("主题预设")
                            fontSize(12f)
                            color(Color(ctx.currentTheme().cellTextSecondary))
                            marginTop(4f)
                            marginBottom(6f)
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); alignItemsCenter() }
                        ToggleChip(label = { "浅色" }, active = { ctx.themeMode is DemoThemeMode.Light }, theme = { ctx.currentTheme() }) {
                            ctx.selectTheme(DemoThemeMode.Light)
                        }
                        ToggleChip(label = { "深色" }, active = { ctx.themeMode is DemoThemeMode.Dark }, theme = { ctx.currentTheme() }) {
                            ctx.selectTheme(DemoThemeMode.Dark)
                        }
                        ToggleChip(label = { "蓝色" }, active = { ctx.themeMode is DemoThemeMode.Blue }, theme = { ctx.currentTheme() }) {
                            ctx.selectTheme(DemoThemeMode.Blue)
                        }
                    }
                }

                ConfigGroup("补充示例", theme = { ctx.currentTheme() }) {
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(label = { "无数据" }, active = { ctx.activeExample == "无数据" }, theme = { ctx.currentTheme() }, emphasis = true) {
                            ctx.selectExample("无数据")
                        }
                        ToggleChip(label = { "加载中" }, active = { ctx.activeExample == "加载中" }, theme = { ctx.currentTheme() }, emphasis = true) {
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
                            ToggleChip(label = { col.title }, active = { ctx.selectedColumn === col }, theme = { ctx.currentTheme() }) {
                                ctx.selectedColumn = col
                            }
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(label = { "左对齐" }, active = { ctx.selectedColumn.alignment is ColumnAlignment.Start }, theme = { ctx.currentTheme() }) {
                            ctx.selectedColumn.alignment = ColumnAlignment.Start
                        }
                        ToggleChip(label = { "居中" }, active = { ctx.selectedColumn.alignment is ColumnAlignment.Center }, theme = { ctx.currentTheme() }) {
                            ctx.selectedColumn.alignment = ColumnAlignment.Center
                        }
                        ToggleChip(label = { "右对齐" }, active = { ctx.selectedColumn.alignment is ColumnAlignment.End }, theme = { ctx.currentTheme() }) {
                            ctx.selectedColumn.alignment = ColumnAlignment.End
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(label = { "内边距 ${if (ctx.compactPadding) "紧凑" else "标准"}" }, active = { ctx.compactPadding }, theme = { ctx.currentTheme() }) {
                            ctx.compactPadding = !ctx.compactPadding
                        }
                        ToggleChip(label = { "行高 ${if (ctx.fixedRowHeight) "固定 48" else "自适应"}" }, active = { ctx.fixedRowHeight }, theme = { ctx.currentTheme() }) {
                            ctx.fixedRowHeight = !ctx.fixedRowHeight
                        }
                    }
                    Text {
                        attr {
                            text("排序：姓名规则=按 id（避免「员工10」字符串乱序）；年龄规则=按 Int。点表头只改 sortState，规则不走事件。当前 ${ctx.sortDescription()}")
                            fontSize(12f)
                            color(Color(ctx.currentTheme().cellTextSecondary))
                            marginBottom(6f)
                        }
                    }
                }

                ConfigGroup("表格样式", theme = { ctx.currentTheme() }) {
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); alignItemsCenter() }
                        listOf("无线", "仅横线", "网格", "自定义").forEach { mode ->
                            ToggleChip(label = { mode }, active = { ctx.tableLineModeLabel() == mode }, theme = { ctx.currentTheme() }) {
                                ctx.tableLineMode = ctx.lineModeForLabel(mode)
                            }
                        }
                        listOf(
                            "圆角0" to TableCornerRadius.None,
                            "圆角8" to TableCornerRadius.Default,
                            "圆角12" to TableCornerRadius.Large,
                        ).forEach { (label, radius) ->
                            ToggleChip(label = { label }, active = { ctx.tableCornerRadius == radius }, theme = { ctx.currentTheme() }) {
                                ctx.applyCornerRadius(radius)
                            }
                        }
                        ToggleChip(label = { "表头 ${if (ctx.compactHeader) "紧凑" else "标准"}" }, active = { ctx.compactHeader }, theme = { ctx.currentTheme() }) {
                            ctx.compactHeader = !ctx.compactHeader
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
                        rowKey = { user -> user.id }
                        zebraStripe = false
                        lineMode = ctx.tableLineMode
                        cornerRadius = ctx.tableCornerRadius
                        cellPaddingH = if (ctx.compactPadding) 8f else 12f
                        cellPaddingV = if (ctx.compactPadding) 6f else 10f
                        rowHeight = if (ctx.fixedRowHeight) 48f else 0f
                        data = ctx.currentData()
                        sortState = ctx.sortState
                        autoIndexColumn = false
                        fixedHeader = true
                        fixedFirstColumn = false
                        themeColors = ctx.currentTheme()
                        loading = ctx.tableState == "加载"
                        emptyText = "暂无员工数据"
                        loadingText = "正在加载员工数据"
                        if (ctx.customStateRendererOn) {
                            emptyRenderer = { ctx.renderCustomEmptyState(this) }
                            loadingRenderer = { ctx.renderCustomLoadingState(this) }
                        }
                        enableOverflowCellClick = true
                        headerStyle = if (ctx.compactHeader) {
                            TableHeaderStyle(
                                fontSize = 13f,
                                fontWeight = TableHeaderFontWeight.Bold,
                                paddingH = 8f,
                                paddingV = 6f,
                                height = 40f,
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
                        sortChange = { state ->
                            ctx.sortState = state
                            ctx.hideOverflowTip()
                        }
                    }
                }
            }

            View {
                attr {
                    absolutePositionAllZero()
                    zIndex(30)
                    visibility(ctx.overflowTipVisible)
                    touchEnable(ctx.overflowTipVisible)
                }
                event {
                    click { ctx.hideOverflowTip() }
                }
                View {
                    attr {
                        absolutePosition(left = ctx.overflowTipLeft, top = ctx.overflowTipTop)
                        width(ctx.overflowTipBubbleWidth)
                        paddingLeft(12f)
                        paddingRight(12f)
                        paddingTop(8f)
                        paddingBottom(8f)
                        borderRadius(8f)
                        backgroundColor(Color(0xEE222222))
                    }
                    event {
                        click {
                            // 消费全文提示内部点击，避免透传关闭浮层。
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
                        src(TIP_ARROW_UP_BASE64)
                    }
                }
            }
        }
    }

    private fun renderShowcaseHeader(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr {
                paddingLeft(16f)
                paddingRight(16f)
                paddingTop(ctx.pagerData.statusBarHeight + 12f)
                paddingBottom(10f)
            }
            Text {
                attr {
                    text("KuiklyTable Showcase")
                    fontSize(20f)
                    fontWeightSemiBold()
                    color(Color(ctx.currentTheme().cellText))
                }
            }
            Text {
                attr {
                    text("KuiklyTable：按章节验证基础能力；DataTable 交互能力请从 table_home 进入 table_data。")
                    fontSize(12f)
                    color(Color(ctx.currentTheme().cellTextSecondary))
                    marginTop(4f)
                }
            }
        }
    }

    private fun renderSectionNavigation(container: ViewContainer<*, *>) {
        val ctx = this
        container.Scroller {
            attr {
                height(48f)
                flexDirectionRow()
                paddingLeft(16f)
                paddingRight(8f)
            }
            listOf("基础", "滚动", "主题", "自定义", "状态").forEach { section ->
                ShowcaseTab(section, active = { ctx.activeSection == section }, theme = { ctx.currentTheme() }) {
                    ctx.activeSection = section
                    ctx.hideOverflowTip()
                }
            }
        }
    }

    private fun renderBasicSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.Scroller {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(20f) }
            SectionIntro(
                "基础表格",
                "列定义、默认外观、列宽策略、分隔线、圆角、对齐、单列撑满、溢出与单列排序。",
                { ctx.currentTheme() },
            )
            ExampleCard(
                "默认样式",
                "开箱默认：zebraStripe=false、lineMode=Grid、cornerRadius=8、rowHeight=0（内容自适应）。",
                { ctx.currentTheme() },
            ) {
                ctx.renderTablePreview(this, ctx.basicColumns, { ctx.users.take(4) }, height = 248f, zebra = { false }, theme = ctx.currentTheme())
            }
            ExampleCard(
                "斑马纹",
                "zebraStripe=true 开启隔行底色；颜色来自主题 rowBackgroundAlt。",
                { ctx.currentTheme() },
            ) {
                ctx.renderTablePreview(
                    this,
                    ctx.basicColumns,
                    { ctx.users.take(4) },
                    height = 248f,
                    zebra = { true },
                    theme = ctx.currentTheme(),
                )
            }
            ExampleCard(
                "单元格超出省略",
                "默认单行 ellipsis；截断时可触发 overflow。cellLongPress 由使用方接宿主能力（本页接剪贴板复制全文）。",
                { ctx.currentTheme() },
            ) {
                ctx.renderOverflowDemo(this)
            }
            ExampleCard(
                "列宽：固定与弹性",
                "按列配置、可同表混用：width 非空则固定且优先于 minWidth；width 为空则从 minWidth 起步，按 flex 分剩余 viewport。全部为固定列时剩余区域留空。",
                { ctx.currentTheme() },
            ) {
                View {
                    attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); marginBottom(8f) }
                    SegmentOption("固定 width", active = { !ctx.columnWidthFlexible }, theme = { ctx.currentTheme() }) {
                        ctx.columnWidthFlexible = false
                    }
                    SegmentOption("弹性 minWidth+flex", active = { ctx.columnWidthFlexible }, theme = { ctx.currentTheme() }) {
                        ctx.columnWidthFlexible = true
                    }
                }
                Text {
                    attr {
                        text(
                            if (ctx.columnWidthFlexible) {
                                "当前：姓名/年龄设 width（固定）；邮箱 width=null，minWidth+flex 参与剩余分配"
                            } else {
                                "当前：三列均设 width（固定优先）；无弹性列，剩余 viewport 留空"
                            },
                        )
                        fontSize(11f)
                        color(Color(ctx.currentTheme().cellTextSecondary))
                        marginBottom(8f)
                    }
                }
                // remount：切换列定义时重建表格，避免列宽布局残留
                vif({ ctx.columnWidthFlexible }) {
                    ctx.renderTablePreview(
                        this,
                        ctx.flexibleWidthColumns,
                        { ctx.users.take(4) },
                        height = 248f,
                        theme = ctx.currentTheme(),
                        fillPreviewWidth = true,
                    )
                }
                vif({ !ctx.columnWidthFlexible }) {
                    ctx.renderTablePreview(
                        this,
                        ctx.fixedWidthColumns,
                        { ctx.users.take(4) },
                        height = 248f,
                        theme = ctx.currentTheme(),
                        fillPreviewWidth = true,
                    )
                }
            }
            ExampleCard(
                "分隔线",
                "lineMode：None / Horizontal / Grid / Custom。与 cornerRadius 独立。",
                { ctx.currentTheme() },
            ) {
                View {
                    attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); marginBottom(8f) }
                    listOf("无线", "仅横线", "网格", "自定义").forEach { mode ->
                        SegmentOption(mode, active = { ctx.tableLineModeLabel() == mode }, theme = { ctx.currentTheme() }) {
                            ctx.tableLineMode = ctx.lineModeForLabel(mode)
                        }
                    }
                }
                vif({ ctx.tableLineMode is TableLineMode.Custom }) {
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); marginBottom(8f) }
                        listOf(
                            "蓝" to 0xFF2E77E5,
                            "灰" to 0xFF9E9E9E,
                            "绿" to 0xFF2E7D32,
                            "橙" to 0xFFE65100,
                            "紫" to 0xFF6A1B9A,
                        ).forEach { (label, color) ->
                            SegmentOption(
                                label,
                                active = { ctx.customLineColor == color },
                                theme = { ctx.currentTheme() },
                            ) {
                                ctx.applyCustomLineColor(color)
                            }
                        }
                    }
                }
                vif({ ctx.tableLineModeLabel() to ctx.customLineColor }) {
                    ctx.renderTablePreview(
                        this,
                        ctx.basicColumns,
                        { ctx.users.take(4) },
                        height = 248f,
                        lineMode = { ctx.tableLineMode },
                        cornerRadius = { TableCornerRadius.Default },
                        fixedRowHeight = { true },
                        theme = ctx.currentTheme(),
                    )
                }
            }
            ExampleCard(
                "圆角",
                "cornerRadius：None=0 / Default=8 / Large=12，或任意 dp。与 lineMode 独立。",
                { ctx.currentTheme() },
            ) {
                View {
                    attr { flexDirectionRow(); alignItemsCenter(); marginBottom(8f) }
                    listOf(
                        "圆角 0" to TableCornerRadius.None,
                        "圆角 8" to TableCornerRadius.Default,
                        "圆角 12" to TableCornerRadius.Large,
                    ).forEach { (label, radius) ->
                        SegmentOption(label, active = { ctx.tableCornerRadius == radius }, theme = { ctx.currentTheme() }) {
                            ctx.applyCornerRadius(radius)
                        }
                    }
                    Input {
                        ref {
                            ctx.cornerRadiusInputRef = it
                            it.view?.setText(ctx.formatCornerRadius(ctx.tableCornerRadius))
                        }
                        attr {
                            width(72f)
                            height(40f)
                            marginLeft(8f)
                            fontSize(13f)
                            color(Color(ctx.currentTheme().cellText))
                            textAlignCenter()
                            placeholder("dp")
                            placeholderColor(Color(ctx.currentTheme().cellTextSecondary))
                            borderRadius(8f)
                            border(Border(1f, BorderStyle.SOLID, Color(ctx.currentTheme().cardBorder)))
                            backgroundColor(Color(ctx.currentTheme().cardBackground))
                        }
                        event {
                            textDidChange {
                                ctx.onCornerRadiusInputChanged(it.text)
                            }
                        }
                    }
                }
                vif({ ctx.formatCornerRadius(ctx.tableCornerRadius) }) {
                    ctx.renderTablePreview(
                        this,
                        ctx.basicColumns,
                        { ctx.users.take(4) },
                        height = 248f,
                        lineMode = { TableLineMode.Grid },
                        cornerRadius = { ctx.tableCornerRadius },
                        fixedRowHeight = { true },
                        theme = ctx.currentTheme(),
                    )
                }
            }
            ExampleCard(
                "列对齐与单列撑满",
                "仅一个业务列且无自动序号时，列宽覆盖为 Table viewport（忽略自身 width/minWidth/flex）。对齐 Start / Center / End，默认 Start。",
                { ctx.currentTheme() },
            ) {
                View {
                    attr { flexDirectionRow(); marginBottom(8f) }
                    listOf("左对齐", "居中", "右对齐").forEach { alignment ->
                        SegmentOption(alignment, active = {
                            when (alignment) {
                                "居中" -> ctx.alignmentColumn.alignment is ColumnAlignment.Center
                                "右对齐" -> ctx.alignmentColumn.alignment is ColumnAlignment.End
                                else -> ctx.alignmentColumn.alignment is ColumnAlignment.Start
                            }
                        }, theme = { ctx.currentTheme() }) {
                            ctx.alignmentColumn.alignment = when (alignment) {
                                "居中" -> ColumnAlignment.Center
                                "右对齐" -> ColumnAlignment.End
                                else -> ColumnAlignment.Start
                            }
                        }
                    }
                }
                ctx.renderTablePreview(this, listOf(ctx.alignmentColumn), { ctx.users.take(3) }, height = 210f, zebra = { false }, theme = ctx.currentTheme())
            }
            ExampleCard(
                "普通列与排序列",
                "sortable + sortComparator 挂在 ColumnModel；sortChange 只回写升/降/取消状态。无 comparator 时按 accessor 字符串比较。当前：${ctx.sortingDemoDescription()}",
                { ctx.currentTheme() },
            ) {
                ctx.renderTablePreview(
                    this,
                    ctx.sortingColumns,
                    { ctx.users.take(5) },
                    height = 286f,
                    zebra = { false },
                    theme = ctx.currentTheme(),
                    controlledSortState = { ctx.sortingDemoSortState },
                    onSortChange = { state -> ctx.sortingDemoSortState = state },
                )
            }
        }
    }

    private fun renderScrollSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(16f) }
            SectionIntro(
                "双向滚动",
                "表头纵滚固定（fixedHeader=false 无效）；横滑表头与表体同步。fixedFirstColumn 须显式 width + 固定行高；另含 scrollToTop 与触底加载更多。",
                { ctx.currentTheme() },
            )
            SettingSwitch(
                "固定列",
                {
                    if (ctx.scrollFixedFirstColumn) {
                        "fixedFirstColumn=true：首列显式 width + rowHeight；横滑时其余列移动"
                    } else {
                        "fixedFirstColumn=false：普通横向滚动"
                    }
                },
                { ctx.scrollFixedFirstColumn },
                { ctx.currentTheme() },
            ) {
                ctx.scrollFixedFirstColumn = it
            }
            View {
                attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); marginBottom(10f) }
                SegmentOption("回到顶部", active = { false }, theme = { ctx.currentTheme() }) {
                    ctx.scrollDemoTableRef?.view?.scrollToTop(animated = true)
                }
                SegmentOption("重置加载", active = { false }, theme = { ctx.currentTheme() }) {
                    ctx.scrollDemoLimit = 20
                    ctx.scrollDemoLoadingMore = false
                    ctx.scrollDemoTableRef?.view?.scrollToTop(animated = true)
                }
            }
            // 固定列开/关 remount，避免 pinned 与普通横滚热切换布局残留
            vif({ ctx.scrollFixedFirstColumn }) {
                ctx.renderTablePreview(
                    this,
                    listOf(ctx.nameColumn, ctx.ageColumn, ctx.wideEmailColumn, ctx.cityColumn, ctx.statusTextColumn),
                    { ctx.users.take(ctx.scrollDemoLimit) },
                    height = null,
                    fixedFirstColumn = { true },
                    fixedRowHeight = { true },
                    theme = ctx.currentTheme(),
                    hasMore = { ctx.scrollDemoLimit < ctx.users.size },
                    loadingMore = { ctx.scrollDemoLoadingMore },
                    onLoadMore = { ctx.loadMoreScrollRows() },
                    tableRef = { ctx.scrollDemoTableRef = it },
                )
            }
            vif({ !ctx.scrollFixedFirstColumn }) {
                ctx.renderTablePreview(
                    this,
                    listOf(ctx.nameColumn, ctx.ageColumn, ctx.wideEmailColumn, ctx.cityColumn, ctx.statusTextColumn),
                    { ctx.users.take(ctx.scrollDemoLimit) },
                    height = null,
                    fixedFirstColumn = { false },
                    fixedRowHeight = { true },
                    theme = ctx.currentTheme(),
                    hasMore = { ctx.scrollDemoLimit < ctx.users.size },
                    loadingMore = { ctx.scrollDemoLoadingMore },
                    onLoadMore = { ctx.loadMoreScrollRows() },
                    tableRef = { ctx.scrollDemoTableRef = it },
                )
            }
        }
    }

    private fun renderThemeSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.Scroller {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(20f) }
            SectionIntro(
                "主题定制",
                "attr.themeColors 覆盖本表配色；内置 Light / Dark / Blue 三套预设，个别颜色用预设.copy(...)。",
                { ctx.currentTheme() },
            )
            ExampleCard("Light", "TableThemeColors.Light：默认浅色预设。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, ctx.columns3, { ctx.users.take(3) }, 210f, theme = TableThemeColors.Light)
            }
            ExampleCard("Dark", "TableThemeColors.Dark：深色预设。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, ctx.columns3, { ctx.users.take(3) }, 210f, theme = TableThemeColors.Dark)
            }
            ExampleCard("Blue", "TableThemeColors.Blue：蓝色预设。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, ctx.columns3, { ctx.users.take(3) }, 210f, theme = TableThemeColors.Blue)
            }
        }
    }

    private fun renderRendererSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.Scroller {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(20f) }
            SectionIntro(
                "自定义 renderer",
                "cellRenderer 可选；未配置走默认 Text。行/单元格点击由 enableRowClick / enableCellClick 显式控制，与是否配置 renderer 无关。",
                { ctx.currentTheme() },
            )
            ExampleCard(
                "默认文本",
                "未配置 cellRenderer：accessor 结果以默认 Text 渲染（fallback）。",
                { ctx.currentTheme() },
            ) {
                ctx.renderTablePreview(
                    this,
                    listOf(
                        ColumnModel(key = "name", title = "姓名", accessor = { it.name }, minWidth = 80f, flex = 1f),
                        ctx.statusTextColumn,
                        ColumnModel(
                            key = "age",
                            title = "年龄",
                            accessor = { it.age.toString() },
                            width = 80f,
                        ),
                    ),
                    { ctx.users.take(3) },
                    210f,
                    theme = ctx.currentTheme(),
                )
            }
            ExampleCard(
                "传入业务 View",
                "cellRenderer 由使用方绘制单元格；自定义内容不自动带 Web title。操作列通常关闭 enableRowClick / enableCellClick，由内部控件自行处理点击。",
                { ctx.currentTheme() },
            ) {
                SettingSwitch(
                    "姓名列 enableRowClick",
                    {
                        if (ctx.demoEnableRowClick) "true：命中该列可触发 rowClick"
                        else "false：命中该列不走行点击"
                    },
                    { ctx.demoEnableRowClick },
                    { ctx.currentTheme() },
                ) { enabled -> ctx.demoEnableRowClick = enabled }
                SettingSwitch(
                    "姓名列 enableCellClick",
                    {
                        if (ctx.demoEnableCellClick) "true：命中该列优先触发 cellClick"
                        else "false：不单独发 cellClick"
                    },
                    { ctx.demoEnableCellClick },
                    { ctx.currentTheme() },
                ) { enabled -> ctx.demoEnableCellClick = enabled }
                ctx.renderTablePreview(
                    this,
                    listOf(
                        ctx.avatarColumn,
                        ColumnModel(
                            key = "name",
                            title = "姓名",
                            accessor = { it.name },
                            minWidth = 72f,
                            flex = 1f,
                            enableRowClick = ctx.demoEnableRowClick,
                            enableCellClick = ctx.demoEnableCellClick,
                            cellRenderer = { user, _ ->
                                val theme = this@TableBasicDemoPage.currentTheme()
                                View {
                                    attr {
                                        flex(1f)
                                        justifyContentCenter()
                                    }
                                    Text {
                                        attr {
                                            text(user.name)
                                            fontSize(14f)
                                            color(Color(theme.cellText))
                                            lines(1)
                                            textOverFlowTail()
                                        }
                                    }
                                }
                            },
                        ),
                        ctx.statusTagColumn,
                        ctx.actionColumn,
                    ),
                    { ctx.users.take(3) },
                    240f,
                    fixedRowHeight = { true },
                    theme = ctx.currentTheme(),
                )
            }
        }
    }

    private fun renderStateSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(16f) }
            SectionIntro(
                "状态反馈",
                "loading / empty 由状态层承接；可传 emptyRenderer / loadingRenderer 覆盖默认。loading 期间保留旧内容并降低交互。",
                { ctx.currentTheme() },
            )
            View {
                attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); marginBottom(10f) }
                listOf("正常", "加载中", "无数据").forEach { state ->
                    SegmentOption(state, active = { ctx.tableStateLabel() == state }, theme = { ctx.currentTheme() }) {
                        ctx.tableState = when (state) {
                            "加载中" -> "加载"
                            "无数据" -> "空"
                            else -> "正常"
                        }
                    }
                }
            }
            SettingSwitch(
                "自定义 Empty / Loading",
                {
                    if (ctx.customStateRendererOn) {
                        "已配置 emptyRenderer / loadingRenderer"
                    } else {
                        "未配置：使用 Table 默认空态 / 加载态"
                    }
                },
                { ctx.customStateRendererOn },
                { ctx.currentTheme() },
            ) { enabled ->
                ctx.customStateRendererOn = enabled
                // 开关只影响空态/加载态；若当前是正常，自动切到无数据以便立刻看到差异
                if (enabled && ctx.tableState == "正常") {
                    ctx.tableState = "空"
                }
            }
            vif({ ctx.tableState == "正常" }) {
                ctx.renderTablePreview(this, ctx.basicColumns, { ctx.users.take(6) }, null, theme = ctx.currentTheme())
            }
            vif({ ctx.tableState == "加载" }) {
                ctx.renderTablePreview(
                    this,
                    ctx.basicColumns,
                    { ctx.users.take(6) },
                    null,
                    loading = { true },
                    theme = ctx.currentTheme(),
                    customStateRenderer = { ctx.customStateRendererOn },
                )
            }
            vif({ ctx.tableState == "空" }) {
                ctx.renderTablePreview(
                    this,
                    ctx.basicColumns,
                    { emptyList() },
                    null,
                    theme = ctx.currentTheme(),
                    customStateRenderer = { ctx.customStateRendererOn },
                )
            }
        }
    }

    /**
     * Showcase 在纵向 Scroller 内时，子 View 默认按内容收缩，Table 拿不到真实容器宽，
     * flex 列就分不到「剩余空间」。必须走组件官方 [TableAttr.tableWidth]，
     * 写成页宽减去章节左右 padding（Scroller paddingLeft/Right 各 16）。
     */
    private fun previewContentWidth(): Float =
        (pagerData.pageViewWidth - 32f).coerceAtLeast(200f)

    private fun renderTablePreview(
        container: ViewContainer<*, *>,
        columns: List<ColumnModel<User>>,
        data: () -> List<User>,
        height: Float?,
        lineMode: () -> TableLineMode = { TableLineMode.Grid },
        cornerRadius: () -> Float = { TableCornerRadius.Default },
        zebra: () -> Boolean = { false },
        fixedRowHeight: () -> Boolean = { false },
        fixedFirstColumn: () -> Boolean = { false },
        theme: TableThemeColors,
        loading: () -> Boolean = { false },
        overflowEnabled: () -> Boolean = { false },
        controlledSortState: () -> TableSortState = { sortState },
        onSortChange: (TableSortState) -> Unit = { state -> sortState = state },
        customStateRenderer: () -> Boolean = { false },
        hasMore: () -> Boolean = { false },
        loadingMore: () -> Boolean = { false },
        onLoadMore: (() -> Unit)? = null,
        tableRef: ((ViewRef<TableView<User>>) -> Unit)? = null,
        /**
         * true（默认）：设 tableWidth=预览区宽，弹性列才能分到剩余空间；
         * 全固定列时表框仍满宽，框内右侧可留白。
         */
        fillPreviewWidth: Boolean = true,
    ) {
        val ctx = this
        val previewWidth = ctx.previewContentWidth()
        container.TableView<User> {
            ref {
                @Suppress("UNCHECKED_CAST")
                tableRef?.invoke(it as ViewRef<TableView<User>>)
            }
            attr {
                // 必须用 tableWidth：body 根节点只认它；单设 ComposeAttr.width 在 Scroller 里常塌成内容宽
                tableWidth = if (fillPreviewWidth) previewWidth else null
                if (height == null) flex(1f) else height(height)
                this.columns = ObservableList(columns.toMutableList())
                this.data = data()
                rowKey = { user -> user.id }
                // 在 attr 内读取 lambda，才能订阅页面 observable，切换外框/圆角等才会生效
                zebraStripe = zebra()
                this.lineMode = lineMode()
                this.cornerRadius = cornerRadius()
                cellPaddingH = if (ctx.compactPadding) 8f else 12f
                cellPaddingV = if (ctx.compactPadding) 6f else 10f
                rowHeight = if (fixedRowHeight()) 48f else 0f
                sortState = controlledSortState()
                autoIndexColumn = false
                this.fixedHeader = true
                this.fixedFirstColumn = fixedFirstColumn()
                themeColors = theme
                this.loading = loading()
                emptyText = "暂无员工数据"
                loadingText = "正在加载员工数据"
                if (customStateRenderer()) {
                    emptyRenderer = { ctx.renderCustomEmptyState(this) }
                    loadingRenderer = { ctx.renderCustomLoadingState(this) }
                } else {
                    emptyRenderer = null
                    loadingRenderer = null
                }
                this.hasMore = hasMore()
                this.loadingMore = loadingMore()
                loadMoreThresholdRows = 3
                enableOverflowCellClick = overflowEnabled()
                headerStyle = if (ctx.compactHeader) TableHeaderStyle(13f, TableHeaderFontWeight.Bold, 8f, 6f, 40f) else TableHeaderStyle.Default
            }
            event {
                rowClick = { user -> ctx.bridgeModule.toast("行点击: ${user.name}") }
                cellClick = { info ->
                    ctx.bridgeModule.toast("单元格点击: ${info.columnKey} / ${info.rowData.name}")
                }
                cellLongPress = { info ->
                    if (info.text.isNotEmpty()) {
                        ctx.bridgeModule.copyToPasteboard(info.text)
                        ctx.bridgeModule.toast("已复制")
                    }
                }
                overflowCellClick = { info -> ctx.showOverflowTip(info) }
                overflowTipDismiss = { ctx.hideOverflowTip() }
                sortChange = onSortChange
                loadMore = { onLoadMore?.invoke() }
            }
        }
    }

    private fun loadMoreScrollRows() {
        if (scrollDemoLoadingMore || scrollDemoLimit >= users.size) return
        // 模拟异步请求：loadingMore=true 期间 Table 不会再次触底，避免同一手势连发
        scrollDemoLoadingMore = true
        setTimeout(120) {
            scrollDemoLimit = min(scrollDemoLimit + 8, users.size)
            scrollDemoLoadingMore = false
        }
    }

    private fun tableStateLabel(): String = when (tableState) {
        "加载" -> "加载中"
        "空" -> "无数据"
        else -> "正常"
    }

    private fun renderCustomEmptyState(container: ViewContainer<*, *>) {
        val theme = currentTheme()
        container.View {
            attr {
                allCenter()
                padding(16f)
            }
            Text {
                attr {
                    text("暂无可展示员工")
                    fontSize(16f)
                    fontWeightSemiBold()
                    color(Color(theme.actionText))
                    marginBottom(6f)
                }
            }
            Text {
                attr {
                    text("这是使用方 emptyRenderer，自定义空态内容")
                    fontSize(12f)
                    color(Color(theme.cellTextSecondary))
                }
            }
        }
    }

    private fun renderCustomLoadingState(container: ViewContainer<*, *>) {
        val theme = currentTheme()
        container.View {
            attr {
                allCenter()
                padding(16f)
            }
            ActivityIndicator {
                attr {
                    isGrayStyle(false)
                    marginBottom(8f)
                }
            }
            Text {
                attr {
                    text("自定义加载中")
                    fontSize(16f)
                    fontWeightSemiBold()
                    color(Color(theme.actionText))
                    marginBottom(6f)
                }
            }
            Text {
                attr {
                    text("这是使用方 loadingRenderer")
                    fontSize(12f)
                    color(Color(theme.cellTextSecondary))
                }
            }
        }
    }

    /** 专用溢出演示：浮层锚在表格容器内，坐标相对单元格，避免页面级硬编码偏移。 */
    private fun renderOverflowDemo(container: ViewContainer<*, *>) {
        val ctx = this
        // 姓名/年龄固定 width；邮箱 width=null + minWidth/flex。长邮箱在剩余宽内仍可能截断。
        val overflowColumns = listOf(
            ColumnModel<User>(
                key = "ovName",
                title = "姓名",
                accessor = { it.name },
                width = 72f,
            ),
            ColumnModel<User>(
                key = "ovAge",
                title = "年龄",
                accessor = { it.age.toString() },
                width = 56f,
            ),
            ColumnModel<User>(
                key = "ovEmail",
                title = "邮箱",
                accessor = { it.email },
                minWidth = 120f,
                flex = 1f,
            ),
        )
        container.View {
            attr {
                positionRelative()
                width(ctx.previewContentWidth())
                height(248f)
            }
            ctx.renderTablePreview(
                this,
                overflowColumns,
                { ctx.users.take(4) },
                height = 248f,
                theme = ctx.currentTheme(),
                overflowEnabled = { true },
            )
            ctx.renderOverflowTip(this)
        }
    }

    private fun renderOverflowTip(container: ViewContainer<*, *>) {
        val ctx = this
        val tipColor = Color(0xEE222222)
        container.View {
            attr {
                absolutePositionAllZero()
                zIndex(30)
                visibility(ctx.overflowTipVisible)
                touchEnable(ctx.overflowTipVisible)
            }
            event { click { ctx.hideOverflowTip() } }
            // 箭头 + 气泡同一绝对锚点；复制改由单元格 cellLongPress，浮层只展示全文
            View {
                attr {
                    absolutePosition(left = ctx.overflowTipLeft, top = ctx.overflowTipTop)
                    width(ctx.overflowTipBubbleWidth)
                }
                event {
                    click {
                        // 消费浮层点击，避免点文字就关
                    }
                }
                Image {
                    attr {
                        marginLeft(ctx.overflowTipArrowLeft)
                        size(TIP_ARROW_WIDTH, TIP_ARROW_HEIGHT)
                        src(TIP_ARROW_UP_BASE64)
                    }
                }
                View {
                    attr {
                        paddingTop(8f)
                        paddingBottom(8f)
                        paddingLeft(10f)
                        paddingRight(10f)
                        borderRadius(8f)
                        backgroundColor(tipColor)
                    }
                    Text {
                        attr {
                            text(ctx.overflowTipText)
                            fontSize(12f)
                            lineHeight(17f)
                            color(Color.WHITE)
                        }
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

    private fun tableLineModeLabel(): String = when (tableLineMode) {
        is TableLineMode.None -> "无线"
        is TableLineMode.Horizontal -> "仅横线"
        is TableLineMode.Grid -> "网格"
        is TableLineMode.Custom -> "自定义"
    }

    private fun lineModeForLabel(mode: String): TableLineMode = when (mode) {
        "仅横线" -> TableLineMode.Horizontal
        "网格" -> TableLineMode.Grid
        "自定义" -> TableLineMode.Custom(demoCustomLineStyle(customLineColor))
        else -> TableLineMode.None
    }

    private fun applyCustomLineColor(color: Long) {
        customLineColor = color
        tableLineMode = TableLineMode.Custom(demoCustomLineStyle(color))
    }

    private fun demoCustomLineStyle(color: Long = customLineColor): TableLineStyle {
        val stroke = TableStroke(color, 2f)
        return TableLineStyle(
            outer = stroke,
            header = stroke,
            row = stroke,
            column = stroke,
        )
    }

    private fun sortDescription(): String = when (sortState.direction) {
        is TableSortDirection.None -> "未排序"
        is TableSortDirection.Ascending -> "${sortColumnTitle()}升序"
        is TableSortDirection.Descending -> "${sortColumnTitle()}降序"
    }

    private fun sortColumnTitle(): String =
        activeColumns.firstOrNull { it.key == sortState.columnKey }?.title ?: ""

    private fun sortingDemoDescription(): String = when (sortingDemoSortState.direction) {
        is TableSortDirection.None -> "未排序"
        is TableSortDirection.Ascending -> "年龄升序"
        is TableSortDirection.Descending -> "年龄降序"
    }

    private fun selectExample(example: String) {
        activeExample = example
        tableState = when (example) {
            "无数据" -> "空"
            "加载中" -> "加载"
            else -> "正常"
        }
        wideTable = example != "基础样式"
        if (example == "主题定制") {
            themeMode = DemoThemeMode.Blue
        }
        if (example == "基础样式") {
            tableLineMode = TableLineMode.Grid
        }
        selectedColumn = ageColumn
        syncActiveColumns()
        hideOverflowTip()
    }

    private fun selectTheme(theme: DemoThemeMode) {
        themeMode = theme
        activeExample = "主题定制"
        tableState = "正常"
        wideTable = true
        selectedColumn = ageColumn
        syncActiveColumns()
        hideOverflowTip()
    }

    private fun exampleDescription(): String = when (activeExample) {
        "基础样式" -> "列定义、边框、内边距与列对齐"
        "双向滚动" -> "表头纵滚固定；横滑同步；可选固定列与触底加载"
        "主题定制" -> "themeColors 覆盖表头、边框、行背景等配色"
        "自定义渲染" -> "cellRenderer 插槽；未配置走默认 Text"
        "无数据" -> "empty 状态层（可 emptyRenderer 覆盖）"
        "加载中" -> "loading 状态层：保留旧内容并降低交互"
        else -> "选择一个场景查看对应能力"
    }

    private fun resolveStatusTagStyle(status: String): TableStatusTagStyle =
        statusTagStyleByText(currentTheme())[status]
            ?: TableStatusTagStyle.fromPreset(TableStatusTagPreset.fromText(status), currentTheme())

    private fun statusTagStyleByText(themeColors: TableThemeColors): Map<String, TableStatusTagStyle> = mapOf(
        "在职" to TableStatusTagStyle(themeColors.statusTagInfoBackground, themeColors.statusTagInfoText),
        "休假" to TableStatusTagStyle(themeColors.statusTagNeutralBackground, themeColors.statusTagNeutralText),
        "离职" to TableStatusTagStyle(themeColors.statusTagBackgroundAlt, themeColors.statusTagTextAlt),
    )

    private fun currentColumns(): List<ColumnModel<User>> =
        if (wideTable) {
            listOf(nameColumn, ageColumn, emailColumn, cityColumn, statusTextColumn)
        } else {
            columns3
        }

    private fun currentData(): List<User> = if (tableState == "空") emptyList() else users

    private fun syncActiveColumns() {
        activeColumns = ObservableList(currentColumns().toMutableList())
    }

    private fun applyCornerRadius(radius: Float) {
        tableCornerRadius = radius.coerceAtLeast(0f)
        cornerRadiusInputRef?.view?.setText(formatCornerRadius(tableCornerRadius))
    }

    private fun onCornerRadiusInputChanged(text: String) {
        val parsed = text.trim().toFloatOrNull() ?: return
        if (parsed < 0f) return
        tableCornerRadius = parsed
    }

    private fun formatCornerRadius(radius: Float): String =
        if (radius == radius.toLong().toFloat()) radius.toLong().toString() else radius.toString()

    private fun hideOverflowTip() {
        overflowTipVisible = false
        overflowTipText = ""
    }

    private fun showOverflowTip(info: TableOverflowCellInfo<User>) {
        // 仅用组件给出的相对 Table 根坐标；浮层须放在 Table 外包 positionRelative 容器内
        val bubbleWidth = max(info.estimatedCellWidth, 160f).coerceAtMost(280f)
        val tipLeft = max(0f, info.estimatedCellX)
        val cellCenter = info.estimatedCellX + info.estimatedCellWidth / 2f
        overflowTipText = info.text
        overflowTipBubbleWidth = bubbleWidth
        overflowTipLeft = tipLeft
        overflowTipTop = info.estimatedCellY + info.estimatedCellHeight + TIP_GAP
        overflowTipArrowLeft = min(
            max(cellCenter - tipLeft - TIP_ARROW_HALF_WIDTH, 8f),
            bubbleWidth - TIP_ARROW_WIDTH - 8f,
        )
        overflowTipVisible = true
    }

    companion object {
        /** 气泡与单元格之间的视觉间距（浮层自身 UI，不是页面布局猜测）。 */
        private const val TIP_GAP = 2f
        private const val TIP_ARROW_WIDTH = 14f
        private const val TIP_ARROW_HEIGHT = 7f
        private const val TIP_ARROW_HALF_WIDTH = 7f
        private const val TIP_ARROW_UP_BASE64 =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAHCAYAAAA4R3wZAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAA4SURBVChTY2DAA5SUlOTQxQgCJSWlSCUlpXcgGl0OJ0DSBMOENWPRRFgzHk24NROhCVMzCZrgmgGyDjlJ4g18hQAAAABJRU5ErkJggg=="
    }

}

private sealed class DemoThemeMode {
    object Light : DemoThemeMode()
    object Dark : DemoThemeMode()
    object Blue : DemoThemeMode()
}

private fun ViewContainer<*, *>.ShowcaseTab(
    label: String,
    active: () -> Boolean,
    theme: () -> TableThemeColors,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(40f)
            paddingLeft(14f)
            paddingRight(14f)
            marginRight(8f)
            borderRadius(20f)
            allCenter()
            backgroundColor(Color(if (active()) theme().actionText else theme().cardBackground))
            border(Border(1f, BorderStyle.SOLID, Color(if (active()) theme().actionText else theme().cardBorder)))
        }
        Text {
            attr {
                text(label)
                fontSize(13f)
                color(Color(if (active()) theme().actionTextOnFill else theme().cellText))
            }
        }
        event { click { onClick() } }
    }
}

private fun ViewContainer<*, *>.SectionIntro(
    title: String,
    description: String,
    theme: () -> TableThemeColors,
) {
    Text {
        attr {
            text(title)
            fontSize(18f)
            fontWeightSemiBold()
            color(Color(theme().cellText))
            marginTop(12f)
        }
    }
    Text {
        attr {
            text(description)
            fontSize(12f)
            color(Color(theme().cellTextSecondary))
            marginTop(4f)
            marginBottom(12f)
        }
    }
}

private fun ViewContainer<*, *>.ExampleCard(
    title: String,
    description: String,
    theme: () -> TableThemeColors,
    content: ViewBuilder,
) {
    View {
        attr { marginBottom(12f) }
        Text {
            attr {
                text(title)
                fontSize(14f)
                fontWeightSemiBold()
                color(Color(theme().cellText))
            }
        }
        Text {
            attr {
                text(description)
                fontSize(11f)
                color(Color(theme().cellTextSecondary))
                marginTop(3f)
                marginBottom(8f)
            }
        }
        View {
            attr { flex(1f); alignSelfStretch() }
            content()
        }
    }
}

private fun ViewContainer<*, *>.SettingSwitch(
    title: String,
    description: () -> String,
    checked: () -> Boolean,
    theme: () -> TableThemeColors,
    onChange: (Boolean) -> Unit,
) {
    View {
        attr {
            minHeight(52f)
            flexDirectionRow()
            alignItemsCenter()
            marginBottom(6f)
        }
        View {
            attr { flex(1f); marginRight(12f) }
            Text { attr { text(title); fontSize(13f); color(Color(theme().cellText)) } }
            Text {
                attr {
                    text(description())
                    fontSize(11f)
                    color(Color(theme().cellTextSecondary))
                    marginTop(2f)
                }
            }
        }
        Switch {
            attr {
                size(40f, 24f)
                isOn(checked())
                onColor(Color(theme().actionText))
                unOnColor(Color(theme().gridLine))
            }
            event { switchOnChanged { onChange(it) } }
        }
    }
}

private fun ViewContainer<*, *>.SegmentOption(
    label: String,
    active: () -> Boolean,
    theme: () -> TableThemeColors,
    onClick: () -> Unit,
) {
    View {
        attr {
            flex(1f)
            height(40f)
            allCenter()
            backgroundColor(Color(if (active()) theme().actionText else theme().cardBackground))
            border(Border(1f, BorderStyle.SOLID, Color(theme().cardBorder)))
        }
        Text { attr { text(label); fontSize(12f); color(Color(if (active()) theme().actionTextOnFill else theme().cellText)) } }
        event { click { onClick() } }
    }
}

/**
 * 可点击的配置选项（chip 样式），顶层扩展函数。
 * label / active 传 lambda（在 attr 块内调用），保证响应式更新。
 */
private fun ViewContainer<*, *>.ToggleChip(
    label: () -> String,
    active: () -> Boolean,
    theme: () -> TableThemeColors,
    emphasis: Boolean = false,
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
            borderRadius(if (emphasis) 8f else 16f)
            backgroundColor(
                Color(
                    if (active()) {
                        if (emphasis) theme().actionText else theme().rowBackgroundAlt
                    } else {
                        theme().cardBackground
                    },
                ),
            )
            border(Border(1f, BorderStyle.SOLID, Color(if (active()) theme().actionText else theme().cardBorder)))
        }
        Text {
            attr {
                text(label())
                fontSize(12f)
                color(
                    Color(
                        if (active()) {
                            if (emphasis) theme().actionTextOnFill else theme().actionText
                        } else {
                            theme().cellText
                        },
                    ),
                )
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
