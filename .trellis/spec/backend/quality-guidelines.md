# 后端质量规范

> 后端变更应保持小而清晰，优先复用现有 Django/DRF 结构，并通过 Ruff 和 pytest 验证。新增功能或修复必须有对应测试或明确说明无法自动化的验证方式。

---

## 总览

来自仓库现有配置和贡献指南的实际要求：

- Python 最低目标版本按 Ruff 配置为 `py310`。
- Ruff 行宽为 119，格式化使用单引号。
- Ruff 默认启用 `F`、`E`、`I001`，忽略 `E501`、`E402`。
- `pyproject.toml` 排除了 `**/migrations/*.py` 的 Ruff 检查。
- 后端测试使用 `pytest`、`pytest-django`、`pytest-cov`、`pytest-mock`、`tavern`、`pytest-xdist`。
- `Makefile` 的后端测试入口是 `cd label_studio && DJANGO_DB=sqlite pytest -v -m "not integration_tests"`。
- GitHub Actions 中 Ruff 通过 pre-commit 执行 `ruff-check --all-files --hook-stage pre-push`。

真实示例：

- `label_studio/tasks/tests/test_api.py`：使用 `APITestCase`、factory、`force_authenticate()` 和完整响应断言。
- `label_studio/projects/tests/test_api.py`：接口计数和模型版本 API 测试。
- `label_studio/data_manager/tests/test_managers.py`：manager/queryset 行为测试，覆盖性能相关字段选择。
- `label_studio/tests/test_prediction_validation.py`：跨模块预测校验测试，使用 `pytest.mark.django_db` 和参数化断言。

---

## 必须遵守

- 变更前先搜索相关字段、常量、状态值和 API 路径的引用。
- 新增 API/模型/业务规则时，优先在对应 app 的 `tests/` 下添加测试。
- bug fix 必须尽量添加回归测试，证明问题已修复。
- 当 Community/OSS 与 Enterprise 共用目录或模板清单时，后端返回社区侧可见数据前必须过滤掉当前仓库不支持的企业壳能力，并同步覆盖分组/聚合结果，避免前端出现空白分组或不可用入口。
- API 变更要同步 serializer、schema、权限和测试，避免只改视图。
- 数据库相关改动必须确认 SQLite 与 PostgreSQL 兼容性。
- 新代码应有类型提示，尤其是公共函数、复杂 helper 和跨模块接口。
- PR/改动应尽量小，贡献指南建议粗略控制在约 400 行变更内；大功能拆分提交。

---

## 禁止/避免模式

- 不要在 API 方法中堆放大量查询、循环和状态转换逻辑；复杂逻辑应下沉到 manager/helper/functions。
- 不要新增只有 PostgreSQL 可用的 SQL 或索引而不提供 SQLite 处理。
- 不要在测试中只断言状态码，关键响应字段和副作用也要断言。
- 不要复制已有工具函数；新增 helper 前先搜索 `core/utils` 和对应 app 的 `functions`。
- 不要在无 feature flag 的情况下直接改动高风险后端基础设施或性能敏感路径；贡献指南说明大变更可能需要 feature flag。
- 不要提交 Ruff 或测试失败的代码。

---

## 测试要求

| 变更类型 | 推荐测试 |
|---|---|
| DRF API | `APITestCase` 或 pytest + DRF client，覆盖权限、状态码、响应体 |
| Serializer/校验 | 直接构造 serializer 或调用校验函数，覆盖 Good/Base/Bad case |
| Model/manager/queryset | Django `TestCase` 或 `pytest.mark.django_db`，断言查询结果和副作用 |
| Migration/数据库 helper | SQLite 与 PostgreSQL 差异要有显式处理；复杂迁移需单独测试 helper |
| 后台任务/异步流程 | mock 调度入口，断言状态更新和失败路径 |
| API endpoint 契约 | 优先使用 tavern 或现有 API 测试模式覆盖公开接口 |

### LLM 输出解析约定

`ml_backends/nlp_llm_backend` 的模板适配器可以让模型优先输出简单标注内容，程序侧继续优先解析 JSON 以兼容旧返回。分类和 taxonomy 可以增加纯文本兜底，前提是输出能由 label config 安全过滤回合法标签或路径，例如文本分类的 `标签1,标签2`、`输出：标签1`，以及 taxonomy 的 `一级,二级`、`路径：一级 > 二级`。

分类和 taxonomy 的纯文本兜底契约：

- Good：拆分后的候选项逐字命中合法标签或路径节点，生成 prediction。
- Base：拆分失败但输出中只出现一个合法标签或一条完整合法路径，生成 prediction。
- Bad：输出中没有合法候选，或出现多个无法消歧的合法候选，返回空 prediction。

需要 span、实体 ID 或关系端点的任务继续要求结构化 JSON。新增解析兜底时必须覆盖 JSON 兼容、纯文本成功、非法候选过滤这三类测试。

常用验证命令：

```bash
cd label_studio && DJANGO_DB=sqlite pytest -v -m "not integration_tests"
pre-commit run ruff-check --all-files --hook-stage pre-push
```

---

## Code Review Checklist

- 代码是否放在正确 app 和正确层级，而不是跨层混放？
- 是否复用了已有 manager/helper/utility？
- 是否保持 SQLite 与 PostgreSQL 兼容？
- API 权限、serializer、schema、URL、测试是否同步更新？
- 错误响应是否遵循 DRF 异常体系或现有接口契约？
- 日志是否包含排障上下文且不暴露敏感信息？
- 是否覆盖正常路径、边界路径和失败路径？
- 是否避免了 N+1 查询、同步大表 DDL、无界循环和大 payload 日志？
