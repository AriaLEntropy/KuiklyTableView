package com.arialentropy.kuiklytable

/**
 * 表头高度与表头+底部分隔线合计高度。
 * 溢出提示等「相对 Table 根」的估算坐标应复用这里，避免业务侧再硬编码 40/44。
 */
internal object TableHeaderMetrics {
    const val DEFAULT_HEIGHT = 44f

    fun resolvedHeight(style: TableHeaderStyle): Float =
        if (style.height > 0f) style.height else DEFAULT_HEIGHT

    /** 表头行高度 + 表头底部分隔线（若有）。 */
    fun blockHeight(
        style: TableHeaderStyle,
        lineMode: TableLineMode,
        theme: TableThemeColors,
    ): Float {
        val divider = lineMode.resolve(theme).header?.width?.coerceAtLeast(0f) ?: 0f
        return resolvedHeight(style) + divider
    }
}
