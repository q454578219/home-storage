# HomeStorage MVP 技术设计

## 1. 架构与分层

单模块 App（:app），MVVM + Repository 模式：

```
UI 层（Compose）
  ├─ 首页 HomeScreen：柜子卡片网格 + 分类筛选 + 顶部搜索
  ├─ 柜子详情 CabinetDetailScreen：照片 + 点位覆盖层 + 点位编辑
  ├─ 点位详情 SpotDetailScreen：物品照片列表 + 文字录入
  ├─ 新建柜子 CreateCabinetScreen：拍照/相册 + 命名（语音/手输）+ 分类
  └─ ViewModel：HomeViewModel / CabinetDetailViewModel / SpotDetailViewModel

数据层
  ├─ Room：CabinetEntity / SpotEntity / ItemEntity / CategoryEntity（含 DAO）
  ├─ Repository：CabinetRepository / SpotRepository / ItemRepository
  └─ ImageStore：图片存取（App 内部 filesDir/imagestore/，按 UUID 命名，返回相对路径）

工具层
  └─ SpeechInput：系统 SpeechRecognizer 语音输入封装（免 Key）
```

单向数据流：UI → ViewModel → Repository → Room。

## 2. 数据模型（Room）

```
CategoryEntity(id, name, isPreset)
CabinetEntity(id, name, coverImagePath, categoryId, createdAt)
SpotEntity(id, cabinetId, x, y, title)        // x,y 为归一化坐标 0~1，适配不同分辨率
ItemEntity(id, spotId, imagePath, name, note, createdAt)
```

- 点位坐标存归一化比例（0~1），渲染时乘图片实际尺寸，图片替换后点位不漂移
- 删除柜子 = 级联删除 Spot/Item + 物理删除图片文件
- ItemEntity.name 为搜索索引主字段

## 3. 核心交互实现

### 3.1 照片上加点位
- 柜子详情页用 `Box` 叠放：`Image`（柜子照片）+ 点位覆盖层
- 点击空白处：`pointerInput` 拿点击坐标 → 换算归一化坐标 → 弹"添加点位"确认
- 点位显示：小圆点 + 物品缩略图角标（有物品的显示图，无物品显示空心点）
- 移动：长按拖动（`detectDragGesturesAfterLongPress`），松手保存新坐标
- 点击点位：弹出底部弹窗（查看详情 / 删除）

### 3.2 图片获取
- 拍照：CameraX 的 `TakePicturePreview`（缩略图级）→ 存 ImageStore
- 相册：系统 `PhotoPicker`（ActivityResultContracts.PickVisualMedia），无需读权限
- 权限：CAMERA（拍照）、RECORD_AUDIO（语音输入），运行时申请

### 3.3 物品文字录入
- 添加物品照片后弹出录入弹窗：名称输入框（必填）+ 备注（可选）
- 输入框旁麦克风按钮 → SpeechRecognizer 识别 → 文字填入输入框，可继续编辑
- 识别失败/无语音服务：提示后手动输入兜底
- 柜子命名同样走此弹窗（名称 + 语音）

### 3.4 搜索
- 单表查询：`WHERE name LIKE %kw% OR cabinet.name LIKE %kw% OR category.name LIKE %kw%`
- 物品命中 → 结果卡片（缩略图+名称+柜子名+分类）→ 点击跳转 CabinetDetailScreen，传 spotId，进入后高亮动画（缩放脉冲 3 次）
- 柜子命中 → 直接跳柜子详情

## 4. 语音输入（SpeechRecognizer）

- 系统 API `android.speech.SpeechRecognizer`，需权限 RECORD_AUDIO
- 中文识别用 Locale.SIMPLIFIED_CHINESE
- 封装为 suspend 函数：startListening() 返回识别文本，错误码映射友好提示
- 部分国产 ROM 语音引擎弱 → 手动输入始终可用，语音仅辅助

## 5. 依赖清单（新增）

- Room（room-runtime / room-ktx / room-compiler，KSP）
- CameraX（camera-core / camera-camera2 / camera-lifecycle）
- Coil（本地图片加载，降采样防 OOM）
- 语音输入：android.speech.SpeechRecognizer（系统 API，无需三方库）
- 无网络层依赖（完全本地应用）

## 6. 风险与取舍

| 项 | 说明 | 对策 |
|----|------|------|
| 语音识别不稳定 | 国产 ROM 语音引擎差异 | 手动输入兜底，语音仅辅助 |
| 大图内存 | 柜子照片可能 10MB+ | Coil 采样降采样 + 点位用 0~1 坐标 |
| 点位坐标漂移 | 换封面图后点位错位 | 归一化坐标，换图前提示 |
| 大量图片占用空间 | 长期使用累积 | 提供"删除物品同时删图"逻辑 |

## 7. 数据流/契约摘要

- 图片存储相对路径：`images/{uuid}.jpg`，ImageStore 负责读写，DB 只存相对路径
- ItemEntity.name 为搜索索引主字段
- 后续迭代接入 AI 识别时：新增 ai 层 + 设置页，识别结果预填 name 即可，无需改数据结构
