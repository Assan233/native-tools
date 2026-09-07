# 给 FE 开发者的 Android Native 指南

这份文档只覆盖本项目最常用的概念，目标是让你能读懂、修改和验证代码，而不是一次学完整个 Android 体系。

## 从哪里开始

应用启动后，系统创建 `MainActivity`。`setContent { ... }` 里的 Compose 代码负责渲染 UI：

```kotlin
setContent {
    NativeToolsTheme {
        ToolsScreen()
    }
}
```

可以把它近似理解为把 React 根组件挂载到页面。Compose 会根据状态变化自动重新执行必要的 `@Composable` 函数并更新 UI。

## 生命周期不是浏览器生命周期

Android 应用可能因为旋转屏幕、切换应用、内存不足等原因被系统暂停、重建甚至终止。因此：

- 不要把需要长期保存的数据只放在 `Activity` 字段中。
- 页面状态优先放在 `ViewModel`，长期数据放在数据库或 DataStore。
- 不要假设应用进程一直存在。
- 需要清理的监听或任务应绑定到 Lifecycle 或协程作用域。

## 线程与异步

主线程类似浏览器 UI 线程，但 Android 对阻塞更敏感。网络、文件和数据库操作不能直接在主线程执行。本项目使用 Kotlin Coroutines 表达异步逻辑：

```kotlin
viewModelScope.launch {
    val result = repository.loadData()
    state.update { it.copy(result = result) }
}
```

不要使用 `Thread.sleep` 等待，也不要手动创建线程，除非已有明确原因。

## 状态管理

功能变复杂后采用单向数据流：

```text
UI event -> ViewModel -> state update -> UI render
```

- Composable 只负责显示状态和发送事件。
- ViewModel 保存页面状态并协调业务逻辑。
- Repository 封装数据来源或系统 API。
- 简单页面不需要为了“架构完整”强行增加所有层。

## 权限与系统能力

Android 权限不仅是 Manifest 配置，部分权限还需要运行时向用户请求。无障碍、通知读取、悬浮窗等特殊能力需要用户进入系统设置授权。

任何新增能力都应该回答：

1. 为什么必须使用这项能力？
2. 能否用权限更小的公开 API 完成？
3. 用户在哪里开启、查看状态和关闭？
4. 失败、被系统回收或授权撤销后会怎样？
5. 是否会接触账号、输入内容或其他敏感信息？

## 调试路径

- 编译错误：先看 Android Studio Build 面板中的第一条错误。
- 运行崩溃：在 Logcat 中按包名 `com.zyb.nativetools` 过滤，查找 `FATAL EXCEPTION`。
- UI 预览：打开带 `@Preview` 的 Composable，使用 Split 或 Design 视图。
- 真机行为：自动化和系统权限高度依赖系统版本，模拟器通过后仍需真机验证。
- 依赖问题：先检查 `gradle/libs.versions.toml`，不要随意复制来源不明的旧版本配置。

## 建议学习顺序

1. Kotlin 基础：空安全、数据类、密封接口、扩展函数、协程。
2. Compose：Composable、Modifier、State、重组、副作用。
3. Android 基础：Activity、Manifest、资源、生命周期和权限。
4. ViewModel、StateFlow 和单向数据流。
5. 具体需求涉及的系统 API，例如 Accessibility Service。
