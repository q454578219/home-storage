# React 与 TypeScript 规范

> 新前端代码优先使用函数组件、TypeScript、Jotai/React Query 和现有包入口。旧 JSX/MobX 代码可按局部风格渐进维护，避免为单个需求做大规模迁移。

---

## React 组件

- 优先使用函数组件，不新增 class component。
- 组件应小而聚焦，复杂逻辑抽到自定义 hook、atom 或 helper。
- 复用通过组合完成，不使用继承。
- 避免不必要的 `useEffect`；确实需要副作用时，保持依赖数组正确并清理订阅/定时器。
- expensive component 可考虑 `React.memo`、`useMemo`、`useCallback`，但不要过度优化。

真实示例：

- `web/apps/labelstudio/src/pages/Home/HomePage.tsx` 使用函数组件、`useQuery`、Jotai atom 和 `@humansignal/ui`。
- `web/libs/ui/src/lib/button/button.tsx` 使用 `forwardRef`、显式 props 类型和组件组合。
- `web/apps/labelstudio/src/components/HeidiTips/HeidiTips.tsx` 是 app 级组件目录内的 TypeScript 组件。

---

## 状态管理

- 局部 UI 状态使用 `useState`。
- 跨组件共享状态使用 Jotai atom，不新增 Context 作为通用全局状态方案。
- 复杂共享状态可使用 derived atom 或 reducer atom。
- API 数据请求优先使用 React Query；Jotai 可保存 UI 局部共享状态。

真实示例：

- `web/apps/labelstudio/src/pages/Home/atoms.ts` 定义 `locationKeyAtom`、`visitedIdsAtom`、`projectsDataAtom`、`sortedProjectsAtom`。
- `web/apps/labelstudio/src/pages/Home/HomePage.tsx` 使用 `useAtom`、`useAtomValue`、`useSetAtom` 和 `useQuery`。

---

## TypeScript 类型

- 对对象形状优先使用 `interface`；联合、交叉、映射类型使用 `type`。
- 避免新增 `any`；未知输入优先使用 `unknown` 并做类型缩小。
- 共享类型放在 `types/` 或靠近使用位置；组件 props 与组件共置。
- 公共函数和跨模块 helper 应写显式返回类型。
- 常量使用 `UPPER_CASE`，变量和函数使用 `camelCase`，类型和组件使用 `PascalCase`。
- React props 命名可使用 `<Component>Props`，例如 `ButtonProps`。

---

## 工具函数与导入

- 通用工具优先从 `@humansignal/core` 或其子路径 wrapper 导入。
- `cloneDeep` 场景使用原生 `structuredClone(obj)`。
- 字符串、节流、防抖、`get`、`uniqBy` 等工具应从 `@humansignal/core` wrapper 导入，不能直接导入 lodash 或 es-toolkit。
- className 合并优先使用项目已有工具，例如 `cn`。

真实示例：

- `web/libs/ui/src/lib/button/button.tsx` 使用 `cn` 和 `@humansignal/core/lib/utils/unwrapRef`。
- `web/apps/labelstudio/src/pages/Home/HomePage.tsx` 从 `@humansignal/core` 导入 `useUpdatePageTitle`。

---

## 禁止/避免模式

- 不要新增 class component。
- 不要把数据请求、状态同步、复杂排序和 JSX 全部堆在一个巨大组件里。
- 不要用 Context API 替代 Jotai 做新的全局状态。
- 不要新增 `import ... from "lodash/..."` 或 `require("lodash")`。
- 不要直接从 `es-toolkit` 或 `es-toolkit/compat` 导入；必须走 `@humansignal/core` wrapper。
