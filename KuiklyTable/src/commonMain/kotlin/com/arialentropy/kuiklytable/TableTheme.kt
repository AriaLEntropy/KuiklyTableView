package com.arialentropy.kuiklytable

/**
 * Table 组件主题色值
 *
 * 色值格式为 Long ARGB（0xAARRGGBB），参照 KuiklyChatUI ChatThemeColors 模式。
 * 默认值取自 KuiklyUI 宿主皮肤 token 方向（bg_default / divider / text_primary 等），
 * 不硬编码竞品 hex。
 */
class TableThemeColors(
    /** 表头背景 */
    val headerBackground: Long = 0xFFF5F5F5,
    /** 表头文字 */
    val headerText: Long = 0xFF333333,
    /** 单元格主文本 */
    val cellText: Long = 0xFF333333,
    /** 网格线 / 边框 */
    val gridLine: Long = 0xFFE6E6E6,
    /** 默认行背景 */
    val rowBackground: Long = 0xFFFFFFFF,
    /** 斑马纹行背景 */
    val rowBackgroundAlt: Long = 0xFFFAFAFA,
) {
    companion object {
        /** 默认浅色主题，语义角色对齐 KuiklyUI 宿主皮肤 token。 */
        val Light = TableThemeColors()

        /** 深色主题，语义角色参考 Material 3 dark color scheme。 */
        val Dark = TableThemeColors(
            headerBackground = 0xFF242326,
            headerText = 0xFFE6E1E5,
            cellText = 0xFFE6E1E5,
            gridLine = 0xFF49454F,
            rowBackground = 0xFF1C1B1F,
            rowBackgroundAlt = 0xFF211F23,
        )
    }
}

/** 表头的结构化视觉样式；颜色仍由 TableThemeColors 提供。 */
class TableHeaderStyle(
    val fontSize: Float = 14f,
    val fontWeight: TableHeaderFontWeight = TableHeaderFontWeight.Medium,
    val paddingH: Float = 12f,
    val paddingV: Float = 10f,
    val height: Float = 0f,
    val bottomBorderWidth: Float = 1f,
) {
    companion object {
        val Default = TableHeaderStyle()
    }
}

sealed class TableHeaderFontWeight {
    object Normal : TableHeaderFontWeight()
    object Medium : TableHeaderFontWeight()
    object Semisolid : TableHeaderFontWeight()
    object Bold : TableHeaderFontWeight()
}
