# 提交规范

> 本指南约束本仓库内 AI/开发会话产生的提交粒度、提交分类和提交信息语言。

---

## 核心规则

- 按任务分类拆分提交；每个提交只包含同一类任务成果的相关文件。
- 提交信息使用中文；Conventional Commit 的 `type(scope):` 保持英文关键字和 ASCII scope。
- 提交标题格式为 `type(scope): 中文说明`，例如 `feat(assignment): 增加任务分配状态追踪`。
- 提交正文使用中文记录变更内容、验证命令、任务或 PRD 关联信息。
- Trellis 归档和会话记录提交也使用中文，例如 `chore(task): 归档 04-23-migrate-to-0.5.0-beta.11`、`chore(journal): 记录会话`。

---

## 任务分类

| 类别 | 推荐 type/scope | 适用内容 |
|---|---|---|
| 新功能 | `feat(<模块>)` | 产品功能、API、UI、数据流程 |
| 修复 | `fix(<模块>)` | bug fix、回归修复、兼容性修复 |
| 测试 | `test(<模块>)` | 单测、集成测试、E2E、测试 helper |
| 文档 | `docs(<主题>)` | README、贡献文档、用户文档 |
| 规范 | `docs(spec)` | `.trellis/spec/`、AGENTS.md、项目约定 |
| 重构 | `refactor(<模块>)` | 行为保持一致的结构调整 |
| 工程事务 | `chore(<主题>)` | 依赖、脚本、任务归档、会话记录 |
| CI | `ci(<主题>)` | GitHub Actions、pre-commit、构建流水线 |

---

## 拆分判断

- 后端、前端、测试、文档、规范、CI 分别形成独立提交。
- 同一任务下的实现与测试可以同提，前提是它们共同验证同一个行为变更。
- `.trellis/tasks/` 归档提交、`.trellis/workspace/` 会话记录提交保持独立提交。
- 存在不认识的 dirty file 时，先确认归属，再纳入提交计划。

---

## 示例

```bash
git commit -m "feat(assignment): 增加任务分配状态追踪"
git commit -m "test(assignment): 覆盖分配权限与状态流转"
git commit -m "docs(spec): 增加中文提交规范"
git commit -m "chore(task): 归档 04-23-migrate-to-0.5.0-beta.11"
git commit -m "chore(journal): 记录会话"
```
