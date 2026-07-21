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
    )

    // 20 行数据，足够触发纵向滚动（验证固定表头）
    private val users = (1..20).map { i ->
        User(
            id = i,
            name = "员工$i",
            age = 20 + (i * 3) % 40,
            email = "user$i@example.com",
            city = listOf("北京", "上海", "广州", "深圳", "杭州")[i % 5],
            department = listOf("技术部", "产品部", "设计部", "运营部")[i % 4],
            position = listOf("工程师", "产品经理", "设计师", "运营专员")[i % 4],
            hireDate = "202${i % 5}-0${i % 9 + 1}-1${i % 9}",
            salary = "${10 + i}k",
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

    // 3 列模式（fits 页面，无横向滚动）
    private val columns3 = listOf(nameColumn, ageColumn, emailColumn)

    // 5 列模式（总宽超页面 → 横向滚动）
    private val columns5 = listOf(
        nameColumn,
        ageColumn,
        wideEmailColumn,
        ColumnModel<User>(key = "city", title = "城市", accessor = { it.city }, width = 100f),
        ColumnModel<User>(key = "department", title = "部门", accessor = { it.department }, width = 100f),
    )

    // ===== 可配置状态（observable，变化触发表格重渲染）=====
    private var wideTable by observable(true)          // 3列 / 5列（横向滚动）
    private var activeColumns: ObservableList<ColumnModel<User>> by observableList()
    private var selectedColumn by observable<ColumnModel<User>>(ageColumn)
    private var zebraOn by observable(true)             // 斑马纹
    private var borderedOn by observable(false)         // 列边框

    init {
        activeColumns.addAll(columns5)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            // 页面标题
            Text {
                attr {
                    text("Table 基础展示")
                    fontSize(18f)
                    fontWeightSemisolid()
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
                        ctx.activeColumns.addAll(ctx.columns5)
                        ctx.selectedColumn = ctx.ageColumn
                    }
                }

                // 第二行：配置列（动态反映当前列）
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

                // 第三行：对齐方式（作用于选中列）
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

                // 第四行：样式配置
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
                        data = ctx.users
                        zebraStripe = ctx.zebraOn
                        bordered = ctx.borderedOn
                    }
                    event {
                        rowClick = { user ->
                            ctx.bridgeModule.toast("点击了: ${user.name}")
                        }
                    }
                }
            }
        }
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
