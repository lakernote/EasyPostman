# EasyPostman 插件开发、更新与发布指南

这份文档面向两类人：

- 想看懂当前插件机制是怎么工作的维护者
- 正在开发、更新、发布插件的开发者

如果你只关心一句话，可以先记住这 4 点：

1. 插件是单独的 JAR，不和宿主一起绑死发版
2. 插件元数据的单一真源是各插件自己的 `pom.xml`
3. GitHub Actions 只负责读取产物、校验一致性、发布和回写 catalog
4. 本地验证优先走“构建插件 JAR -> 插件管理安装 -> 重启验证”这条路径

## 1. 组件分层和原理

当前插件体系分成 6 层：

```text
easy-postman-parent
├── easy-postman-plugin-api
├── easy-postman-plugin-bridge
├── easy-postman-plugin-ui
├── easy-postman-plugin-runtime
├── easy-postman-app
└── easy-postman-plugins
    ├── plugin-manager
    ├── plugin-redis
    ├── plugin-kafka
    ├── plugin-decompiler
    └── plugin-client-cert
```

### 1.1 每层负责什么

- `easy-postman-plugin-api`
  - 插件对宿主暴露的稳定 SPI
  - 包含 `EasyPostmanPlugin`、`PluginContext`、`PluginDescriptor`
  - 新插件的入口类必须实现这里的接口
- `easy-postman-plugin-bridge`
  - 宿主和插件共享的桥接契约、工具类、消息 key、公共模型
  - 插件需要和宿主共享某些非 UI 能力时，优先放这里
- `easy-postman-plugin-ui`
  - 插件和宿主公用的 UI 基础组件和视觉常量
  - 插件面板如果需要复用统一 UI 风格，优先依赖这一层
- `easy-postman-plugin-runtime`
  - 负责扫描、读取 descriptor、版本兼容校验、类加载、生命周期调用、扩展点注册
  - 可以把它理解成插件运行时内核
- `easy-postman-app`
  - 宿主应用本身
  - 从 runtime 和 registry 里消费插件注册出来的能力
- `easy-postman-plugins`
  - 官方插件集合
  - `plugin-manager` 负责安装、catalog 解析、安装来源记录
  - 其他 `plugin-*` 是具体业务插件

### 1.2 插件是怎么被宿主加载的

真实运行链路大致是：

```text
本地 JAR / 远程 catalog
  -> plugin-manager 负责安装和下载
  -> 插件 JAR 进入 plugins/installed 和 plugins/packages
  -> runtime 扫描插件目录
  -> 读取 META-INF/easy-postman/*.properties
  -> 解析 PluginDescriptor
  -> 校验 app 版本 + platform 版本兼容性
  -> 为每个插件创建独立 URLClassLoader
  -> 反射创建 entryClass
  -> 调用 onLoad(context)
  -> 插件向 registry 注册脚本 API / Toolbox / 补全 / Snippet / Service
  -> 宿主消费这些能力
```

关键点：

- 插件不是直接依赖宿主内部实现，而是通过 `PluginContext` 声明能力
- 插件之间不共享类加载器，避免依赖冲突污染宿主
- 同一 `plugin.id` 存在多个版本时，运行时只会选择最高版本加载
- 安装阶段就会做兼容性拦截，避免出现“安装成功但运行时跳过”的假成功状态

### 1.3 插件能扩展哪些能力

当前 `PluginContext` 支持这几类扩展点：

- `registerScriptApi`
  - 给脚本环境暴露 `pm.xxx`
- `registerService`
  - 给宿主或桥接层按类型取服务
- `registerToolboxContribution`
  - 往 Toolbox 增加面板
- `registerScriptCompletionContributor`
  - 给脚本编辑器增加自动补全
- `registerSnippet`
  - 增加脚本片段

以 `plugin-kafka` 为例，`KafkaPlugin` 在 `onLoad` 里一次性注册了：

- `pm.kafka`
- Kafka Toolbox 面板
- Kafka 相关脚本补全
- Kafka 示例片段

也就是说，插件入口类的职责不是“自己跑业务逻辑”，而是“在加载时把能力挂到宿主上”。

## 2. 版本模型和兼容性

### 2.1 为什么要区分 app 和 platform

当前体系里有两种版本边界：

- `app version`
  - 宿主 EasyPostman 的发行版版本
  - 例如 `5.3.16`
- `plugin platform version`
  - 插件 SPI / 运行时装配边界版本
  - 当前由根 `pom.xml` 里的 `plugin.platform.version` 管理

简单理解：

- `platform` 管“这套插件接口还能不能接上”
- `app` 管“当前宿主有没有插件依赖的具体功能”

### 2.2 什么时候改 platform version

只有插件机制本身发生不兼容变化时，才应该提升 `plugin.platform.version`，例如：

- `PluginContext` 有不兼容修改
- `PluginDescriptor` 语义发生破坏性变化
- runtime 的装配方式变了，老插件会失效
- 某类扩展点契约被替换

如果只是：

- 宿主修了一个普通 bug
- 宿主发了一个补丁版本
- 某个插件自己新增功能

通常都不应该 bump `plugin.platform.version`。

### 2.3 什么时候加 min/maxAppVersion

只有插件确实依赖某个宿主能力时，才建议加：

- 宿主新增了一个插件要调用的 bridge/service
- 宿主某个 UI 容器、入口或事件只在新版本存在
- 插件明确不支持某个老宿主版本

如果插件只是普通扩展，通常只写 `min/maxPlatformVersion` 就够了。

### 2.4 当前 descriptor 和 catalog 的关系

当前规则是：

- 插件自己的 `pom.xml` 维护元数据
- Maven 过滤生成 `META-INF/easy-postman/*.properties`
- GitHub Actions 从构建好的 JAR 里读取 descriptor
- workflow 再把 descriptor 内容同步回 `catalog.json`

所以真正的顺序是：

```text
pom.xml -> descriptor -> release asset -> catalog
```

不是：

```text
workflow 手写字段 -> catalog -> 再反推插件
```

这能避免多处手填导致漂移。

## 3. 开发一个新插件应该怎么做

下面按当前仓库的真实做法给出最短路径。

### 3.1 新建模块

建议在 `easy-postman-plugins/` 下新建：

```text
easy-postman-plugins/plugin-xxx
├── pom.xml
├── src/main/java
└── src/main/resources/META-INF/easy-postman/plugin-xxx.properties
```

然后把模块加入聚合构建。

### 3.2 写插件 `pom.xml`

推荐直接参考现有官方插件，例如：

- `easy-postman-plugins/plugin-kafka/pom.xml`
- `easy-postman-plugins/plugin-redis/pom.xml`

至少要有这些字段：

```xml
<version>5.3.16</version>
<artifactId>easy-postman-plugin-xxx</artifactId>

<properties>
    <host.version>${revision}</host.version>
    <plugin.id>plugin-xxx</plugin.id>
    <plugin.name>XXX Plugin</plugin.name>
    <plugin.entryClass>com.laker.postman.plugin.xxx.XxxPlugin</plugin.entryClass>
    <plugin.description>...</plugin.description>
    <plugin.minPlatformVersion>${plugin.platform.version}</plugin.minPlatformVersion>
    <plugin.maxPlatformVersion>${plugin.platform.version}</plugin.maxPlatformVersion>
</properties>
```

说明：

- `host.version`
  - 编译时依赖哪一版宿主平台包
- `plugin.id`
  - 插件唯一 ID
  - 发布、安装、升级都靠它识别
- `plugin.entryClass`
  - 插件入口类
- `plugin.min/maxPlatformVersion`
  - 当前官方插件默认都绑定到平台版本

如果插件确实依赖宿主特定功能，可以额外加：

```xml
<plugin.minAppVersion>5.3.16</plugin.minAppVersion>
<plugin.maxAppVersion>5.4.0</plugin.maxAppVersion>
```

但要注意，当前官方 descriptor 模板默认没有把这两个字段写进去；你需要同时更新 descriptor 模板。

### 3.3 写 descriptor 模板

示例：

```properties
plugin.id=${plugin.id}
plugin.name=${plugin.name}
plugin.version=${project.version}
plugin.entryClass=${plugin.entryClass}
plugin.description=${plugin.description}
plugin.homepage=${plugin.homepage}
plugin.minPlatformVersion=${plugin.minPlatformVersion}
plugin.maxPlatformVersion=${plugin.maxPlatformVersion}
```

如果你要使用 `minAppVersion` / `maxAppVersion`，记得把它们也加进模板。

当前设计原则是：

- descriptor 是构建产物的一部分
- descriptor 的真实值来自 `pom.xml`
- 不要在 descriptor 里手工写死版本和兼容范围

### 3.4 写入口类

入口类必须实现 `EasyPostmanPlugin`：

```java
public class XxxPlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        // 注册扩展点
    }
}
```

常见模式：

```java
context.registerScriptApi("xxx", ScriptXxxApi::new);
context.registerToolboxContribution(...);
context.registerScriptCompletionContributor(...);
context.registerSnippet(...);
```

建议：

- `onLoad`
  - 只做注册，不做重量级初始化
- `onStart`
  - 放真正需要启动时执行的逻辑
- `onStop`
  - 做资源释放

### 3.5 依赖应该怎么选

优先规则：

- 只依赖 `plugin-api` / `plugin-bridge` / `plugin-ui`
- 不要直接反向依赖 `easy-postman-app` 内部类
- 第三方库如果插件自身需要，跟随插件一起打进插件 JAR
- 宿主已经提供、且插件只需要编译期引用的依赖，优先用 `provided`

### 3.6 新插件开发 checklist

建议按这个清单走：

1. 新建 `plugin-xxx` 模块
2. 配好 `pom.xml`
3. 写 descriptor 模板
4. 写入口类并实现 `EasyPostmanPlugin`
5. 注册需要的扩展点
6. 本地打包生成 JAR
7. 本地安装验证
8. 补测试
9. 再走 GitHub 发布

## 4. 更新一个已有插件应该怎么做

更新已有插件，优先回答这 3 个问题：

1. 这次是插件自己的功能更新，还是平台兼容边界变化
2. 需要不需要提升插件版本
3. 需不需要调整 app/platform 兼容范围

### 4.1 最常见的更新：插件功能更新

如果只是：

- 新增一个脚本 API 方法
- 新增一个 Toolbox 功能
- 修插件自己的 bug
- 调整插件自己的 UI

通常只需要：

1. 修改 `plugin-xxx` 代码
2. 提升该插件自己的 `<version>`
3. 重新构建和验证

一般不需要改：

- 宿主 `revision`
- `plugin.platform.version`

### 4.2 需要调整兼容范围的更新

如果插件依赖了新的宿主能力：

1. 在插件 `pom.xml` 里新增或调整 `plugin.minAppVersion`
2. 在 descriptor 模板里同步加上 `plugin.minAppVersion`
3. 本地验证老版本宿主确实不再适配

如果是插件平台 SPI 变了：

1. 先提升根 `pom.xml` 里的 `plugin.platform.version`
2. 再更新受影响插件的 `plugin.min/maxPlatformVersion`
3. 验证旧插件被正确判为不兼容

### 4.3 更新已有插件的推荐顺序

```text
改代码
  -> 决定是否 bump 插件版本
  -> 决定是否修改 min/maxAppVersion
  -> 决定是否修改 min/maxPlatformVersion
  -> 本地构建
  -> 本地安装验证
  -> dry run 发布
  -> 正式发布
```

## 5. 在 GitHub 发布插件应该怎么做

当前官方插件独立发版走：

- `.github/workflows/plugin-release.yml`

### 5.1 正式发布前你要先改什么

至少确认下面几件事：

1. 目标插件的 `pom.xml` 版本已经提升
2. descriptor 模板字段齐全
3. 本地构建通过
4. 本地验证通过
5. 如果涉及兼容性变化，`min/maxAppVersion` 或 `min/maxPlatformVersion` 已正确更新

### 5.2 workflow 会做什么

这个 workflow 当前会做：

1. 只构建你选中的插件模块
2. 产出单插件 JAR
3. 计算 JAR 的 `sha256`
4. 从 JAR 内的 descriptor 读取插件元数据
5. 校验 `plugin id / version / tag` 一致性
6. 发布 GitHub Release
7. 可选发布 Gitee Release
8. 可选更新 GitHub / Gitee catalog

### 5.3 workflow 的关键约束

当前正式发版有这几个规则：

- 正式发版只能从默认分支触发
- 非 `dry_run` 情况下，官方 GitHub catalog 必须一起更新
- workflow 不再自己维护兼容字段，而是从 JAR descriptor 读取

### 5.4 推荐发布步骤

建议每次都按这个顺序：

1. 本地改完代码和版本
2. 本地构建并验证
3. 在 GitHub Actions 手动触发 `plugin-release.yml`
4. 先跑一次 `dry_run=true`
5. 确认产物、descriptor、版本都对
6. 再跑正式发布

### 5.5 workflow_dispatch 参数怎么选

你通常只需要关心这些输入：

- `plugin`
  - 选择要发布的插件，例如 `kafka`
- `dry_run`
  - 建议先 `true`
- `publish_gitee_release`
  - 是否同步 Gitee Release
- `update_github_catalog`
  - 是否回写 GitHub catalog
- `update_gitee_catalog`
  - 是否回写 Gitee catalog
- `release_notes`
  - 可选

### 5.6 发布后会得到什么

正式发布后，通常会得到：

- 一个单插件 JAR 附件
- 一个 `.sha256.txt` 文件
- 一条 GitHub Release
- 更新后的 `plugin-catalog/catalog-github.json`
- 如开启 Gitee，同步得到对应 Gitee Release 和 catalog 更新

## 6. 本地怎么验证

本地验证建议分成 4 层，不要只做编译通过。

### 6.1 第一层：构建通过

构建单个插件：

```bash
mvn -pl easy-postman-plugins/plugin-kafka -am clean package -DskipTests
```

构建多个插件：

```bash
mvn -pl easy-postman-plugins/plugin-kafka,easy-postman-plugins/plugin-redis -am clean package -DskipTests
```

### 6.2 第二层：检查 descriptor 是否正确

至少检查两处：

1. 构建输出目录

```text
easy-postman-plugins/plugin-kafka/target/classes/META-INF/easy-postman/plugin-kafka.properties
```

2. 最终 JAR 内的 descriptor

你需要确认：

- `plugin.id`
- `plugin.version`
- `plugin.entryClass`
- `plugin.description`
- `plugin.min/maxPlatformVersion`
- 如有需要，`plugin.min/maxAppVersion`

这一层的目标是确认：

- `pom.xml` -> descriptor 模板 -> 构建产物
  没有漂移

### 6.3 第三层：安装验证

最推荐的本地验证方式：

1. 打开 EasyPostman
2. 进入插件管理
3. 选择“安装本地 JAR”
4. 选中刚打出来的插件 JAR
5. 重启应用
6. 验证插件能力是否真的出现

按插件类型，至少要检查：

- Toolbox 面板是否出现
- `pm.xxx` 是否可用
- 脚本补全是否出现
- Snippet 是否出现
- 禁用 / 启用 / 卸载后状态是否正确

### 6.4 第四层：兼容性验证

如果这次改动涉及兼容范围，建议专门验证：

- 不兼容插件在安装阶段是否被拦截
- 兼容插件是否能正常安装和加载
- 插件管理界面的兼容文案是否符合预期
- 市场列表里的安装按钮是否被正确禁用或允许

### 6.5 可选：开发期扫描目录

当前还支持一个开发期附加扫描目录：

- `-DeasyPostman.plugins.dir=/your/path`

它适合：

- 调试额外插件目录
- 企业预置插件目录验证

但对单插件开发来说，最稳定的验证路径仍然是“构建 JAR 后通过插件管理安装”。

## 7. 用户安装和分发方式

虽然这份文档更偏开发者，但用户安装方式最好也统一理解。

当前对用户只保留 2 条路径：

1. 本地安装
   - 直接安装单个插件 JAR
2. 在线安装
   - 通过远程 `catalog.json` 加载并安装

当前官方 catalog：

- GitHub
  - `https://raw.githubusercontent.com/lakernote/easy-postman/master/plugin-catalog/catalog-github.json`
- Gitee
  - `https://gitee.com/lakernote/easy-postman/raw/master/plugin-catalog/catalog-gitee.json`

### 7.1 为什么本地和在线都保留

因为两者解决的是不同问题：

- 本地 JAR
  - 适合开发联调、手工分发、最低心智成本安装
- 在线 catalog
  - 适合长期维护、官方分发、团队内网插件源

### 7.2 当前目录结构的意义

用户机器上默认会看到两个目录：

- `EasyPostman/plugins/installed/`
- `EasyPostman/plugins/packages/`

含义：

- `installed/`
  - 当前用于实际加载的副本
- `packages/`
  - 保留包副本
  - 为重装、升级和某些文件锁场景做缓冲

## 8. 常见问题

### Q1：开发一个插件，最少要写哪些文件

至少这些：

1. `plugin-xxx/pom.xml`
2. `src/main/java/.../XxxPlugin.java`
3. `src/main/resources/META-INF/easy-postman/plugin-xxx.properties`

如果插件有 UI 或脚本 API，再加对应类。

### Q2：更新插件一定要改宿主版本吗

不一定，通常不用。

大多数情况下：

- 只改插件代码
- 只提升插件自己的 `<version>`

只有插件机制本身发生不兼容变化时，才需要改 `plugin.platform.version`。

### Q3：为什么 GitHub Actions 不直接维护兼容性字段

因为兼容性定义应该属于插件自己，而不是 workflow。

当前正确的数据流是：

- 插件 `pom.xml` 定义元数据
- Maven 生成 descriptor
- workflow 从 JAR descriptor 读取并校验
- workflow 更新 catalog

这样不容易出现多处手填漂移。

### Q4：本地验证为什么不推荐直接手拷文件

因为插件管理这条路径会顺带验证：

- JAR 是否有效
- descriptor 是否可读
- 兼容性是否满足
- 安装来源是否被记录

这比手工复制更接近真实用户路径。

### Q5：我应该如何描述这套插件体系

推荐统一成一句话：

> EasyPostman 的插件以单 JAR 为交付单元，通过稳定的插件 API、独立的插件运行时和远程 catalog 机制，实现独立开发、独立升级和独立发布。
