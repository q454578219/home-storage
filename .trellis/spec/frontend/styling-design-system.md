# 样式与设计系统规范

> UI 应遵循 HumanSignal 设计系统，优先复用 `@humansignal/ui`、`@humansignal/icons` 和语义设计 token。可访问性目标为 WCAG 2.1 AA。

---

## 组件复用

- 创建新 UI 前，先检查 `@humansignal/ui` 是否已有可复用组件。
- 常见替换：原生 `<button>` 优先使用 `Button`；信息提示优先使用 `Message`；空状态优先使用 `EmptyState`；tooltip 使用 `Tooltip`。
- 图标从 `@humansignal/icons` 导入。
- UI 组件应补 Storybook story，参考 `web/libs/ui/src/lib/button/button.stories.tsx`。

真实示例：

- `web/apps/labelstudio/src/pages/Home/HomePage.tsx` 从 `@humansignal/ui` 导入 `Button`、`SimpleCard`、`Spinner`、`Tooltip`、`Typography`。
- `web/libs/ui/src/lib/button/button.tsx` 定义 Button variants、looks、sizes，并组合 `Tooltip`。
- `web/libs/ui/src/lib/empty-state/empty-state.stories.tsx` 展示空状态组件 story。

---

## Token 与 Tailwind

- Tailwind 类应使用语义 token，例如 `p-tight`、`text-body-medium`、`bg-primary-surface`。
- 避免使用数字 token，例如 `p-200`、`text-16`、`bg-grape-600`。
- Figma token 来源是 `web/libs/ui/src/tokens/tokens.prefix.css`，重新生成使用 `cd web && yarn design-tokens`。
- 禁止使用未由 Figma Variable Exporter 建立的 token。
- 默认不要硬编码颜色、间距、字号；canvas/JS 绘制场景可使用 `getTokenColor` 将语义 token 转为运行时颜色。

真实示例：

- `web/libs/ui/src/lib/button/button.module.css` 使用 `--color-primary-surface`、`--color-neutral-content-subtlest` 等语义 CSS 变量。
- `web/libs/ui/src/lib/button/button.tsx` 使用 `gap-tight`、`rounded-smaller`、`px-tight` 等 Tailwind 语义 utility。

---

## CSS 组织

- 新 UI 组件优先使用 co-located `.module.css`。
- 历史 app 代码中存在 `.prefix.css`，如 `web/apps/labelstudio/src/components/Card/Card.prefix.css`；局部维护时保持现有体系。
- 自定义 CSS 只在 Tailwind utility 难以清晰表达或组件可维护性更好时使用。
- CSS 中组件级变量应引用语义 token，形成可维护 variant，例如 `--component-bg: var(--color-neutral-surface)`。

---

## 可访问性

- 所有交互元素必须可键盘访问。
- 焦点态需要可见且对比度足够。
- 颜色不能作为唯一信息传达方式。
- 表单、按钮、图标按钮需要明确 label 或可访问名称。
- 图片需要合适的 alt；装饰性图标应避免干扰读屏。
- 页面在 200% zoom 下不应丢失内容。

---

## 禁止/避免模式

- 不要在同一屏幕放多个 primary/filled 主 CTA。
- 不要直接用硬编码颜色或尺寸破坏暗色模式和主题。
- 不要直接导入 `web/libs/ui/src/shad`；只从 `@humansignal/ui` 导入公开组件。
- 不要跳过 Storybook story，特别是新增 `web/libs/ui` 组件时。
- 不要新增不可键盘访问的 clickable `div`。
