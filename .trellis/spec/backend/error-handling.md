# 错误处理规范

> 后端错误处理以 DRF 异常体系为主：请求/业务校验错误抛 `ValidationError`，权限和资源错误使用 DRF/Django 标准异常，可复用的 API 错误定义为 `LabelStudioAPIException` 子类。

---

## 总览

当前项目常见错误处理方式：

- 请求参数或业务校验失败：`rest_framework.exceptions.ValidationError` 或 `serializers.ValidationError`。
- 权限不足：`PermissionDenied`、`ViewClassPermission`、`all_permissions`。
- 资源不存在：`Http404` 或 DRF `NotFound`。
- 可复用 API 错误：继承 `core.utils.exceptions.LabelStudioAPIException`。
- 数据库唯一性/锁等特殊错误：捕获底层异常后转换成业务 API 异常。
- 外部异常转用户可读文本：使用 `core.utils.exceptions.extract_message()`。

真实示例：

- `label_studio/core/utils/exceptions.py`：定义 `LabelStudioAPIException`、`LabelStudioDatabaseException`、`ProjectExistException` 等公共错误。
- `label_studio/tasks/exceptions.py`：`AnnotationDuplicateError` 继承 `LabelStudioAPIException` 并返回 HTTP 409。
- `label_studio/projects/api.py`：`ProjectListAPI.perform_create()` 捕获 `IntegrityError`，将重复标题转换为 `ProjectExistException`。
- `label_studio/tasks/validation.py`：任务导入校验集中抛 `ValidationError`，由 DRF 转成标准错误响应。

---

## 错误类型

| 场景 | 推荐错误类型 | 示例 |
|---|---|---|
| 请求字段缺失、格式错误、业务校验失败 | `ValidationError` | `tasks.validation.validate_task()` |
| Serializer 字段校验失败 | `serializers.ValidationError` | `users.serializers` 自定义 hotkey 校验 |
| 权限失败 | `PermissionDenied` 或 permission class | `users.views`、`ViewClassPermission` |
| 资源不存在 | `Http404` / `NotFound` | `projects.api`、DRF retrieve API |
| 需要统一 status code 的业务 API 错误 | `LabelStudioAPIException` 子类 | `ProjectExistException`、`AnnotationDuplicateError` |
| 数据库底层错误 | 捕获后转换为业务异常 | `LabelStudioDatabaseException` |

---

## API 错误响应模式

- API 方法优先抛异常交给 DRF 统一处理，不要手写不一致的错误结构。
- 当需要返回固定状态码和消息时，定义 `LabelStudioAPIException` 子类，并设置 `status_code`、`default_detail`。
- 创建/更新接口使用 serializer 的 `is_valid(raise_exception=True)` 进行校验。
- 对批量导入、异步导入这类复杂流程，应区分“请求级错误”和“后台处理错误”；参考 `data_import.api.ImportAPI` 的注释和接口描述。
- 直接返回 `Response({'error': ...}, status=...)` 只适合已有接口已采用该响应契约的局部场景；新增公共 API 优先使用异常体系和 serializer 错误。

---

## 日志与错误传播

- 捕获异常后如果要继续抛出，应保留原始异常上下文，避免吞掉 traceback。
- 后台任务或 migration 失败应使用 `logger.exception()` 并记录状态，例如 `core.migration_helpers.execute_sql_job()` 将状态置为 `STATUS_ERROR`。
- 用户可见错误消息不要暴露 secret、token、凭据、完整本地路径或内部堆栈。
- 需要从未知异常提取简短消息时，使用 `extract_message(exc)`，不要直接把复杂异常对象塞进 API 响应。

---

## 常见陷阱

- 不要用裸 `except` 静默吞错；若必须兼容历史逻辑，至少记录日志并说明原因。
- 不要把数据库底层异常原样返回给客户端。
- 不要在多个 API 中复制同一类错误响应结构；应复用异常类或 serializer 校验。
- 不要把后台异步任务的数据级错误混同为同步 HTTP 请求失败，需保持现有异步导入契约。

## Scenario: NLP LLM Backend Flask HTTP 合约

### 1. Scope / Trigger

- Trigger: `ml_backends/nlp_llm_backend/app.py` 自定义 Flask 服务替代官方 `label_studio_ml.api.init_app` 路由。
- Label Studio 会调用 `GET /health`、`POST /setup`、`POST /predict`，并在创建 ML backend 后自动注册 `POST /webhook`。

### 2. Signatures

- `GET /health` -> `200 {"status":"UP", ...}`
- `POST /setup` -> `200 {"model_version": str, "detected_template_type": str, "prompt_key": str}`
- `POST /predict` -> `200 {"results": list}`
- `POST /webhook` -> `200 {"status":"ok"}`

### 3. Contracts

- `/setup` 请求字段：`project`、`schema`、`extra_params`；`extra_params` 支持 JSON 对象或 JSON 字符串。
- `/setup` 的 `extra_params.model_id` 为必填字段。
- Pydantic 配置校验错误统一转成 `ConfigValidationError`，响应 `400 {"error": "..."}`，错误文本只包含字段和原因。
- Flask `HTTPException` 保留原始状态码，响应 `{"error": error.description}`。
- 未知异常走兜底 `500 {"error": str(error)}`，日志使用 `logger.exception()`。

### 4. Validation & Error Matrix

| Condition | Response |
|---|---|
| `extra_params` 非 JSON 对象 | `400 {"error": "extra_params ..."}` |
| `extra_params.model_id` 缺失 | `400`，错误文本包含 `model_id` |
| 未注册路由 | Flask 原始 `404` |
| 方法错误 | Flask 原始 `405` |
| 未分类异常 | `500` 并记录 traceback |

### 5. Good/Base/Bad Cases

- Good: `/setup` 带合法 `model_id`，返回模板识别信息并缓存配置。
- Base: `/webhook` 收到 Label Studio 事件，返回 `{"status":"ok"}`。
- Bad: `/setup` 缺少 `model_id`，返回业务校验错误。

### 6. Tests Required

- `/setup` 成功路径断言状态码、`detected_template_type` 和脱敏日志。
- `/setup` 缺少 `model_id` 断言 `400` 和错误字段。
- `/webhook` 断言 `200 {"status":"ok"}`。
- 未知路由断言 `404`，防止通用异常处理吞掉 HTTP 状态。

### 7. Wrong vs Correct

#### Wrong

```python
@app.errorhandler(Exception)
def handle_unexpected_error(error):
    logger.exception('未处理异常')
    return jsonify({'error': str(error)}), 500
```

#### Correct

```python
@app.errorhandler(HTTPException)
def handle_http_error(error):
    return jsonify({'error': error.description}), error.code
```
