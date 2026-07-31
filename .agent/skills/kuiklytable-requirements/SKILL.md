---
name: kuiklytable-requirements
description: >-
  从 KuiklyTable 愿景提炼可验收需求。产出需求条目、验收条件、优先级与 Issue 归属。
  触发：提炼需求、写验收、拆 Issue、需求不清、验收条件、ST-/PT-、优先级、
  能不能验收、验收标准、requirements、acceptance criteria。
  上游不足时先提示跑 kuiklytable-vision。不要用于：改愿景/做不做（vision）；
  UI 文案与 Demo 面板（uiux-design）；布局/Windowed 方案（tech-design）；
  直接实现代码（implementation）。
---

# KuiklyTable 提炼需求

上游：`kuiklytable-vision`。下游：`kuiklytable-uiux-design` / `kuiklytable-tech-design`。红线：`kuiklytable-redlines`。

把愿景收成**可验收软件需求**，不写交互稿全文、不写渲染算法、不直接改代码。

## 目标

1. 每条需求有唯一编号意图（或对应本仓库 Issue）。
2. 验收条件可观察（运行 Showcase / 看截图 / 跑单测），禁止纯形容词。
3. 标优先级与状态：已交付 | 进行中 | 待验证 | 后续 | 范围外。
4. 明确 Issue：已有则对齐范围；没有则「待建」并给出建议标题/范围（正文遵守 redlines）。

## 工作步骤

1. **核对愿景**：范围与非目标是否已固定；冲突先回 vision。
2. **拆条**：一能力一条；交互与视图层（如筛选 vs Windowed）分开写正交关系。
3. **写验收**：给定操作 → 可观察结果；含默认/fallback（如 `zebraStripe` 默认关）。
4. **挂 Issue**：P0/P1 必须有归属或「待建」；后续能力可只记账不建票。
5. **输出需求表**（给用户）：编号或临时 ID | 需求 | 验收 | 优先级 | Issue | 状态。
6. **交接**：视觉交互 → uiux-design；架构/API → tech-design；可以开工 → implementation。

## 需求条目模板

```text
ID:
陈述: 作为…/组件应…
验收:
  - …
非目标/边界: …
优先级: P0|P1|P2|后续|范围外
Issue: #N | 待建
状态: 已交付|进行中|待验证|后续|范围外
```

## 边界

| 本 Skill | 交给其它 Skill |
| --- | --- |
| 验收条件、优先级、Issue 范围 | 技术选型长文 → tech-design / vision |
| 默认值作为验收点（对齐代码/`site`） | 完整 `site` API 表复述 |
| 指出缺设计再实现 | 直接改源码 → implementation |

## 完成前检查

- [ ] 每条验收可测；未验证未标已交付
- [ ] 范围外/后续未混进当前必验
- [ ] Issue 正文建议未含官方 `owner/repo#N`、未引本地私有路径（见 redlines）
- [ ] 已指出下游 Skill
