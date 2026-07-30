package com.arialentropy.kuiklytable

/**
 * Table 组件主题色值
 *
 * 色值格式为 Long ARGB（0xAARRGGBB）。
 * 默认值按语义角色对齐 KuiklyUI 宿主皮肤 token 方向（bg_default / divider / text_primary 等）。
 */
class TableThemeColors(
    /** 表头背景 */
    val headerBackground: Long = 0xFFF5F5F5,
    /** 表头文字 */
    val headerText: Long = 0xFF333333,
    /** 单元格主文本 */
    val cellText: Long = 0xFF333333,
    /** 单元格次要文本 / List 模式标签 */
    val cellTextSecondary: Long = 0xFF999999,
    /** 网格线 / 边框 */
    val gridLine: Long = 0xFFE6E6E6,
    /** 默认行背景 */
    val rowBackground: Long = 0xFFFFFFFF,
    /** 斑马纹行背景 */
    val rowBackgroundAlt: Long = 0xFFFAFAFA,
    /** 选中行背景；对齐 info 浅底语义 */
    val selectedRowBackground: Long = 0xFFEAF4FF,
    /** List 模式卡片背景 */
    val cardBackground: Long = 0xFFFFFFFF,
    /** List 模式卡片边框 */
    val cardBorder: Long = 0xFFE6E6E6,
    /** Success 语义状态标签背景 */
    val statusTagBackground: Long = 0xFFE8F5E9,
    /** Success 语义状态标签文字 */
    val statusTagText: Long = 0xFF2E7D32,
    /** Warning 语义状态标签背景 */
    val statusTagBackgroundAlt: Long = 0xFFFFF4E5,
    /** Warning 语义状态标签文字 */
    val statusTagTextAlt: Long = 0xFFFF9800,
    /** Danger 语义状态标签背景 */
    val statusTagDangerBackground: Long = 0xFFFFEFF0,
    /** Danger 语义状态标签文字；取宿主 text_warning 方向 */
    val statusTagDangerText: Long = 0xFFFF5967,
    /** Neutral 语义状态标签背景；取宿主 bg_backplate 方向 */
    val statusTagNeutralBackground: Long = 0xFFF5F5F5,
    /** Neutral 语义状态标签文字；取宿主 text_secondary 方向 */
    val statusTagNeutralText: Long = 0xFF999999,
    /** Info 语义状态标签背景 */
    val statusTagInfoBackground: Long = 0xFFEAF4FF,
    /** Info 语义状态标签文字；取宿主 text_link 方向 */
    val statusTagInfoText: Long = 0xFF2E77E5,
    /** 选择框未选中时的填充色 */
    val selectionControlBackground: Long = rowBackground,
    /** 加载 / 空 / 错误状态层背景；默认全透明，只覆盖在表格区域之上 */
    val stateOverlayBackground: Long = 0x00FFFFFF,
    /** 状态层提示文字 */
    val stateText: Long = 0xFF666666,
    /** 状态层主操作文字（无填充样式） */
    val actionText: Long = 0xFF2E77E5,
    /** 状态层主操作文字（有填充背景时） */
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
            selectedRowBackground = 0xFF0E2D4A,
            cardBackground = 0xFF242326,
            cardBorder = 0xFF49454F,
            statusTagBackground = 0xFF1B3A26,
            statusTagText = 0xFFA5D6A7,
            statusTagBackgroundAlt = 0xFF3E2A10,
            statusTagTextAlt = 0xFFFFCC80,
            statusTagDangerBackground = 0xFF4A1618,
            statusTagDangerText = 0xFFF2B8B5,
            statusTagNeutralBackground = 0xFF2B2930,
            statusTagNeutralText = 0xFFCAC4D0,
            statusTagInfoBackground = 0xFF0E2D4A,
            statusTagInfoText = 0xFF90CAF9,
            selectionControlBackground = 0xFF2B2930,
            stateOverlayBackground = 0x001C1B1F,
            stateText = 0xFFCAC4D0,
            actionText = 0xFF90CAF9,
            actionTextOnFill = 0xFF10223A,
        )

        /** 蓝色示例主题；只覆盖蓝色调相关色值，danger / neutral / info 沿用默认值。 */
        val Blue = TableThemeColors(
            headerBackground = 0xFF0D47A1,
            headerText = 0xFFFFFFFF,
            cellText = 0xFF12304A,
            cellTextSecondary = 0xFF55758F,
            gridLine = 0xFF90CAF9,
            rowBackground = 0xFFEAF4FF,
            rowBackgroundAlt = 0xFFDCEEFF,
            selectedRowBackground = 0xFFBBDEFB,
            cardBackground = 0xFFF4F9FF,
            cardBorder = 0xFF90CAF9,
            statusTagBackground = 0xFFC8E6C9,
            statusTagText = 0xFF2E7D32,
            statusTagBackgroundAlt = 0xFFFFE0B2,
            statusTagTextAlt = 0xFFE65100,
            selectionControlBackground = 0xFFEAF4FF,
            stateOverlayBackground = 0x00EAF4FF,
            stateText = 0xFF365A75,
            actionText = 0xFF0D47A1,
            actionTextOnFill = 0xFFFFFFFF,
        )
    }
}

/** List 模式状态标签色板。 */
class TableStatusTagStyle(
    val background: Long,
    val text: Long,
) {
    companion object {
        fun fromPreset(
            preset: TableStatusTagPreset,
            themeColors: TableThemeColors,
        ): TableStatusTagStyle = when (preset) {
            is TableStatusTagPreset.Success -> TableStatusTagStyle(
                background = themeColors.statusTagBackground,
                text = themeColors.statusTagText,
            )
            is TableStatusTagPreset.Warning -> TableStatusTagStyle(
                background = themeColors.statusTagBackgroundAlt,
                text = themeColors.statusTagTextAlt,
            )
            is TableStatusTagPreset.Danger -> TableStatusTagStyle(
                background = themeColors.statusTagDangerBackground,
                text = themeColors.statusTagDangerText,
            )
            is TableStatusTagPreset.Neutral -> TableStatusTagStyle(
                background = themeColors.statusTagNeutralBackground,
                text = themeColors.statusTagNeutralText,
            )
            is TableStatusTagPreset.Info -> TableStatusTagStyle(
                background = themeColors.statusTagInfoBackground,
                text = themeColors.statusTagInfoText,
            )
        }
    }
}

/**
 * List 模式状态标签的语义预设。
 *
 * [fromText] 只是内置的兜底映射；业务应优先用 statusTagPresetByText / statusTagStyleResolver
 * 指定自己的状态文本对应哪个语义。
 */
sealed class TableStatusTagPreset {
    object Success : TableStatusTagPreset()
    object Warning : TableStatusTagPreset()
    object Danger : TableStatusTagPreset()
    object Neutral : TableStatusTagPreset()
    object Info : TableStatusTagPreset()

    companion object {
        private val successTexts = setOf("在职", "正常", "启用", "成功", "已完成", "通过", "active", "enabled", "success")
        private val warningTexts = setOf("休假", "待处理", "进行中", "处理中", "warning", "pending", "processing", "in progress")
        private val dangerTexts = setOf("离职", "停用", "异常", "错误", "失败", "禁用", "error", "failed", "disabled", "inactive")
        private val neutralTexts = setOf("草稿", "未知", "无", "未开始", "default", "neutral", "draft", "unknown")
        private val infoTexts = setOf("信息", "新建", "通知", "info", "new", "notice")

        /** 未命中任何内置词表时返回 [Neutral]。 */
        fun fromText(text: String): TableStatusTagPreset {
            val normalized = text.trim().lowercase()
            return when {
                normalized in successTexts -> Success
                normalized in warningTexts -> Warning
                normalized in dangerTexts -> Danger
                normalized in neutralTexts -> Neutral
                normalized in infoTexts -> Info
                else -> Neutral
            }
        }
    }
}

/** 表头的结构化视觉样式；颜色仍由 TableThemeColors 提供。 */
class TableHeaderStyle(
    val fontSize: Float = 14f,
    val fontWeight: TableHeaderFontWeight = TableHeaderFontWeight.Medium,
    val paddingH: Float = 12f,
    val paddingV: Float = 10f,
    /** 固定表头高度；0 表示由 padding 与文字撑开 */
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
