[English](README.md) | 中文

# EasyPostman

> 🚀 一款高仿 Postman + 简易版 JMeter 的开源接口调试与压测工具，专为开发者优化，界面简洁、功能强大，内置 Git 集成，支持团队协作与版本控制。

![GitHub license](https://img.shields.io/github/license/lakernote/easy-postman)
![Java](https://img.shields.io/badge/Java-17+-orange)
![Platform](https://img.shields.io/badge/Platform-Windows%20|%20macOS%20|%20Linux-blue)

## 💡 项目介绍

EasyPostman 致力于为开发者提供媲美 Postman 的本地 API 调试体验，并集成简易版 JMeter 的批量请求与压力测试能力。项目采用
Java Swing 技术栈，支持跨平台运行，无需联网即可使用，保护您的接口数据隐私。同时，内置 Git
工作区功能，支持接口数据的版本管理与团队协作，轻松实现多端同步与协作开发。

### 🔥 开发理念

- **🎯 专注核心功能** - 简洁而不简单，功能丰富而不臃肿
- **🔒 隐私优先** - 本地存储，数据不上云，保护开发者隐私
- **🚀 性能至上** - 原生 Java 应用，启动快速，运行流畅

---

## 🔗 相关链接

- 🌟 GitHub: [https://github.com/lakernote/easy-postman](https://github.com/lakernote/easy-postman)
- 🏠 Gitee: [https://gitee.com/lakernote/easy-postman](https://gitee.com/lakernote/easy-postman)
- 📦 **下载地址**: [https://github.com/lakernote/easy-postman/releases](https://github.com/lakernote/easy-postman/releases)
    - 🌏 国内镜像: [https://gitee.com/lakernote/easy-postman/releases](https://gitee.com/lakernote/easy-postman/releases)
    - 🍎 Mac (Apple Silicon - M1/M2/M3/M4): `EasyPostman-{版本号}-macos-arm64.dmg`
    - 🍏 Mac (Intel 芯片): `EasyPostman-{版本号}-macos-x86_64.dmg`
    - 🪟 Windows: 
        - **MSI 安装包**: `EasyPostman-{版本号}-windows-x64.msi` - 安装到系统，创建桌面快捷方式，支持自动升级
        - **便携版 ZIP**: `EasyPostman-{版本号}-windows-x64-portable.zip` - 解压即用，无需安装，绿色便携
    - 🐧 Ubuntu/Debian: `easypostman_{版本号}_amd64.deb`
    - ☕ 跨平台 JAR: `easy-postman-{版本号}.jar` - 需要 Java 17+ 环境

> ⚠️ **安全提示**:
> 
> **Windows 用户**: 首次运行时，Windows SmartScreen 可能会显示"Windows 已保护你的电脑"警告。这是因为应用未购买代码签名证书（证书费用约 $100-400/年）。本应用完全开源且安全，您可以：
> - **MSI 安装包**：点击"更多信息" → "仍要运行"，安装后支持自动升级
> - **便携版 ZIP**：解压后直接运行 EasyPostman.exe，可能仍会触发 SmartScreen，同样点击"更多信息" → "仍要运行"即可
> - 💡 两种方式安全性相同，SmartScreen 警告会随着下载量增加逐渐消失
> 
> **macOS 用户**: 首次打开时，macOS 可能会提示"无法打开，因为无法验证开发者"。这同样是因为未购买 Apple 开发者证书（$99/年）。本应用安全且开源，解决方法：
> - 方法1：右键点击应用 → 选择"打开" → 在弹窗中点击"打开"
> - 方法2：系统设置 → 隐私与安全性 → 在底部找到被拦截的应用 → 点击"仍要打开"
> - 方法3：终端执行：`sudo xattr -rd com.apple.quarantine /Applications/EasyPostman.app`

- 💬 微信：**lakernote**

---

## ✨ 功能特性

- 🚦 支持常用 HTTP 方法（GET/POST/PUT/DELETE 等）
- 📡 支持 SSE（Server-Sent Events）和 WebSocket 协议
- 🌏 多环境变量管理，轻松切换测试环境
- 🕑 请求历史自动保存，便于回溯与复用
- 📦 批量请求与压力测试（简易版 JMeter），满足多场景需求，支持报告、结果树、趋势图可视化
- 📝 语法高亮请求编辑器
- 🌐 多语言支持（简体中文、英文）
- 💾 本地数据存储，隐私安全
- 📂 支持导入导出 Postman v2.1、curl格式
- 📊 响应结果可视化，支持 JSON/XML 格式
- 🔍 支持请求参数、头部、Cookie 等配置
- 📂 支持文件上传下载
- 📑 支持请求脚本（Pre-request Script、Tests）
- 🔗 支持请求链路（Chaining）
- 🧪 支持网络请求详细事件监控与分析
- 🏢 工作区管理 - 支持本地工作区和Git工作区，实现项目级别的数据隔离与版本控制
- 🔄 Git 集成 - 支持 commit、push、pull 等版本控制操作
- 👥 团队协作 - 通过 Git 工作区实现团队间的接口数据共享

---

## 🖼️ 截图预览

|                                    预览                                    |                                     预览                                     |
|:------------------------------------------------------------------------:|:--------------------------------------------------------------------------:|
|                          ![icon](docs/icon.png)                          |                        ![welcome](docs/welcome.png)                        |
|                          ![home](docs/home.png)                          |                     ![workspaces](docs/workspaces.png)                     |
|                   ![collections](docs/collections.png)                   |             ![collections-import](docs/collections-import.png)             |
|                  ![environments](docs/environments.png)                  |                     ![functional](docs/functional.png)                     |
|                  ![functional_1](docs/functional_1.png)                  |                   ![functional_2](docs/functional_2.png)                   |
|                       ![history](docs/history.png)                       |               ![history-timeline](docs/history-timeline.png)               |
|                ![history-events](docs/history-events.png)                |                     ![networklog](docs/networklog.png)                     |
|                   ![performance](docs/performance.png)                   |             ![performance-report](docs/performance-report.png)             |
|        ![performance-resultTree](docs/performance-resultTree.png)        |              ![performance-trend](docs/performance-trend.png)              |
| ![performance-threadgroup-fixed](docs/performance-threadgroup-fixed.png) | ![performance-threadgroup-rampup](docs/performance-threadgroup-rampup.png) |
| ![performance-threadgroup-spike](docs/performance-threadgroup-spike.png) | ![performance-threadgroup-stairs](docs/performance-threadgroup-stairs.png) |
|                    ![script-pre](docs/script-pre.png)                    |                    ![script-post](docs/script-post.png)                    |
|               ![script-snippets](docs/script-snippets.png)               |           ![workspaces-gitcommit](docs/workspaces-gitcommit.png)           |

---

## 🏗️ 系统架构

```
EasyPostman
├── 🎨 用户界面层 (UI Layer)
│   ├── Workspace 工作区管理
│   ├── Collections 接口集合管理
│   ├── Environments 环境变量配置
│   ├── History 请求历史记录
│   ├── Performance 性能测试模块
│   └── NetworkLog 网络请求监控
├── 🔧 业务逻辑层 (Business Layer)
│   ├── HTTP 请求处理引擎
│   ├── 工作区切换与隔离引擎
│   ├── Git 版本控制引擎
│   ├── 环境变量解析器
│   ├── 脚本执行引擎
│   ├── 数据导入导出模块
│   └── 性能测试执行器
├── 💾 数据访问层 (Data Layer)
│   ├── 工作区存储管理
│   ├── 本地文件存储
│   ├── Git 仓库管理
│   ├── 配置管理
│   └── 历史记录管理
└── 🌐 网络通信层 (Network Layer)
    ├── HTTP/HTTPS 客户端
    ├── WebSocket 客户端
    ├── SSE 客户端
    └── Git 远程仓库通信
```

---

## 🛠️ 技术选型说明

### 核心技术栈

- **Java 17**: 使用最新 LTS 版本，享受现代 Java 特性
- **JavaSwing**: 原生桌面 GUI 框架，跨平台兼容性好
- **jlink & jpackage**: 官方打包工具，生成原生安装包

### UI 组件库

- **FlatLaf**: 现代化 UI 主题，支持暗色模式和高分辨率显示
- **RSyntaxTextArea**: 语法高亮文本编辑器，支持 JSON/XML/JavaScript 等
- **jIconFont-Swing**: 矢量图标字体支持
- **SwingX**: 扩展 Swing 组件库
- **MigLayout**: 强大的布局管理器

### 网络与工具库

- **OkHttp**: 高性能 HTTP 客户端
- **Nashorn/GraalVM**: JavaScript 脚本引擎支持
- **SLF4J + Logback**: 日志框架

---

## 🎯 核心特性详解

### 🏢 工作区管理 - 重大功能更新！

- ✅ 本地工作区：适合个人项目，数据完全本地存储，隐私安全
- ✅ Git工作区：支持版本控制和团队协作的工作区类型
    - 从远程仓库克隆：直接从 GitHub/Gitee 等平台克隆项目
    - 本地初始化：在本地创建 Git 仓库，后续推送到远程
- ✅ 项目级别数据隔离：每个工作区独立管理接口集合、环境变量
- ✅ 工作区快速切换：一键切换不同项目，互不干扰
- ✅ Git操作集成：
    - 提交（Commit）：保存本地变更到版本控制
    - 推送（Push）：将本地提交推送到远程仓库
    - 拉取（Pull）：从远程仓库获取最新变更
    - 冲突检测与智能处理
- ✅ 团队协作支持：通过 Git 工作区实现接口数据的团队共享
- ✅ 多种认证方式：支持用户名密码、Personal Access Token、SSH Key

### 🔌 接口调试功能

- ✅ 支持 HTTP/1.1 和 HTTP/2 协议
- ✅ 完整的 REST API 方法支持（GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS）
- ✅ 多种请求体格式：Form Data、x-www-form-urlencoded、JSON、XML、Binary
- ✅ 文件上传下载支持（支持拖拽）
- ✅ Cookie 自动管理和手动编辑
- ✅ 请求头、查询参数可视化编辑
- ✅ 响应数据格式化显示（JSON、XML、HTML）
- ✅ 响应时间、状态码、大小统计

### 🌍 环境管理

- ✅ 多环境快速切换（开发/测试/生产）
- ✅ 全局变量和环境变量支持
- ✅ 变量嵌套引用：`{{baseUrl}}/api/{{version}}`
- ✅ 动态变量：`{{$timestamp}}`、`{{$randomInt}}`
- ✅ 环境变量导入导出

### 📝 脚本支持

- ✅ Pre-request Script：请求前执行脚本
- ✅ Tests Script：响应后测试脚本
- ✅ 内置代码片段库
- ✅ JavaScript 运行时环境
- ✅ 断言测试支持

### ⚡ 性能测试

- ✅ 多种线程组模式：
    - 固定线程数：稳定负载测试
    - 递增式：逐步增加负载
    - 阶梯式：分阶段负载测试
    - 尖刺式：突发流量测试
- ✅ 实时性能监控
- ✅ 详细测试报告（响应时间、TPS、错误率）
- ✅ 结果树分析
- ✅ 性能趋势图表

### 📊 数据分析

- ✅ 请求历史时间线
- ✅ 网络事件详细日志
- ✅ 响应数据统计分析
- ✅ 错误请求自动分类

### 🔄 数据迁移

- ✅ Postman Collection v2.1 导入
- ✅ cURL 命令导入
- ✅ HAR 文件导入（开发中）
- ✅ OpenAPI/Swagger 导入（开发中）

---

## 🚀 快速开始

### 环境要求

- Java 17 或更高版本
- 内存：至少 512MB 可用内存
- 磁盘：至少 100MB 可用空间

### 从源码构建

```bash
# 克隆项目
git clone https://gitee.com/lakernote/easy-postman.git
cd easy-postman

# 或者打包后运行
mvn clean package
java -jar target/easy-postman-*.jar
```

### 生成安装包

```bash
# macOS
chmod +x build/mac.sh
./build/mac.sh

# Windows
build/win.bat
```

---

## 📖 使用指南

### 0️⃣ 工作区管理（新功能！）

#### 创建工作区

1. 点击左侧 **Workspace** 选项卡
2. 点击 **+ 新建** 按钮
3. 选择工作区类型：
    - **本地工作区**：适合个人项目，数据仅存储在本地
    - **Git 工作区**：支持版本控制和团队协作
4. 填写工作区名称、描述和存储路径
5. 如果选择 Git 工作区，配置 Git 相关信息：
    - **从远程克隆**：输入 Git 仓库 URL 和认证信息
    - **本地初始化**：创建本地 Git 仓库，后续可配置远程仓库

#### 工作区协作流程

1. **团队领导者**：
    - 创建 Git 工作区（从远程克隆或本地初始化）
    - 配置接口集合和环境变量
    - 提交并推送到远程仓库
2. **团队成员**：
    - 创建 Git 工作区（从远程克隆）
    - 获取最新的接口数据和环境配置
    - 本地修改后提交并推送更新
3. **日常协作**：
    - 开始工作前：先执行 **Pull** 拉取最新变更
    - 完成修改后：执行 **Commit** 提交本地变更
    - 分享更新：执行 **Push** 推送到远程仓库

### 1️⃣ 创建第一个请求

1. 点击 **Collections** 选项卡
2. 右键创建新的集合和请求
3. 输入 URL 和选择 HTTP 方法
4. 配置请求参数、头部信息
5. 点击 **Send** 发送请求

### 2️⃣ 环境变量配置

1. 点击 **Environments** 选项卡
2. 创建新环境（如：dev、test、prod）
3. 添加变量：如 `baseUrl = https://api.example.com`
4. 在请求中使用：`{{baseUrl}}/users`

### 3️⃣ 性能测试

1. 点击 **Performance** 选项卡
2. 配置线程组参数
3. 添加要测试的接口
4. 启动测试并查看实时报告

---

## 🤝 贡献指南

我们欢迎任何形式的贡献！

### 贡献方式

1. 🐛 **Bug 报告**: [提交 Issue](https://gitee.com/lakernote/easy-postman/issues)
2. 💡 **功能建议**: [功能请求](https://gitee.com/lakernote/easy-postman/issues)
3. 📝 **代码贡献**: Fork -> 修改 -> Pull Request
4. 📚 **文档改进**: 完善 README、Wiki 等文档

### 开发规范

- 遵循 Java 编码规范
- 提交前请运行测试：`mvn test`
- 提交信息，格式：`feat: 添加新功能` 或 `fix: 修复bug`

---

## ❓ 常见问题

### Q: 为什么选择本地存储而不是云同步？

A: 我们重视开发者的隐私安全，本地存储可以确保您的接口数据不会泄露给第三方。

### Q: 如何导入 Postman 数据？

A: 在 Collections 界面点击导入按钮，选择 Postman v2.1 格式的 JSON 文件即可。

### Q: 性能测试结果准确吗？

A: 基于 Java 多线程实现，测试结果具有一定参考价值，但建议与专业压测工具结果进行对比验证。

### Q: 支持团队协作吗？

A: ✅ **现已支持！** 通过 Git 工作区功能，团队可以共享接口集合、环境变量等数据，实现真正的团队协作。

### Q: 工作区之间的数据会相互影响吗？

A: 不会。每个工作区都是完全独立的，拥有自己的接口集合、环境变量等，切换工作区时数据完全隔离。

### Q: Git 工作区支持哪些平台？

A: 支持所有标准的 Git 平台，包括 GitHub、Gitee、GitLab、自建 Git 服务器等，只要提供标准的 Git URL 即可。

### Q: 如何解决 Git 操作冲突？

A: 系统内置了冲突检测机制，在执行 Git 操作前会检查潜在冲突并提供解决方案，如自动提交本地变更、暂存变更等。

### Q: 可以在不同设备间同步工作区吗？

A: 可以！通过 Git 工作区功能，你可以在不同设备上克隆同一个远程仓库，实现跨设备的数据同步。

---

## 💖 支持项目

如果这个项目对您有帮助，欢迎：

- ⭐ 给项目点个 Star
- 🍴 Fork 项目参与贡献
- 📢 向朋友推荐本项目
- ☕ 请作者喝杯咖啡

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=lakernote/easy-postman&type=date&legend=top-left)](https://www.star-history.com/#lakernote/easy-postman&type=date&legend=top-left)

---

## 🙏 致谢

感谢以下开源项目的支持：

- [FlatLaf](https://github.com/JFormDesigner/FlatLaf) - 现代化 Swing 主题
- [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) - 语法高亮编辑器
- [OkHttp](https://github.com/square/okhttp) - HTTP 客户端

---

<div align="center">

**让 API 调试更简单，让性能测试更直观**

Made with ❤️ by [laker](https://github.com/lakernote)

</div>
