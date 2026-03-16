# EasyPostman 插件架构与使用说明

这份文档是当前插件体系的统一入口，覆盖：

- 整体插件架构
- 模块目录说明
- 本地开发与调试
- 在线安装、离线安装、直接安装
- 官方插件分发建议

## 1. 先看整体结构

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
    └── plugin-decompiler
```

职责边界：

- `easy-postman-plugin-api`
  放稳定扩展接口和插件上下文，给插件实现依赖。
- `easy-postman-plugin-runtime`
  负责插件扫描、加载、注册、生命周期管理。
- `easy-postman-app`
  宿主程序，只保留核心能力和插件桥接点。
- `easy-postman-plugins`
  官方插件聚合目录，下面每个 `plugin-*` 都是独立 Maven 模块。

## 2. 为什么这样拆

目标很直接：

- 主包更轻，用户默认下载成本更低
- 可选能力按需安装，不再强绑到主包
- 每个插件都可以单独构建、打包、分发
- 本地开发和验证有统一入口，不再散在多个脚本和文档里

已经拆出去的能力：

- `plugin-redis`: Redis 面板、`pm.redis`、补全、Snippet
- `plugin-kafka`: Kafka 面板、`pm.kafka`、补全、Snippet
- `plugin-git`: Git 工作区能力
- `plugin-decompiler`: CFR 反编译工具面板

## 3. 运行时是怎么工作的

插件运行链路大致是：

```text
插件 jar / catalog
  -> plugin-manager 解析 catalog / 安装包
  -> runtime 扫描 plugins/installed
  -> 加载插件 descriptor
  -> 创建插件实例
  -> 通过 PluginContext 注册扩展和服务
  -> app 通过桥接层消费插件能力
```

关键点：

- 插件管理支持 `HTTP(S)`、`file://`、本地目录、本地 `catalog.json`
- 在线安装和离线安装走同一套 catalog 解析逻辑
- 卸载默认只删 `installed` 副本，不删缓存包
- 宿主对可选插件做降级处理，没装插件时隐藏入口，不再抛错误

## 4. 目录和产物怎么理解

以 `plugin-git` 为例：

```text
easy-postman-plugins/plugin-git
├── pom.xml
├── src/main/java
├── src/main/resources/META-INF/easy-postman/plugin-git.properties
└── target
    ├── easy-postman-4.3.55-plugin-git.jar
    └── plugin-market/offline/easy-postman-plugin-git/
        ├── catalog.json
        └── easy-postman-4.3.55-plugin-git.jar
```

产物说明：

- `target/easy-postman-<version>-plugin-*.jar`
  适合直接安装。
- `target/plugin-market/offline/easy-postman-plugin-*/`
  适合离线分发，整个目录直接给用户即可。

## 5. 本地开发怎么做

### 5.1 最常用命令

统一用：

```bash
./scripts/plugin-dev.sh
```

支持的插件：

```bash
./scripts/plugin-dev.sh list
```

当前输出：

- `redis`
- `kafka`
- `git`
- `decompiler`

### 5.2 只构建一个插件

```bash
./scripts/plugin-dev.sh build redis
./scripts/plugin-dev.sh build git
```

这会一起构建：

- `easy-postman-app`
- 指定插件模块
- 依赖到的插件平台模块

如果要一次性构建全部插件：

```bash
./scripts/plugin-dev.sh build all
```

### 5.3 本地验证安装链路

推荐流程：

```bash
./scripts/plugin-dev.sh prepare redis
./scripts/plugin-dev.sh run-clean redis
```

`prepare` 会做几件事：

- 构建 app + 目标插件
- 校验主包和插件包边界
- 生成本地 `catalog.json`
- 生成一个隔离的数据目录
- 生成 macOS 双击可用的 `.command` 辅助脚本

生成位置示例：

```text
temp/plugin-dev/redis/
├── artifacts/
├── catalog.json
├── easy-postman-plugin-redis.sha256.txt
├── launch-clean.command
├── install-plugin.command
├── launch-with-plugin.command
└── reset-verify.command
```

## 6. 本地调试时怎么装插件

EasyPostman 现在有 3 种低成本安装方式，用户和开发者都能共用。

### 6.1 方式一：直接安装本地 jar

适合：

- 开发联调
- 用户拿到单个插件包快速安装

操作：

1. 打开插件管理
2. 选择本地插件 JAR
3. 选择 `easy-postman-<version>-plugin-*.jar`
4. 安装后重启应用

脚本辅助命令：

```bash
./scripts/plugin-dev.sh install-direct redis
./scripts/plugin-dev.sh run-with-plugin redis
```

### 6.2 方式二：离线安装

适合：

- 公司内网
- 不能访问公网
- 想把插件包和 catalog 一起交付给用户

操作：

1. 构建插件
2. 找到离线目录：

```text
easy-postman-plugins/plugin-redis/target/plugin-market/offline/easy-postman-plugin-redis/
```

3. 把整个目录发给用户
4. 用户在插件管理里选择：
   - 这个目录
   - 或目录里的 `catalog.json`
5. 安装后重启应用

这种方式对用户最省心，因为：

- 不需要自己拼 URL
- 不需要自己计算校验值
- `catalog.json` 已经和 jar 放在一起

### 6.3 方式三：在线安装

适合：

- 官方插件市场
- 团队内部 HTTP 插件源
- GitHub Release + raw catalog 场景

操作：

1. 提供一个 `catalog.json` URL
2. 在插件管理里填入这个 URL
3. 选择插件安装
4. 重启应用

catalog 的 `downloadUrl` 可以是：

- `https://...`
- `file://...`
- 相对路径

相对路径的好处是：离线目录和在线目录可以复用同一份 catalog 结构。

## 7. 推荐给用户的最小心智负担方案

从降低用户使用成本看，推荐优先级是：

1. 直接安装单个插件 jar
2. 提供离线目录
3. 提供在线 catalog URL

原因：

- 普通用户最容易理解的是“选一个 jar 安装”
- 企业内网最容易落地的是“给一个完整离线目录”
- 在线 catalog 更适合长期托管和自动更新

如果你要发布官方插件，建议这样发：

- GitHub Release 附件里放插件 jar
- 同时放离线目录 zip
- 文档里给一个公开 catalog URL

这样覆盖三类用户：

- 想直接点 jar 的用户
- 内网离线用户
- 想持续在线更新的用户

## 8. 本地开发推荐流程

### 开发插件代码

```bash
./scripts/plugin-dev.sh build git
```

### 验证插件市场安装

```bash
./scripts/plugin-dev.sh prepare git
./scripts/plugin-dev.sh run-clean git
```

然后在应用里从本地 catalog 安装。

### 验证直接安装

```bash
./scripts/plugin-dev.sh run-with-plugin git
```

### 清理隔离数据

```bash
./scripts/plugin-dev.sh reset git
```

## 9. 现有目录怎么分工

为了不再分散，约定如下：

- `build/`
  只放发行打包脚本，服务于 DMG / EXE / DEB / RPM。
- `scripts/`
  只放本地开发、调试、验证辅助脚本。
- `docs/PLUGINS_zh.md`
  作为插件主文档。
- `docs/BUILD_zh.md`
  只讲源码构建和发行打包。

## 10. 常见问题

### Q1：本地开发时需要先 install 吗

不一定。优先从根工程 reactor 构建：

```bash
mvn clean compile -f pom.xml
```

或者：

```bash
./scripts/plugin-dev.sh build redis
```

只有在你强行单独跑某个子模块时，才需要先 `install`。

### Q2：卸载会不会把插件包也删掉

默认不会。

当前策略是：

- `installed/` 放已安装副本
- `cache/` 放缓存包

卸载时只删除已安装副本，缓存包保留，便于后续重新安装。

### Q3：为什么有些功能没了

如果某块功能已经插件化，而当前没有安装对应插件，宿主会隐藏入口或降级处理。

例如：

- 没装 `plugin-git`，Git 工作区入口不会显示
- 没装 `plugin-decompiler`，Toolbox 里不会显示反编译器
