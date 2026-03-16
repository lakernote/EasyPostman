# EasyPostman 插件架构与安装说明

这份文档基于当前仓库里的真实实现整理，目标只有两件事：

- 讲清楚现在的插件架构到底怎么工作
- 把用户安装路径收敛成 2 种，而不是 3 种概念

结论先说：

1. 对用户来说，EasyPostman 只需要 2 种安装模式
   - 离线安装：本地 JAR 安装，或者本地离线包 / 本地 `catalog.json` 安装
   - 在线安装：通过远程 `catalog.json` 安装
2. “直接安装 JAR”和“离线目录安装”不是两套架构，只是同一个离线场景下的两个入口
3. 当前代码已经基本满足这两个模式，文档和命名需要继续收敛

## 1. 当前插件架构

当前仓库分成 4 层：

```text
easy-postman-parent
├── easy-postman-plugin-api
├── easy-postman-plugin-runtime
├── easy-postman-app
└── easy-postman-plugins
    ├── plugin-manager
    ├── plugin-redis
    ├── plugin-kafka
    ├── plugin-git
    ├── plugin-decompiler
    └── plugin-client-cert
```

职责边界：

- `easy-postman-plugin-api`
  - 稳定 SPI 和插件上下文
- `easy-postman-plugin-runtime`
  - 扫描插件目录
  - 读取插件 descriptor
  - 判定启用 / 禁用 / 待卸载 / 兼容性
  - 创建独立 `URLClassLoader`
  - 注册脚本 API、Toolbox、Snippet 等扩展点
- `easy-postman-app`
  - 宿主应用和插件桥接层
- `easy-postman-plugins`
  - 官方插件聚合目录
  - `plugin-manager` 负责安装和 catalog 解析
  - 其他 `plugin-*` 负责具体业务能力

已经插件化的能力：

- `plugin-redis`
- `plugin-kafka`
- `plugin-git`
- `plugin-decompiler`
- `plugin-client-cert`

## 2. 运行时实际怎么工作

当前代码的真实链路是：

```text
本地 JAR / 本地 catalog / 远程 catalog
  -> plugin-manager 解析安装来源
  -> plugin-installer 校验插件 JAR
  -> 复制到 data/plugins/packages 和 data/plugins/installed
  -> runtime 扫描插件目录
  -> 读取 META-INF/easy-postman/*.properties
  -> 按 plugin id 选最高版本且判定兼容性
  -> 为每个插件创建独立 ClassLoader
  -> 插件注册脚本 API / Toolbox / Snippet / 服务
  -> 宿主通过 bridge 和 registry 消费插件能力
```

几个关键实现点：

- 支持的 catalog 来源
  - `http://` / `https://`
  - `file://`
  - 本地目录
  - 本地 `catalog.json`
- catalog 中的 `downloadUrl` 可以是相对路径
  - 所以同一份 catalog 结构既能做离线包，也能做在线源
- 安装时会同时保留两份文件
  - `plugins/installed/`：当前已安装副本
  - `plugins/packages/`：保留的本地包副本
- 卸载默认只删 `installed/`，不删 `packages/`
  - 这是为了支持后续重装和 Windows 文件锁场景
- 同一插件 ID 存在多个版本时
  - 运行时只加载最高版本
- 插件可通过 `plugin.minAppVersion` / `plugin.maxAppVersion` 控制兼容范围

## 3. 插件目录和数据目录

### 3.1 仓库内的插件产物

以 `plugin-git` 为例：

```text
easy-postman-plugins/plugin-git
├── pom.xml
├── src/main/java
├── src/main/resources/META-INF/easy-postman/plugin-git.properties
├── src/packaging/offline/catalog.json
└── target
    ├── easy-postman-5.3.16-plugin-git.jar
    └── plugin-market/offline/easy-postman-plugin-git/
        ├── catalog.json
        └── easy-postman-5.3.16-plugin-git.jar
```

产物含义：

- `target/easy-postman-<plugin-version>-plugin-*.jar`
  - 单文件安装包
- `target/plugin-market/offline/easy-postman-plugin-*/`
  - 离线安装目录
  - 里面已经带好 `catalog.json`

### 3.2 用户机器上的插件目录

运行时默认使用两个目录：

- `EasyPostman/plugins/installed/`
- `EasyPostman/plugins/packages/`

如果是 portable 模式，则放到应用目录下的 `data/`。

额外支持两个系统属性：

- `easyPostman.plugins.dir`
  - 追加一个开发期扫描目录
- `easyPostman.plugins.catalogUrl`
  - 覆盖插件市场 catalog 地址

## 4. 用户视角只保留 2 种安装模式

这是这份文档最重要的收敛点。

### 4.1 模式一：离线安装

离线安装下其实有两个入口，但它们属于同一个模式。

#### 入口 A：安装本地 JAR

适合：

- 用户手里已经有单个插件 JAR
- 开发联调
- 最低认知成本的安装方式

操作：

1. 打开插件管理
2. 选择本地插件 JAR
3. 选择 `easy-postman-<version>-plugin-*.jar`
4. 安装后重启应用

这个入口最适合“想直接点 jar 的用户”。

#### 入口 B：安装本地离线包 / 本地 catalog

适合：

- 公司内网
- 不能访问公网
- 希望一次性交付“插件 + 元数据”
- 希望给用户一个更稳定的离线分发包

操作：

1. 把整个离线目录发给用户
2. 用户在插件管理里选择：
   - 这个目录
   - 或目录里的 `catalog.json`
3. 从列表里安装插件
4. 安装后重启应用

这个入口本质上仍然是离线安装，只是把“单个 JAR”升级成“带 catalog 的本地源”。

### 4.2 模式二：在线安装

在线安装就是提供一个远程 `catalog.json` 地址，让应用去拉取插件列表并下载安装。

适合：

- 官方插件市场
- 团队内部 HTTP 插件源
- 长期维护的在线更新场景

当前已经支持：

- GitHub 官方 catalog
- Gitee 官方 catalog
- 任意团队内部 `http(s)` catalog

操作：

1. 在插件管理里填入 `catalog.json` URL
2. 加载插件列表
3. 选择插件安装
4. 安装后重启应用

## 5. 为什么不要再讲“3 种安装方式”

以前文档里把下面三件事拆成 3 类：

1. 直接安装 JAR
2. 离线安装
3. 在线安装

问题在于：

- “直接安装 JAR”本质上就是离线安装
- “离线目录安装”和“在线 catalog 安装”共用一套 catalog 解析逻辑
- 对用户来说，分类过细只会增加理解成本

更适合产品表达的方式是：

1. 离线安装
   - 本地 JAR
   - 本地离线包 / 本地 catalog
2. 在线安装
   - 官方源
   - 团队源

这也更符合现在的代码结构。

## 6. 当前推荐的产品表达

如果你准备继续优化插件管理 UI、文档和发布页，建议统一成下面这套说法。

### 6.1 面向用户的文案

- 离线安装
  - 安装本地 JAR
  - 加载本地插件源
- 在线安装
  - 官方插件源
  - 自定义插件源

### 6.2 面向实现的术语

- `jar install`
  - 直接导入单个插件包
- `catalog install`
  - 通过 catalog 安装
  - catalog 可以是本地的，也可以是远程的

换句话说：

- 用户层：2 种模式
- 技术层：2 条入口
  - 单 JAR 安装
  - catalog 安装

## 7. 本地构建与发版

当前仓库里原来文档提到的一些脚本已经不在工作树中，现阶段应以 Maven 和 GitHub Actions 为准。

### 7.1 本地构建单个插件

示例：

```bash
mvn -pl easy-postman-app,easy-postman-plugins/plugin-redis -am clean package -DskipTests
```

说明：

- 会把宿主和目标插件一起构建出来
- 插件模块的 `package` 阶段会自动产出离线目录

### 7.2 本地构建全部插件

```bash
mvn clean package -DskipTests
```

### 7.3 官方独立发版

当前插件独立发版走：

- `.github/workflows/plugin-release.yml`

这个 workflow 会做几件事：

- 构建指定插件
- 产出单插件 JAR
- 产出离线安装 ZIP
- 计算 JAR `sha256`
- 创建 GitHub Release
- 可选同步 Gitee Release
- 可选回写官方 GitHub / Gitee catalog

当前发布边界是合理的：

- 主包继续单独发版
- 插件继续独立发版
- 插件版本可以单独演进
- 兼容边界继续由 `plugin.minAppVersion` 控制

## 8. 官方分发建议

如果你要面向三类人群交付插件：

- 想直接点 jar 的用户
- 内网离线用户
- 想持续在线更新的用户

最合适的交付物其实就 3 个，但安装模式仍然只有 2 个：

1. 单插件 JAR
   - 给“直接安装”的用户
2. 离线包 ZIP
   - 给内网 / 离线用户
3. 官方 catalog URL
   - 给在线安装和持续更新用户

所以建议保持这个发布矩阵：

- Release 附件里放插件 JAR
- Release 附件里放离线包 ZIP
- 文档里固定给出官方 catalog URL

## 9. 借鉴其他产品后，你还可以继续优化什么

下面这些建议不是空想，基本都能在 JetBrains IDE 插件体系和 VS Code 扩展体系里找到对应做法。

参考资料：

- JetBrains 插件安装与自定义仓库
  - [Install plugin from disk](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk)
  - [Custom plugin repository](https://plugins.jetbrains.com/docs/intellij/custom-plugin-repository.html)
- VS Code 扩展安装
  - [Install from VSIX](https://code.visualstudio.com/docs/editor/extension-marketplace#_install-from-a-vsix)

### 9.1 第一优先级：把“本地文件”和“本地源”分开命名

现在你产品上的真实场景是：

- 我有一个 JAR
- 我有一个本地离线包
- 我有一个在线源

建议 UI 和文档直接写成三个按钮或三个入口名，但只归到两个模式里：

- 离线安装
  - 安装本地 JAR
  - 加载本地插件源
- 在线安装
  - 使用官方源
  - 使用自定义源

这样比“直接安装 / 离线安装 / 在线安装”更稳定。

### 9.2 第二优先级：把“官方源”做成默认值，而不是说明文字

你现在代码里已经有：

- GitHub 官方 catalog
- Gitee 官方 catalog
- 远程失败时回退到内置 bundled catalog

这已经很接近成熟产品了。可以继续收敛成：

- 第一次打开插件管理时默认展示官方源
- 用户只在有企业内网需求时才切到自定义源

这能减少大多数用户对 URL 的感知。

### 9.3 第三优先级：离线包最好保持“可转发、可存档、可重复安装”

JetBrains 的离线安装体验之所以稳定，是因为“磁盘安装包”和“自定义仓库”是两个清晰入口。

对 EasyPostman 来说，离线包最好明确承诺下面几点：

- 包内自带 `catalog.json`
- `downloadUrl` 使用相对路径
- 整个目录可直接打 ZIP 转发
- 不依赖外网
- 可反复导入

你现在的离线目录产物已经满足大半，这块主要是文档和发布页要说清楚。

### 9.4 第四优先级：在线更新策略建议继续做“显式检查”，不要急着做隐式自动升级

很多成熟产品支持在线市场，但真正自动升级都会非常谨慎。

对你当前阶段，更建议做：

- 加载 catalog 时提示“发现新版本”
- 用户点击后再安装升级
- 保留旧包直到重启完成切换

不建议一上来做：

- 后台自动下载
- 静默替换已安装插件

原因很简单：

- 你现在已有 `pending uninstall` 和双目录复制逻辑
- 显式升级比静默升级更容易解释和维护

### 9.5 第五优先级：校验能力要真正落地到官方 catalog

当前安装器已经支持校验 `sha256`，但官方 catalog 里的 `sha256` 还是空字符串。

这是一个明显可继续优化的点：

- 官方 GitHub catalog 填充 `sha256`
- 官方 Gitee catalog 填充 `sha256`
- UI 在详情页展示“已校验 / 未校验”

这个改动的收益很高，因为：

- 在线安装更可信
- 内网转发离线包也能做一致性校验
- 后续如果做企业白名单，会更顺手

### 9.6 第六优先级：考虑增加“企业预置插件目录”能力

VS Code 和 JetBrains 在企业场景里都强调集中分发或自定义源。

你已经支持：

- `easyPostman.plugins.dir`

这个点很适合继续产品化：

- 文档里明确“企业可预置共享插件目录”
- 启动参数或配置页里暴露这个能力
- 让运维可以统一投放一批经过审核的插件

这比让企业用户手工逐个导入 JAR 更稳。

## 10. 常见问题

### Q1：卸载会不会把插件包也删掉

默认不会。

当前策略是：

- `installed/` 放已安装副本
- `packages/` 放本地包副本

卸载时只删除 `installed/`，`packages/` 保留。

### Q2：为什么有些功能没了

因为部分能力已经插件化，未安装时宿主会降级或隐藏入口。

例如：

- 没装 `plugin-git`，Git 工作区入口不会显示
- 没装 `plugin-decompiler`，Toolbox 里不会显示反编译器

### Q3：在线安装和离线安装是不是两套实现

不是。

当前实现里：

- 本地 catalog
- 远程 catalog

本质上走的是同一套 catalog 解析逻辑。

真正独立的一条路径只有“直接安装单个 JAR”。

### Q4：我该怎么对外介绍插件安装

推荐统一成一句话：

> EasyPostman 支持两种插件安装方式：离线安装和在线安装。离线安装既支持直接导入 JAR，也支持导入本地离线包；在线安装通过官方或自定义插件源完成。
