# 户型图模式：技术设计

## 1. 数据模型（Room 迁移）

```
FloorPlanEntity(id, name, imagePath, createdAt)          // 新表
CabinetEntity + (floorPlanId: String?, x: Float, y: Float) // 加 3 列
```

- `floorPlanId`：关联户型图，可空（未挂载 = null）
- `x`/`y`：柜子标记在户型图上的归一化坐标 0~1（默认 0.5/0.5）
- 迁移：bump version 至当前+1，`ALTER TABLE cabinet ADD COLUMN floor_plan_id TEXT / x REAL / y REAL`（SQLite ALTER TABLE 一次只能加一列）

## 2. 页面与导航

```
首页 HomeScreen
 └─ 顶部图标按钮 "户型图" ─→ FloorPlanListScreen（列表）
                              ├─ 新增户型图（ImagePicker 拍照/相册）→ 命名 → 列表刷新
                              └─ 点击进入 FloorPlanDetailScreen
                                   ├─ 点击空白 → 底部弹窗（新建柜子并挂载 / 挂载已有柜子）
                                   ├─ 长按移动柜子标记（拖动手势）
                                   └─ 点击标记 → 跳转 CabinetDetailScreen
```

- 导航方式：沿用现有 Navigation Compose（检查 MainActivity 现有路由结构，加两条 route）
- ViewModel：FloorPlanListViewModel + FloorPlanDetailViewModel

## 3. 核心交互实现

### 3.1 户型图详情页布局
- 复用 CabinetDetailScreen 的叠加层思路：`Box` = AsyncImage（户型图）+ 柜子标记层 + 手势覆盖层
- 点击空白：拿点击坐标 → 归一化 → 弹 ModalBottomSheet（新建柜子 / 挂载已有柜子）
  - 新建柜子：跳转 CreateCabinetScreen（复用现有流程），保存成功后携带 floorPlanId+坐标写回
  - 挂载已有柜子：弹列表（未挂载的柜子），选中后 `updateCabinetFloorPlan(cabinetId, planId, x, y)`
- 柜子标记：复用 SpotMarker 样式思路（圆形缩略图，36dp），无封面时显示名称首字
- 长按拖动：`detectDragGesturesAfterLongPress` 实时更新归一化坐标（同 CabinetDetailScreen 的 moveSpot 思路）

### 3.2 柜子详情与户型图联动
- CabinetDetailScreen 顶部可显示所属户型图信息（可选，第一版可加一个简单 Chip，低优先级）
- 首页柜子卡片不受影响

## 4. Repository / DAO 新增

```
FloorPlanDao: insert/delete/rename/observeAll/observeById
CabinetDao 增加: observeByFloorPlan(planId), updateFloorPlan(cabinetId, planId, x, y), clearFloorPlan(cabinetId)
HomeRepository: 对应转发方法
```

## 5. 数据流/契约

- 图片路径：复用 ImageStore（`images/{uuid}.jpg`），DB 存相对路径
- 归一化坐标契约：0~1 浮点，渲染时乘图片实际尺寸（与 spot 一致）

## 6. 风险与取舍

| 项 | 说明 | 对策 |
|----|------|------|
| DB 迁移失败 | 老用户升级崩溃 | Migration 显式声明，测试覆盖 1→新版本迁移 |
| 户型图换图坐标漂移 | 图片尺寸变化 | 归一化坐标（已定） |
| 手势与点击冲突 | 点击弹窗与长按拖动 | 复用 CabinetDetailScreen 已验证的双 pointerInput 方案 |
| 挂载柜子选择列表长 | 柜子很多时难找 | 第一版简单列表（名称+封面），后续加搜索 |
