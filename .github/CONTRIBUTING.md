# 贡献指南 | Contributing Guide

[English](#english) | [中文](#中文)

---

## 中文

感谢你对 EasyPostman 的关注！我们欢迎任何形式的贡献。

### 🚀 开始之前

在提交贡献之前，请确保：

1. 已阅读 [README](../README_zh.md) 了解项目
2. 已搜索 [Issues](https://github.com/lakernote/easy-postman/issues) 确认问题未被报告
3. 已阅读本贡献指南

### 📋 贡献方式

#### 1. 报告 Bug

如果你发现了 Bug，请：

1. 在 [Issues](https://github.com/lakernote/easy-postman/issues) 中搜索是否已有相同问题
2. 如果没有，创建新的 Issue，选择 "🐛 Bug 报告" 模板
3. 填写完整的信息，包括：
   - 复现步骤
   - 期望行为和实际行为
   - 环境信息（操作系统、版本号等）
   - 相关日志和截图

#### 2. 功能建议

如果你有新功能的想法：

1. 先在 [Discussions](https://github.com/lakernote/easy-postman/discussions) 中讨论
2. 如果获得认可，创建 Issue，选择 "✨ 功能请求" 模板
3. 详细描述：
   - 使用场景
   - 期望的解决方案
   - 备选方案
   - 界面设计（如果涉及 UI）

#### 3. 提交代码

我们非常欢迎代码贡献！

##### 环境准备

```bash
# 1. Fork 项目到你的 GitHub 账号

# 2. 克隆你的 Fork
git clone https://github.com/YOUR_USERNAME/easy-postman.git
cd easy-postman

# 3. 添加上游仓库
git remote add upstream https://github.com/lakernote/easy-postman.git

# 4. 安装依赖
# 确保已安装 Java 17+
mvn clean install
```

##### 开发流程

```bash
# 1. 同步最新代码
git checkout main
git pull upstream main

# 2. 创建功能分支
git checkout -b feature/your-feature-name
# 或修复分支
git checkout -b fix/your-bug-fix

# 3. 进行开发
# ... 编码 ...

# 4. 编译测试
mvn clean package
mvn test

# 5. 提交代码
git add .
git commit -m "feat: 添加新功能描述"
# 或
git commit -m "fix: 修复某个问题"

# 6. 推送到你的 Fork
git push origin feature/your-feature-name

# 7. 创建 Pull Request
# 在 GitHub 上创建 PR，从你的分支到 upstream/main
```

##### 代码规范

- **编码风格**：遵循 Java 编码规范
- **提交信息**：使用语义化提交信息
  - `feat:` 新功能
  - `fix:` Bug 修复
  - `docs:` 文档更新
  - `style:` 代码格式调整
  - `refactor:` 代码重构
  - `perf:` 性能优化
  - `test:` 测试相关
  - `chore:` 构建/工具相关
- **注释**：为复杂逻辑添加清晰的注释
- **测试**：为新功能添加测试用例

##### Pull Request 检查清单

在提交 PR 前，请确保：

- [ ] 代码可以成功编译（`mvn clean package`）
- [ ] 所有测试通过（`mvn test`）
- [ ] 代码遵循项目的编码规范
- [ ] 添加了必要的注释
- [ ] 更新了相关文档（如果需要）
- [ ] PR 描述清晰，说明了改动内容和原因
- [ ] 关联了相关的 Issue（如果有）

#### 4. 改进文档

文档同样重要！你可以：

- 修正错别字或不准确的描述
- 补充使用示例
- 翻译文档
- 添加常见问题解答

### 🎯 开发建议

#### 项目结构

```
src/main/java/com/laker/
├── postman/           # 主应用
│   ├── ui/           # UI 组件
│   ├── service/      # 业务逻辑
│   ├── model/        # 数据模型
│   ├── utils/        # 工具类
│   └── network/      # 网络请求
└── ...
```

#### 技术栈

- **UI 框架**: JavaFX / Swing + FlatLaf
- **HTTP 客户端**: OkHttp
- **JSON 处理**: Jackson / Gson
- **Git 集成**: JGit
- **脚本引擎**: GraalVM Polyglot

#### 调试技巧

```bash
# 开发模式运行
mvn clean compile exec:java -Dexec.mainClass="com.laker.postman.App"

# 启用调试日志
# 修改 src/main/resources/logback.xml 中的日志级别
```

### 🤝 行为准则

- 尊重他人，友好交流
- 接受建设性的批评
- 专注于对项目最有利的事情
- 对新手保持耐心和包容

### 📞 联系方式

- **GitHub Issues**: [提交 Issue](https://github.com/lakernote/easy-postman/issues)
- **Discussions**: [参与讨论](https://github.com/lakernote/easy-postman/discussions)
- **微信**: lakernote

### 🙏 感谢

感谢所有为 EasyPostman 做出贡献的开发者！

---

## English

Thank you for your interest in EasyPostman! We welcome all forms of contributions.

### 🚀 Before You Start

Before contributing, please ensure:

1. You've read the [README](../README.md) to understand the project
2. You've searched [Issues](https://github.com/lakernote/easy-postman/issues) to confirm the issue hasn't been reported
3. You've read this contributing guide

### 📋 Ways to Contribute

#### 1. Report Bugs

If you find a bug:

1. Search [Issues](https://github.com/lakernote/easy-postman/issues) for existing reports
2. If none exist, create a new Issue using the "🐛 Bug Report" template
3. Fill in complete information including:
   - Reproduction steps
   - Expected vs actual behavior
   - Environment info (OS, version, etc.)
   - Relevant logs and screenshots

#### 2. Suggest Features

If you have a feature idea:

1. Discuss it in [Discussions](https://github.com/lakernote/easy-postman/discussions) first
2. If approved, create an Issue using the "✨ Feature Request" template
3. Describe in detail:
   - Use cases
   - Desired solution
   - Alternative solutions
   - UI mockups (if applicable)

#### 3. Submit Code

We welcome code contributions!

##### Setup Environment

```bash
# 1. Fork the project to your GitHub account

# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/easy-postman.git
cd easy-postman

# 3. Add upstream remote
git remote add upstream https://github.com/lakernote/easy-postman.git

# 4. Install dependencies
# Ensure Java 17+ is installed
mvn clean install
```

##### Development Workflow

```bash
# 1. Sync latest code
git checkout main
git pull upstream main

# 2. Create feature branch
git checkout -b feature/your-feature-name
# or fix branch
git checkout -b fix/your-bug-fix

# 3. Develop
# ... code ...

# 4. Build and test
mvn clean package
mvn test

# 5. Commit changes
git add .
git commit -m "feat: add new feature description"
# or
git commit -m "fix: fix some issue"

# 6. Push to your fork
git push origin feature/your-feature-name

# 7. Create Pull Request
# Create PR on GitHub from your branch to upstream/main
```

##### Code Standards

- **Code Style**: Follow Java coding standards
- **Commit Messages**: Use semantic commit messages
  - `feat:` New feature
  - `fix:` Bug fix
  - `docs:` Documentation update
  - `style:` Code formatting
  - `refactor:` Code refactoring
  - `perf:` Performance improvement
  - `test:` Test related
  - `chore:` Build/tools related
- **Comments**: Add clear comments for complex logic
- **Tests**: Add test cases for new features

##### Pull Request Checklist

Before submitting a PR, ensure:

- [ ] Code compiles successfully (`mvn clean package`)
- [ ] All tests pass (`mvn test`)
- [ ] Code follows project coding standards
- [ ] Added necessary comments
- [ ] Updated relevant documentation (if needed)
- [ ] PR description is clear, explaining changes and reasons
- [ ] Linked related Issues (if any)

#### 4. Improve Documentation

Documentation is equally important! You can:

- Fix typos or inaccurate descriptions
- Add usage examples
- Translate documentation
- Add FAQ entries

### 🎯 Development Tips

#### Project Structure

```
src/main/java/com/laker/
├── postman/           # Main application
│   ├── ui/           # UI components
│   ├── service/      # Business logic
│   ├── model/        # Data models
│   ├── utils/        # Utilities
│   └── network/      # Network requests
└── ...
```

#### Tech Stack

- **UI Framework**: JavaFX / Swing + FlatLaf
- **HTTP Client**: OkHttp
- **JSON Processing**: Jackson / Gson
- **Git Integration**: JGit
- **Script Engine**: GraalVM Polyglot

#### Debug Tips

```bash
# Run in development mode
mvn clean compile exec:java -Dexec.mainClass="com.laker.postman.App"

# Enable debug logging
# Modify log level in src/main/resources/logback.xml
```

### 🤝 Code of Conduct

- Respect others, communicate kindly
- Accept constructive criticism
- Focus on what's best for the project
- Be patient and inclusive with newcomers

### 📞 Contact

- **GitHub Issues**: [Submit Issue](https://github.com/lakernote/easy-postman/issues)
- **Discussions**: [Join Discussion](https://github.com/lakernote/easy-postman/discussions)
- **WeChat**: lakernote

### 🙏 Acknowledgments

Thanks to all developers who have contributed to EasyPostman!

