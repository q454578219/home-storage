# 家庭储物 HomeStorage

把家里的柜子"装进手机"：拍一张柜子照片，标注格子位置，每个格子里的物品都可以拍照 + AI 自动识别关键词，之后一句话就能搜到东西放在哪。

一个完全本地、无账号、无云端的个人物品管理工具。

## 功能特性

- **柜子地图**：拍下柜子照片，在照片上打点标记格子位置，支持拖动调整、编辑标题
- **AI 物品识别**：录入物品时自动识别照片中的物品，生成名称和搜索关键词（智谱 GLM-4V-Flash，永久免费）
- **语音输入**：按住说话输入物品名称和备注，无需打字
- **搜索直达**：按物品名 / 关键词 / 柜子名 / 分类搜索，命中后直接跳转到对应格子的位置并高亮
- **批量录入**：一次选择多张照片，逐个 AI 识别自动入库
- **数量管理**：同种物品多件时记录数量，列表角标显示
- **分类统计**：客厅 / 卧室 / 厨房……分类筛选，每类显示物品总数
- **最近添加**：首页横向展示最新录入的物品，一键直达
- **物品移动**：录错格子了？一键移动到同柜其他格子
- **回收站**：删除的柜子先进回收站，可恢复或彻底删除
- **备份恢复**：一键把数据库 + 全部照片打包导出到"下载"目录，换机恢复无忧
- **深色模式**：跟随系统自动切换

## 技术栈

| 模块 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin + Jetpack Compose (Material 3) |
| 数据库 | Room 2.6（含多版本迁移） |
| 图片加载 | Coil |
| 拍摄 / 选择 | CameraX + Photo Picker |
| AI 识别 | 智谱 GLM-4V-Flash（OpenAI 兼容接口，OkHttp） |
| 语音 | 系统 SpeechRecognizer（无界面自绘） |
| 最低版本 | Android 10 (API 29) |

## 环境要求

- JDK 17
- Android Studio（Koala 或更新）
- 本地 Gradle 8.13（或直接使用 Android Studio 同步）

## 构建

方案一：Android Studio 打开项目，等 Gradle 同步完成后直接 Run。

方案二：命令行

```bash
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 配置 AI 识别（可选）

不配置也能正常使用，只是 AI 自动识别物品功能会禁用（可手动输入名称）。

1. 注册 [智谱开放平台](https://open.bigmodel.cn) ，在控制台创建 API Key（GLM-4V-Flash 模型永久免费）
2. 在项目根目录的 `local.properties` 中添加（该文件已被 .gitignore 忽略，不会提交）：

```properties
zhipuApiKey=你的密钥
```

## 权限说明

- **麦克风**：语音输入时使用（识别由系统语音引擎完成）
- **相机 / 相册**：拍摄或选择柜子、物品照片
- 所有数据（数据库、照片）仅存储在应用私有目录，不含任何网络追踪或账号体系

## 目录结构

```
app/src/main/java/com/example/homestorage/
├── MainActivity.kt            # 导航入口
├── data/
│   ├── ai/ImageRecognizer.kt  # 智谱 GLM-4V-Flash 图片识别封装
│   ├── backup/BackupManager.kt# ZIP 备份 / 恢复
│   ├── db/                    # Room 实体 / DAO / 数据库迁移
│   ├── image/ImageStore.kt    # 图片文件存储
│   └── repo/HomeRepository.kt # 数据仓库（业务逻辑）
└── ui/
    ├── home/                  # 首页：柜子网格 / 搜索 / 分类 / 回收站
    ├── cabinet/               # 柜子详情：照片打点 / 手势拖动点位
    ├── spot/                  # 点位详情：物品列表 / AI 识别录入
    ├── create/                # 新建柜子
    └── common/                # 公共组件（语音输入 / 抽屉菜单 / 主按钮）
```

## 许可证

[MIT](./LICENSE)