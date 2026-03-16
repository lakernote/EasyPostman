# EasyPostman 插件市场与模块化方案

Redis 插件的当前落地与本地 / GitHub 使用方式，见：

- `docs/REDIS_PLUGIN_zh.md`

## 1. 目标

当前 EasyPostman 采用单体 fat jar 打包，新增能力会持续拉大安装包、启动扫描范围和维护成本。结合现有代码结构，更合适的方向不是“做一个复杂平台”，而是先把项目拆成：

- `core`：默认安装，负责主框架、HTTP 调试主流程、基础 UI、工作区与本地存储
- `official plugins`：官方插件，按需安装
- `community plugins`：社区插件，通过统一协议接入
- `market/index`：插件目录与元数据索引

目标是同时解决 4 个问题：

1. 主安装包尽量小，默认只带高频核心能力
2. 新功能以插件发布，不再强绑主版本
3. 插件安装、升级、禁用、卸载都有统一入口
4. 整套托管、构建、分发尽量基于 GitHub 免费设施完成

## 2. 结合当前仓库的拆分建议

从当前 `pom.xml` 和代码目录看，以下能力最适合优先插件化。

### 2.1 建议保留在 Core 的部分

- 主窗口、导航、主题、国际化
- HTTP/HTTPS 请求编辑与发送主链路
- 环境变量、集合、历史记录、本地工作区
- 基础响应查看
- 插件管理器本身
- 自动更新与插件更新框架

这些能力决定产品能不能开箱即用，不建议变成可选安装。

### 2.2 第一批建议拆出去的插件

#### A. 脚本引擎插件

当前重量级依赖：

- GraalVM JS
- 内置 JS libs

建议插件：

- `plugin-script-js`

提供能力：

- Pre-request / Test Script 执行
- `pm.*` 基础脚本 API
- 代码片段与脚本自动补全扩展

收益：

- 这是包体和启动复杂度的大头之一
- 不用脚本的用户可以完全不装

#### B. 数据源脚本插件

当前已经有明确边界：

- `ScriptRedisApi`
- `ScriptKafkaApi`
- `ScriptElasticsearchApi`
- `ScriptInfluxDbApi`

建议拆成独立插件：

- `plugin-script-redis`
- `plugin-script-kafka`
- `plugin-script-elasticsearch`
- `plugin-script-influxdb`

收益：

- 外部中间件能力完全按需装
- 某个插件坏了不会拖累整个主程序升级

#### C. Toolbox 工具插件

当前适合拆出去：

- Kafka 工具面板
- Elasticsearch 工具面板
- InfluxDB 工具面板
- Markdown 工具面板

建议插件：

- `plugin-toolbox-kafka`
- `plugin-toolbox-elasticsearch`
- `plugin-toolbox-influxdb`
- `plugin-toolbox-markdown`

#### D. 导入器插件

建议拆出去：

- Swagger / OpenAPI 导入
- HAR 导入
- ApiPost 导入
- IntelliJ HTTP 导入

建议插件：

- `plugin-import-openapi`
- `plugin-import-har`
- `plugin-import-apipost`
- `plugin-import-idea-http`

#### E. Git 工作区插件

当前依赖：

- JGit

建议插件：

- `plugin-workspace-git`

说明：

- 如果你认为 Git 工作区是产品核心卖点，也可以先保留在 core
- 但从“减包”和“低频用户不必安装”的角度，它非常适合插件化

#### F. 性能测试插件

当前相关：

- `panel/performance`
- `jfreechart`

建议插件：

- `plugin-performance`

说明：

- 这块功能完整度高，但并非所有用户都需要
- 拆出去后主程序可以更聚焦 API 调试

## 3. 目标工程结构

建议从单模块 Maven 逐步演进为多模块，不要一次性大迁移。

```text
easy-postman/
├── pom.xml                       # parent
├── easy-postman-app/            # 启动器/桌面壳
├── easy-postman-core-api/       # 插件 API、扩展点接口、公共模型
├── easy-postman-core-runtime/   # 插件管理器、类加载、安装/更新/校验
├── easy-postman-core-ui/        # 核心 UI
├── easy-postman-plugin-sdk/     # 插件开发脚手架
├── plugins/
│   ├── plugin-script-js/
│   ├── plugin-script-redis/
│   ├── plugin-script-kafka/
│   ├── plugin-script-elasticsearch/
│   ├── plugin-script-influxdb/
│   ├── plugin-workspace-git/
│   ├── plugin-performance/
│   ├── plugin-import-openapi/
│   └── ...
└── docs/
```

最重要的原则：

- `core-api` 只放稳定接口，不放具体实现
- 插件只能依赖 `core-api`，不要反向依赖 `app`
- 每个插件单独产出制品，独立发布版本

## 4. 插件运行时设计

### 4.1 插件包格式

建议不要直接下载裸 jar，而是定义一个插件包，例如 `.epp`：

```text
plugin-script-kafka-1.2.0.epp
├── plugin.json
├── plugin.jar
├── lib/
│   └── *.jar
├── icon.png
└── checksums.txt
```

`plugin.json` 示例：

```json
{
  "id": "plugin-script-kafka",
  "name": "Kafka Script API",
  "version": "1.2.0",
  "entryClass": "com.laker.postman.plugins.kafka.KafkaScriptPlugin",
  "description": "Provide pm.kafka APIs and Kafka toolbox panels.",
  "provider": "lakernote",
  "homepage": "https://github.com/lakernote/easy-postman-plugin-kafka",
  "minCoreVersion": "5.0.0",
  "maxCoreVersion": "5.x",
  "dependencies": [
    {
      "id": "plugin-script-js",
      "version": ">=1.0.0"
    }
  ],
  "permissions": [
    "network",
    "filesystem",
    "script-api"
  ]
}
```

### 4.2 插件生命周期

建议定义统一接口：

```java
public interface EasyPostmanPlugin {
    PluginDescriptor descriptor();
    void onLoad(PluginContext context);
    void onStart();
    void onStop();
}
```

### 4.3 扩展点

不要让插件直接改主程序内部类，统一走扩展点注册。

首批扩展点建议：

- `ToolPanelExtension`
- `ImportProviderExtension`
- `ScriptApiExtension`
- `SidebarTabExtension`
- `MenuActionExtension`
- `RequestProtocolExtension`
- `SettingsPageExtension`

这样你现有的 Kafka、导入器、脚本 API、工具箱，都能自然映射进去。

### 4.4 类加载隔离

建议每个插件一个独立 `URLClassLoader`。

规则：

- `core-api` 由主程序 classloader 提供
- 插件自身依赖放插件 classloader
- 插件之间默认不可互相直接访问
- 插件依赖插件时，通过运行时导出的 API 或服务注册访问

这样可以避免：

- Kafka/JGit/GraalVM 这类依赖把主包拖大
- 插件之间依赖版本打架
- 单个插件崩溃污染全局类路径

## 5. 插件市场设计

### 5.1 最省钱的整体方案

推荐采用下面这套：

- GitHub Releases：存放插件安装包 `.epp`
- GitHub Pages：存放插件市场索引页和 `plugins.json`
- GitHub Actions：自动构建插件、生成校验信息、发布 Release、更新索引
- GitHub Discussions：插件讨论、评价、反馈

这是最接近“整套免费”的方案。

### 5.2 为什么这样分工

#### 1. 插件二进制放 GitHub Releases

原因：

- 适合发布可下载二进制
- 单个 release 最多可挂 1000 个资产
- 单文件上限 2 GiB
- 官方文档明确说明 release 总大小和带宽都不限

这比把插件二进制直接塞到 Pages 更稳妥。

#### 2. 插件目录索引放 GitHub Pages

原因：

- 适合公开 JSON 索引和一个轻量市场页面
- 免费、简单、可直接被桌面端读取

但 Pages 不适合扛插件包本体，因为官方文档给出：

- 发布站点不超过 1 GB
- 100 GB/月软带宽

所以 Pages 只放“索引”，不要放插件包。

### 5.3 市场索引结构

建议单独建一个仓库，例如：

- `easy-postman-plugin-index`

其中 Pages 暴露：

- `https://your-org.github.io/easy-postman-plugin-index/plugins.json`

示例：

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-03-15T10:00:00Z",
  "coreCompatibility": {
    "min": "5.0.0"
  },
  "plugins": [
    {
      "id": "plugin-script-kafka",
      "name": "Kafka Script API",
      "version": "1.2.0",
      "category": "script",
      "author": "lakernote",
      "source": "official",
      "description": "Kafka script API and toolbox.",
      "iconUrl": "https://your-org.github.io/easy-postman-plugin-index/icons/plugin-script-kafka.png",
      "homepage": "https://github.com/lakernote/easy-postman-plugin-kafka",
      "repo": "lakernote/easy-postman-plugin-kafka",
      "releaseUrl": "https://github.com/lakernote/easy-postman-plugin-kafka/releases/tag/v1.2.0",
      "downloadUrl": "https://github.com/lakernote/easy-postman-plugin-kafka/releases/download/v1.2.0/plugin-script-kafka-1.2.0.epp",
      "sha256": "xxx",
      "minCoreVersion": "5.0.0",
      "maxCoreVersion": "5.x",
      "permissions": ["network", "filesystem", "script-api"],
      "dependencies": ["plugin-script-js"],
      "screenshots": [],
      "tags": ["kafka", "script", "toolbox"]
    }
  ]
}
```

## 6. 插件安装与升级流程

### 6.1 安装流程

1. 桌面端请求 `plugins.json`
2. 用户在“插件市场”点击安装
3. 下载 `.epp`
4. 校验 SHA-256
5. 解压到本地插件目录
6. 写入已安装元数据
7. 提示“重启后生效”或支持热加载

本地目录建议：

```text
~/.easy-postman/
├── plugins/
│   ├── plugin-script-js/
│   │   └── 1.0.0/
│   ├── plugin-script-kafka/
│   │   └── 1.2.0/
│   └── installed.json
└── cache/
```

### 6.2 升级流程

建议复用你现有的 GitHub Release 更新思路。

当前项目已经有主程序更新检查逻辑，后续可以新增：

- `PluginUpdateChecker`
- `PluginCatalogService`
- `PluginInstaller`
- `PluginRegistry`

这样主程序更新和插件更新采用同一套远端访问模型，维护成本低。

## 7. 安全模型

这里要说清楚一个现实问题：Java 本地插件本质上是执行用户机器上的代码，不可能像浏览器扩展一样天然安全。

所以建议至少做这几层：

### 7.1 官方插件白名单

默认市场只展示：

- `official`
- `verified`

社区插件默认折叠到“第三方”分组。

### 7.2 签名与校验

最少做：

- `sha256`

更完整的做法：

- 官方私钥签名
- 客户端内置公钥验签

### 7.3 权限提示

安装时显示：

- 需要网络
- 需要本地文件读写
- 需要脚本 API
- 需要请求钩子

虽然这不能真正限制 Java 代码的所有行为，但能提升透明度。

### 7.4 兼容性限制

每个插件都必须声明：

- `minCoreVersion`
- `maxCoreVersion`

避免 core 升级后插件直接炸掉。

## 8. UI 设计建议

建议在设置页或顶部菜单新增“插件中心”。

分 4 个页签：

- `市场`
- `已安装`
- `可更新`
- `开发者模式`

每个插件卡片展示：

- 图标
- 名称
- 简介
- 作者
- 官方/第三方标记
- 兼容的 core 版本
- 权限列表
- 安装量或 GitHub stars

按钮状态：

- 安装
- 更新
- 禁用
- 卸载

附加入口：

- 跳转 GitHub 仓库
- 查看 Release Notes
- 查看源代码

## 9. 免费托管建议

### 9.1 推荐方案

如果你希望尽量全免费，优先级建议如下：

#### 方案 A：推荐

- 市场索引：GitHub Pages
- 插件包：GitHub Releases
- CI：GitHub Actions
- 讨论区：GitHub Discussions

适用：

- 开源项目
- 官方插件量不算夸张
- 社区插件主要通过 GitHub 仓库托管

#### 方案 B：兼顾国内下载

- 国际：GitHub Pages + GitHub Releases
- 国内镜像：Gitee Release 或者你现有 Gitee 镜像

适用：

- 中国大陆用户较多
- 某些网络环境下需要备用下载源

### 9.2 不建议优先采用的方案

#### GitHub Packages

虽然公开包本身可以免费，但对桌面应用插件分发并不是最省事的方案：

- 客户端处理包仓库协议更重
- 存储与 Actions 工件额度存在联动
- 对“插件市场下载”这个场景不如 Releases 直接

所以桌面插件安装优先走 Release 资产下载更简单。

## 10. 与现有代码的衔接方式

### 10.1 先不要一次性重写 IOC

你当前已有自研 IOC：

- `ApplicationContext`
- `BeanFactory`

第一阶段没必要先把它推翻。更现实的做法是：

- core 继续使用现有 IOC
- 插件启动后，把插件对象或扩展点实例注册进 runtime registry
- core UI 从 registry 中读取扩展点

也就是说，插件系统先作为“IOC 之外的一层扩展机制”落地。

### 10.2 先抽“稳定接口”，再搬代码

顺序建议：

1. 先从现有代码中抽接口
2. 再让原有实现继续留在主工程里跑通
3. 最后把实现移动到插件模块

这样风险最小。

## 11. 分阶段实施路线

### Phase 1：插件运行时骨架

目标：

- 能识别本地插件目录
- 能读取 `plugin.json`
- 能加载插件 jar
- 能注册最简单的一个扩展点

这阶段先不做市场下载。

### Phase 2：插件中心 UI + 远端索引

目标：

- 读取 GitHub Pages `plugins.json`
- 展示插件列表
- 支持下载、安装、卸载、更新

### Phase 3：先迁一个低风险插件

最推荐先迁：

- `plugin-import-openapi`
  或
- `plugin-toolbox-markdown`

原因：

- 依赖相对单纯
- 对核心链路影响较小
- 能快速验证插件机制是否成立

### Phase 4：迁中等复杂度插件

- `plugin-workspace-git`
- `plugin-performance`
- `plugin-toolbox-kafka`

### Phase 5：迁脚本引擎和数据源插件

- `plugin-script-js`
- `plugin-script-redis`
- `plugin-script-kafka`
- `plugin-script-elasticsearch`
- `plugin-script-influxdb`

这阶段最复杂，因为会碰到：

- 自动补全
- 代码片段
- 脚本上下文注入
- 生命周期管理

## 12. 我对你这个项目的最优建议

如果目标是“控制安装包膨胀 + 后续功能都插件化”，我建议你这样做：

1. 把 EasyPostman 定位成 `core + official plugins`
2. 第一版市场只做“官方插件市场”，先不开放任意第三方上传
3. 托管统一采用：
   - 插件索引用 GitHub Pages
   - 插件包用 GitHub Releases
   - 发布流水线用 GitHub Actions
4. 第一批先拆：
   - OpenAPI 导入
   - Markdown 工具
   - Kafka 工具
   - 性能测试
5. 第二批再拆：
   - GraalJS 脚本
   - Redis/Kafka/ES/InfluxDB 脚本 API
   - Git 工作区

原因很简单：

- 这样最省钱
- 这样改动路径最稳
- 这样最符合你当前 Java Swing + Maven + GitHub Release 的现状
- 这样可以逐步把“功能膨胀”问题转成“插件生态”问题

## 13. GitHub 官方限制与方案依据

以下结论基于 GitHub 官方文档，适合当前方案选型：

- GitHub Releases：
  - 每个 release 最多 1000 个资产
  - 单文件必须小于 2 GiB
  - release 总大小和带宽不限
- GitHub Pages：
  - 发布站点不超过 1 GB
  - 100 GB/月软带宽
  - 更适合放市场索引，不适合放大量插件包
- GitHub Actions：
  - 公共仓库使用标准 GitHub-hosted runners 是免费的

参考：

- https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
- https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits
- https://docs.github.com/en/billing/managing-billing-for-github-actions/about-billing-for-github-actions
- https://docs.github.com/en/billing/managing-billing-for-your-products/managing-billing-for-github-packages/about-billing-for-github-packages
