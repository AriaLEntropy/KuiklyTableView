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
    /** 单元格次要文本 / Mobile List 标签 */
    val cellTextSecondary: Long = 0xFF999999,
    /** 网格线 / 边框 */
    val gridLine: Long = 0xFFE6E6E6,
    /** 默认行背景 */
    val rowBackground: Long = 0xFFFFFFFF,
    /** 斑马纹行背景 */
    val rowBackgroundAlt: Long = 0xFFFAFAFA,
    /** Mobile List 卡片背景 */
    val cardBackground: Long = 0xFFFFFFFF,
    /** Mobile List 卡片边框 */
    val cardBorder: Long = 0xFFE6E6E6,
    /** 状态标签背景（在职/正常 → 浅绿） */
    val statusTagBackground: Long = 0xFFC8E6C9,
    /** 状态标签文字（在职/正常 → 绿） */
    val statusTagText: Long = 0xFF4CAF50,
    /** 次要状态标签背景（如警告/休假） */
    val statusTagBackgroundAlt: Long = 0xFFFFE0B2,
    /** 次要状态标签文字 */
    val statusTagTextAlt: Long = 0xFFFF9800,
    /** 状态层遮罩背景 */
    val stateOverlayBackground: Long = 0x00FFFFFF,
    /** 状态层正文 */
    val stateText: Long = 0xFF666666,
    /** 错误状态文字 */
    val errorText: Long = 0xFFFF5967,
    /** 状态层主操作 */
    val actionText: Long = 0xFF2E77E5,
    /** 状态层主操作填充上的文字 */
    val actionTextOnFill: Long = 0xFFFFFFFF,
) {
    companion object {
        /** 默认浅色主题，语义角色对齐 KuiklyUI 宿主皮肤 token。 */
        val Light = TableThemeColors()

        /** 深色主题，语义角色参考 Material 3 dark color scheme。 */
        val Dark = TableThemeColors(
            headerBackground = 0xFF242326,
            headerText = 0xFFE6E1E5,
            cellText = 0xFFE6E1E5,
            cellTextSecondary = 0xFFCAC4D0,
            gridLine = 0xFF49454F,
            rowBackground = 0xFF1C1B1F,
            rowBackgroundAlt = 0xFF211F23,
            cardBackground = 0xFF242326,
            cardBorder = 0xFF49454F,
            statusTagBackground = 0xFF1B3A26,
            statusTagText = 0xFFA5D6A7,
            statusTagBackgroundAlt = 0xFF3E2A10,
            statusTagTextAlt = 0xFFFFCC80,
            stateOverlayBackground = 0x001C1B1F,
            stateText = 0xFFCAC4D0,
            errorText = 0xFFF2B8B5,
            actionText = 0xFF90CAF9,
            actionTextOnFill = 0xFF10223A,
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
