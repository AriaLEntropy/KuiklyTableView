package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
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
        width = 80f,
        alignment = ColumnAlignment.End,
        sortable = true,
        sortComparator = compareBy { it.age },
    )

    private val nameColumn = ColumnModel<User>(
        key = "name",
        title = "姓名",
        accessor = { it.name },
        width = 80f,
        sortable = true,
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
            alignment = ColumnAlignment.End,
        ),
        ColumnModel<User>(
            key = "basicEmail",
            title = "邮箱",
            accessor = { it.email },
            minWidth = 140f,
            flex = 2f,
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
            title = "姓名（普通列）",
            accessor = { it.name },
            minWidth = 140f,
            sortable = false,
        ),
        ColumnModel<User>(
            key = "sortableAge",
            title = "年龄（可排序）",
            accessor = { it.age.toString() },
            minWidth = 140f,
            alignment = ColumnAlignment.End,
            sortable = true,
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
    private var fixedHeaderOn by observable(true)
    private var sortState by observable(TableSortState())
    private var sortingDemoSortState by observable(TableSortState())
    private var themeMode: DemoThemeMode by observable(DemoThemeMode.Light)
    private var compactHeader by observable(false)
    private var customStatusRendererOn by observable(true)
    private var displayMode: TableDisplayMode by observable(TableDisplayMode.Table)
    private var tableState by observable("正常")
    private var customStateRendererOn by observable(false)
    private var overflowTipOn by observable(true)
    private var notifyStateById: Map<Int, Boolean> by observable(users.associate { it.id to (it.name.hashCode() % 2 == 0) })
    private var overflowTipVisible by observable(false)
    private var overflowTipText by observable("")
    private var overflowTipLeft by observable(24f)
    private var overflowTipTop by observable(220f)
    private var overflowTipArrowLeft by observable(40f)
    private var scrollDemoTable: TableView<User>? = null
    private var scrollDemoLimit by observable(12)
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
                vif({ ctx.activeSection == "模式" }) { ctx.renderMobileSection(this) }
                vif({ ctx.activeSection == "Playground" }) { ctx.renderPlaygroundSection(this) }
            }
            ctx.renderOverflowTip(this)
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
                        ToggleChip(label = { "List 模式" }, active = { ctx.activeExample == "List 模式" }, theme = { ctx.currentTheme() }) {
                            ctx.selectExample("List 模式")
                        }
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
                            text("排序：点击姓名或年龄表头切换升序、降序、取消；当前 ${ctx.sortDescription()}")
                            fontSize(12f)
                            color(Color(ctx.currentTheme().cellTextSecondary))
                            marginBottom(6f)
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(label = { "固定表头 ${if (ctx.fixedHeaderOn) "开" else "关"}" }, active = { ctx.fixedHeaderOn }, theme = { ctx.currentTheme() }) {
                            ctx.fixedHeaderOn = !ctx.fixedHeaderOn
                        }
                    }
                }

                ConfigGroup("表格样式", theme = { ctx.currentTheme() }) {
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); alignItemsCenter() }
                        ToggleChip(label = { "斑马纹 ${if (ctx.zebraOn) "开" else "关"}" }, active = { ctx.zebraOn }, theme = { ctx.currentTheme() }) {
                            ctx.zebraOn = !ctx.zebraOn
                        }
                        ToggleChip(label = { "网格边框 ${if (ctx.borderedOn) "开" else "关"}" }, active = { ctx.borderedOn }, theme = { ctx.currentTheme() }) {
                            ctx.borderedOn = !ctx.borderedOn
                        }
                        ToggleChip(label = { "表头 ${if (ctx.compactHeader) "紧凑" else "标准"}" }, active = { ctx.compactHeader }, theme = { ctx.currentTheme() }) {
                            ctx.compactHeader = !ctx.compactHeader
                        }
                    }
                }

                ConfigGroup("扩展能力", theme = { ctx.currentTheme() }) {
                    View {
                        attr {
                            minHeight(44f)
                            flexDirectionRow()
                            alignItemsCenter()
                            marginBottom(8f)
                        }
                        View {
                            attr { flex(1f); marginRight(12f) }
                            Text {
                                attr {
                                    text("状态标签 renderer")
                                    fontSize(13f)
                                    color(Color(ctx.currentTheme().cellText))
                                }
                            }
                            Text {
                                attr {
                                    text(if (ctx.customStatusRendererOn) "开启：Demo 绘制彩色标签" else "关闭：Table 渲染普通文本")
                                    fontSize(11f)
                                    color(Color(ctx.currentTheme().cellTextSecondary))
                                    marginTop(3f)
                                }
                            }
                        }
                        Switch {
                            attr {
                                size(40f, 24f)
                                isOn(ctx.customStatusRendererOn)
                                onColor(Color(ctx.currentTheme().actionText))
                                unOnColor(Color(ctx.currentTheme().gridLine))
                            }
                            event {
                                switchOnChanged { enabled -> ctx.selectStatusRenderer(enabled) }
                            }
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        ToggleChip(label = { "溢出提示 ${if (ctx.overflowTipOn) "开" else "关"}" }, active = { ctx.overflowTipOn }, theme = { ctx.currentTheme() }) {
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
                        rowKey = { user -> user.id }
                        zebraStripe = ctx.zebraOn
                        bordered = ctx.borderedOn
                        cellPaddingH = if (ctx.compactPadding) 8f else 12f
                        cellPaddingV = if (ctx.compactPadding) 6f else 10f
                        rowHeight = if (ctx.fixedRowHeight) 48f else 0f
                        data = ctx.currentData()
                        sortState = ctx.sortState
                        autoIndexColumn = false
                        fixedHeader = ctx.fixedHeaderOn
                        fixedColumnCount = 0
                        themeColors = ctx.currentTheme()
                        displayMode = ctx.displayMode
                        listPrimaryColumnKey = "name"
                        listStatusColumnKey = "status"
                        listStatusTagStyleByText = ctx.statusTagStyleByText(ctx.currentTheme())
                        loading = ctx.tableState == "加载"
                        emptyText = "暂无员工数据"
                        loadingText = "正在加载员工数据"
                        if (ctx.customStateRendererOn) {
                            emptyRenderer = { ctx.renderCustomEmptyState(this) }
                            loadingRenderer = { ctx.renderCustomLoadingState(this) }
                        }
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
                        src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAICAYAAADwdn+XAAAANElEQVR42mNgwAOUlJTewTADqQBZM8mGYNNMtCH4NBM0hBjNOA0hRTOGIeRohhtCiWYQBgBrGmjtRbMqEAAAAABJRU5ErkJggg==")
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
                    text("按场景查看用法；完整运行时配置集中在 Playground")
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
            listOf("基础", "滚动", "主题", "自定义", "状态", "模式", "Playground").forEach { section ->
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
            SectionIntro("基础表格", "多个固定示例直接对照，不需要记忆切换前后的样式。", { ctx.currentTheme() })
            ExampleCard("默认样式", "3 列 × 4 行，使用默认文本、水平分隔线和自适应行高。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, ctx.basicColumns, ctx.users.take(4), height = 248f, zebra = false, theme = ctx.currentTheme())
            }
            ExampleCard("边框与斑马纹", "同一份数据开启完整网格和斑马纹。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, ctx.basicColumns, ctx.users.take(4), height = 248f, bordered = true, zebra = true, theme = ctx.currentTheme())
            }
            ExampleCard("列对齐与单列撑满", "该列配置 width=180，但作为唯一列时会使用 Table 全宽；选择对齐方式观察实时变化。", { ctx.currentTheme() }) {
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
                ctx.renderTablePreview(this, listOf(ctx.alignmentColumn), ctx.users.take(3), height = 210f, bordered = true, zebra = false, theme = ctx.currentTheme())
            }
            ExampleCard("普通列与排序列", "姓名是普通列；点击年龄表头依次切换未排序、升序、降序。当前：${ctx.sortingDemoDescription()}", { ctx.currentTheme() }) {
                ctx.renderTablePreview(
                    this,
                    ctx.sortingColumns,
                    ctx.users.take(5),
                    height = 286f,
                    zebra = false,
                    theme = ctx.currentTheme(),
                    controlledSortState = ctx.sortingDemoSortState,
                    onSortChange = { state -> ctx.sortingDemoSortState = state },
                )
            }
        }
    }

    private fun renderScrollSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(16f) }
            SectionIntro("双向滚动", "5 列 × 20 行。横向滚动时表头同步，纵向滚动验证固定表头、滚动控制和轻量加载更多。", { ctx.currentTheme() })
            SettingSwitch("固定表头", if (ctx.fixedHeaderOn) "表头固定在列表上方" else "表头随内容滚动", ctx.fixedHeaderOn, { ctx.currentTheme() }) {
                ctx.fixedHeaderOn = it
            }
            View {
                attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP); marginBottom(10f) }
                SegmentOption("回到顶部", active = { false }, theme = { ctx.currentTheme() }) {
                    ctx.scrollDemoTable?.scrollToTop(animated = true)
                }
                SegmentOption("滚到第16行", active = { false }, theme = { ctx.currentTheme() }) {
                    ctx.scrollDemoTable?.scrollToRow(15, animated = true)
                }
                SegmentOption("重置加载", active = { false }, theme = { ctx.currentTheme() }) {
                    ctx.scrollDemoLimit = 12
                    ctx.scrollDemoLoadingMore = false
                    ctx.scrollDemoTable?.scrollToTop(animated = true)
                }
            }
            View {
                attr { flex(1f); borderRadius(12f); overflow(true); border(Border(1f, BorderStyle.SOLID, Color(ctx.currentTheme().cardBorder))) }
                ctx.renderTablePreview(
                    this,
                    listOf(ctx.nameColumn, ctx.ageColumn, ctx.wideEmailColumn, ctx.cityColumn, ctx.statusTextColumn),
                    ctx.users.take(ctx.scrollDemoLimit),
                    height = null,
                    fixedHeader = ctx.fixedHeaderOn,
                    fixedRowHeight = true,
                    theme = ctx.currentTheme(),
                    hasMore = ctx.scrollDemoLimit < ctx.users.size,
                    loadingMore = ctx.scrollDemoLoadingMore,
                    onLoadMore = { ctx.loadMoreScrollRows() },
                    tableRef = { ctx.scrollDemoTable = it },
                )
            }
        }
    }

    private fun renderThemeSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.Scroller {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(20f) }
            SectionIntro("主题定制", "相同结构使用三套语义色，直接比较而不是切换整页主题。", { ctx.currentTheme() })
            ExampleCard("Light", "默认浅色语义角色。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, ctx.columns3, ctx.users.take(3), 210f, theme = TableThemeColors.Light)
            }
            ExampleCard("Dark", "暗色背景、文字和分隔线保持可读。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, ctx.columns3, ctx.users.take(3), 210f, theme = TableThemeColors.Dark)
            }
            ExampleCard("Blue", "展示使用方整体覆盖语义色。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, ctx.columns3, ctx.users.take(3), 210f, theme = TableThemeColors.Blue)
            }
        }
    }

    private fun renderRendererSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.Scroller {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(20f) }
            SectionIntro("自定义 renderer", "上方是不传 renderer 的默认文本；下方标签、头像和 Switch 都由 Demo 使用方代码绘制。", { ctx.currentTheme() })
            ExampleCard("默认文本 fallback", "状态列未配置 cellRenderer。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, listOf(ctx.nameColumn, ctx.statusTextColumn, ctx.ageColumn), ctx.users.take(3), 210f, theme = ctx.currentTheme())
            }
            ExampleCard("使用方 renderer", "cellRenderer 嵌入头像、彩色状态标签和可交互 Switch。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, listOf(ctx.avatarColumn, ctx.nameColumn, ctx.statusRendererColumn, ctx.notifyColumn), ctx.users.take(3), 210f, theme = ctx.currentTheme())
            }
        }
    }

    private fun renderStateSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(16f) }
            SectionIntro("状态反馈", "同一组 Table 配置切换正常、加载和空数据路径，并验证默认状态层与自定义 renderer fallback。", { ctx.currentTheme() })
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
                SegmentOption("默认状态层", active = { !ctx.customStateRendererOn }, theme = { ctx.currentTheme() }) {
                    ctx.customStateRendererOn = false
                }
                SegmentOption("自定义 renderer", active = { ctx.customStateRendererOn }, theme = { ctx.currentTheme() }) {
                    ctx.customStateRendererOn = true
                }
            }
            View {
                attr { flex(1f); borderRadius(12f); overflow(true); border(Border(1f, BorderStyle.SOLID, Color(ctx.currentTheme().cardBorder))) }
                vif({ ctx.tableState == "正常" }) {
                    ctx.renderTablePreview(this, ctx.columns3, ctx.users.take(6), null, theme = ctx.currentTheme())
                }
                vif({ ctx.tableState == "加载" }) {
                    ctx.renderTablePreview(this, ctx.columns3, ctx.users.take(6), null, loading = true, theme = ctx.currentTheme(), customStateRenderer = ctx.customStateRendererOn)
                }
                vif({ ctx.tableState == "空" }) {
                    ctx.renderTablePreview(this, ctx.columns3, emptyList(), null, theme = ctx.currentTheme(), customStateRenderer = ctx.customStateRendererOn)
                }
            }
        }
    }

    private fun renderMobileSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.Scroller {
            attr { flex(1f); paddingLeft(16f); paddingRight(16f); paddingBottom(20f) }
            SectionIntro("Table / List 模式", "相同数据分别使用 Table 模式和 List 模式展示。", { ctx.currentTheme() })
            ExampleCard("Table", "适合横向对比和报表。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, listOf(ctx.nameColumn, ctx.statusTextColumn, ctx.ageColumn), ctx.users.take(3), 210f, theme = ctx.currentTheme())
            }
            ExampleCard("List 模式", "显式选择 List 模式，不按列数自动切换。", { ctx.currentTheme() }) {
                ctx.renderTablePreview(this, listOf(ctx.nameColumn, ctx.ageColumn, ctx.cityColumn, ctx.statusTextColumn), ctx.users.take(3), 286f, displayMode = TableDisplayMode.List, theme = ctx.currentTheme())
            }
        }
    }

    private fun renderPlaygroundSection(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr { flex(1f) }
            Scroller {
                attr { height(250f); marginLeft(16f); marginRight(16f); marginBottom(10f) }
                ConfigGroup("列与对齐", first = true, theme = { ctx.currentTheme() }) {
                    Scroller {
                        attr { height(44f); flexDirectionRow() }
                        vfor({ ctx.activeColumns }) { col ->
                            ToggleChip(label = { col.title }, active = { ctx.selectedColumn === col }, theme = { ctx.currentTheme() }) { ctx.selectedColumn = col }
                        }
                    }
                    View {
                        attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                        listOf("左对齐", "居中", "右对齐").forEach { alignment ->
                            ToggleChip(label = { alignment }, active = {
                                when (alignment) {
                                    "居中" -> ctx.selectedColumn.alignment is ColumnAlignment.Center
                                    "右对齐" -> ctx.selectedColumn.alignment is ColumnAlignment.End
                                    else -> ctx.selectedColumn.alignment is ColumnAlignment.Start
                                }
                            }, theme = { ctx.currentTheme() }) {
                                ctx.selectedColumn.alignment = when (alignment) {
                                    "居中" -> ColumnAlignment.Center
                                    "右对齐" -> ColumnAlignment.End
                                    else -> ColumnAlignment.Start
                                }
                            }
                        }
                    }
                }
                ConfigGroup("外观", theme = { ctx.currentTheme() }) {
                    SettingSwitch("斑马纹", "交替显示行背景", ctx.zebraOn, { ctx.currentTheme() }) { ctx.zebraOn = it }
                    SettingSwitch("完整网格", "显示外框和竖向分隔线", ctx.borderedOn, { ctx.currentTheme() }) { ctx.borderedOn = it }
                    SettingSwitch("固定 48dp 行高", "关闭后由内容和内边距决定", ctx.fixedRowHeight, { ctx.currentTheme() }) { ctx.fixedRowHeight = it }
                    SettingSwitch("紧凑内边距", "减小单元格水平和垂直留白", ctx.compactPadding, { ctx.currentTheme() }) { ctx.compactPadding = it }
                }
                ConfigGroup("Renderer 与提示", theme = { ctx.currentTheme() }) {
                    SettingSwitch("状态标签 renderer", if (ctx.customStatusRendererOn) "Demo 绘制彩色标签" else "Table 使用默认文本", ctx.customStatusRendererOn, { ctx.currentTheme() }) {
                        ctx.customStatusRendererOn = it
                        ctx.rendererExampleColumns = true
                        ctx.syncActiveColumns()
                    }
                    SettingSwitch("溢出提示", "截断默认文本可点击查看全文", ctx.overflowTipOn, { ctx.currentTheme() }) {
                        ctx.overflowTipOn = it
                        ctx.hideOverflowTip()
                    }
                }
            }
            View {
                attr { flex(1f); marginLeft(16f); marginRight(16f); marginBottom(16f); borderRadius(12f); overflow(true); border(Border(1f, BorderStyle.SOLID, Color(ctx.currentTheme().cardBorder))) }
                ctx.renderTablePreview(this, ctx.activeColumns, ctx.users, null, ctx.borderedOn, ctx.zebraOn, ctx.fixedRowHeight, ctx.fixedHeaderOn, ctx.currentTheme(), TableDisplayMode.Table, false, ctx.overflowTipOn)
            }
        }
    }

    private fun renderTablePreview(
        container: ViewContainer<*, *>,
        columns: List<ColumnModel<User>>,
        data: List<User>,
        height: Float?,
        bordered: Boolean = false,
        zebra: Boolean = true,
        fixedRowHeight: Boolean = false,
        fixedHeader: Boolean = true,
        theme: TableThemeColors,
        displayMode: TableDisplayMode = TableDisplayMode.Table,
        loading: Boolean = false,
        overflowEnabled: Boolean = false,
        controlledSortState: TableSortState = sortState,
        onSortChange: (TableSortState) -> Unit = { state -> sortState = state },
        customStateRenderer: Boolean = false,
        hasMore: Boolean = false,
        loadingMore: Boolean = false,
        onLoadMore: (() -> Unit)? = null,
        tableRef: ((TableView<User>) -> Unit)? = null,
    ) {
        val ctx = this
        container.TableView<User> {
            tableRef?.invoke(this)
            attr {
                if (height == null) flex(1f) else height(height)
                this.columns.addAll(columns)
                this.data = data
                rowKey = { user -> user.id }
                zebraStripe = zebra
                this.bordered = bordered
                cellPaddingH = if (ctx.compactPadding) 8f else 12f
                cellPaddingV = if (ctx.compactPadding) 6f else 10f
                rowHeight = if (fixedRowHeight) 48f else 0f
                sortState = controlledSortState
                autoIndexColumn = false
                this.fixedHeader = fixedHeader
                fixedColumnCount = 0
                themeColors = theme
                this.displayMode = displayMode
                listPrimaryColumnKey = "name"
                listStatusColumnKey = "status"
                listStatusTagStyleByText = ctx.statusTagStyleByText(theme)
                this.loading = loading
                emptyText = "暂无员工数据"
                loadingText = "正在加载员工数据"
                if (customStateRenderer) {
                    emptyRenderer = { ctx.renderCustomEmptyState(this) }
                    loadingRenderer = { ctx.renderCustomLoadingState(this) }
                }
                this.hasMore = hasMore
                this.loadingMore = loadingMore
                loadMoreThresholdRows = 3
                enableOverflowCellClick = overflowEnabled
                headerStyle = if (ctx.compactHeader) TableHeaderStyle(13f, TableHeaderFontWeight.Bold, 8f, 6f, 40f, 2f) else TableHeaderStyle.Default
            }
            event {
                rowClick = { user -> ctx.bridgeModule.toast("点击了: ${user.name}") }
                overflowCellClick = { info -> ctx.showOverflowTip(info) }
                overflowTipDismiss = { ctx.hideOverflowTip() }
                sortChange = onSortChange
                loadMore = { onLoadMore?.invoke() }
            }
        }
    }

    private fun loadMoreScrollRows() {
        if (scrollDemoLoadingMore || scrollDemoLimit >= users.size) return
        scrollDemoLoadingMore = true
        scrollDemoLimit = min(scrollDemoLimit + 4, users.size)
        scrollDemoLoadingMore = false
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

    private fun renderOverflowTip(container: ViewContainer<*, *>) {
        val ctx = this
        container.View {
            attr {
                absolutePositionAllZero()
                zIndex(30)
                visibility(ctx.overflowTipVisible && ctx.overflowTipOn)
                touchEnable(ctx.overflowTipVisible && ctx.overflowTipOn)
            }
            event { click { ctx.hideOverflowTip() } }
            View {
                attr {
                    absolutePosition(left = ctx.overflowTipLeft, top = ctx.overflowTipTop)
                    width(ctx.overflowTipWidth())
                    padding(8f)
                    paddingLeft(12f)
                    paddingRight(12f)
                    borderRadius(8f)
                    backgroundColor(Color(0xEE222222))
                }
                Text { attr { text(ctx.overflowTipText); fontSize(13f); color(Color.WHITE) } }
            }
        }
    }

    private fun currentTheme(): TableThemeColors = when (themeMode) {
        is DemoThemeMode.Dark -> TableThemeColors.Dark
        is DemoThemeMode.Blue -> TableThemeColors.Blue
        is DemoThemeMode.Light -> TableThemeColors.Light
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
        displayMode = if (example == "List 模式") TableDisplayMode.List else TableDisplayMode.Table
        wideTable = example != "基础样式"
        rendererExampleColumns = example == "自定义渲染"
        customStatusRendererOn = example == "自定义渲染" || example == "List 模式"
        if (example == "主题定制") {
            themeMode = DemoThemeMode.Blue
        }
        if (example == "基础样式") {
            borderedOn = true
        }
        selectedColumn = ageColumn
        syncActiveColumns()
        hideOverflowTip()
    }

    private fun selectTheme(theme: DemoThemeMode) {
        themeMode = theme
        activeExample = "主题定制"
        tableState = "正常"
        displayMode = TableDisplayMode.Table
        wideTable = true
        rendererExampleColumns = false
        selectedColumn = ageColumn
        syncActiveColumns()
        hideOverflowTip()
    }

    private fun selectStatusRenderer(custom: Boolean) {
        activeExample = "自定义渲染"
        tableState = "正常"
        displayMode = TableDisplayMode.Table
        wideTable = true
        rendererExampleColumns = true
        customStatusRendererOn = custom
        selectedColumn = ageColumn
        syncActiveColumns()
        hideOverflowTip()
    }

    private fun exampleDescription(): String = when (activeExample) {
        "基础样式" -> "验证多行多列、行列边框、内边距和任意列对齐"
        "双向滚动" -> "5 列 × 20 行数据，验证横纵滚动和固定表头"
        "主题定制" -> "验证表头、边框、行背景等语义颜色可整体覆盖"
        "自定义渲染" -> "验证单元格和表头 renderer 可嵌入 KuiklyUI 子组件"
        "List 模式" -> "显式 List 模式，验证 grouped list 展示"
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
                currentStatusColumn(),
                ageColumn,
                wideEmailColumn,
                cityColumn,
                notifyColumn,
            )
            wideTable -> listOf(nameColumn, ageColumn, wideEmailColumn, cityColumn, currentStatusColumn())
            else -> columns3
        }

    private fun currentData(): List<User> = if (tableState == "空") emptyList() else users

    private fun notifyEnabled(user: User): Boolean =
        notifyStateById[user.id] ?: (user.name.hashCode() % 2 == 0)

    private fun setNotifyEnabled(user: User, enabled: Boolean) {
        notifyStateById = notifyStateById + (user.id to enabled)
    }

    private fun syncActiveColumns() {
        activeColumns = ObservableList(currentColumns().toMutableList())
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
        attr {
            backgroundColor(Color(theme().cardBackground))
            borderRadius(12f)
            border(Border(1f, BorderStyle.SOLID, Color(theme().cardBorder)))
            padding(10f)
            marginBottom(12f)
        }
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
            attr { borderRadius(8f); overflow(true) }
            content()
        }
    }
}

private fun ViewContainer<*, *>.SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
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
            Text { attr { text(description); fontSize(11f); color(Color(theme().cellTextSecondary)); marginTop(2f) } }
        }
        Switch {
            attr {
                size(40f, 24f)
                isOn(checked)
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
