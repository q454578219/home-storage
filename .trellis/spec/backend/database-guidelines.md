# 数据库规范

> 本项目使用 Django ORM，后端必须同时兼容 SQLite 和 PostgreSQL。数据库变更要优先保证迁移安全、查询可维护和大表操作不阻塞发布。

---

## 总览

| 主题 | 当前实践 |
|---|---|
| ORM | Django ORM，模型定义在各 app 的 `models.py` |
| 查询组织 | 简单查询放 API/业务函数；复杂查询放 manager/queryset/helper |
| Migration | Django migrations，存放在 `<app>/migrations/` |
| 数据库兼容性 | 贡献指南明确要求后端兼容 SQLite 和 PostgreSQL |
| 大表 DDL | 使用异步迁移模式，PostgreSQL 使用 `CONCURRENTLY`，SQLite 提供降级路径或跳过 |

真实示例：

- `label_studio/tasks/models.py`：`Task` 使用 `JSONField`、`ForeignKey`、`ManyToManyField`、`Meta.indexes` 定义业务表结构和索引。
- `label_studio/projects/models.py`：`ProjectManager.with_counts_annotate()` 将统计注解集中到 manager 中，避免 API 层重复拼查询。
- `label_studio/core/migration_helpers.py`：封装异步 SQL migration 的调度、状态记录、重试和 SQLite 跳过逻辑。
- `label_studio/tasks/migrations/0054_add_brin_index_updated_at.py`：大表索引异步创建，PostgreSQL 使用 `CREATE INDEX CONCURRENTLY`，SQLite 使用普通索引。

---

## 查询模式

- 用户/组织作用域查询应显式过滤，例如 `Project.objects.filter(organization=request.user.active_organization)` 或使用已有 manager。
- 列表接口应考虑 `select_related()`、`prefetch_related()`、`annotate()`，避免 N+1；例如 `ProjectListAPI.get_queryset()` 返回前预取 `members`、`created_by`。
- 复杂统计和筛选优先封装到 manager 或 helper，参考 `ProjectManager.with_counts_annotate()` 和 `data_manager.managers.apply_ordering()`。
- 大 QuerySet 迭代优先使用 `core.utils.iterators.iterate_queryset()`，不要直接使用 `.iterator()`；注意该工具不保序，顺序敏感场景不要使用。
- 修改任何字段名、索引名、表名、状态值前必须先全局搜索引用，避免遗漏 serializer、API、测试和迁移。

---

## 迁移规范

- 普通字段变更使用 Django 自动 migration，并把 migration 放在对应 app 的 `migrations/` 目录。
- 大表索引、长时间 DDL、数据回填或可能超过部署超时的操作，应使用异步迁移模式。
- PostgreSQL 特有能力必须检查数据库类型，使用 `connection.vendor` 或 `connections[db_alias].vendor` 分支处理。
- PostgreSQL 并发索引必须设置 `atomic = False`，并使用 `CREATE INDEX CONCURRENTLY` / `DROP INDEX CONCURRENTLY`。
- SQLite 不支持 `CONCURRENTLY`、BRIN、GIN 等 PostgreSQL 特性，必须提供普通 SQL 降级路径或显式跳过。
- migration 中需要记录执行状态时，使用 `AsyncMigrationStatus`，参考 `core.migration_helpers.execute_sql_job()`。
- 异步 migration 的任务调度使用 `core.redis.start_job_async_or_sync()`，必要时配置 `rq.Retry`。

---

## 命名约定

- Django model 类使用 PascalCase，例如 `Task`、`Project`、`ProjectImport`。
- 数据库表名可通过 `Meta.db_table` 明确指定；例如 `Task.Meta.db_table = 'task'`。
- 索引名应可读且稳定，例如 `task_updated_at_brin_idx`，避免依赖自动生成名称。
- 反向关系使用明确 `related_name`，例如 `Task.project` 的 `related_name='tasks'`。

---

## 常见陷阱

- 不要在后端代码中只针对 PostgreSQL 实现查询或迁移，而忽略 SQLite 测试环境。
- 不要对大表直接同步创建耗时索引，避免发布流水线被长时间阻塞。
- 不要在 API 层复制复杂 annotate/filter 逻辑；应放到 manager/helper 中复用。
- 不要在循环里逐条查询关联对象；优先使用批量查询、`prefetch_related()` 或专用 helper。
- 不要在顺序敏感逻辑中盲目使用 `iterate_queryset()`，因为它不保证原始排序。
