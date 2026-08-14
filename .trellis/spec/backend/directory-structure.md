# 后端目录结构

> 本文档记录 Label Studio 后端在当前代码库中的实际组织方式。新增后端功能时，优先贴近现有 Django/DRF 模块结构，避免创建孤立的新目录或跨层混放逻辑。

---

## 总览

后端主代码位于 `label_studio/`，整体是 Django 项目 + Django REST Framework API。核心业务按 Django app 切分，例如 `projects`、`tasks`、`data_import`、`data_manager`、`io_storages`、`users`、`organizations`、`ml`、`webhooks`。

每个业务 app 通常包含：

```text
label_studio/<app>/
├── api.py              # DRF API 视图、ViewSet、接口 schema
├── models.py           # Django model、manager、queryset
├── serializers.py      # DRF serializer 与请求/响应校验
├── urls.py             # app 级 URL 路由
├── migrations/         # Django migration
├── tests/              # 单元/接口测试
└── functions/          # 复杂业务函数或后台任务辅助逻辑，按需存在
```

真实示例：

- `label_studio/projects/api.py`：项目相关 DRF API，使用 `generics.*APIView`、`ViewClassPermission`、`extend_schema`。
- `label_studio/tasks/models.py`：任务、标注、预测等核心模型，模型字段、索引、manager 集中在 app 内。
- `label_studio/data_import/functions.py`：导入任务的异步/流式处理逻辑，从 API 层拆出以降低 `api.py` 复杂度。
- `label_studio/tasks/tests/test_api.py`：任务 API 测试，使用 `APITestCase`、factory 和真实响应断言。

---

## 模块组织规则

| 场景 | 放置位置 | 依据 |
|---|---|---|
| 新增/修改 API 端点 | 对应 app 的 `api.py` 和 `urls.py` | 现有 `projects`、`tasks`、`organizations` 均采用 app 内 API 文件 |
| 请求/响应校验 | 对应 app 的 `serializers.py` | DRF serializer 是主要校验入口 |
| 持久化模型或 query manager | 对应 app 的 `models.py` 或已有 `managers.py` | `projects.models.ProjectManager`、`data_manager.managers` 是现有模式 |
| 复杂业务流程 | 对应 app 的 `functions/` 或 `functions.py` | `projects/functions/next_task.py`、`data_import/functions.py` 已使用该拆分 |
| 复用工具 | `label_studio/core/utils/` | 例如 `core.utils.exceptions`、`core.utils.db`、`core.utils.common` |
| 后台/管理命令 | `<app>/management/commands/` | 例如 `tasks/management/commands/calculate_stats.py` |
| 测试 | 对应 app 的 `tests/` 或 `label_studio/tests/` | app 内测试优先，全局跨模块测试放 `label_studio/tests/` |

---

## 命名约定

- Python 文件和目录使用小写蛇形命名，例如 `data_import`、`io_storages`、`next_task.py`。
- Django app 内沿用框架约定文件名：`api.py`、`models.py`、`serializers.py`、`urls.py`、`apps.py`、`admin.py`。
- 测试文件使用 `test_*.py`，测试类常见命名为 `Test<Feature>`，例如 `TestTaskAPI`、`TestProjectCountsListAPI`。
- migration 文件保留 Django 编号前缀，例如 `0054_add_brin_index_updated_at.py`。
- 常量使用大写命名，例如 `DATETIME_FORMAT`、`COUNTER_FIELDS`；DRF schema 局部常量可按现有文件使用模块级变量。

---

## 推荐实践

- 新功能优先落在已有 app 中，除非它代表清晰的新业务边界。
- API 层只做鉴权、参数解析、序列化、调用业务逻辑和返回响应，复杂处理应拆到 `functions.py` 或 `functions/`。
- 跨 app 复用逻辑不要复制粘贴，先搜索 `core/utils`、`projects/functions`、`data_manager/functions` 等已有工具。
- 对公开 API 新增/变更时，同步维护 `drf_spectacular` 的 `extend_schema` 信息，参考 `label_studio/projects/api.py` 和 `label_studio/data_import/api.py`。

---

## 禁止/避免模式

- 避免把模型查询、复杂数据转换、异步任务调度全部堆在 API 方法里。
- 避免为了一个小函数创建新的顶层包；优先放入对应 app 或 `core/utils`。
- 避免跨 app 直接复制 manager/queryset 逻辑；先提取复用函数或复用已有 manager。
- 避免新增无测试目录或无测试文件的功能模块。
