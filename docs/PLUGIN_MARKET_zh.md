# EasyPostman 插件市场说明

当前插件主文档已经统一到：

- `docs/PLUGINS_zh.md`

这份文档只保留插件市场相关的最小说明。

## 支持的安装来源

插件管理当前支持：

- 在线 `HTTP(S)` catalog URL
- 本地 `file://` catalog URL
- 本地目录
- 本地 `catalog.json`
- 直接选择插件 jar

## 推荐的发布方式

如果你要给用户分发插件，建议同时提供：

1. 单独插件 jar
2. 离线目录
3. 在线 catalog URL

这样分别覆盖：

- 想最快安装的用户
- 内网离线用户
- 想持续在线更新的用户

## 离线目录长什么样

以 Redis 为例：

```text
easy-postman-plugins/plugin-redis/target/plugin-market/offline/easy-postman-plugin-redis/
├── catalog.json
└── easy-postman-4.3.55-plugin-redis.jar
```

用户在插件管理里选择这个目录，或者直接选里面的 `catalog.json` 即可。

## 本地调试入口

推荐直接使用：

```bash
./scripts/plugin-dev.sh prepare redis
./scripts/plugin-dev.sh run-clean redis
```

更完整的架构、开发流程、安装说明见：

- `docs/PLUGINS_zh.md`

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
├── easy-postman-plugins/
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
