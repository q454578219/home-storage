# 日志规范

> 本项目后端使用 Python 标准 `logging`。日志应服务于排障和审计，避免噪声、敏感信息泄露和重复记录。

---

## 总览

当前通用模式是在模块顶部定义：

```python
import logging

logger = logging.getLogger(__name__)
```

真实示例：

- `label_studio/projects/api.py`、`label_studio/tasks/models.py`、`label_studio/data_import/api.py` 都使用 `logging.getLogger(__name__)`。
- `label_studio/core/migration_helpers.py` 使用 `logger.info()` 记录 migration 跳过/已完成，使用 `logger.exception()` 记录失败。
- `label_studio/data_import/functions.py` 使用 `logger.info()` 记录异步/流式导入批次进度。
- `label_studio/tasks/validation.py` 使用 `logger.warning()` 记录导入数据结构异常。

---

## 日志级别

| 级别 | 使用场景 | 示例 |
|---|---|---|
| `debug` | 开发/诊断细节，默认不影响生产可读性 | migration 开始/完成的细节日志 |
| `info` | 重要业务流程节点、后台任务状态、配置生效 | 导入任务批次完成、migration 已执行或跳过 |
| `warning` | 可恢复异常、降级路径、输入数据异常但流程可继续 | CSV 分隔符检测失败后降级为逗号 |
| `exception` | 捕获异常且需要 traceback | migration 执行失败、外部依赖初始化失败 |
| `error` | 不需要 traceback 但代表失败的事件 | 仅在没有异常上下文时使用 |

---

## 推荐实践

- 模块级 logger 使用 `logging.getLogger(__name__)`，不要直接使用 root logger。
- 捕获异常并需要堆栈时用 `logger.exception()`；已有异常对象但只需说明可恢复降级时用 `logger.warning(..., exc_info=True)`。
- 日志消息要包含排障关键 ID，例如 `project.id`、`import.id`、`migration_name`、batch number。
- 长流程应记录开始、关键分支和完成状态，但不要对每条记录逐条打 info 日志。
- 对第三方库噪声可按现有模式调低级别，例如 Azure SDK HTTP logging 被设置为 `WARNING`。

---

## 不应记录的内容

- 不记录 API token、云存储密钥、OAuth 凭据、JWT、cookie、密码。
- 不记录完整用户上传数据、任务全文、标注结果大 payload，除非经过脱敏且确有排障必要。
- 不记录完整本地文件路径或云存储私有 URL 中的敏感部分。
- 不把用户可控字符串拼进高频日志造成日志注入或噪声。

---

## 常见陷阱

- 不要在循环中为每个 task/annotation 打 `info`，批量流程应按批次汇总。
- 不要使用 `print()` 做后端运行日志。
- 不要捕获异常后只记录日志但不更新任务状态；后台任务应像 `AsyncMigrationStatus` 一样留下可追踪状态。
- 不要在日志中暴露异常的完整外部服务响应体，先确认其中没有敏感信息。

## ML Backend 调试日志例外

`ml_backends/nlp_llm_backend` 的一次 task 预标注对应一次外部模型调用。该服务可以在 `info` 日志记录每条预标注的 `project_id`、`task_id`、模板类型、耗时、结果数量和分数，用于定位慢请求。任务原文、prompt、模型请求 messages、模型响应正文、解析结果和 prediction payload 只通过 `log_predict_detail()` 输出，并继续受 `PREDICT_DEBUG_LOGGING`、`PREDICT_DEBUG_MAX_CHARS`、脱敏规则控制。
