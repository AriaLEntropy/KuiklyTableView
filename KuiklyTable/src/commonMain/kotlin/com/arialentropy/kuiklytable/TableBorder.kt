package com.arialentropy.kuiklytable

/** Table 根容器外框配置。默认值为 [Default]。 */
sealed class TableBorderMode {
    /** 不绘制外框，也不绘制列间竖线。 */
    object None : TableBorderMode()

    /** 使用当前主题的 [TableThemeColors.gridLine] 绘制 1dp 外框（attr 默认）。 */
    object Default : TableBorderMode()

    /** 使用调用方指定的颜色和宽度绘制外框。 */
    class Custom(
        val color: Long,
        val width: Float = 1f,
    ) : TableBorderMode()
}

/** 常用圆角预设；[TableAttr.cornerRadius] 仍接受任意 dp 值。 */
object TableCornerRadius {
    /** 无圆角。 */
    const val None = 0f

    /** 默认圆角（8dp）。 */
    const val Default = 8f

    /** 较大圆角，适合强调卡片感的场景。 */
    const val Large = 12f
}
