package com.arialentropy.kuiklytable

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color

internal fun TableBorderMode.hasVisibleLines(): Boolean = this !is TableBorderMode.None

internal fun TableBorderMode.borderSpec(themeColors: TableThemeColors): Border? = when (this) {
    is TableBorderMode.None -> null
    is TableBorderMode.Default -> Border(1f, BorderStyle.SOLID, Color(themeColors.gridLine))
    is TableBorderMode.Custom -> Border(width.coerceAtLeast(0f), BorderStyle.SOLID, Color(color))
}
