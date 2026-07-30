package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color

/** 单条描边：颜色为 ARGB Long，宽度为 dp。 */
data class TableStroke(
    val color: Long,
    val width: Float = 1f,
)

/**
 * 表格各区域描边。字段为 null 表示该线关闭。
 *
 * - [outer] 根容器外框
 * - [header] 表头底部分隔线
 * - [row] Table 模式行间横线
 * - [column] 列间竖线
 * - [listRow] List 模式行间分隔线
 */
data class TableLineStyle(
    val outer: TableStroke? = null,
    val header: TableStroke? = null,
    val row: TableStroke? = null,
    val column: TableStroke? = null,
    val listRow: TableStroke? = null,
)

/**
 * 表格线模式。默认 [Grid]，对齐历史 `borderMode=Default` 的网格观感。
 *
 * 斑马纹 / 选中 / 编辑 / 展开不得隐式改写本配置。
 */
sealed class TableLineMode {
    /** 关闭所有表格线。 */
    object None : TableLineMode()

    /** 仅表头底线、行线、List 行线。 */
    object Horizontal : TableLineMode()

    /** 外框 + 表头线 + 行线 + 列线（及 List 行线）。 */
    object Grid : TableLineMode()

    /** 按 [TableLineStyle] 逐项控制。 */
    class Custom(val style: TableLineStyle) : TableLineMode()
}

internal fun TableThemeColors.effectiveDividerColor(): Long = dividerColor ?: gridLine

internal fun TableThemeColors.effectiveFrozenDividerColor(): Long =
    frozenDividerColor ?: effectiveDividerColor()

internal fun TableLineMode.resolve(theme: TableThemeColors): TableLineStyle {
    val hair = TableStroke(theme.effectiveDividerColor(), 1f)
    return when (this) {
        is TableLineMode.None -> TableLineStyle()
        is TableLineMode.Horizontal -> TableLineStyle(
            header = hair,
            row = hair,
            listRow = hair,
        )
        is TableLineMode.Grid -> TableLineStyle(
            outer = hair,
            header = hair,
            row = hair,
            column = hair,
            listRow = hair,
        )
        is TableLineMode.Custom -> style
    }
}

internal fun TableStroke.toBorder(): Border =
    Border(width.coerceAtLeast(0f), BorderStyle.SOLID, Color(color))

internal fun TableStroke?.toOuterBorderOrTransparent(): Border =
    this?.toBorder() ?: Border(0f, BorderStyle.SOLID, Color(0x00000000))

internal fun TableStroke?.columnDividerWidth(isLastColumn: Boolean): Float =
    if (this != null && !isLastColumn) width.coerceAtLeast(0f) else 0f
