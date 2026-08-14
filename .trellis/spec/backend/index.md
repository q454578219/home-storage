# 后端开发规范

> 本目录记录 Label Studio 后端 `label_studio/` 的真实开发约定。新增或修改 Django / DRF 后端功能时，先阅读本索引，再按变更类型进入对应细分规范。

---

## 规范索引

| 指南 | 说明 | 状态 |
|---|---|---|
| [目录结构](./directory-structure.md) | Django app 分层、API/serializer/model/functions/tests 的放置规则 | Filled |
| [数据库规范](./database-guidelines.md) | Django ORM、迁移、大表 DDL、SQLite/PostgreSQL 兼容性 | Filled |
| [错误处理](./error-handling.md) | DRF 异常体系、业务异常封装、错误响应约定 | Filled |
| [日志规范](./logging-guidelines.md) | `logging` 使用方式、日志级别、排障字段与敏感信息边界 | Filled |
| [质量规范](./quality-guidelines.md) | Ruff、pytest、review checklist、禁止模式与验证命令 | Filled |

---

## 开发前检查清单

- [ ] 明确改动属于 API、serializer、model/manager、business function、migration、management command、test 中哪一层。
- [ ] 全局搜索相关字段、常量、状态值、URL 路径和权限类，确认没有遗漏引用点。
- [ ] 若涉及数据库、索引或 migration，先阅读 [数据库规范](./database-guidelines.md)，确认 SQLite 与 PostgreSQL 都有处理路径。
- [ ] 若涉及 API 响应、校验或业务异常，先阅读 [错误处理](./error-handling.md)。
- [ ] 若涉及后台任务、导入流程或异常追踪，先阅读 [日志规范](./logging-guidelines.md)。
- [ ] 修改完成后按 [质量规范](./quality-guidelines.md) 执行最小必要验证。

---

## 事实来源

- `pyproject.toml`
- `Makefile`
- `label_studio/core/utils/exceptions.py`
- `label_studio/core/migration_helpers.py`
- `label_studio/projects/api.py`
- `label_studio/projects/models.py`
- `label_studio/tasks/models.py`
- `label_studio/tasks/validation.py`
- `label_studio/data_import/api.py`
- `label_studio/data_import/functions.py`
- `label_studio/data_manager/tests/test_managers.py`

---

**语言**：本工作区文档使用中文。
