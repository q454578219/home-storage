# HomeStorage 项目指引

## 开发流程（Trellis：需求 → 实现 → 测试）

本项目使用 Trellis 流程管理开发，完整说明见 `.trellis/workflow.md`，核心路径：

1. **需求梳理**：`trellis-brainstorm` 技能，把需求聊清楚（PRD）
2. **规格说明**：`.trellis/spec/` 中按包/层写编码规范
3. **任务创建**：`python3 ./.trellis/scripts/task.py create "<标题>"`，产出 `task.json` + `prd.md`
4. **开发前检查**：`trellis-before-dev` 技能，读规格、确认任务上下文
5. **实现**：增量开发，一次一个任务（trellis-implement）
6. **检查/测试**：`trellis-check` 技能，对照质量检查清单验证
7. **收尾**：`trellis-finish-work` 技能，记录学习、归档任务

技能入口（按平台）：
- `.agents/skills/`、`.claude/skills/`、`.cursor/skills/` 下的 `trellis-*` 技能

首次使用初始化开发者身份：
```bash
python3 ./.trellis/scripts/init_developer.py <你的名字>
```

## 项目定位与托管决策

- **托管**：GitHub 官方（用户在 GitHub 上创建仓库后推送，未创建前不要自行建仓库）
- **开源**：MIT 协议，开源 + 捐赠（GitHub Sponsors）/ 未来付费功能（云同步等）方向
- **API Key**：智谱 GLM-4V-Flash 由用户自行配置在 `local.properties`（已 gitignore）

## 构建与运行

- 构建：`gradle assembleDebug`（无 gradlew 时用本地 Gradle 8.13，JDK 用 `D:\dev-envirment\jdk17`）
- 运行/调试：Android Studio 连真机 Run / Debug

## 构建环境坑（重要）

- **必须设置 `GRADLE_USER_HOME=D:\gradle-home`**：本机用户名是中文"王者"，Gradle 默认用户目录 `C:\Users\王者\.gradle` 下的 worker classpath 文件（UTF-8 编码）被 Java 按 GBK 读取，中文路径乱码 → `testDebugUnitTest` 报 `ClassNotFoundException: GradleWorkerMain`。已把 `~/.gradle` 整体复制到 `D:\gradle-home`（无中文路径）解决。
- 完整命令示例：`$env:JAVA_HOME="D:\dev-envirment\jdk17"; $env:GRADLE_USER_HOME="D:\gradle-home"; & "D:\gradle-home\wrapper\dists\gradle-8.13-bin\5xuhj0ry160q40clulazy9h7d\gradle-8.13\bin\gradle.bat" <任务>`
