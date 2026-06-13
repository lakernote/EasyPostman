<div align="center">

<img src="docs/icon.png" alt="EasyPostman Logo" width="100" />

# EasyPostman

**开源 Postman 风格接口调试 + JMeter 风格性能测试桌面工具**<br>
*高仿 Postman 交互体验 · JMeter 风格压测能力 · Java 桌面端 · 本地优先*

[![GitHub license](https://img.shields.io/github/license/lakernote/easy-postman?style=flat-square)](https://github.com/lakernote/easy-postman/blob/main/LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/lakernote/easy-postman?style=flat-square&color=brightgreen)](https://github.com/lakernote/easy-postman/releases)
[![GitHub stars](https://img.shields.io/github/stars/lakernote/easy-postman?style=flat-square&color=yellow)](https://github.com/lakernote/easy-postman/stargazers)
[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-0078D4?style=flat-square&logo=windows&logoColor=white)](https://github.com/lakernote/easy-postman/releases)

[![GitHub](https://img.shields.io/badge/GitHub-lakernote-0969DA?style=flat-square&logo=github&logoColor=white)](https://github.com/lakernote)
[![Gitee](https://img.shields.io/badge/Gitee-lakernote-C71D23?style=flat-square&logo=gitee)](https://gitee.com/lakernote)

[English](README.md) · [简体中文](README_zh.md) · [📦 下载](https://github.com/lakernote/easy-postman/releases) · [📖 文档](docs/FEATURES_zh.md) · [💬 讨论区](https://github.com/lakernote/easy-postman/discussions) · 微信：`lakernote`

</div>

---

## 📖 目录

- [💡 项目简介](#-项目简介)
- [🖼️ 视觉导览](#️-视觉导览)
- [🧭 典型使用场景](#-典型使用场景)
- [✨ 功能特性](#-功能特性)
- [📦 下载](#-下载)
- [🚀 快速开始](#-快速开始)
- [🏗️ 项目结构](#️-项目结构)
- [🛠️ 开发指南](#️-开发指南)
- [🖼️ 截图预览](#️-截图预览)
- [🤝 贡献指南](#-贡献指南)
- [🎨 第三方资源](#-第三方资源)
- [📚 文档](#-文档)
- [❓ 常见问题](#-常见问题)
- [💖 支持项目](#-支持项目)

---

## 💡 项目简介

EasyPostman 的核心亮点是把**高仿 Postman 的接口调试体验**和**JMeter 风格性能测试能力**放到一个本地优先的桌面应用里。项目基于 Java 17、Swing 和 FlatLaf 构建，默认把数据保存在本地；当团队需要同步、评审和版本控制时，可以通过 Git 工作区协作，不依赖托管云服务。

| 🎯 高仿 Postman 调试 | ⚡ JMeter 风格压测 | 🔒 本地优先桌面端 |
|:---:|:---:|:---:|
| 集合、环境、认证、脚本、导入、历史和响应查看 | 线程组、定时器、提取器、断言、实时指标、报告和分布式执行 | 除非主动选择 Git 工作区，否则接口和测试数据只保存在本机 |

---

## 🖼️ 视觉导览

EasyPostman 是 GUI 优先的桌面工具，项目价值要同时展示两条主线：高仿 Postman 的接口调试，以及 JMeter 风格的压测编排。下面截图均来自当前桌面应用。

<div align="center">

| Postman 风格接口调试 | JMeter 风格性能测试 |
|:--------------------:|:-------------------:|
| ![接口集合和响应查看器](docs/collections.png) | ![性能趋势面板](docs/performance-trend.png) |

| 脚本、断言 & 代码片段 | Git 工作区协作 |
|:----------------------:|:---------------:|
| ![脚本代码片段和编辑器支持](docs/script-snippets.png) | ![Git 工作区管理](docs/workspaces-gitcommit.png) |

</div>

📸 **[查看完整截图集 →](docs/SCREENSHOTS_zh.md)**

---

## 🧭 典型使用场景

| 场景 | 实际使用方式 |
|------|--------------|
| **像 Postman 一样调试 REST API** | 创建或导入集合，选择环境，发送请求，查看格式化响应体、headers、cookies、耗时和网络事件日志。 |
| **用脚本串联请求** | 使用请求前脚本和测试脚本读取变量、生成签名、提取响应数据、执行断言，并把值传给后续请求。 |
| **通过 Git 协作接口资产** | 工作区数据保存在本地，通过 Git 工作区提交、拉取、推送和审查集合/环境变更。 |
| **像 JMeter 一样编排压测** | 在界面中编排压测计划，导出 `plan.json`，再用无头 CLI 或 master/worker 模式执行，同时保持全局用户和 CSV 分片规则。 |

---

## ✨ 功能特性

### 🏢 工作区 & 协作
- **本地工作区** - 个人 API 项目完全保存在本机磁盘
- **Git 工作区** - 通过自己的 Git 仓库提交、拉取、推送和共享集合/环境
- **工作区隔离** - 每个工作区独立保存集合、环境、设置和历史记录
- **便携模式** - 通过 `.portable` 标记或系统属性让数据跟随应用目录

### 🔌 Postman 风格接口测试
- **HTTP/HTTPS** - 支持 headers、params、cookies、auth、redirects 和请求体编辑的 REST 调试
- **SSE & WebSocket** - 支持流式响应和实时协议场景
- **多种请求体** - Form Data、x-www-form-urlencoded、JSON、XML、文本和二进制
- **变量体系** - 支持环境、全局、请求和迭代数据，让请求和压测可重复执行
- **导入导出** - 支持 Postman v2.1、cURL，HAR 和 OpenAPI/Swagger 路径持续完善中

### ⚡ JMeter 风格性能测试
- **GUI 场景编排** - 线程组、定时器、提取器、断言和结果视图
- **线程组模式** - 固定、递增、阶梯、尖刺
- **实时监控** - TPS/QPS、响应时间、错误率、趋势图和结果树
- **无头与分布式压测** - GUI 导出 `plan.json` 后，可在服务器上用 CLI 或 master/worker 模式执行
- **总并发分片** - GUI 虚拟用户数就是全局总并发，workers 按连续区间分摊，CSV 行也跟随同一区间避免重复读取

### 🧩 脚本、断言 & 插件
- **请求前和测试脚本** - Postman 风格 `pm` API、断言、变量和请求链路
- **内置 JS 辅助库** - `crypto-js`、`lodash`、`moment`
- **脚本扩展点** - 插件可注册脚本 API、补全、代码片段、工具箱面板和服务
- **官方插件** - 插件管理、客户端证书、抓包代理、Redis、Kafka、Java 反编译
- **网络事件日志** - 详细的请求/响应和流式协议诊断

### 🎨 用户体验
- **亮色暗色模式** - 任何光线下舒适观看
- **多语言** - 中文、English
- **语法高亮** - JSON、XML、JavaScript
- **跨平台** - Windows、macOS、Linux

📖 **[查看所有功能 →](docs/FEATURES_zh.md)**

---

## 📦 下载

### 最新版本

🔗 **[GitHub Releases](https://github.com/lakernote/easy-postman/releases)** | **[Gitee 镜像（国内）](https://gitee.com/lakernote/easy-postman/releases)**

### 平台下载

| 平台 | 安装包 | 说明 |
|------|--------|------|
| 🍎 **macOS (Apple Silicon)** | `EasyPostman-{版本号}-macos-arm64.dmg` | M1/M2/M3/M4 |
| 🍏 **macOS (Intel)** | `EasyPostman-{版本号}-macos-x86_64.dmg` | Intel Mac |
| 🪟 **Windows (安装版)** | `EasyPostman-{版本号}-windows-x64.exe` | 支持自动更新 |
| 🪟 **Windows (便携版)** | `EasyPostman-{版本号}-windows-x64-portable.zip` | 解压即用 |
| 🐧 **Linux AMD64（通用）** | `EasyPostman-{版本号}-linux-amd64.deb` | 适用于常见 `x86_64` / `amd64` Linux 系统 |
| 🐧 **Linux ARM64（通用）** | `EasyPostman-{版本号}-linux-arm64.deb` | 适用于常见 `aarch64` / `arm64` Linux 系统 |
| 🐧 **Linux ARM64（兼容版）** | `EasyPostman-{版本号}-linux-arm64-compat.deb` | 与通用 ARM64 包内容相同，仅为旧版 Debian / Ubuntu `dpkg` 环境重打包 |
| 🐧 **RHEL / Rocky / CentOS / Fedora（x64）** | `EasyPostman-{版本号}-1.x86_64.rpm` | 仅 GitHub Releases 提供 |
| 🐧 **RHEL / Rocky / CentOS / Fedora（ARM64）** | `EasyPostman-{版本号}-1.aarch64.rpm` | 仅 GitHub Releases 提供 |
| ☕ **跨平台 JAR** | `easy-postman-{版本号}.jar` | 需要 Java 17+ |

> 🐧 **ARM64 兼容版 DEB 的作用**
>
> 兼容版和 `linux-arm64.deb` 包含同一个 EasyPostman 应用和同一套运行时，不是功能增强版，也不是旧版本。它只是把 DEB 包内部成员改为 xz 压缩格式，方便旧版 `dpkg` 安装；这些旧环境可能无法识别 `control.tar.zst` 或 `data.tar.zst` 等较新的压缩格式。正常情况下优先下载 `linux-arm64.deb`，只有当通用包因为 DEB 压缩格式兼容问题安装失败时，再改用 `linux-arm64-compat.deb`。

> ⚠️ **首次运行提示**
>
> - **Windows**：SmartScreen 警告 → "更多信息" → "仍要运行"
> - **macOS**：提示"无法打开" → 右键 → "打开" → "打开"
>
> 本应用完全开源，这些警告是因为未购买代码签名证书。

> 🌏 **Gitee 镜像** 仅提供 macOS（ARM）DMG 和 Windows 包。Linux 的 DEB / RPM 安装包仅发布在 GitHub Releases。

---

## 🚀 快速开始

### 方式一：下载预编译版本

1. 从 [Releases](https://github.com/lakernote/easy-postman/releases) 下载适合您平台的安装包
2. 安装并运行：

| 平台 | 操作 |
|------|------|
| macOS | 打开 DMG → 拖拽到应用程序 |
| Windows 安装版 | 运行 `.exe`，按向导操作 |
| Windows 便携版 | 解压 ZIP → 运行 `EasyPostman.exe` |
| Linux DEB（AMD64 通用） | `sudo dpkg -i EasyPostman-{版本号}-linux-amd64.deb` |
| Linux DEB（ARM64 通用） | `sudo dpkg -i EasyPostman-{版本号}-linux-arm64.deb` |
| Linux DEB（ARM64 兼容版） | `sudo dpkg -i EasyPostman-{版本号}-linux-arm64-compat.deb` |
| Linux RPM（x64） | `sudo rpm -ivh EasyPostman-{版本号}-1.x86_64.rpm` |
| Linux RPM（ARM64） | `sudo rpm -ivh EasyPostman-{版本号}-1.aarch64.rpm` |
| JAR | `java -jar easy-postman-{版本号}.jar` |

如果不确定该下载哪个 Linux 安装包，先执行 `uname -m`：

- `x86_64` -> 选择 `EasyPostman-{版本号}-linux-amd64.deb` 或 `x86_64.rpm`
- `aarch64` -> 选择 `EasyPostman-{版本号}-linux-arm64.deb`
- 如果安装通用 ARM64 包时，`dpkg` 提示不支持某种归档压缩格式 -> 改用 `EasyPostman-{版本号}-linux-arm64-compat.deb`

### 方式二：从源码构建

```bash
git clone https://github.com/lakernote/easy-postman.git
cd easy-postman
mvn -pl easy-postman-app -am -DskipTests clean package
java -jar easy-postman-app/target/easy-postman-*.jar
```

📖 **[构建指南 →](docs/BUILD_zh.md)**<br>
🔌 **[插件架构与安装 →](docs/PLUGINS_zh.md)**

### 第一步

1. **创建工作区** — 本地（个人）或 Git（团队协作）
2. **创建集合** — 组织您的 API 请求
3. **发送第一个请求** — 输入 URL，配置参数，点击发送
4. **设置环境** — 轻松切换开发 / 测试 / 生产环境

---

## 🏗️ 项目结构

EasyPostman 是 Java 17 + Maven 多模块项目。入口类是 `com.laker.postman.App`；GUI 启动分支会初始化主题/字体、自定义 IOC 容器、插件运行时和 `MainFrame`，性能测试 CLI 则走无头启动分支。

```text
easy-postman-parent
├── easy-postman-foundation
├── easy-postman-request-core
├── easy-postman-http-runtime
├── easy-postman-collection-core
├── easy-postman-plugin-api
├── easy-postman-platform
├── easy-postman-performance-core
├── easy-postman-ui
├── easy-postman-plugin-runtime
├── easy-postman-plugins/
└── easy-postman-app
```

| 模块 | 职责 |
|------|------|
| `easy-postman-foundation` | 最底层非 UI 基础层：常量、路径、JSON、系统工具、用户偏好、国际化和共享模型 |
| `easy-postman-request-core` | 无头请求规格模型：请求、headers、params、body rows、cookies、auth、redirects 和 saved responses |
| `easy-postman-http-runtime` | UI 中立 HTTP 传输运行时：prepared request、response、OkHttp adapter、SSL、SSE、Cookie、redirect、运行设置和观测端口 |
| `easy-postman-collection-core` | 集合领域模型和中立的 Postman collection 解析 |
| `easy-postman-plugin-api` | 宿主和插件共享的稳定插件 SPI 与服务契约 |
| `easy-postman-platform` | 宿主平台框架，当前包括自定义 IOC 和更新发现核心 |
| `easy-postman-performance-core` | 无头压测计划、运行时契约、统计、worker assignment 和报告快照 |
| `easy-postman-ui` | 共享 Swing 设计系统：字体、图标、语义色、通用控件、编辑器主题和 UI 单例基类 |
| `easy-postman-plugin-runtime` | 插件扫描、类加载、descriptor 解析、registry、生命周期和禁用/卸载状态 |
| `easy-postman-plugins/*` | 官方插件 JAR：manager、client certificate、capture、Redis、Kafka、decompiler |
| `easy-postman-app` | 宿主应用组装层：入口、主框架、具体面板、菜单、启动 wiring、设置/更新 UX 和 app 侧适配器 |

更完整的模块归属规则见 [模块边界规范](docs/ARCHITECTURE_MODULES_zh.md)。

---

## 🛠️ 开发指南

### 常用命令

| 任务 | 命令 |
|------|------|
| 全量打包，跳过测试 | `mvn clean package -DskipTests` |
| 快速打包宿主应用 | `mvn -pl easy-postman-app -am -DskipTests clean package` |
| 快速编译检查 | `mvn -q -pl easy-postman-app -am -DskipTests compile` |
| 构建宿主应用和单个插件 | `mvn -pl easy-postman-app,easy-postman-plugins/plugin-redis -am clean package -DskipTests` |
| 无头运行指定测试类 | `mvn -q -pl easy-postman-app -am -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test` |

宿主 JAR 输出到 `easy-postman-app/target/easy-postman-{版本号}.jar`。原生安装包脚本位于 `build/`，通过 `jpackage` 生成各平台安装包。

### 架构约束

- EasyPostman 使用 `com.laker.postman.ioc` 下的自定义 IOC 注解，不引入 Spring 注解。
- 共享非 UI 工具放 `foundation`；请求 DTO 放 `request-core`；传输执行放 `http-runtime`；共享 Swing 组件放 `ui`；插件契约放 `plugin-api`；插件加载放 `plugin-runtime`；具体宿主页面和组装放 `app`。
- 用户可见文案应走对应模块的 i18n bundle，不直接硬编码。
- 官方普通插件依赖 plugin API 和共享模块，不反向依赖 `easy-postman-app`。

---

## 🖼️ 截图预览

上面的视觉导览展示了核心界面。完整截图集还包含首页、工作区、集合、环境、功能测试、脚本、历史、网络日志、亮暗主题和多种性能测试视图。

📸 **[查看所有截图 →](docs/SCREENSHOTS_zh.md)**

---

## 🤝 贡献指南

我们欢迎任何形式的贡献 — Bug 报告、功能建议、代码或文档！

| 类型 | 方式 |
|------|------|
| 🐛 报告 Bug | [提交 Issue](https://github.com/lakernote/easy-postman/issues/new/choose) |
| ✨ 功能建议 | [功能请求](https://github.com/lakernote/easy-postman/issues/new/choose) |
| 💻 提交代码 | Fork → 分支 → PR |
| 📝 改进文档 | 修正错别字、添加示例、翻译 |

每个 PR 都会自动触发：构建检查、测试执行、代码质量验证和格式校验。

📖 **[贡献指南 →](.github/CONTRIBUTING.md)**

---

## 🎨 第三方资源

SVG 图标来自 [Lucide](https://lucide.dev/) / [lucide-icons/lucide](https://github.com/lucide-icons/lucide)，许可为 [ISC License](https://lucide.dev/license)。

---

## 📚 文档

| 文档 | 说明 |
|------|------|
| 📖 [功能详细说明](docs/FEATURES_zh.md) | 全面的功能文档 |
| 🏗️ [系统架构](docs/ARCHITECTURE_zh.md) | 技术栈和架构设计 |
| 🧱 [模块边界规范](docs/ARCHITECTURE_MODULES_zh.md) | Maven 模块职责和代码归属规则 |
| 🚀 [构建指南](docs/BUILD_zh.md) | 从源码构建和生成安装包 |
| ⚡ [集群压测使用指南](docs/PERFORMANCE_CLUSTER_LOAD_TEST_zh.md) | GUI 远程模式、CLI master/worker、CSV 分片、实时刷新和结果明细 |
| 🔌 [插件架构与安装](docs/PLUGINS_zh.md) | 插件模块、开发流程、在线/离线安装 |
| 🖼️ [截图展示](docs/SCREENSHOTS_zh.md) | 所有应用截图 |
| 📝 [脚本 API 参考](docs/SCRIPT_API_REFERENCE_zh.md) | 请求前和测试脚本 API，含 Redis/Kafka/ES/InfluxDB |
| 📝 [脚本代码片段速查](docs/SCRIPT_SNIPPETS_QUICK_REFERENCE.md) | 内置代码片段，含数据源读写和断言示例 |
| 🔐 [客户端证书](docs/CLIENT_CERTIFICATES.md) | mTLS 配置 |
| 🐧 [Linux 构建指南](docs/LINUX_BUILD.md) | 在 Linux 上构建 |
| ❓ [常见问题](docs/FQA.MD) | 常见问题解答 |

---

## ❓ 常见问题

<details>
<summary><b>Q: 为什么选择本地存储而不是云同步？</b></summary>

我们重视开发者的隐私安全。本地存储确保您的接口数据不会泄露给第三方。您可以选择使用 Git 工作区进行团队协作，同时保持对数据的完全控制。
</details>

<details>
<summary><b>Q: 如何导入 Postman 数据？</b></summary>

在 Collections 界面点击 **导入** 按钮，选择 Postman v2.1 格式的 JSON 文件即可。工具会自动转换集合、请求和环境变量。
</details>

<details>
<summary><b>Q: 为什么 Windows/macOS 提示安全警告？</b></summary>

- **Windows SmartScreen**：未购买代码签名证书（约 $100–400/年）。→ 点击"更多信息" → "仍要运行"，随下载量增加警告会逐渐减少。
- **macOS Gatekeeper**：未购买 Apple 开发者证书（$99/年）。→ 右键"打开"，或终端执行：`sudo xattr -rd com.apple.quarantine /Applications/EasyPostman.app`

本项目**完全开源**，代码可在 GitHub 审查。
</details>

<details>
<summary><b>Q: 支持团队协作吗？</b></summary>

✅ 支持！使用 **Git 工作区**可以：共享接口集合和环境变量、版本控制跟踪变更（commit/push/pull）、跨多设备协作，无需任何云服务。
</details>

<details>
<summary><b>Q: 工作区之间的数据会相互影响吗？</b></summary>

不会。每个工作区完全独立，拥有自己的集合、环境变量和历史记录，切换时数据完全隔离。
</details>

<details>
<summary><b>Q: Git 工作区支持哪些平台？</b></summary>

支持所有标准 Git 平台：GitHub、Gitee、GitLab、Bitbucket 及自建 Git 服务器（HTTPS 或 SSH）。
</details>

---

## 💖 支持项目

如果 EasyPostman 对您有帮助：

- ⭐ **给项目点个 Star** — 这对我们很重要！
- 🍴 **Fork 并贡献** — 帮助改进项目
- 📢 **向朋友推荐** — 传播好工具
- 💬 **加入微信群** — 添加 **lakernote** 直接交流
- 💬 **GitHub 讨论区** — [提问和分享想法](https://github.com/lakernote/easy-postman/discussions)
- 📮 **联系我** — 微信：`lakernote`

---

## ⭐ Star History

<div align="center">

[![Star History Chart](https://api.star-history.com/svg?repos=lakernote/easy-postman&type=date&legend=top-left)](https://www.star-history.com/#lakernote/easy-postman&type=date&legend=top-left)

</div>

---

## 🙏 致谢

感谢以下优秀的开源项目：

| 项目 | 用途 |
|------|------|
| [FlatLaf](https://github.com/JFormDesigner/FlatLaf) | 现代化 Swing 主题 |
| [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) | 语法高亮编辑器 |
| [OkHttp](https://github.com/square/okhttp) | HTTP 客户端 |
| [Termora](https://github.com/TermoraDev/termora) | 终端模拟器灵感来源 |

---

<div align="center">

**让接口调试像 Postman 一样顺手，让性能测试像 JMeter 一样完整**

[![GitHub](https://img.shields.io/badge/GitHub-lakernote-0969DA?style=flat-square&logo=github&logoColor=white)](https://github.com/lakernote)
[![Gitee](https://img.shields.io/badge/Gitee-lakernote-C71D23?style=flat-square&logo=gitee)](https://gitee.com/lakernote)

Made with ❤️ by [laker](https://github.com/lakernote)

</div>
