# HomeStorage MVP 实现计划

## 实现顺序（每个步骤后构建验证）

### 阶段 1：工程基础（构建通过）
1. [x] 添加依赖：Room(KSP)、CameraX、Coil 到 `app/build.gradle.kts`
2. [x] 创建包结构：`data/`、`data/db/`、`data/repo/`、`data/image/`、`ui/`、`ui/theme/`
3. [x] 主题定制：品牌色板 + 浅灰背景 + 圆角卡片（`ui/theme/Color.kt`、`Theme.kt`）
4. [x] 验证：`gradle assembleDebug` 成功

### 阶段 2：数据层（Room + 图片存储）
5. [x] Entity + DAO + Database（Category/Cabinet/Spot/Item）
6. [x] 预置分类数据（客厅/卧室/书房/厨房/卫生间/阳台/其他）
7. [x] ImageStore（存/读/删，filesDir/imagestore，UUID 命名，压缩保存）
8. [x] Repository 三件套
9. [x] 验证：构建通过

### 阶段 3：柜子管理（UI）
10. [x] 首页 HomeScreen：柜子卡片网格 + 分类筛选（Chips）+ 悬浮新建按钮
11. [x] 新建柜子 CreateCabinetScreen：拍照/相册 → 命名（语音/手输）→ 分类选择
12. [x] 柜子长按菜单：重命名/改分类/换封面/删除（确认弹窗）
13. [ ] 验证：真机安装，新建柜子全流程走通（待设备）

### 阶段 4：点位与物品（核心交互）
14. [x] CabinetDetailScreen：照片 + 点位覆盖层（点击添加/长按移动/点击弹窗详情）
15. [x] SpotDetailScreen：物品照片列表 + 添加照片（拍照/相册）
16. [x] 点位显示物品缩略图角标
17. [ ] 验证：真机测试加点/移动/删点/挂物品（待设备）

### 阶段 5：语音输入
18. [x] SpeechInput 封装（SpeechRecognizer，中文，suspend 函数）
19. [x] 录入弹窗集成麦克风按钮（柜子命名 + 物品名称）
20. [x] RECORD_AUDIO 权限申请
21. [ ] 验证：真机语音录入柜子名/物品名（待设备）

### 阶段 6：搜索
22. [x] 首页顶部搜索框：物品名称 + 柜子名 + 分类名 LIKE 查询
23. [x] 结果列表：物品命中卡片 / 柜子命中卡片
24. [x] 点击跳转 + 点位高亮动画（脉冲 3 次）
25. [ ] 验证：录入多物品后搜索命中并跳转正确（待设备）

### 阶段 7：收尾
26. [ ] RECORD_AUDIO 运行时权限收尾（CAMERA 不需要：走系统相机）
27. [ ] 全流程真机回归测试（新建→加点→录物品→搜索→管理操作）
28. [ ] trellis-check 质量检查清单走查
29. [ ] trellis-finish-work 收尾

## 验证命令

- 构建：`gradle assembleDebug`（JDK: `D:\dev-envirment\jdk17`，无 gradlew 用本地 Gradle 8.13）
- 安装：`adb install -r app\build\outputs\apk\debug\app-debug.apk`
- 日志：`adb logcat -s HomeStorage`

## 风险文件/回滚点

- `app/build.gradle.kts`：依赖版本冲突风险（Room/KSP 与 AGP 8.12 / Kotlin 2.0.21 兼容性）
- `CabinetDetailScreen.kt`：点位手势逻辑最复杂，先做点击加点，再上长按移动
- 语音识别：不同 ROM 差异大，识别失败一律手动输入兜底
- 每个阶段结束可独立回滚，阶段间无强耦合

## 开始前检查

- [ ] 真机已连接（调试用）
- [ ] 读 `.trellis/spec/` 规格（Android 规格待 bootstrap，先按 AGENTS.md 规范执行）
