# GitHub 配置总结

本文档说明了为 EasyPostman 项目添加的 GitHub 配置文件。

---

## 📁 文件结构

```
.github/
├── workflows/                          # GitHub Actions 工作流
│   ├── pr-check.yml                   # PR 自动检查
│   ├── welcome.yml                    # 欢迎新贡献者
│   ├── auto-label.yml                 # 自动标签
│   ├── sync-labels.yml                # 同步标签定义
│   ├── release.yml                    # 发布构建（多平台）
│   └── codeql-analysis.yml            # CodeQL 代码安全分析
├── ISSUE_TEMPLATE/                    # Issue 模板
│   ├── bug_report.yml                 # Bug 报告模板
│   ├── feature_request.yml            # 功能请求模板
│   ├── question.yml                   # 问题咨询模板
│   └── config.yml                     # Issue 模板配置
├── PULL_REQUEST_TEMPLATE.md           # PR 模板
├── CONTRIBUTING.md                    # 贡献指南
├── labels.yml                         # 标签定义
├── dependabot.yml                     # Dependabot 依赖更新配置
└── README.md                          # 本文档（配置说明）
```

---

## ⚙️ GitHub Actions 工作流详解

### 1. PR 自动检查 (`pr-check.yml`)

**触发时机**: 当有 PR 提交到 main/master/develop 分支时

**检查内容**:
- ✅ **构建测试**
  - Maven 编译
  - 运行单元测试
  - 上传构建产物
- ✅ **代码质量**
  - 代码格式检查
  - 编译验证
- ✅ **PR 验证**
  - 检查 PR 标题是否为空
  - 检查 PR 描述是否完整
  - 检查是否有合并冲突
- ✅ **检查总结**
  - 汇总所有检查结果
  - 在 PR 中显示检查状态

**作用**: 确保每个 PR 都符合质量标准，自动化审查流程

---

### 2. 欢迎新贡献者 (`welcome.yml`)

**触发时机**: 当有新的 Issue 或 PR 被创建时

**功能**:
- 🎉 自动欢迎首次贡献者
- 📖 提供文档和讨论区链接
- 💡 给出下一步行动建议

**作用**: 让新贡献者感到受欢迎，提高社区友好度

---

### 3. 自动标签 (`auto-label.yml`)

**触发时机**: 当 Issue 或 PR 被创建或编辑时

**智能识别**:
- 🏷️ 从标题和内容中识别问题类型（bug、feature、question）
- 🧩 识别相关组件（ui、api、performance、workspace 等）
- 💻 识别操作系统（Windows、macOS、Linux）
- 🌐 识别语言（中文、英文）

**作用**: 自动分类问题，方便管理和查找

---

### 4. 同步标签 (`sync-labels.yml`)

**触发时机**: 当 `.github/labels.yml` 文件更新时，或手动触发

**功能**:
- 📋 根据 `labels.yml` 定义同步仓库标签
- 🔄 自动创建缺失的标签
- 🎨 统一标签颜色和描述

**作用**: 保持标签系统的一致性和规范性

---

### 5. 发布构建 (`release.yml`)

**触发时机**: 当创建新的 Release 时，或手动触发

**构建内容**:
- 📦 **跨平台 Fat JAR** - 使用 Maven 构建，适用于所有平台
- 🪟 **Windows MSI** - 使用 jpackage 和 WiX 工具集创建安装包
- 🍎 **macOS DMG** - 为 Intel 和 Apple Silicon (M1、M2、M3、M4) 创建安装包
- 🐧 **Linux DEB/RPM** - 创建 Debian 和 RedHat 系列发行版安装包

**特性**:
- ✅ 使用 jlink 创建精简 JRE，减小安装包体积
- ✅ 自动提取版本号
- ✅ 所有平台共享同一个 Fat JAR
- ✅ 自动上传到 Release 页面

**作用**: 自动化多平台发布流程，确保发布质量和一致性

---

### 6. 代码安全分析 (`codeql-analysis.yml`)

**触发时机**: 
- 推送到 master 分支时
- 针对 master 分支的 PR
- 每周日 UTC 02:00 定时运行

**功能**:
- 🔍 使用 GitHub CodeQL 进行静态代码分析
- 🛡️ 检测潜在的安全漏洞
- 📊 识别代码质量问题
- ⚠️ 在仓库的 Security 标签中显示警报

**作用**: 持续监控代码安全性，及早发现和修复安全问题

---

## 🔧 其他配置文件详解

### 1. Dependabot 配置 (`dependabot.yml`)

**功能**:
- 📦 自动检测 Maven 依赖更新
- 🔒 自动创建安全更新 PR
- 📅 每周检查一次依赖更新
- 🎯 限制同时打开的 PR 数量（最多 5 个）

**作用**: 
- 保持依赖库最新，及时修复安全漏洞
- 减少手动维护依赖的工作量
- 配合 GitHub Security Alerts 提供全面的安全保障

---

## 📝 Issue 模板详解

### 1. Bug 报告模板 (`bug_report.yml`)

**必填信息**:
- ✅ 版本号
- ✅ 操作系统
- ✅ 问题描述
- ✅ 复现步骤
- ✅ 期望行为
- ✅ 实际行为
- ✅ 严重程度

**可选信息**:
- 截图
- 日志
- Java 版本
- 补充信息

**优势**: 结构化的信息收集，提高问题定位效率

---

### 2. 功能请求模板 (`feature_request.yml`)

**必填信息**:
- ✅ 功能类型（API 调试/性能测试/工作区等）
- ✅ 问题描述（痛点）
- ✅ 期望的解决方案
- ✅ 使用场景
- ✅ 优先级
- ✅ 受益用户范围

**可选信息**:
- 备选方案
- 界面设计
- 参考实现
- 贡献意愿

**优势**: 全面了解功能需求，促进社区讨论

---

### 3. 问题咨询模板 (`question.yml`)

**必填信息**:
- ✅ 问题类别
- ✅ 问题描述

**可选信息**:
- 相关信息（版本、操作系统）
- 已尝试的方法
- 补充信息

**优势**: 快速响应用户问题，建立知识库

---

## 📋 PR 模板详解

**PR 模板** (`PULL_REQUEST_TEMPLATE.md`)

**包含内容**:
- 📝 PR 描述
- 🔗 相关 Issue
- 🎯 改动类型（Bug 修复/新功能/文档更新等）
- 📋 改动内容列表
- 🧪 测试说明
- 📸 截图（UI 相关改动）
- ✅ 检查清单（代码规范/测试/文档）
- 💡 其他说明

**优势**: 
- 标准化 PR 格式
- 确保必要信息完整
- 提高代码审查效率

---

## 📖 贡献指南详解

**贡献指南** (`CONTRIBUTING.md`)

**包含内容**:
- 🚀 开始之前的准备
- 📋 贡献方式（报告 Bug/建议功能/提交代码/改进文档）
- 💻 开发环境配置
- 🔄 开发流程
- 📏 代码规范
- ✅ PR 检查清单
- 🎯 开发建议（项目结构/技术栈/调试技巧）
- 🤝 行为准则
- 📞 联系方式

**优势**: 
- 完整的贡献指引
- 降低新手贡献门槛
- 统一开发流程

---

## 🏷️ 标签系统详解

**标签定义** (`labels.yml`)

**标签分类**:

1. **问题类型** (Issue Types)
   - `bug` - Bug 报告
   - `enhancement` - 功能增强
   - `question` - 问题咨询
   - `documentation` - 文档相关

2. **优先级** (Priority)
   - `priority-high` - 高优先级
   - `priority-medium` - 中优先级
   - `priority-low` - 低优先级

3. **状态** (Status)
   - `需要确认 | need-confirm` - 需要更多信息
   - `需要讨论 | need-discussion` - 需要社区讨论
   - `进行中 | in-progress` - 正在处理
   - `已解决 | resolved` - 已解决
   - `不会修复 | wontfix` - 不会处理
   - `duplicate` - 重复问题
   - `invalid` - 无效问题

4. **组件标签** (Component Labels)
   - `ui` - UI 相关
   - `api` - API 调试相关
   - `performance` - 性能测试相关
   - `workspace` - 工作区管理
   - `environment` - 环境变量
   - `script` - 脚本功能
   - `network` - 网络协议
   - `import-export` - 导入导出
   - `git` - Git 集成

5. **平台标签** (Platform Labels)
   - `windows` - Windows 平台
   - `macos` - macOS 平台
   - `linux` - Linux 平台

6. **语言标签** (Language)
   - `chinese` - 中文
   - `english` - 英文

7. **特殊标签** (Special)
   - `good first issue` - 适合新手
   - `help wanted` - 需要帮助
   - `security` - 安全相关
   - `dependencies` - 依赖更新

**优势**: 
- 清晰的分类系统
- 双语标签支持
- 便于问题管理和筛选

---

## 🎯 使用说明

### 对于贡献者

1. **报告 Bug**:
   - 访问 [Issues](https://github.com/lakernote/easy-postman/issues/new/choose)
   - 选择 "🐛 Bug 报告" 模板
   - 填写完整信息并提交

2. **建议功能**:
   - 访问 [Issues](https://github.com/lakernote/easy-postman/issues/new/choose)
   - 选择 "✨ 功能请求" 模板
   - 描述使用场景和期望方案

3. **提交 PR**:
   - Fork 项目并创建分支
   - 开发和测试
   - 提交 PR（会自动填充模板）
   - 等待自动检查完成
   - 根据反馈进行修改

### 对于维护者

1. **管理 Issues**:
   - 自动标签会协助分类
   - 使用标签筛选和管理
   - 及时回复和处理

2. **审查 PR**:
   - 查看自动检查结果
   - 进行代码审查
   - 提供建设性反馈
   - 合并或请求修改

3. **维护标签**:
   - 编辑 `.github/labels.yml`
   - 推送后自动同步
   - 保持标签系统整洁

---

## 📊 预期效果

### 自动化程度提升
- ✅ PR 自动构建和测试
- ✅ 自动标签分类
- ✅ 自动欢迎新贡献者

### 贡献体验改善
- ✅ 清晰的贡献指南
- ✅ 结构化的 Issue 模板
- ✅ 标准化的 PR 流程

### 项目管理优化
- ✅ 问题分类清晰
- ✅ 优先级明确
- ✅ 进度可追踪

### 社区建设
- ✅ 降低贡献门槛
- ✅ 提高响应效率
- ✅ 增强社区活跃度

---

## 📞 需要帮助？

如有问题，请：
- 📖 查看 [贡献指南](.github/CONTRIBUTING.md)
- 💬 访问 [讨论区](https://github.com/lakernote/easy-postman/discussions)
- 📧 联系维护者

---

**感谢你为 EasyPostman 做出贡献！** 🎉
**Thank you for contributing to EasyPostman!** 🎉

