package com.arialentropy.kuiklytable

import com.arialentropy.kuiklytable.base.BasePager
import com.arialentropy.kuiklytable.base.bridgeModule
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * KuiklyDataTable 使用示例：突出 DataTable 数据交互，并说明复用的 Table 基础能力。
 *
 * 页面铬（标题 / 配置区）使用固定浅色；仅表格消费 [themeColors] 预设。
 */
@Page("table_data", supportInLocal = true)
internal class TableDataDemoPage : BasePager() {

    data class User(
        val id: String,
        val name: String,
        val age: Int,
        val city: String,
        val status: String,
        val department: String,
        val role: String,
        val email: String,
        val phone: String,
    )

    /** 数据交互页默认数据集：足够演示多页翻页与筛选后仍有分页。 */
    private val users = (1..DEMO_ROW_COUNT).map { createUser(it) }
    private val largeUsers by lazy { (1..LARGE_ROW_COUNT).map { createUser(it) } }

    private fun createUser(i: Int): User =
        User(
            id = "u$i",
            name = "员工$i",
            age = 20 + (i * 3) % 40,
            city = listOf("北京", "上海", "广州", "深圳")[i % 4],
            status = if (i % 3 == 0) "离职" else if (i % 2 == 0) "休假" else "在职",
            department = listOf("研发中心", "产品中心", "设计中心")[i % 3],
            role = listOf("高级工程师", "产品经理", "交互设计师")[i % 3],
            email = "employee$i@example.com",
            phone = "1380000${i.toString().padStart(4, '0')}",
        )

    /** 页面铬固定浅色，不跟随表格主题预设。 */
    internal val pageChrome = TableThemeColors.Light

    private var themeMode: DataDemoThemeMode by observable(DataDemoThemeMode.Light)
    private var enableRowSelection by observable(true)
    private var enablePagination by observable(true)
    private var enableWindowed by observable(false)
    private var fixedHeader by observable(true)
    private var fixedFirstColumn by observable(false)
    private var pageIndex by observable(0)
    private var pageSize by observable(10)
    private var statusFilter by observable("全部")
    private var selectedKeys by observable(emptyList<Any>())
    private var sortState by observable(TableSortState())
    private var lastEvent by observable("尚未操作")
    private var activePanel by observable("数据交互")

    /** 仅表格使用的主题预设。 */
    internal fun tableTheme(): TableThemeColors = when (themeMode) {
        is DataDemoThemeMode.Dark -> TableThemeColors.Dark
        is DataDemoThemeMode.Blue -> TableThemeColors.Blue
        is DataDemoThemeMode.Light -> TableThemeColors.Light
    }

    private fun currentFilter(): ((User) -> Boolean)? =
        when (statusFilter) {
            "全部" -> null
            else -> { user -> user.status == statusFilter }
        }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color(ctx.pageChrome.rowBackgroundAlt))
            }
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight + 12f)
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingBottom(8f)
                    alignSelfStretch()
                }
                Text {
                    attr {
                        text("KuiklyDataTable")
                        fontSize(18f)
                        fontWeightSemiBold()
                        color(Color(ctx.pageChrome.cellText))
                        lines(1)
                        // 避免末尾字符被裁切
                        marginRight(8f)
                    }
                }
                Text {
                    attr {
                        marginTop(4f)
                        text("选择、筛选、分页是数据层；虚拟滚动是视图层。两者可组合，通过 rowKey 保持交互状态。")
                        fontSize(12f)
                        color(Color(ctx.pageChrome.cellTextSecondary))
                        lines(2)
                    }
                }
            }

            Scroller {
                attr {
                    // Switch 行比 chip 高，给足首屏高度让分页开关可见
                    height((ctx.pagerData.pageViewHeight * 0.38f).coerceIn(240f, 320f))
                    marginLeft(12f)
                    marginRight(12f)
                    marginBottom(10f)
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingTop(10f)
                    paddingBottom(10f)
                    borderRadius(12f)
                    backgroundColor(Color(ctx.pageChrome.cardBackground))
                    border(Border(1f, BorderStyle.SOLID, Color(ctx.pageChrome.cardBorder)))
                    alignSelfStretch()
                }
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                        marginBottom(4f)
                    }
                    listOf("数据交互", "大量数据", "接入关系").forEach { panel ->
                        DataToggleChip(label = { panel }, active = { ctx.activePanel == panel }, chrome = ctx.pageChrome) {
                            ctx.activePanel = panel
                            // 进入大量数据默认开 Windowed，避免 3000 行 Standard 挂满节点
                            if (panel == "大量数据") {
                                if (!ctx.enableWindowed) {
                                    ctx.enableWindowed = true
                                    ctx.fixedFirstColumn = false
                                    ctx.pageIndex = 0
                                    ctx.lastEvent =
                                        "大量数据：${LARGE_ROW_COUNT} 行 + Windowed($VIRTUAL_SCROLL_WINDOW)"
                                }
                            } else if (ctx.enableWindowed) {
                                ctx.enableWindowed = false
                                ctx.lastEvent = "已回到数据交互数据集（$DEMO_ROW_COUNT 行）"
                            }
                        }
                    }
                }
                vif({ ctx.activePanel == "数据交互" }) {
                    ctx.renderDataInteractionPanel(this)
                }
                vif({ ctx.activePanel == "大量数据" }) {
                    ctx.renderLargeDataPanel(this)
                }
                vif({ ctx.activePanel == "接入关系" }) {
                    ctx.renderIntegrationPanel(this)
                }
            }

            View {
                attr {
                    flex(1f)
                    marginLeft(12f)
                    marginRight(12f)
                    marginBottom(12f)
                }
                // rowRenderMode 创建后不可热切换，开关时 remount
                vif({ ctx.enableWindowed }) {
                    ctx.renderMainDataTable(this, windowed = true)
                }
                vif({ !ctx.enableWindowed }) {
                    ctx.renderMainDataTable(this, windowed = false)
                }
            }
        }
    }

    private fun renderMainDataTable(container: ViewContainer<*, *>, windowed: Boolean) {
        val ctx = this
        container.DataTableView<User> {
            attr {
                flex(1f)
                enableRowSelection = ctx.enableRowSelection
                selectedKeys = ctx.selectedKeys
                enablePagination = ctx.enablePagination
                pageIndex = ctx.pageIndex
                pageSize = ctx.pageSize
                filterPredicate = ctx.currentFilter()
                themeColors = ctx.tableTheme()
                sortState = ctx.sortState
                // 「大量数据」页始终用 3000 行；其它页用 100 行做选择/分页演示
                data = if (ctx.activePanel == "大量数据") ctx.largeUsers else ctx.users
                rowKey = { it.id }
                zebraStripe = true
                fixedHeader = ctx.fixedHeader
                // Windowed 需要固定行高；普通模式也固定 48，便于与固定列组合演示
                rowHeight = 48f
                fixedFirstColumn = if (windowed) false else ctx.fixedFirstColumn
                rowRenderMode = if (windowed) {
                    TableRowRenderMode.Windowed(VIRTUAL_SCROLL_WINDOW)
                } else {
                    TableRowRenderMode.Standard
                }
                columns.clear()
                columns.add(
                    ColumnModel(
                        key = "name",
                        title = "姓名",
                        accessor = { it.name },
                        width = 96f,
                        sortable = true,
                    ),
                )
                columns.add(
                    ColumnModel(
                        key = "age",
                        title = "年龄",
                        accessor = { it.age.toString() },
                        width = 72f,
                        sortable = true,
                        sortComparator = compareBy { it.age },
                    ),
                )
                // 状态列放在城市前，首屏完整展示彩色标签，避免贴右缘被圆角裁切
                columns.add(
                    ColumnModel(
                        key = "status",
                        title = "状态",
                        accessor = { it.status },
                        width = 100f,
                        // 彩色标签由 Showcase 传入，不是 DataTable 内置列
                        cellRenderer = { user, _ ->
                            val tagStyle = ctx.resolveStatusTagStyle(user.status)
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
                    ),
                )
                columns.add(
                    ColumnModel(
                        key = "city",
                        title = "城市",
                        accessor = { it.city },
                        width = 120f,
                    ),
                )
                columns.add(
                    ColumnModel(
                        key = "dept",
                        title = "部门",
                        accessor = { it.department },
                        width = 144f,
                    ),
                )
                columns.add(
                    ColumnModel(
                        key = "role",
                        title = "职位",
                        accessor = { it.role },
                        width = 144f,
                    ),
                )
                columns.add(
                    ColumnModel(
                        key = "email",
                        title = "邮箱",
                        accessor = { it.email },
                        width = 240f,
                    ),
                )
                columns.add(
                    ColumnModel(
                        key = "phone",
                        title = "手机号",
                        accessor = { it.phone },
                        width = 160f,
                    ),
                )
            }
            event {
                selectionChange = { keys ->
                    ctx.selectedKeys = keys
                    ctx.lastEvent = "selectionChange: ${keys.joinToString()}"
                    ctx.bridgeModule.toast("已选 ${keys.size} 项")
                }
                sortChange = { state ->
                    ctx.sortState = state
                    ctx.lastEvent =
                        "sortChange: ${state.columnKey ?: "-"}；选中仍为 ${ctx.selectedKeys.joinToString()}"
                }
                pageChange = { index ->
                    ctx.pageIndex = index
                    ctx.lastEvent = "pageChange: 第 ${index + 1} 页"
                }
                pageSizeChange = { size ->
                    ctx.pageSize = size
                    ctx.pageIndex = 0
                    ctx.lastEvent = "pageSize: $size"
                }
            }
        }
    }

    private fun renderDataInteractionPanel(container: ViewContainer<*, *>) {
        val ctx = this
        container.apply {
            // 布尔项用 Switch 放顶部，避免配置 Scroller 首屏看不到
            DataSettingSwitch(
                title = "行选择",
                description = {
                    if (ctx.enableRowSelection) {
                        "表头复选框：未选 / 半选 / 全选；排序后 rowKey 保持"
                    } else {
                        "关闭后无选择列、清空高亮"
                    }
                },
                checked = { ctx.enableRowSelection },
                chrome = ctx.pageChrome,
            ) { on ->
                ctx.enableRowSelection = on
                if (!on) ctx.selectedKeys = emptyList()
                ctx.lastEvent = if (on) "已开启行选择" else "已关闭选择并清空选中"
            }
            DataSettingSwitch(
                title = "客户端分页",
                description = {
                    if (ctx.enablePagination) {
                        "共 $DEMO_ROW_COUNT 行，按 pageSize 切片；关闭后展示筛选后全量"
                    } else {
                        "已关闭：展示筛选后全量行"
                    }
                },
                checked = { ctx.enablePagination },
                chrome = ctx.pageChrome,
            ) { on ->
                ctx.enablePagination = on
                ctx.pageIndex = 0
                ctx.lastEvent = if (on) "已开启分页" else "已关闭分页，展示筛选后全量"
            }
            configLabel("状态筛选", ctx)
            View {
                attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                listOf("全部", "在职", "休假", "离职").forEach { status ->
                    DataToggleChip(label = { status }, active = { ctx.statusFilter == status }, chrome = ctx.pageChrome) {
                        ctx.statusFilter = status
                        ctx.pageIndex = 0
                        ctx.lastEvent = "已筛选 $status，并回到第 1 页"
                    }
                }
                DataToggleChip(label = { "清空选中" }, active = { ctx.selectedKeys.isNotEmpty() }, chrome = ctx.pageChrome) {
                    ctx.selectedKeys = emptyList()
                    ctx.lastEvent = "已清空选中"
                }
            }
            DataPanelStatus(
                primary = { "已选择 ${ctx.selectedKeys.size} 行${if (ctx.selectedKeys.isEmpty()) "" else "：${ctx.selectedKeys.joinToString()}"}" },
                secondary = { ctx.lastEvent },
                chrome = ctx.pageChrome,
            )
        }
    }

    private fun renderLargeDataPanel(container: ViewContainer<*, *>) {
        val ctx = this
        container.apply {
            DataGuideText(
                "大量数据",
                "用 ${LARGE_ROW_COUNT} 行验证虚拟滚动；筛选/分页仍可开关。开启后只挂载窗口内行节点。",
                ctx.pageChrome,
            )
            configLabel("虚拟滚动", ctx)
            DataSettingSwitch(
                title = "虚拟滚动",
                description = {
                    if (ctx.enableWindowed) {
                        "当前：${LARGE_ROW_COUNT} 行全量在内存，只挂载约 $VIRTUAL_SCROLL_WINDOW 行节点"
                    } else {
                        "关闭后仍为 ${LARGE_ROW_COUNT} 行，但 Standard 会为每行建节点，debug 下可能卡顿"
                    }
                },
                checked = { ctx.enableWindowed },
                chrome = ctx.pageChrome,
            ) { on ->
                ctx.enableWindowed = on
                if (on) {
                    ctx.fixedFirstColumn = false
                    ctx.pageIndex = 0
                    ctx.lastEvent =
                        "已开启虚拟滚动：${LARGE_ROW_COUNT} 行 + Windowed($VIRTUAL_SCROLL_WINDOW) + rowHeight=48；与固定列互斥"
                } else {
                    ctx.lastEvent = "已关闭虚拟滚动：${LARGE_ROW_COUNT} 行改用 Standard（可能卡顿）"
                }
            }
            configLabel("滚动组合", ctx)
            DataSettingSwitch(
                title = "固定表头",
                description = {
                    if (ctx.fixedHeader) "表头固定在列表上方" else "表头随内容滚动"
                },
                checked = { ctx.fixedHeader },
                chrome = ctx.pageChrome,
            ) { on ->
                ctx.fixedHeader = on
                ctx.lastEvent = if (on) "已固定表头" else "表头随内容滚动"
            }
            DataSettingSwitch(
                title = "固定列",
                description = {
                    when {
                        ctx.enableWindowed -> "虚拟滚动开启时不可用"
                        ctx.fixedFirstColumn -> "已固定选择列和首个业务列"
                        else -> "关闭后普通横向滚动"
                    }
                },
                checked = { ctx.fixedFirstColumn },
                chrome = ctx.pageChrome,
            ) { on ->
                if (ctx.enableWindowed) {
                    ctx.lastEvent = "虚拟滚动开启时不可开固定列"
                    ctx.bridgeModule.toast("请先关闭虚拟滚动")
                } else {
                    ctx.fixedFirstColumn = on
                    ctx.lastEvent =
                        if (on) "已固定选择列和首个业务列" else "已恢复普通横向滚动"
                }
            }
            configLabel("表格主题", ctx)
            View {
                attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                DataToggleChip(label = { "浅色" }, active = { ctx.themeMode is DataDemoThemeMode.Light }, chrome = ctx.pageChrome) { ctx.themeMode = DataDemoThemeMode.Light }
                DataToggleChip(label = { "深色" }, active = { ctx.themeMode is DataDemoThemeMode.Dark }, chrome = ctx.pageChrome) { ctx.themeMode = DataDemoThemeMode.Dark }
                DataToggleChip(label = { "蓝色" }, active = { ctx.themeMode is DataDemoThemeMode.Blue }, chrome = ctx.pageChrome) { ctx.themeMode = DataDemoThemeMode.Blue }
            }
        }
    }

    private fun renderIntegrationPanel(container: ViewContainer<*, *>) {
        val ctx = this
        container.apply {
            DataGuideText("1. 稳定行身份", "data 提供源数据，rowKey = { it.id } 关联选择状态。", ctx.pageChrome)
            DataGuideText("2. 受控选择", "selectedKeys 传入当前值，selectionChange 回写新的 key 列表。", ctx.pageChrome)
            DataGuideText("3. 受控分页", "pageIndex / pageSize 传入状态，pageChange / pageSizeChange 回写。", ctx.pageChrome)
            DataGuideText("4. 筛选条件", "filterPredicate 为 null 时不过滤；条件变化后由页面把 pageIndex 重置为 0。", ctx.pageChrome)
            DataGuideText(
                "5. 虚拟滚动",
                "rowRenderMode = Windowed(n) 限制挂载行节点；需固定 rowHeight，不与固定列组合；创建后勿热切换。",
                ctx.pageChrome,
            )
        }
    }

    private fun resolveStatusTagStyle(status: String): TableStatusTagStyle {
        val theme = tableTheme()
        return statusTagStyleByText(theme)[status]
            ?: TableStatusTagStyle.fromPreset(TableStatusTagPreset.fromText(status), theme)
    }

    private fun statusTagStyleByText(themeColors: TableThemeColors): Map<String, TableStatusTagStyle> = mapOf(
        "在职" to TableStatusTagStyle(themeColors.statusTagInfoBackground, themeColors.statusTagInfoText),
        "休假" to TableStatusTagStyle(themeColors.statusTagNeutralBackground, themeColors.statusTagNeutralText),
        "离职" to TableStatusTagStyle(themeColors.statusTagBackgroundAlt, themeColors.statusTagTextAlt),
    )
}

private sealed class DataDemoThemeMode {
    data object Light : DataDemoThemeMode()
    data object Dark : DataDemoThemeMode()
    data object Blue : DataDemoThemeMode()
}

private const val DEMO_ROW_COUNT = 100
private const val LARGE_ROW_COUNT = 3_000
private const val VIRTUAL_SCROLL_WINDOW = 40

/** 布尔配置用 Kuikly Switch，与 table_basic SettingSwitch 对齐；多选项仍用 chip。 */
private fun ViewContainer<*, *>.DataSettingSwitch(
    title: String,
    description: () -> String,
    checked: () -> Boolean,
    chrome: TableThemeColors,
    onChange: (Boolean) -> Unit,
) {
    View {
        attr {
            minHeight(48f)
            flexDirectionRow()
            alignItemsCenter()
            marginBottom(4f)
        }
        View {
            attr { flex(1f); marginRight(12f) }
            Text { attr { text(title); fontSize(13f); color(Color(chrome.cellText)) } }
            Text {
                attr {
                    text(description())
                    fontSize(11f)
                    color(Color(chrome.cellTextSecondary))
                    marginTop(2f)
                }
            }
        }
        Switch {
            attr {
                size(40f, 24f)
                isOn(checked())
                onColor(Color(chrome.actionText))
                unOnColor(Color(chrome.gridLine))
            }
            event { switchOnChanged { onChange(it) } }
        }
    }
}

private fun ViewContainer<*, *>.configLabel(text: String, page: TableDataDemoPage) {
    Text {
        attr {
            marginTop(8f)
            text(text)
            fontSize(12f)
            color(Color(page.pageChrome.cellTextSecondary))
            marginBottom(4f)
        }
    }
}

private fun ViewContainer<*, *>.DataGuideText(
    title: String,
    body: String,
    chrome: TableThemeColors,
) {
    Text {
        attr {
            text(title)
            fontSize(13f)
            fontWeightSemiBold()
            color(Color(chrome.cellText))
            marginTop(6f)
        }
    }
    Text {
        attr {
            text(body)
            fontSize(11f)
            color(Color(chrome.cellTextSecondary))
            marginTop(2f)
            marginBottom(4f)
        }
    }
}

private fun ViewContainer<*, *>.DataPanelStatus(
    primary: () -> String,
    secondary: () -> String,
    chrome: TableThemeColors,
) {
    Text {
        attr {
            marginTop(4f)
            text(primary())
            fontSize(12f)
            color(Color(chrome.cellText))
        }
    }
    Text {
        attr {
            marginTop(2f)
            text(secondary())
            fontSize(11f)
            color(Color(chrome.actionText))
        }
    }
}

private fun ViewContainer<*, *>.DataToggleChip(
    label: () -> String,
    active: () -> Boolean,
    chrome: TableThemeColors,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(44f)
            marginRight(8f)
            marginBottom(8f)
            paddingLeft(12f)
            paddingRight(12f)
            alignItemsCenter()
            justifyContentCenter()
            borderRadius(16f)
            // 与 table_basic ToggleChip 对齐：激活用浅灰底+主色描边，不用选中行浅蓝底
            backgroundColor(
                Color(if (active()) chrome.rowBackgroundAlt else chrome.cardBackground),
            )
            border(
                Border(
                    lineWidth = 1f,
                    lineStyle = BorderStyle.SOLID,
                    color = Color(if (active()) chrome.actionText else chrome.cardBorder),
                ),
            )
        }
        event { click { onClick() } }
        Text {
            attr {
                text(label())
                fontSize(13f)
                color(Color(if (active()) chrome.actionText else chrome.cellText))
            }
        }
    }
}
