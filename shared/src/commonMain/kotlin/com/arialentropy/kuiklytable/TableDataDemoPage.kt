package com.arialentropy.kuiklytable

import com.arialentropy.kuiklytable.base.BasePager
import com.arialentropy.kuiklytable.base.bridgeModule
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * KuiklyDataTable Showcase：选择、筛选、分页、左侧固定列。
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
    private var fixedFirstColumn by observable(false)
    private var pageIndex by observable(0)
    private var pageSize by observable(10)
    private var statusFilter by observable("全部")
    private var selectedKeys by observable(emptyList<Any>())
    private var sortState by observable(TableSortState())
    private var lastEvent by observable("尚未操作")

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
                        text("选择 / 筛选 / 分页 / 左侧固定列；主题预设只作用于表格")
                        fontSize(12f)
                        color(Color(ctx.pageChrome.cellTextSecondary))
                    }
                }
            }

            View {
                attr {
                    paddingLeft(12f)
                    paddingRight(12f)
                    paddingBottom(8f)
                    alignSelfStretch()
                }
                configLabel("选择", ctx)
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
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

                configLabel("筛选（状态）", ctx)
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    listOf("全部", "在职", "休假", "离职").forEach { status ->
                        DataToggleChip(label = { status }, active = { ctx.statusFilter == status }, chrome = ctx.pageChrome) {
                            ctx.statusFilter = status
                            ctx.pageIndex = 0
                            ctx.lastEvent = "筛选:$status（已回第 1 页）"
                        }
                    }
                }

                configLabel("分页", ctx)
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    DataToggleChip(label = { "分页:开" }, active = { ctx.enablePagination }, chrome = ctx.pageChrome) {
                        ctx.enablePagination = true
                        ctx.pageIndex = 0
                        ctx.lastEvent = "已开启分页"
                    }
                    DataToggleChip(label = { "分页:关" }, active = { !ctx.enablePagination }, chrome = ctx.pageChrome) {
                        ctx.enablePagination = false
                        ctx.pageIndex = 0
                        ctx.lastEvent = "已关闭分页（展示筛选后全量）"
                    }
                }

                configLabel("固定第一列（被固定列须有 width；单列忽略；可拖表头/表体右侧）", ctx)
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    DataToggleChip(label = { "固定:关" }, active = { !ctx.fixedFirstColumn }, chrome = ctx.pageChrome) {
                        ctx.fixedFirstColumn = false
                        ctx.lastEvent = "已关闭固定列（普通横向滚动）"
                    }
                    DataToggleChip(label = { "固定:开" }, active = { ctx.fixedFirstColumn }, chrome = ctx.pageChrome) {
                        ctx.fixedFirstColumn = true
                        ctx.lastEvent = "已固定左列（表头或表体右侧横滑，左列应钉住）"
                    }
                }

                configLabel("表格主题预设（只改表格，不改页面外观）", ctx)
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    DataToggleChip(label = { "浅色" }, active = { ctx.themeMode is DataDemoThemeMode.Light }, chrome = ctx.pageChrome) {
                        ctx.themeMode = DataDemoThemeMode.Light
                        ctx.lastEvent = "表格主题:浅色"
                    }
                    DataToggleChip(label = { "深色" }, active = { ctx.themeMode is DataDemoThemeMode.Dark }, chrome = ctx.pageChrome) {
                        ctx.themeMode = DataDemoThemeMode.Dark
                        ctx.lastEvent = "表格主题:深色"
                    }
                    DataToggleChip(label = { "蓝色" }, active = { ctx.themeMode is DataDemoThemeMode.Blue }, chrome = ctx.pageChrome) {
                        ctx.themeMode = DataDemoThemeMode.Blue
                        ctx.lastEvent = "表格主题:蓝色"
                    }
                }

                Text {
                    attr {
                        marginTop(8f)
                        text("选中 ${ctx.selectedKeys.size} 项：${ctx.selectedKeys.joinToString()}")
                        fontSize(12f)
                        color(Color(ctx.pageChrome.cellText))
                    }
                }
                Text {
                    attr {
                        marginTop(4f)
                        text(ctx.lastEvent)
                        fontSize(12f)
                        color(Color(ctx.pageChrome.actionText))
                    }
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
                        fixedHeader = true
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
                                alignment = ColumnAlignment.End,
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

private fun ViewContainer<*, *>.DataToggleChip(
    label: () -> String,
    active: () -> Boolean,
    chrome: TableThemeColors,
    onClick: () -> Unit,
) {
    View {
        attr {
            marginRight(8f)
            marginBottom(8f)
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(8f)
            paddingBottom(8f)
            borderRadius(16f)
            border(
                Border(
                    lineWidth = 1f,
                    lineStyle = BorderStyle.SOLID,
                    color = Color(if (active()) chrome.actionText else chrome.gridLine),
                ),
            )
            backgroundColor(Color(if (active()) chrome.selectedRowBackground else chrome.rowBackground))
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
