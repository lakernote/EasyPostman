# EasyPostman 插件 Runtime 架构说明

这份文档聚焦 `easy-postman-plugin-runtime` 在当前重构后的实现结构。

它不重复讲“怎么开发插件”或“怎么发布插件”，而是回答下面几个问题：

- runtime 现在分成了哪些职责块
- 插件从安装到加载的真实链路是什么
- 宿主如何消费插件贡献
- 当前冲突策略和测试覆盖到了哪里
- 后面继续演进时，优先改哪里

相关总览文档见：

- [PLUGINS_zh.md](./PLUGINS_zh.md)

## 1. 当前结构

当前 runtime 相关的主类可以按职责理解为：

```text
easy-postman-plugin-runtime
├── PluginRuntime
├── PluginScanner
├── PluginCandidateResolver
├── PluginLoader
├── PluginRegistry
├── PluginStateStore
├── PluginSettingsStore
├── PluginRuntimePaths
├── RuntimeVersionResolver
├── PluginCompatibility
├── PluginFileInfo
└── PluginVersionComparator
```

### 1.1 每个类负责什么

- `PluginRuntime`
  - 运行时总编排入口
  - 协调启动、关闭、候选选择、生命周期收尾
  - 对外暴露 `initialize()`、`shutdown()`、`getInstalledPlugins()` 等静态入口
- `PluginScanner`
  - 扫描插件目录
  - 读取插件 jar 内的 descriptor
  - 生成磁盘层的 `PluginFileInfo`
- `PluginCandidateResolver`
  - 从扫描结果里选出真正应该加载的插件
  - 处理禁用、待卸载、兼容性过滤、同 `plugin.id` 多版本择优
- `PluginLoader`
  - 创建 `URLClassLoader`
  - 反射实例化插件入口类
  - 调用 `onLoad()` / `onStart()` / `onStop()`
  - 把 `PluginContext` 注册动作接到 `PluginRegistry`
- `PluginRegistry`
  - 保存插件注册出来的脚本 API、服务、Toolbox、补全、Snippet
  - 宿主运行时从这里消费插件能力
- `PluginStateStore`
  - 维护禁用插件和待卸载插件状态
- `PluginSettingsStore`
  - 底层 JSON 持久化
- `PluginRuntimePaths`
  - 统一运行时数据目录、安装目录、包缓存目录
- `RuntimeVersionResolver`
  - 解析 app version 和 plugin platform version
  - 打包态优先读资源，开发态回退读 `pom.xml`

## 2. 真实加载链路

当前插件加载链路可以简化成：

```text
PluginManager / 本地放入 jar
  -> plugins/installed 和 plugins/packages
  -> StartupCoordinator.prepareMainFrame()
  -> PluginRuntime.initialize()
  -> cleanupPendingUninstallPlugins()
  -> PluginScanner.resolvePluginDirs()
  -> PluginScanner.listPluginsFromDirectory()
  -> PluginCandidateResolver.resolveLoadCandidates()
  -> PluginLoader.loadPluginJar()
  -> plugin.onLoad(context)
  -> PluginRegistry 收集贡献
  -> PluginLoader.startPlugins()
  -> 宿主 UI / 脚本 / bridge 层消费贡献
```

这里有几个关键设计点：

- 安装和加载分离
  - `plugin-manager` 负责落盘和校验
  - `runtime` 只负责“当前进程应该加载谁”
- 扫描和选择分离
  - `PluginScanner` 负责“看见什么”
  - `PluginCandidateResolver` 负责“最终选谁”
- 注册和消费分离
  - 插件只在 `onLoad()` 里声明能力
  - 宿主稍后再从 `PluginRegistry` 统一读取

## 3. 宿主消费链路

宿主 app 层已经不再四处直连 `PluginRuntime.getRegistry()`，而是统一收口到：

- [PluginAccess.java](../easy-postman-app/src/main/java/com/laker/postman/plugin/bridge/PluginAccess.java)

当前消费关系大致是：

```text
PluginRegistry
  -> PluginAccess
    -> PostmanApiContext.createScriptApis()
    -> ToolboxPanel.getToolboxContributions()
    -> ScriptSnippetManager.getScriptCompletionContributors()
    -> SnippetDialog.getSnippetDefinitions()
    -> ClientCertificatePluginServices.getService(...)
```

这样做的价值是：

- app 层不用到处依赖 `PluginRuntime`
- 未来如果 registry 或 runtime 实现变化，app 改动面会小很多

## 4. 插件入口模式

插件入口类当前建议只做“能力声明”，不做复杂业务。

官方插件已经开始统一走：

- `PluginContributionSupport`
- `PluginAccess`
- `RedisI18n` / `KafkaI18n`

例如 `KafkaPlugin` / `RedisPlugin` 的 `onLoad()` 现在更接近：

```text
registerScriptApi
registerToolbox
registerScriptCompletionContributor
registerExampleSnippet
```

这比原来直接在入口类里手写大量 `ToolboxContribution` / `ShorthandCompletion` / `SnippetDefinition` 更容易 review。

## 5. 当前冲突策略

### 5.1 Script API alias / service type

`PluginRegistry` 目前对两类冲突做了基础保护：

- 脚本 API alias 冲突
- service type 冲突

当前策略是：

- 保持兼容：后注册覆盖前注册
- 不再静默：会打 `warn` 日志
- 日志会带 owner 信息，指出“哪个插件覆盖了哪个插件”

也就是说，现在 registry 已经会记录：

- 某个 script API alias 属于哪个 pluginId
- 某个 service type 属于哪个 pluginId

这一步还不是最终方案，但已经足够让冲突可观测。

### 5.2 还没做的部分

下面这些冲突目前还没有统一治理：

- `ToolboxContribution` 的重复 `id`
- `SnippetDefinition` 的重复标题或语义冲突
- `ScriptCompletionContributor` 的重复补全项

这些可以后续再做。

## 6. 当前测试覆盖

runtime 测试目前主要在：

- [PluginRuntimeTest.java](../easy-postman-plugin-runtime/src/test/java/com/laker/postman/plugin/runtime/PluginRuntimeTest.java)
- [PluginRegistryTest.java](../easy-postman-plugin-runtime/src/test/java/com/laker/postman/plugin/runtime/PluginRegistryTest.java)

已经覆盖的关键行为包括：

- 重新启用插件时会清理 pending uninstall
- 自定义数据目录会生效
- plugin platform version 独立于 app version
- 不兼容平台版本会被拒绝
- pending uninstall 文件清理
- 同一 `plugin.id` 多版本时只加载最高版本
- 最新版本不兼容时，回退到次新兼容版本
- 某个插件加载失败时，其他插件继续加载
- `shutdown()` 会调用插件 `onStop()`
- registry 对 alias/type 的重复注册遵循“后注册覆盖前注册”
- registry 会保留最新注册者的 owner 信息

## 7. 当前仍然存在的限制

这轮重构之后，结构已经清楚很多，但 runtime 还不是终点版本。

目前仍然存在这些限制：

- `PluginRuntime` 仍然是静态全局入口
  - 测试虽然可控，但还不是真正的实例化 runtime
- registry 对 UI 贡献没有 owner 级别追踪
  - 暂时只追踪了 script API 和 service
- 不支持真正的热卸载
  - 当前启用/禁用/卸载仍以“下次启动生效”思路为主
- `PluginSettingsStore` 还是通用 key-value JSON 包装
  - 还没有 typed state model

## 8. 后续演进建议

如果继续演进，建议优先级如下：

1. 给 `ToolboxContribution` / `SnippetDefinition` / completion 也补 owner 追踪
2. 把 `PluginRuntime` 从静态入口再包一层实例化 facade
3. 给 `PluginStateStore` 引入 typed state model，而不只是字符串集合
4. 评估是否支持无需重启的热启停
5. 把当前冲突策略整理成对插件开发者可见的契约文档

## 9. 阅读顺序建议

如果要顺着代码理解现在这套 runtime，推荐顺序是：

1. [PluginRuntime.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginRuntime.java)
2. [PluginScanner.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginScanner.java)
3. [PluginCandidateResolver.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginCandidateResolver.java)
4. [PluginLoader.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginLoader.java)
5. [PluginRegistry.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginRegistry.java)
6. [PluginRuntimeTest.java](../easy-postman-plugin-runtime/src/test/java/com/laker/postman/plugin/runtime/PluginRuntimeTest.java)
7. [PluginRegistryTest.java](../easy-postman-plugin-runtime/src/test/java/com/laker/postman/plugin/runtime/PluginRegistryTest.java)
