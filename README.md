# Native Tools

一个用于解决日常 Android 手机使用问题的原生工具集。项目会从小而明确的需求开始，例如通过自动化步骤减少重复点击。

## 技术选型

- Kotlin：Android 官方推荐语言，语法相对简洁。
- Jetpack Compose：声明式 UI，与 React/Vue 的组件化思路较接近。
- Material 3：Android 官方设计组件库。
- Gradle Version Catalog：在 `gradle/libs.versions.toml` 集中管理依赖版本。
- 最低 Android 8.0（API 26），当前目标 Android 14（API 34）。

当前保持单 `app` 模块。只有当代码规模和边界确实需要时才拆分模块，避免初期过度设计。

## 本地运行

推荐安装最新版稳定版 Android Studio，并在 SDK Manager 中安装 Android SDK 34。

1. 用 Android Studio 打开仓库根目录。
2. 等待 Gradle Sync 完成。
3. 启动 API 26 或更高版本的模拟器，或连接开启 USB 调试的 Android 手机。
4. 选择 `app` 配置并点击 Run。

也可以使用命令行：

```bash
./gradlew test
./gradlew assembleDebug
./gradlew installDebug
```

## FE 概念对照

| Android / Compose | 类似的 FE 概念 |
| --- | --- |
| `Activity` | 页面入口和宿主容器 |
| `@Composable` 函数 | React 函数组件 / Vue 组件 |
| `State` / `StateFlow` | 响应式状态 / Store |
| `ViewModel` | 页面级状态与业务逻辑层 |
| `Modifier` | 一部分样式、布局和交互属性 |
| `AndroidManifest.xml` | 应用入口、能力与权限声明清单 |
| Gradle | package manager + build tool |

更完整的开发说明见 [`docs/NATIVE_FOR_FE.md`](docs/NATIVE_FOR_FE.md)。

## 项目结构

```text
app/src/main/
├── AndroidManifest.xml              # 应用组件与权限声明
├── java/com/zyb/nativetools/        # Kotlin 源码
│   ├── MainActivity.kt              # 当前应用入口和工具列表
│   └── ui/theme/                    # Compose 主题
└── res/                             # 字符串、主题等 Android 资源
```

新增功能时，优先按功能建立包，例如 `features/autoclick/`，而不是一开始建立大量抽象层。

## 自动化能力说明

自动点击通常需要 Android Accessibility Service（无障碍服务）。这是用户必须在系统设置中手动开启的敏感能力，能够读取界面内容并执行操作。实现时必须遵循以下原则：

- 仅在用户明确操作后运行，状态始终可见且可立即停止。
- 权限按需申请，并在界面中解释用途和风险。
- 不收集或上传界面内容、输入内容等敏感数据。
- 不绕过系统安全限制，不用于支付、账号验证等高风险流程。
- 优先使用公开 Android API，不依赖固定坐标或机型特有行为。

### 美柚瓶喂母乳

首页的“美柚瓶喂母乳”工具会在用户主动点击后：

1. 启动包名为 `com.lingan.seeyou` 的美柚 App。
2. 等待首页广告结束并出现可操作内容，不点击广告或使用固定延时盲点。
3. 严格按“喂养记录”→“瓶喂母乳”的路径进入表单，每次点击后等待页面加载。
4. 找到“输入奶量”表单控件并输入 `150`。
5. 停在美柚确认页面，由用户检查并手动保存。

首次使用必须在系统无障碍设置中手动开启“美柚瓶喂记录自动化”。服务仅监听美柚，不使用屏幕坐标；广告或页面在 60 秒内始终未出现目标节点时会停止。由于美柚页面和输入框的无障碍结构可能随版本更新，首次使用需要在安装美柚的真机上验证节点文案和输入结果。

### 钉钉自动打卡

首页的“钉钉自动打卡”工具会在用户主动点击后：

1. 启动包名为 `com.alibaba.android.rimet` 的钉钉 App。
2. 等待钉钉页面加载，严格依次点击“工作台”和“考勤打卡”。
3. 仅当页面同时存在考勤上下文时，点击精确文案为“打卡”的按钮。
4. 点击后立即结束自动化，由用户在钉钉中核对打卡结果。

首次使用必须在系统无障碍设置中单独开启“钉钉自动打卡”。服务仅监听钉钉，不读取或操作其他 App，不使用固定屏幕坐标；60 秒内未识别到预期页面会安全停止。钉钉工作台可能因企业配置、版本或管理员设置而不同，首次使用必须在真机上核对入口文案和最终打卡结果。

## 开发约定

- 遵循 Kotlin 官方代码风格和 Android 官方架构建议。
- UI 使用单向数据流：状态向下传递，事件向上传递。
- I/O 或耗时任务不得阻塞主线程，使用 Kotlin Coroutines。
- 用户可见文案放在资源文件中；当前首页内的临时原型文案后续功能化时迁移。
- 新业务逻辑需要单元测试；关键用户流程需要 UI 或设备测试。
- 不提交 `local.properties`、签名文件、密钥或其他本机配置。

## 常用检查

```bash
./gradlew test            # JVM 单元测试
./gradlew lint            # Android 静态检查
./gradlew assembleDebug   # 构建可安装的 Debug APK
```
