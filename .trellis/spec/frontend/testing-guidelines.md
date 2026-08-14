# 前端测试规范

> 前端测试按层选择工具：组件/逻辑用 Jest + React Testing Library，UI 组件补 Storybook，Label Studio Frontend 交互流程用 Cypress integration/e2e 与集中 helper。

---

## 测试入口

| 场景 | 命令 |
|---|---|
| 所有前端 unit | `cd web && yarn test:unit` |
| unit 覆盖率 | `cd web && yarn test:unit:coverage` |
| 主应用 unit | `cd web && yarn ls:unit` |
| editor unit | `cd web && yarn lsf:unit` |
| datamanager unit | `cd web && yarn dm:unit` |
| editor integration | `cd web && yarn lsf:integration` |
| Label Studio e2e | `cd web && yarn ls:e2e` |
| Storybook | `cd web && yarn storybook:serve` 或 `cd web && yarn ui:serve` |

这些命令来自 `web/package.json` 和各 Nx `project.json`。

---

## 单元与组件测试

- UI 组件使用 Jest + React Testing Library。
- 测试文件通常与组件共置，命名为 `.spec.tsx` 或 `.test.tsx`。
- 断言不仅检查渲染成功，也应覆盖关键 props、交互和可访问角色。

真实示例：

- `web/libs/ui/src/lib/checkbox/checkbox.spec.tsx` 使用 `render`、`fireEvent`、`getByRole` 覆盖 checked、onChange、indeterminate、className 和 style。
- `web/libs/ui/src/lib/json-viewer/json-viewer.test.tsx` 是 UI 组件测试示例。
- `web/libs/editor/src/core/__tests__/Tree.test.tsx` 是 editor core 测试示例。

---

## Storybook

- 新增 `web/libs/ui` 组件时应补 `*.stories.tsx`。
- story title 使用 UI 分组，例如 `UI/Button`。
- story 应覆盖主要 variant、size、disabled、loading/waiting、复杂 children 等状态。

真实示例：

- `web/libs/ui/src/lib/button/button.stories.tsx`
- `web/libs/ui/src/lib/badge/badge.stories.tsx`
- `web/libs/ui/src/lib/date-range-picker/date-range-picker.stories.tsx`

---

## Cypress integration/e2e

- LSF/editor integration 测试放在 `web/libs/editor/tests/integration/e2e/`。
- 测试文件命名为 `*.cy.ts`，按语义目录组织，例如 `core/`、`image_segmentation/`、`control_tags/`、`audio/`、`video/`。
- 测试数据放在 `web/libs/editor/tests/integration/data/` 的对应语义目录。
- 优先从 `@humansignal/frontend-test/helpers/LSF` 导入集中 helper，例如 `LabelStudio`、`ImageView`、`Sidebar`、`Labels`、`Hotkeys`。

真实示例：

- `web/libs/frontend-test/src/helpers/LSF/LabelStudio.ts` 提供 fluent `LabelStudio.params().config(...).data(...).withResult(...).init()` 初始化模式。
- `web/libs/frontend-test/src/helpers/LSF/Hotkeys.ts` 封装 Mac/PC 快捷键差异。
- `web/libs/editor/tests/integration/e2e/image_segmentation/basic.cy.ts` 是图像分割测试示例。

---

## Cypress 编写规则

- 初始化后调用 `LabelStudio.waitForObjectsReady()`。
- 图片/视频/音频等媒体场景按需等待对应 helper，例如 `ImageView.waitForImage()`。
- 用状态断言等待 UI 就绪，例如 `Sidebar.hasRegions(1)`，不要随意 `cy.wait(milliseconds)`。
- 坐标操作优先使用 relative 方法，例如 `ImageView.drawRectRelative()`。
- 键盘操作优先用 `Hotkeys` helper，不要直接写平台相关 `{ctrl}` / `{cmd}`。
- helper 中应包含 `cy.log()`，便于失败定位。

---

## 禁止/避免模式

- 不要用任意长时间 `cy.wait(500)` 代替状态等待。
- 不要在测试里复制完整复杂流程到 helper；helper 保持可复用的 UI 交互和断言。
- 不要在跨平台快捷键测试中直接使用 `cy.get("body").type("{ctrl}z")`，优先用 `Hotkeys.undo()`。
- 不要只断言“元素存在”，关键用户行为应断言状态变化或序列化结果。
