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
 * - [row] 行间横线
 * - [column] 列间竖线
 */
data class TableLineStyle(
    val outer: TableStroke? = null,
    val header: TableStroke? = null,
    val row: TableStroke? = null,
    val column: TableStroke? = null,
)

/**
 * 表格线模式。默认 [Grid]，对齐历史 `borderMode=Default` 的网格观感。
 *
 * 斑马纹 / 选中 / 编辑 / 展开不得隐式改写本配置。
 */
sealed class TableLineMode {
    /** 关闭所有表格线。 */
    object None : TableLineMode()

    /** 仅表头底线与行线。 */
    object Horizontal : TableLineMode()

    /** 外框 + 表头线 + 行线 + 列线。 */
    object Grid : TableLineMode()

    /** 按 [TableLineStyle] 逐项控制。 */
    class Custom(val style: TableLineStyle) : TableLineMode()
}

internal fun TableThemeColors.effectiveDividerColor(): Long = dividerColor ?: gridLine

/**
 * 固定列右侧分隔色。未显式设 [frozenDividerColor] 时，在普通分隔色上略加深（保持色相），
 * 避免与 Grid 列线糊成一条。
 */
internal fun TableThemeColors.effectiveFrozenDividerColor(): Long =
    frozenDividerColor ?: darkenArgb(effectiveDividerColor(), factor = 0.72f)

/** 保持 alpha，将 RGB 按 [factor]（0~1）压暗。 */
internal fun darkenArgb(argb: Long, factor: Float): Long {
    val a = (argb ushr 24) and 0xFF
    val r = (((argb ushr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
    val g = (((argb ushr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
    val b = ((argb and 0xFF) * factor).toInt().coerceIn(0, 255)
    return (a shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

internal fun TableLineMode.resolve(theme: TableThemeColors): TableLineStyle {
    val hair = TableStroke(theme.effectiveDividerColor(), 1f)
    return when (this) {
        is TableLineMode.None -> TableLineStyle()
        is TableLineMode.Horizontal -> TableLineStyle(
            header = hair,
            row = hair,
        )
        is TableLineMode.Grid -> TableLineStyle(
            outer = hair,
            header = hair,
            row = hair,
            column = hair,
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
