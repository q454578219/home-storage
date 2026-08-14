# Thinking Guides

> **Purpose**: Expand your thinking to catch things you might not have considered.

---

## Why Thinking Guides?

**Most bugs and tech debt come from "didn't think of that"**, not from lack of skill:

- Didn't think about what happens at layer boundaries → cross-layer bugs
- Didn't think about code patterns repeating → duplicated code everywhere
- Didn't think about edge cases → runtime errors
- Didn't think about future maintainers → unreadable code

These guides help you **ask the right questions before coding**.

---

## Available Guides

| Guide | Purpose | When to Use |
|-------|---------|-------------|
| [Code Reuse Thinking Guide](./code-reuse-thinking-guide.md) | Identify patterns and reduce duplication | When you notice repeated patterns |
| [Cross-Layer Thinking Guide](./cross-layer-thinking-guide.md) | Think through data flow across layers | Features spanning multiple layers |
| [静态资源外置指南](./static-asset-offload.md) | 避免大体积媒体资源重新进入 Git | 新增或迁移视频、图片、音频、PDF 等静态资源 |
| [提交规范](./commit-guidelines.md) | 约束提交拆分、提交分类和中文提交信息 | 提交代码、归档任务、记录会话前 |

---

## Quick Reference: Thinking Triggers

### When to Think About Cross-Layer Issues

- [ ] Feature touches 3+ layers (API, Service, Component, Database)
- [ ] Data format changes between layers
- [ ] Multiple consumers need the same data
- [ ] You're not sure where to put some logic

→ Read [Cross-Layer Thinking Guide](./cross-layer-thinking-guide.md)

### When to Think About Code Reuse

- [ ] You're writing similar code to something that exists
- [ ] You see the same pattern repeated 3+ times
- [ ] You're adding a new field to multiple places
- [ ] **You're modifying any constant or config**
- [ ] **You're creating a new utility/helper function** ← Search first!

→ Read [Code Reuse Thinking Guide](./code-reuse-thinking-guide.md)

### 提交前检查

- [ ] 本轮变更已按任务分类拆分提交计划。
- [ ] 每个提交只包含同一类任务成果。
- [ ] 提交标题和正文使用中文，标题格式为 `type(scope): 中文说明`。
- [ ] Trellis 任务归档和会话记录提交使用中文。

→ 阅读 [提交规范](./commit-guidelines.md)

### When Adding Static Media Assets

- [ ] 文件是否为视频、图片、音频、PDF 等二进制静态资源。
- [ ] 文件是否接近或超过 `1MB`。
- [ ] 能否放入公司 Nextcloud 并通过 README 索引记录相对路径。
- [ ] 是否需要在 `.gitignore` 中加入精确路径，避免本地副本重新进入 Git。

→ 阅读 [静态资源外置指南](./static-asset-offload.md)

---

## Pre-Modification Rule (CRITICAL)

> **Before changing ANY value, ALWAYS search first!**

```bash
# Search for the value you're about to change
grep -r "value_to_change" .
```

This single habit prevents most "forgot to update X" bugs.

---

## How to Use This Directory

1. **Before coding**: Skim the relevant thinking guide
2. **During coding**: If something feels repetitive or complex, check the guides
3. **After bugs**: Add new insights to the relevant guide (learn from mistakes)

---

## Contributing

Found a new "didn't think of that" moment? Add it to the relevant guide.

---

**Core Principle**: 30 minutes of thinking saves 3 hours of debugging.
