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
    /** 成功状态标签背景（在职/正常） */
    val statusTagBackground: Long = 0xFFE8F5E9,
    /** 成功状态标签文字（在职/正常） */
    val statusTagText: Long = 0xFF2E7D32,
    /** 警告状态标签背景（如休假/待处理） */
    val statusTagBackgroundAlt: Long = 0xFFFFF4E5,
    /** 警告状态标签文字 */
    val statusTagTextAlt: Long = 0xFFFF9800,
    /** 危险状态标签背景（如离职/异常） */
    val statusTagDangerBackground: Long = 0xFFFFEFF0,
    /** 危险状态标签文字；取宿主 text_warning 方向 */
    val statusTagDangerText: Long = 0xFFFF5967,
    /** 中性状态标签背景；取宿主 bg_backplate 方向 */
    val statusTagNeutralBackground: Long = 0xFFF5F5F5,
    /** 中性状态标签文字；取宿主 text_secondary 方向 */
    val statusTagNeutralText: Long = 0xFF999999,
    /** 信息状态标签背景 */
    val statusTagInfoBackground: Long = 0xFFEAF4FF,
    /** 信息状态标签文字；取宿主 text_link 方向 */
    val statusTagInfoText: Long = 0xFF2E77E5,
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
            statusTagDangerBackground = 0xFF4A1618,
            statusTagDangerText = 0xFFF2B8B5,
            statusTagNeutralBackground = 0xFF2B2930,
            statusTagNeutralText = 0xFFCAC4D0,
            statusTagInfoBackground = 0xFF0E2D4A,
            statusTagInfoText = 0xFF90CAF9,
            stateOverlayBackground = 0x001C1B1F,
            stateText = 0xFFCAC4D0,
            errorText = 0xFFF2B8B5,
            actionText = 0xFF90CAF9,
            actionTextOnFill = 0xFF10223A,
        )
    }
}

/** Mobile List 状态标签色板。 */
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

/** Mobile List 状态标签预设，业务可通过 resolver 自行决定状态文本对应的语义。 */
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
