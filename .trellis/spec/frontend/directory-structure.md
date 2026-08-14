# 前端目录结构

> 前端代码位于 `web/`，采用 Nx workspace 组织。新增代码时优先放入已有 app/lib 的清晰边界内，避免从 `libs` 反向依赖 `apps`。

---

## 总览

| 路径 | 角色 | 示例 |
|---|---|---|
| `web/apps/labelstudio` | 主 Label Studio 前端应用 | `web/apps/labelstudio/src/pages/Home/HomePage.tsx` |
| `web/apps/labelstudio-e2e` | 主应用 e2e 项目 | `web/apps/labelstudio-e2e/project.json` |
| `web/apps/playground` | standalone playground app | `web/apps/playground/project.json` |
| `web/libs/ui` | HumanSignal UI 组件库 | `web/libs/ui/src/lib/button/button.tsx` |
| `web/libs/app-common` | 跨 app 共享的页面级 block / app 组件 | `web/libs/app-common/src/blocks/StorageProviderForm/index.tsx` |
| `web/libs/core` | 核心工具、通用函数和 wrapper | `@humansignal/core` 导出工具 |
| `web/libs/editor` | Label Studio Frontend/editor 核心 | `web/libs/editor/src/LabelStudio.tsx` |
| `web/libs/frontend-test` | Cypress/LSF 测试 helper | `web/libs/frontend-test/src/helpers/LSF/LabelStudio.ts` |
| `web/libs/storybook` | Storybook 配置 | `web/libs/storybook/project.json` |

---

## 分层与导入边界

- `web/apps` 可以导入 `web/libs`。
- 普通 `web/libs` 不应导入 `web/apps`。
- `web/libs/app-common` 用于共享 app 级页面块，可以导入其他 `web/libs` 或 `web/apps` 中允许的共享入口；其他 `web/libs` 不应反向依赖 `app-common`。
- UI 基础组件放 `web/libs/ui` 并通过 `@humansignal/ui` 导入，不要从 `web/libs/ui/src/shad` 直接导入。
- 图标使用 `@humansignal/icons`。
- 通用工具优先从 `@humansignal/core` 或其 wrapper 子路径导入。

---

## 组件与文件组织

推荐的新 UI 组件结构：

```text
component-name/
├── component-name.tsx
├── component-name.module.css
├── component-name.stories.tsx
├── component-name.test.tsx 或 component-name.spec.tsx
└── index.ts
```

现有真实示例：

- `web/libs/ui/src/lib/button/button.tsx`
- `web/libs/ui/src/lib/button/button.module.css`
- `web/libs/ui/src/lib/button/button.stories.tsx`
- `web/libs/ui/src/lib/checkbox/checkbox.spec.tsx`
- `web/libs/ui/src/lib/empty-state/index.ts`

---

## 命名约定

- 新 UI 组件文件和文件夹优先使用 kebab-case，例如 `badge-group/badge-group.tsx`、`date-range-picker/date-range-picker.tsx`。
- 应用中已有大量历史 PascalCase/JSX 文件，例如 `web/apps/labelstudio/src/components/Breadcrumbs/Breadcrumbs.jsx`；修改历史代码时保持局部一致，不强行大规模重命名。
- React 组件使用 PascalCase 导出，例如 `HomePage`、`Button`。
- Jotai atom 文件通常命名为 `atoms.ts`，按实体或页面局部意图组织，例如 `web/apps/labelstudio/src/pages/Home/atoms.ts`。
- CSS module 使用 `.module.css`；历史 app 样式存在 `.prefix.css`，修改时保持原文件体系。

---

## 禁止/避免模式

- 不要在 `web/libs` 中导入 `web/apps` 的私有实现。
- 不要绕过 package 入口直接深度引用 UI 内部 `src/shad`。
- 不要为一次性页面细节创建全局 UI 组件；先判断是否应留在页面目录或 `app-common`。
- 不要新增 lodash 依赖或 lodash import；详见 [质量规范](./quality-guidelines.md)。
