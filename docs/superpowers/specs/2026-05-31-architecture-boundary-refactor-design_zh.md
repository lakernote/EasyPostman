# 架构边界重构设计规格

## 目标

重构 EasyPostman 的架构边界，使包名、模块归属和插件 SPI 契约能准确表达真实职责。本次重构优先保证依赖方向和可读性，不以新增功能为目标。

## 当前问题

多个非 UI 的执行类和模型类放在 `com.laker.postman.panel.performance` 下，导致 app CLI、worker、runtime 代码 import 了名字上属于 Swing UI 的包。

部分 model 和 service 类依赖 UI 类型：

- `FunctionalPersistenceService` 依赖 `CsvDataPanel.CsvState`。
- `SidebarTab` 放在 `model` 包里，但通过 `UiSingletonFactory` 创建图标和面板。
- `RequestItemProtocolEnum` 和 `MessageType` 把领域枚举值、Swing 图标、国际化显示文案混在一起。
- `HttpResponse` 依赖 HTTP service 包里的 `EasyHttpHeaders`。

插件 SPI 暴露了具体 UI/编辑器实现类型。`ToolboxContribution` 暴露 `JPanel` 对桌面 UI 插件还可以接受，但 `ScriptCompletionContributor` 直接暴露 RSyntaxAutoComplete 的 `DefaultCompletionProvider`，让稳定插件 API 绑定到某个编辑器库。

仓库存在较多跨模块 split package，尤其是 `com.laker.postman.model`、`com.laker.postman.util`、`com.laker.postman.common.component`、`com.laker.postman.common.constants`。非 JPMS 下可以运行，但会增加 owner 判断和 import review 成本。

`easy-postman-plugins/plugin-manager` 被宿主 app 直接作为插件管理库依赖，但目录位置让它看起来像普通运行时插件。

## 决策

采用破坏性重构。此次迁移不保留插件 SPI 兼容性。官方插件将在同一个变更批次内同步迁移，并提升 `plugin.platform.version`。

第一阶段不新增宽泛的 `easy-postman-api-core` 模块。第一阶段先在现有模块结构内修正包归属和依赖方向。后续当请求执行契约稳定后，再考虑抽出独立 API core 模块。

## 目标包归属

`com.laker.postman.panel.*` 只放 UI 代码。它可以包含 Swing 面板、UI state、presenter、view factory 和 UI adapter。它不应包含可复用运行时引擎、headless adapter、持久化 DTO 或领域模型。

`com.laker.postman.performance.*` 负责 app 侧压测执行。包括 GUI/headless 共享执行适配、传输适配、run/session 类、app 侧结果模型、非 Swing view 的 report mapper，以及 worker/master app 集成。

`com.laker.postman.functional.*` 负责 functional runner 配置、持久化 DTO、执行模型和非 UI functional runner 逻辑。

`com.laker.postman.navigation.*` 负责宿主导航元数据，例如 sidebar tab、panel factory、title key、icon path。领域 model 包不能知道 sidebar 面板。

`com.laker.postman.service.http.*` 暂时可以继续保留具体 HTTP/OkHttp 实现代码，但不能直接弹 Swing 对话框，也不能暴露 UI component 类型。UI 反馈应通过结构化结果返回，或委托给 UI 层 callback。

`easy-postman-performance-core` 仍然是 headless 压测领域核心。它负责 plan 数据、可执行 run plan JSON 模型、runtime 契约、线程组调度、worker 协议 DTO、统计、趋势和 report snapshot。它不能依赖 app、Swing、OkHttp、workspace service 或 app settings。

## 压测重构

把当前 `com.laker.postman.panel.performance.execution`、`runtime`、`model`、`plan`、`report` 下的非 UI 类迁移到 app 侧非 UI 包：

- `com.laker.postman.performance.execution`
- `com.laker.postman.performance.runtime`
- `com.laker.postman.performance.model`
- `com.laker.postman.performance.plan`
- `com.laker.postman.performance.report`

Swing 面板、属性面板、表格面板、图表面板、树视图、UI controller 和 view factory 继续保留在 `com.laker.postman.panel.performance`。

app CLI/runtime 包不能再 import `com.laker.postman.panel.performance.*`，除非它们确实是在打开 Swing UI。

如果现有 `easy-postman-performance-core` 类型已经表达了稳定 core 概念，就优先复用。不要强行把 app 侧类下沉到 core，尤其是仍依赖 `HttpRequestItem`、`PreparedRequest`、`HttpResponse`、`SettingManager`、脚本服务、OkHttp 或 workspace/app 执行状态的类。

## UI 依赖清理

引入非 UI 的 functional CSV state DTO，例如 `com.laker.postman.functional.model.FunctionalCsvDataState`。`CsvDataPanel` 负责和这个 DTO 相互转换，`FunctionalPersistenceService` 只持久化 DTO。

把 `SidebarTab` 从 `com.laker.postman.model` 移到 navigation/UI 包。它可以在那里保留 panel supplier、icon、title key，因为这些是宿主导航职责，不是领域 model 职责。

拆分协议和流消息的领域数据与 UI metadata：

- `RequestItemProtocolEnum` 只保留协议值和协议判断方法。
- `RequestProtocolUiMetadata` 这类 UI mapper 提供图标和显示文案。
- `MessageType` 只保留消息类型值。
- `StreamMessageUiMetadata` 这类 UI mapper 提供图标和国际化标签。

把 model 需要使用的 HTTP header 常量下沉到 foundation，或把 header normalize 逻辑移出 `HttpResponse`，放到 HTTP response mapper。`HttpResponse` 不能依赖 `com.laker.postman.service.http`。

## 插件 SPI 破坏性变更

用不绑定具体编辑器库的 SPI 替换 `ScriptCompletionContributor`：

```java
@FunctionalInterface
public interface ScriptCompletionContributor {
    void contribute(ScriptCompletionSink sink);
}
```

`easy-postman-plugin-api` 定义小型 completion DTO 和 sink 接口。app 编辑器层再把这些 DTO 适配为 RSyntaxAutoComplete 类型。

新 SPI 需要覆盖 Redis 和 Kafka 官方插件当前的补全场景：

- 类似 `pm.redis` 的变量式补全
- function 或 method 补全
- 可选简短描述
- 可选 replacement text
- 可选 summary text

`PluginContributionSupport` 改为构建 DTO-backed completion，不再直接修改 `DefaultCompletionProvider`。

因为第三方插件如果使用 completion contributor 必须重新编译和迁移，所以本次需要提升 `plugin.platform.version`。

`ToolboxContribution` 本阶段可以继续暴露 `JPanel`，因为 toolbox contribution 明确是桌面 Swing UI 扩展点。未来如果需要 plugin-web 或 headless plugin 模型，应新增不同扩展点。

## Plugin Manager 归属

把 `easy-postman-plugins/plugin-manager` 文档化为宿主插件管理库特例。它虽然位于 `easy-postman-plugins` 下，但不是普通运行时插件。

第一阶段不移动这个模块。移动模块目录会影响 Maven path、release script 和文档，应在包边界重构稳定后作为单独后续任务处理。

## Split Package 策略

不要在一次重构里消除所有 split package。第一阶段的目标是避免新代码或被重构代码继续进入模糊 owner 的包。

迁移代码时使用 owner 明确的包：

- `com.laker.postman.performance.*` 放 app 侧压测非 UI 代码。
- `com.laker.postman.functional.*` 放 functional runner 非 UI 代码。
- `com.laker.postman.navigation.*` 放宿主导航 UI registry 代码。
- `com.laker.postman.common.*` 只用于 `easy-postman-ui` 中真正共享的 UI 设计系统代码。

迁移期间，如果现有 import 来自宽泛或模糊包，应优先改成 owner 明确的包。

## 重构后的数据流

Collection 请求执行保持如下流程：

1. UI 面板收集表单数据。
2. 请求准备阶段构建并 finalize `PreparedRequest`。
3. 协议分发选择 HTTP、SSE 或 WebSocket 执行。
4. 传输代码执行请求并映射 transport response 数据。
5. UI presenter 渲染请求、响应、流消息、历史记录和测试输出。

压测执行改为如下职责划分：

1. UI 或 headless 代码创建 performance plan。
2. core 编译并调度 plan。
3. app 侧 performance runtime 把 sampler 适配到 EasyPostman 请求执行链。
4. transport adapter 执行 HTTP、SSE 或 WebSocket sample。
5. result collector 输出 core stats 和 app/UI result event。
6. UI 面板只在收到 app 侧 result view model 后渲染事件。

插件补全流程改为：

1. 插件通过 `ScriptCompletionSink` 注册 completion DTO。
2. plugin runtime 只保存 plugin-api 类型的 contributor。
3. app editor 向 contributor 获取中立 completion item。
4. app editor 把 completion item 适配为 RSyntaxAutoComplete。

## 错误处理

传输和响应处理代码必须返回结构化结果信息，或抛出领域级异常。它不能直接显示 Swing 对话框。

大文件下载过大等 UI 警告，由 UI 调用方检查响应处理结果后显示。

Headless 执行不能依赖 Swing 类上报错误。它应通过日志、CLI result、worker response 或结构化 report 字段上报。

插件 completion adapter 的错误按插件 contributor 隔离。单个插件 contributor 失败时应记录日志并跳过，不能破坏整个 editor completion provider。

## 测试策略

每个迁移批次后运行架构边界测试和全量编译：

```bash
mvn -q -pl easy-postman-app -am -Dtest=ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -DskipTests compile
```

新增或更新测试覆盖：

- Functional CSV persistence DTO round-trip。
- Request protocol enum 不再依赖 Swing/icon 类。
- Script completion DTO 到 RSyntax adapter 的适配。
- Redis 和 Kafka 官方插件使用新 SPI 注册补全。
- GUI 和 headless 路径仍能编译并执行 performance plan。

编译后手动冒烟检查：

- Collection HTTP 请求可以发送并渲染响应。
- Collection SSE 和 WebSocket tab 仍能接收流事件。
- Functional runner 能加载保存的 CSV state。
- Performance GUI run 能记录 sample result。
- Headless performance run 能生成 JSON report。
- Redis 和 Kafka 插件脚本补全能出现在脚本编辑器中。

## 验收标准

- `easy-postman-app/src/main/java/com/laker/postman/panel/performance` 只包含 Swing/UI 类和 UI adapter。
- app CLI、worker、headless runtime 代码不再 import `com.laker.postman.panel.performance.*`。
- `com.laker.postman.model` 不再 import `javax.swing`、`FlatSVGIcon`、`IconUtil`、`UiSingletonFactory` 或具体面板类。
- 持久化 service 不再依赖 Swing panel 内部类。
- `easy-postman-plugin-api` 不再 import RSyntaxAutoComplete 类。
- 官方插件能编译并使用新 completion SPI。
- `plugin.platform.version` 已提升。
- `mvn -q -DskipTests compile` 通过。

## 非目标

本阶段不新增 `easy-postman-api-core` 模块。

本阶段不把 `plugin-manager` 移出 `easy-postman-plugins`。

本阶段不替换 OkHttp、不重写脚本引擎，也不故意改变 collection/performance 行为。

本阶段不消除仓库里的所有 split package。
