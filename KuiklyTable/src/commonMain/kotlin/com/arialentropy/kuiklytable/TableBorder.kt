package com.arialentropy.kuiklytable

/** Table 根容器外框配置。 */
sealed class TableBorderMode {
    /** 不绘制外框，也不绘制列间竖线。 */
    object None : TableBorderMode()

    /** 使用当前主题的 [TableThemeColors.gridLine] 绘制 1dp 外框。 */
    object Default : TableBorderMode()

    /** 使用调用方指定的颜色和宽度绘制外框。 */
    class Custom(
        val color: Long,
        val width: Float = 1f,
    ) : TableBorderMode()
}
