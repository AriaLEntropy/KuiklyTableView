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

    private val users = (1..30).map { i ->
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
    }

    /** 页面铬固定浅色，不跟随表格主题预设。 */
    internal val pageChrome = TableThemeColors.Light

    private var themeMode: DataDemoThemeMode by observable(DataDemoThemeMode.Light)
    private var enableRowSelection by observable(true)
    private var enablePagination by observable(true)
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
                        text("在 KuiklyTable 之上增加选择、筛选与客户端分页；通过 rowKey 保持数据状态。")
                        fontSize(12f)
                        color(Color(ctx.pageChrome.cellTextSecondary))
                        lines(2)
                    }
                }
            }

            Scroller {
                attr {
                    height((ctx.pagerData.pageViewHeight * 0.32f).coerceIn(196f, 260f))
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
                    listOf("数据交互", "基础组合", "接入关系").forEach { panel ->
                        DataToggleChip(label = { panel }, active = { ctx.activePanel == panel }, chrome = ctx.pageChrome) {
                            ctx.activePanel = panel
                        }
                    }
                }
                vif({ ctx.activePanel == "数据交互" }) {
                    ctx.renderDataInteractionPanel(this)
                }
                vif({ ctx.activePanel == "基础组合" }) {
                    ctx.renderBaseCompositionPanel(this)
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
                DataTableView<User> {
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
                        data = ctx.users
                        rowKey = { it.id }
                        zebraStripe = true
                        fixedHeader = ctx.fixedHeader
                        rowHeight = 48f
                        fixedFirstColumn = ctx.fixedFirstColumn
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
                                key = "status",
                                title = "状态",
                                accessor = { it.status },
                                width = 88f,
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
        }
    }

    private fun renderDataInteractionPanel(container: ViewContainer<*, *>) {
        val ctx = this
        container.apply {
            configLabel("行选择（DataTable 新增）", ctx)
            View {
                attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                DataToggleChip(label = { "选择:开" }, active = { ctx.enableRowSelection }, chrome = ctx.pageChrome) {
                    ctx.enableRowSelection = true
                    ctx.lastEvent = "已开启行选择"
                }
                DataToggleChip(label = { "选择:关" }, active = { !ctx.enableRowSelection }, chrome = ctx.pageChrome) {
                    ctx.enableRowSelection = false
                    ctx.selectedKeys = emptyList()
                    ctx.lastEvent = "已关闭选择并清空选中"
                }
                DataToggleChip(label = { "清空选中" }, active = { ctx.selectedKeys.isNotEmpty() }, chrome = ctx.pageChrome) {
                    ctx.selectedKeys = emptyList()
                    ctx.lastEvent = "已清空选中"
                }
            }
            Text {
                attr {
                    text("表头复选框表示未选 / 半选 / 全选；点年龄排序后，选中 rowKey 保持。")
                    fontSize(11f)
                    color(Color(ctx.pageChrome.cellTextSecondary))
                    marginBottom(4f)
                }
            }
            configLabel("筛选与客户端分页（DataTable 新增）", ctx)
            View {
                attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                listOf("全部", "在职", "休假", "离职").forEach { status ->
                    DataToggleChip(label = { status }, active = { ctx.statusFilter == status }, chrome = ctx.pageChrome) {
                        ctx.statusFilter = status
                        ctx.pageIndex = 0
                        ctx.lastEvent = "已筛选 $status，并回到第 1 页"
                    }
                }
                DataToggleChip(label = { if (ctx.enablePagination) "分页:开" else "分页:关" }, active = { ctx.enablePagination }, chrome = ctx.pageChrome) {
                    ctx.enablePagination = !ctx.enablePagination
                    ctx.pageIndex = 0
                    ctx.lastEvent = if (ctx.enablePagination) "已开启分页" else "已关闭分页，展示筛选后全量"
                }
            }
            DataPanelStatus(
                primary = { "已选择 ${ctx.selectedKeys.size} 行${if (ctx.selectedKeys.isEmpty()) "" else "：${ctx.selectedKeys.joinToString()}"}" },
                secondary = { ctx.lastEvent },
                chrome = ctx.pageChrome,
            )
        }
    }

    private fun renderBaseCompositionPanel(container: ViewContainer<*, *>) {
        val ctx = this
        container.apply {
            DataGuideText("复用 KuiklyTable", "固定表头、固定列和主题来自底层 Table；这里仅验证它们能与 DataTable 交互组合。", ctx.pageChrome)
            configLabel("滚动组合", ctx)
            View {
                attr { flexDirectionRow(); flexWrap(FlexWrap.WRAP) }
                DataToggleChip(label = { if (ctx.fixedHeader) "固定表头:开" else "固定表头:关" }, active = { ctx.fixedHeader }, chrome = ctx.pageChrome) {
                    ctx.fixedHeader = !ctx.fixedHeader
                    ctx.lastEvent = if (ctx.fixedHeader) "已固定表头" else "表头随内容滚动"
                }
                DataToggleChip(label = { if (ctx.fixedFirstColumn) "固定列:开" else "固定列:关" }, active = { ctx.fixedFirstColumn }, chrome = ctx.pageChrome) {
                    ctx.fixedFirstColumn = !ctx.fixedFirstColumn
                    ctx.lastEvent = if (ctx.fixedFirstColumn) "已固定选择列和首个业务列" else "已恢复普通横向滚动"
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
        }
    }
}

private sealed class DataDemoThemeMode {
    data object Light : DataDemoThemeMode()
    data object Dark : DataDemoThemeMode()
    data object Blue : DataDemoThemeMode()
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
