# 前端开发规范

> 本目录记录 Label Studio `web/` 前端的真实开发约定。新增或修改前端功能时，先阅读本索引，再按变更类型读取对应细分文档。

---

## 规范索引

| 指南 | 说明 | 状态 |
|---|---|---|
| [目录结构](./directory-structure.md) | Nx workspace、apps/libs 分层、导入边界 | Filled |
| [React 与 TypeScript](./react-typescript-guidelines.md) | React 组件、状态管理、类型约定、工具函数 | Filled |
| [样式与设计系统](./styling-design-system.md) | `@humansignal/ui`、Tailwind 语义 token、CSS module、可访问性 | Filled |
| [测试规范](./testing-guidelines.md) | Jest/RTL、Storybook、Cypress integration/e2e helper 规则 | Filled |
| [质量规范](./quality-guidelines.md) | Biome、禁止模式、review checklist、验证命令 | Filled |

---

## 开发前检查清单

- [ ] 明确变更属于 app、shared app block、UI component、editor core、frontend-test helper 中哪一层。
- [ ] 搜索是否已有可复用组件、hook、atom、helper 或 `@humansignal/ui` 组件。
- [ ] 若新增 UI，先阅读 [样式与设计系统](./styling-design-system.md)，确认 token、组件和 Storybook 要求。
- [ ] 若新增 React/TypeScript 逻辑，阅读 [React 与 TypeScript](./react-typescript-guidelines.md)。
- [ ] 若新增测试或测试 helper，阅读 [测试规范](./testing-guidelines.md)。
- [ ] 修改完成后按 [质量规范](./quality-guidelines.md) 运行最小必要验证。

---

## 事实来源

- `.cursor/rules/react.mdc`
- `.cursor/rules/typescript.mdc`
- `.cursor/rules/design.mdc`
- `.cursor/rules/tailwind.mdc`
- `.cursor/rules/no-lodash.mdc`
- `.cursor/rules/cypress_tests.mdc`
- `web/package.json`
- `web/biome.json`
- `web/.editorconfig`
- `web/apps/labelstudio/project.json`
- `web/libs/ui/project.json`

---

**语言**：本工作区文档使用中文。
