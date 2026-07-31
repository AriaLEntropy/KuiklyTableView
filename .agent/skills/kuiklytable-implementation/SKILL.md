---
name: kuiklytable-implementation
description: >-
  按已定设计在 KuiklyTableView 内实现与验证。改 KuiklyTable/shared、补 Showcase、
  同步 README/site、跑构建与单测。
  触发：实现、写代码、改 TableView、改 Demo、补截图、编译、单测、落地、修复 bug、
  同步 site/README。
  设计/需求未定时先 vision → requirements → uiux/tech-design。不要用于：只讨论做不做
  （vision）；只写验收表（requirements）；纯文案规范且不改文件时可先 uiux-design。
---

# KuiklyTable 实现

上游：`kuiklytable-tech-design` + `kuiklytable-uiux-design`（及已确认需求）。红线：`kuiklytable-redlines`。

设计指导实现：先有结论再改码；先 Showcase 验证再固化组件。

## 目标

1. 在 `KuiklyTable` / `shared` 落地已设计能力。
2. 用 Showcase 证明开/关与 fallback；需要时更新 `site/`、README。
3. 构建/单测通过；验证写明平台。

## 工作步骤

1. **准入**：对应 Issue/验收与设计结论在；缺则停并指回上游 Skill。
2. **改码**：优先复用 Kuikly DSL；小步；默认与互斥以源码为准。
3. **Showcase**：可点控件有行为；热区 ≥44dp；预览设 `tableWidth`。
4. **公开面**：API 变了则 README 摘要 ↔ `site` 完整表同步；README 不写进度语。
5. **验证**（在仓库根）：

```powershell
.\gradlew.bat :KuiklyTable:compileDebugKotlinAndroid
.\gradlew.bat :shared:compileDebugKotlinAndroid
.\gradlew.bat :androidApp:compileDebugKotlin
.\gradlew.bat :KuiklyTable:testDebugUnitTest
```

6. **收尾**：结论写平台与未验证项；截图给人确认（redlines）。

## 边界

| 做 | 不做 |
| --- | --- |
| 源码、Showcase、site/README、测试 | 向官方仓库提交 |
| 按设计实现 | 借实现偷偷扩范围 |
| 记录已知限制 | 未验证宣称三端 |

## 完成前检查

- [ ] 行为对上验收条件
- [ ] 默认值（如 `zebraStripe=false`）与 `site` 一致
- [ ] 验证范围与未验证项已写明
