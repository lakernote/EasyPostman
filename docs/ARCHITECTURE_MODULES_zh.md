# EasyPostman 模块边界规范

这份文档定义模块职责和放置规则。目标是让后续重构和 vibecoding 时优先维护边界，而不是把新代码塞进最近的目录。

## 当前模块图

```text
easy-postman-parent
├── easy-postman-foundation
├── easy-postman-plugin-api
├── easy-postman-platform
├── easy-postman-ui
├── easy-postman-performance-core
├── easy-postman-plugin-runtime
├── easy-postman-plugins/*
└── easy-postman-app
```

## 模块职责

`easy-postman-foundation` 是最底层非 UI 基础层。它可以放共享 DTO、enum、常量、路径、JSON、系统工具、用户设置工具、国际化机制、基础消息 key，以及可跨宿主/插件复用的通用解析和格式化工具，例如 Cron、JSON Path、XML、文件大小、文件扩展名、时间显示、HTTP header 常量。它不能依赖 Swing、插件运行时、宿主 app、压测运行实现或业务 UI。

`easy-postman-plugin-api` 是插件契约层。它放插件 SPI、插件描述、插件上下文、扩展点数据结构和插件服务接口，例如 `GitPluginService`、`ClientCertificatePluginService`、`RequestCollectionImportService`。插件和宿主通过这里的类型建立契约。

`easy-postman-platform` 是宿主平台框架层。当前已承接自定义 IOC 容器 `com.laker.postman.ioc`，以及可脱离具体界面的更新发现核心 `com.laker.postman.platform.update`：版本比较、发布源选择、安装包资产匹配、变更日志抓取/格式化、更新结果模型。后续启动、欢迎页、帮助、设置中心、主题/字体应用编排等平台能力，如果能脱离具体 app 面板和业务服务，也优先迁到这里。

`easy-postman-ui` 是共享 Swing 设计系统。它放 `FontsUtil`、`IconUtil`、`NotificationUtil`、`EditorThemeUtil`、`ModernColors`、`UiSingletonFactory`/`UiSingletonPanel`/`UiSingletonMenuBar`、`DebouncedSaveSupport`、`IRefreshable`、公共按钮/搜索/表格/输入控件等组件，例如 `EasyComboBox`、`EasyJSpinner`、`EasyPasswordField`、`EditButton`、`SaveButton`、`WrapToggleButton`，以及这些组件直接引用的 icons/theme 资源。主色 button 上的 icon 应使用 on-primary 颜色策略，保持白色，不跟随明暗主题切换。

资源归属按唯一 owner 管理：`easy-postman-ui/src/main/resources/icons` 放通用控件/动作/状态图标，例如 save、copy、paste、search、clear、cancel、close、delete、duplicate、eye、info、warning、arrow、chevron、wrap、start、stop、send、connect、collapse、expand、more、detail、import、export；`easy-postman-app/src/main/resources/icons` 只放宿主业务、品牌、协议、工作区、压测、集合树等 app 专属图标；插件入口图标放插件自己的 `src/main/resources/icons`。两个模块不要保留同路径同名资源，避免 classpath 资源遮蔽和图标版本漂移。官方插件若引用 `icons/*.svg`，该图标必须来自插件自身或 `easy-postman-ui`，不能依赖 app resources。

其他资源也按 owner 管理：`easy-postman-app/src/main/resources/js-libs` 是宿主脚本运行时内置库，`logback.xml` 是宿主运行配置，`easy-postman-plugins/plugin-manager/src/main/resources/plugin-catalog` 是宿主侧插件市场兜底目录，普通插件 descriptor 和插件专属 icon/message bundle 跟随各自插件模块。

`easy-postman-plugin-runtime` 只负责插件扫描、descriptor 解析、classloader、registry、生命周期和状态持久化。它不放具体业务插件能力，也不负责 catalog 解析、下载或安装 UI。

`easy-postman-performance-core` 放无 UI、无传输实现绑定的压测领域核心：编辑态计划节点数据、运行态 `plan.json` 模型、运行时契约、线程组规划、统计、趋势、报告快照、worker assignment/asset reference 这类 GUI、CLI、worker 都要复用的契约。它不直接依赖 OkHttp、Swing、workspace 服务或 app 执行链。

压测的具体执行适配当前留在 `easy-postman-app`：GUI 运行、headless CLI、worker server 先复用 app 内完整执行链，包含变量解析、环境/全局变量、脚本、断言、提取器、CSV inline/file asset、multipart 文件、证书和 HTTP/SSE/WebSocket 传输。后续只有在这些非 UI 语义能从 app 干净抽离后，才考虑新增独立 headless/runner 模块。

`easy-postman-app` 是宿主组装层。它可以放主入口、MainFrame、菜单、具体 app 面板、具体启动 wiring、设置页、更新页、欢迎页、帮助页、app-only 服务，以及宿主侧插件访问适配 `com.laker.postman.plugin.host`。后续迁移平台能力时，从这里逐步迁到 `easy-postman-platform`。

`easy-postman-plugins/*` 通常是官方插件 JAR。普通插件不得反向依赖宿主 app 内部实现；需要扩展宿主时通过 `easy-postman-plugin-api` 注册能力，需要共享基础 DTO/工具时依赖 `easy-postman-foundation`，需要统一 Swing 风格时依赖 `easy-postman-ui`。

`easy-postman-plugins/plugin-manager` 是这个目录下的归属特例：它是宿主 app 直接依赖的宿主侧插件管理辅助模块，负责 catalog 解析、在线/离线安装门面和安装来源记录；它不是由 `easy-postman-plugin-runtime` 扫描加载的普通运行时插件，也不能作为普通插件模板。它继续放在 `easy-postman-plugins` 聚合目录下，是历史路径和发布组织选择；新的宿主侧插件管理库不应默认继续放到 `easy-postman-plugins/*`，后续可以迁到更清晰的模块名或目录。依赖方向上，普通插件仍不得依赖 `easy-postman-app`，但宿主 app 可以依赖 `plugin-manager` 这个特例。

## 国际化、字体、主题放置

国际化机制和跨模块基础词汇放 `easy-postman-foundation`。`I18nUtil`、`CommonI18n`、`CommonMessageKeys`、`common-messages.properties` / `common-messages_zh.properties`、locale/settings key 属于基础能力。通用词汇包括 OK、Cancel、Save、Copy、Close、Search、Success、Error、Warning、Tip 这类无具体业务 owner 的短标签。

国际化资源跟随所属模块。公共 UI 组件专属文案放 `easy-postman-ui`，例如 `ui-messages.properties` / `ui-messages_zh.properties`；宿主页面和宿主业务文案放 `easy-postman-app` 的 `messages_en.properties` / `messages_zh.properties`；插件文案放各插件模块，例如 `redis-messages.properties`、`kafka-messages.properties`。不要让公共 UI 组件或插件依赖 app resources 才能显示文案；如果多个模块需要同一个短标签，先放 foundation common bundle，并用 `CommonI18n.get(CommonMessageKeys...)`，不要在 `ui-messages*`、app 或插件 bundle 里重复定义同名 common key。

字体工具和字体 token 放 `easy-postman-ui`。全局启动时读取设置并应用字体的编排当前在 app，迁移时属于 `easy-postman-platform`。

主题 token、语义色、图标主题适配、RSyntaxTextArea 编辑器主题 XML 和公共 UI 资源放 `easy-postman-ui`。FlatLaf 的安装、主题切换动画和启动应用编排当前在 app，迁移时属于 `easy-postman-platform`。FlatLaf token 覆盖文件如果依赖 app 内的具体 `EasyLightLaf` / `EasyDarkLaf` 类路径，先跟随 app。

## 判断一个类该放哪里

先问 5 个问题：

1. 是否完全无 UI 且多个模块要用？是则优先 `foundation`。
2. 是否是插件对宿主声明能力的契约？是则 `plugin-api`。
3. 是否是可复用 Swing 组件、颜色、字体、图标或 UI 工具？是则 `ui`。
4. 是否是插件加载、扫描、生命周期或 registry？是则 `plugin-runtime`。
5. 是否是平台框架能力，例如 IOC、启动编排、升级、欢迎页、帮助、设置中心、主题/字体应用编排？能脱离具体 app UI 时放 `platform`，否则先留 `app`。
6. 是否是具体宿主页面、菜单或业务组装？是则放 `app`。

更新能力按这条边界拆：版本检查、更新源、资产解析、变更日志和 `UpdateInfo`/`UpdateCheckFrequency` 这类平台数据放 `easy-postman-platform`；`AutoUpdateManager`、`UpdateUIManager`、`UpdateDownloader`、更新弹窗、安装/退出流程、插件市场更新 UI 留在 `easy-postman-app`。platform 不能直接依赖 app 的 `SettingManager`，需要通过 `UpdateSettingsProvider` 这类最小接口由 app 适配。

只有满足至少两个条件时才拆新模块：多个领域复用、依赖方向清晰、可独立测试、发布/风险节奏不同、当前包已经承担多种职责。否则先用 package 组织，不为名字好看而拆模块。

## 禁止事项

- 不要把 Swing 组件放进 `foundation`。
- 不要把插件服务接口放进 `foundation`。
- 不要让普通官方插件依赖 `easy-postman-app`；`easy-postman-plugins/plugin-manager` 是宿主 app 可直接依赖的插件管理特例，不能作为新插件模板。
- 不要让公共 UI 组件依赖 app resources 才能加载自身 icons。
- 不要在 app 和 ui 中重复放同名 `icons/*.svg`；通用图标归 `easy-postman-ui`，业务图标归使用方模块。
- 不要在 app 面板里新增一套私有按钮/颜色/字体规则，优先沉淀到 `easy-postman-ui`。
- 不要把“暂时不知道放哪”的代码放进一个泛化 common 包。

## 后续简化拆分目标

后续可以按收益逐步抽取，不需要一次性完成：

```text
easy-postman-platform
  已有：IOC、更新发现核心
  后续：启动、欢迎页、帮助、设置中心、主题/字体应用编排

easy-postman-api-core
  API 请求/响应模型、集合、环境、变量、导入导出、脚本流水线核心
```

抽取时保持 app 只做组装，核心模块不反向依赖 app UI。
