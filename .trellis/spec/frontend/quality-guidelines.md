# 前端质量规范

> 前端变更应保持小而聚焦，遵守 Nx/Biome/设计系统边界，优先复用已有组件和 helper。任何新 UI 或交互逻辑都应有最小必要测试或 Storybook 覆盖。

---

## 工具与格式

| 工具 | 当前配置 |
|---|---|
| 包管理/脚本 | `web/package.json` 使用 Yarn + Nx |
| 格式与 lint | Biome，配置在 `web/biome.json` |
| 格式参数 | 2 空格、line width 120、`formatWithErrors: true` |
| CSS | Biome CSS parser 启用 CSS modules 和 Tailwind directives |
| EditorConfig | `web/.editorconfig`：UTF-8、2 空格、trim trailing whitespace、insert final newline |
| TypeScript | `typescript` 5.8.x，React 18.3.x |

常用命令：

```bash
cd web && yarn lint
cd web && yarn lint-css
cd web && yarn test:unit
cd web && yarn test:e2e
```

---

## 必须遵守

- 新增或修改代码前先搜索现有组件、hook、atom、helper、token 和 util。
- UI 优先复用 `@humansignal/ui` 和 `@humansignal/icons`。
- 工具函数优先走 `@humansignal/core` wrapper。
- 社区版界面不得暴露“仅企业版可用但当前仓库无完整实现”的空壳入口；对这类能力应优先隐藏入口，而不是保留 badge、禁用表单或占位提示。
- 如果后端接口会返回带有 `type: enterprise` 或等价企业属性的目录/模板数据，前端仍应保留防御性过滤，避免缓存或旧响应再次把空壳入口暴露出来。
- 新增共享 UI 组件时补 Storybook story 和必要单测。
- 新增或修改测试 helper 时，同步导出到 `web/libs/frontend-test/src/helpers/LSF/index.ts`。
- 涉及可访问性的交互必须验证键盘路径和可访问名称。
- 涉及布局的改动要考虑 375px、768px、1024px+ 等响应式断点。

---

## 禁止模式

- 禁止新增 lodash import 或 require。
- 禁止直接从 `es-toolkit` 或 `es-toolkit/compat` 导入应用代码。
- 禁止直接从 `web/libs/ui/src/shad` 导入组件。
- 禁止在样式中硬编码主题色、关键间距、字号，除非有明确无法 token 化的技术原因。
- 禁止新增不带用户友好错误态/加载态的异步 UI。
- 禁止用任意 `cy.wait(milliseconds)` 掩盖状态同步问题。
- 禁止 `web/libs` 反向依赖 `web/apps` 私有实现。

---

## lodash 替代规则

| 需求 | 推荐 |
|---|---|
| `cloneDeep` | `structuredClone(obj)` |
| `debounce` | `@humansignal/core/lib/utils/debounce` |
| `camelCase`、`snakeCase`、`kebabCase`、`capitalize`、`startCase`、`pascalCase` | `@humansignal/core/lib/utils/string` |
| `throttle`、`clamp`、`get`、`isMatch`、`uniqBy` | `@humansignal/core/lib/utils/lodash-replacements` |

如果需要新的 lodash 等价函数，先检查 es-toolkit，再添加到 `@humansignal/core` wrapper，不要在 app 代码中直接引入第三方实现。

---

## Review Checklist

- 代码是否放在正确的 app/lib 层？
- 是否复用 `@humansignal/ui`、`@humansignal/icons`、`@humansignal/core`？
- 是否避免 lodash、直接 shad import、硬编码 token？
- 是否遵守 Jotai/React Query 的状态和数据请求边界？
- 新 UI 是否有 Storybook 和必要单测？
- Cypress 测试是否使用集中 helper、状态等待和相对坐标？
- 是否覆盖 loading、empty、error、disabled、keyboard 等关键状态？
- 是否运行了与变更范围匹配的最小验证命令？
