package com.arialentropy.kuiklytable

import com.arialentropy.kuiklytable.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 组件族总览：KuiklyTable（基础表格）与 KuiklyDataTable（数据表格）正式 Showcase 入口。
 * 只做导览跳转，不堆配置开关。
 */
@Page("table_home", supportInLocal = true)
internal class TableHomePage : BasePager() {

    private val chrome = TableThemeColors.Light

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color(ctx.chrome.rowBackgroundAlt))
            }
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight + 16f)
                    paddingLeft(16f)
                    paddingRight(16f)
                    paddingBottom(8f)
                    alignSelfStretch()
                }
                Text {
                    attr {
                        text("KuiklyTable 组件族")
                        fontSize(20f)
                        fontWeightSemiBold()
                        color(Color(ctx.chrome.cellText))
                    }
                }
                Text {
                    attr {
                        marginTop(6f)
                        text("两级正式表格：Basic 负责展示与布局，DataTable 负责选择、筛选与分页。")
                        fontSize(13f)
                        color(Color(ctx.chrome.cellTextSecondary))
                        lines(3)
                    }
                }
            }

            View {
                attr {
                    flex(1f)
                    paddingLeft(16f)
                    paddingRight(16f)
                    paddingBottom(24f)
                    alignSelfStretch()
                }
                LevelEntryCard(
                    title = "KuiklyTable",
                    badge = "TableView",
                    summary = "基础展示、列布局、分隔线、横纵滚动、固定表头/左固定列、主题、renderer、状态层、单列排序、溢出提示、加载更多与回顶。按章节验证基础能力。",
                    pageName = "table_basic",
                    chrome = ctx.chrome,
                ) { ctx.openShowcase("table_basic") }
                LevelEntryCard(
                    title = "KuiklyDataTable",
                    badge = "DataTableView",
                    summary = "在 KuiklyTable 之上增加行选择、筛选与客户端分页；「大量数据」用 3000 行验证虚拟滚动，可与筛选组合。",
                    pageName = "table_data",
                    chrome = ctx.chrome,
                ) { ctx.openShowcase("table_data") }
            }
        }
    }

    private fun openShowcase(pageName: String) {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName, JSONObject())
    }
}

private fun ViewContainer<*, *>.LevelEntryCard(
    title: String,
    badge: String,
    summary: String,
    pageName: String,
    chrome: TableThemeColors,
    onClick: () -> Unit,
) {
    View {
        attr {
            marginTop(12f)
            padding(16f)
            borderRadius(12f)
            backgroundColor(Color(chrome.cardBackground))
            border(Border(1f, BorderStyle.SOLID, Color(chrome.cardBorder)))
            alignSelfStretch()
        }
        event {
            click { onClick() }
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                marginBottom(8f)
            }
            Text {
                attr {
                    text(title)
                    fontSize(17f)
                    fontWeightSemiBold()
                    color(Color(chrome.cellText))
                    flex(1f)
                }
            }
            View {
                attr {
                    paddingLeft(8f)
                    paddingRight(8f)
                    paddingTop(4f)
                    paddingBottom(4f)
                    borderRadius(8f)
                    backgroundColor(Color(chrome.rowBackgroundAlt))
                    border(Border(1f, BorderStyle.SOLID, Color(chrome.actionText)))
                }
                Text {
                    attr {
                        text(badge)
                        fontSize(11f)
                        color(Color(chrome.actionText))
                    }
                }
            }
        }
        Text {
            attr {
                text(summary)
                fontSize(13f)
                color(Color(chrome.cellTextSecondary))
                lines(5)
                marginBottom(10f)
            }
        }
        Text {
            attr {
                text("打开 $pageName →")
                fontSize(13f)
                fontWeightSemiBold()
                color(Color(chrome.actionText))
            }
        }
    }
}
