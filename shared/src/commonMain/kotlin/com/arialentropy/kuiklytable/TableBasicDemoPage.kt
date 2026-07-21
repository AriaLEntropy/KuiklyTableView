package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*
import com.arialentropy.kuiklytable.base.BasePager
import com.arialentropy.kuiklytable.base.bridgeModule

/**
 * Table 基础展示 Demo
 *
 * 验证 ST-1：ColumnModel + 表头/单元格/行渲染 + 斑马纹 + 文字截断 + 可配置对齐。
 * 配置切换用可点击选项（参照 Kuikly 官方 AppSettingPage 的 observable + click 模式），
 * 而非文档网站式的多个静态 demo 并列。
 */
@Page("table_basic", supportInLocal = true)
internal class TableBasicDemoPage : BasePager() {

    data class User(
        val id: Int,
        val name: String,
        val age: Int,
        val email: String,
    )

    private val users = listOf(
        User(1, "张三", 28, "zhangsan@example.com"),
        User(2, "李四", 34, "lisi@example.com"),
        User(3, "王五", 22, "wangwu@example.com"),
        User(4, "赵六", 41, "zhaoliu@example.com"),
        User(5, "孙七", 19, "sunqi@example.com"),
    )

    // 列定义：分别持有引用，配置面板可针对任意一列调整对齐
    private val nameColumn = ColumnModel<User>(
        key = "name",
        title = "姓名",
        accessor = { it.name },
        width = 80f,
    )
    private val ageColumn = ColumnModel<User>(
        key = "age",
        title = "年龄",
        accessor = { it.age.toString() },
        width = 60f,
        alignment = ColumnAlignment.End,
    )
    private val emailColumn = ColumnModel<User>(
        key = "email",
        title = "邮箱",
        accessor = { it.email },
        // 无固定宽度，flex 填充剩余空间
    )

    // 列定义列表保持稳定，不整体重建（整体重建不会触发表格重渲染）
    private val columns = listOf(nameColumn, ageColumn, emailColumn)

    // 可配置状态（observable，变化触发表格重渲染）
    // 当前选中列（配置面板作用对象），默认年龄列；对齐状态直接存在各列的 alignment（响应式）
    private var selectedColumn by observable(ageColumn)
    private var zebraOn by observable(true)

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

            // 配置切换区（可点击选项，参照 Kuikly 官方 AppSettingPage 的 observable+click 模式）
            View {
                attr {
                    marginLeft(16f)
                    marginRight(16f)
                    marginBottom(12f)
                }
                // 第一行：选择要配置的列
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    ToggleChip(
                        label = { "配置列：姓名" },
                        active = { ctx.selectedColumn === ctx.nameColumn },
                    ) { ctx.selectedColumn = ctx.nameColumn }
                    ToggleChip(
                        label = { "配置列：年龄" },
                        active = { ctx.selectedColumn === ctx.ageColumn },
                    ) { ctx.selectedColumn = ctx.ageColumn }
                    ToggleChip(
                        label = { "配置列：邮箱" },
                        active = { ctx.selectedColumn === ctx.emailColumn },
                    ) { ctx.selectedColumn = ctx.emailColumn }
                }
                // 第二行：选中列的对齐方式 + 斑马纹
                View {
                    attr {
                        flexDirectionRow()
                        flexWrap(FlexWrap.WRAP)
                    }
                    ToggleChip(
                        label = { "左对齐" },
                        active = { ctx.selectedColumn.alignment is ColumnAlignment.Start },
                    ) { ctx.selectedColumn.alignment = ColumnAlignment.Start }
                    ToggleChip(
                        label = { "居中" },
                        active = { ctx.selectedColumn.alignment is ColumnAlignment.Center },
                    ) { ctx.selectedColumn.alignment = ColumnAlignment.Center }
                    ToggleChip(
                        label = { "右对齐" },
                        active = { ctx.selectedColumn.alignment is ColumnAlignment.End },
                    ) { ctx.selectedColumn.alignment = ColumnAlignment.End }
                    ToggleChip(
                        label = { "斑马纹：${if (ctx.zebraOn) "开" else "关"}" },
                        active = { ctx.zebraOn },
                    ) { ctx.zebraOn = !ctx.zebraOn }
                }
            }

            // 表格（左右留白 16dp）
            View {
                attr {
                    flex(1f)
                    marginLeft(16f)
                    marginRight(16f)
                }
                TableView<User> {
                    attr {
                        columns = ctx.columns
                        data = ctx.users
                        zebraStripe = ctx.zebraOn
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
 * 可点击的配置选项（chip 样式），顶层扩展函数（与内置 View/Text 同样的调用方式）。
 * 触摸热区通过 padding 保证 ≥44dp 高度。
 */
private fun ViewContainer<*, *>.ToggleChip(
    label: () -> String,
    active: () -> Boolean,
    onClick: () -> Unit,
) {
    View {
        attr {
            paddingLeft(14f)
            paddingRight(14f)
            paddingTop(11f)
            paddingBottom(11f)
            marginRight(8f)
            marginBottom(8f)
            borderRadius(16f)
            backgroundColor(Color(if (active()) 0xFF4F8FFF else 0xFFEEEEEE))
        }
        Text {
            attr {
                text(label())
                fontSize(13f)
                color(Color(if (active()) 0xFFFFFFFF else 0xFF666666))
            }
        }
        event {
            click { onClick() }
        }
    }
}
