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
)
