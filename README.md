<div align="center">

[简体中文](README_zh.md) | English

</div>

# EasyPostman

> 🚀 An open-source API debugging and stress testing tool inspired by Postman and a simplified JMeter, optimized for developers with a clean UI and powerful features. Built-in Git integration for team collaboration and version control.

![GitHub license](https://img.shields.io/github/license/lakernote/easy-postman)
![Java](https://img.shields.io/badge/Java-17+-orange)
![Platform](https://img.shields.io/badge/Platform-Windows%20|%20macOS%20|%20Linux-blue)

---

## 📖 Table of Contents

- [✨ Features](#-features)
- [📦 Download](#-download)
- [🚀 Quick Start](#-quick-start)
- [🖼️ Screenshots](#️-screenshots)
- [🤝 Contributing](#-contributing)
- [📚 Documentation](#-documentation)
- [❓ FAQ](#-faq)
- [💖 Support](#-support)

---

## 💡 About

EasyPostman provides developers with a **local, privacy-first** API debugging experience comparable to Postman, plus simplified JMeter-style performance testing. Built with Java Swing for cross-platform support, it works completely offline and includes built-in Git workspace support for team collaboration and version control.

### 🔥 Philosophy

- **🎯 Focus on Core Features** - Simple yet powerful, rich features without bloat
- **🔒 Privacy First** - 100% local storage, no cloud sync, your data stays private
- **🚀 Performance Oriented** - Native Java app, fast startup, smooth experience


---

## ✨ Features

### 🏢 Workspace & Collaboration
- **Local Workspace** - Personal projects with local storage
- **Git Workspace** - Version control and team collaboration
- **Multi-device Sync** - Share API data via Git repositories
- **Project Isolation** - Each workspace manages its own collections and environments

### 🔌 API Testing
- **HTTP/HTTPS** - Full REST API support (GET, POST, PUT, DELETE, etc.)
- **WebSocket & SSE** - Real-time protocol support
- **Multiple Body Types** - Form Data, JSON, XML, Binary
- **File Upload/Download** - Drag & drop support
- **Environment Variables** - Multi-environment management with dynamic variables

### ⚡ Performance Testing
- **Thread Group Modes** - Fixed, Ramp-up, Stair-step, Spike
- **Real-time Monitoring** - TPS, response time, error rate
- **Visual Reports** - Performance trend charts and result trees
- **Batch Requests** - Simplified JMeter-style testing

### 📝 Advanced Features
- **Pre-request Scripts** - JavaScript execution before requests
- **Test Scripts** - Assertions and response validation
- **Request Chaining** - Extract data and pass to next request
- **Network Event Log** - Detailed request/response analysis
- **Import/Export** - Postman v2.1, cURL, HAR (in progress)

### 🎨 User Experience
- **Light & Dark Mode** - Comfortable viewing in any lighting
- **Multi-language** - English, 简体中文
- **Syntax Highlighting** - JSON, XML, JavaScript
- **Cross-platform** - Windows, macOS, Linux

📖 **[View All Features →](docs/FEATURES.md)**

---

## 📦 Download

### Latest Release

🔗 **[GitHub Releases](https://github.com/lakernote/easy-postman/releases)** | **[Gitee Mirror (China)](https://gitee.com/lakernote/easy-postman/releases)**

### Platform Downloads

| Platform | Download | Notes |
|----------|----------|-------|
| 🍎 **macOS (Apple Silicon)** | `EasyPostman-{version}-macos-arm64.dmg` | For M1/M2/M3/M4 Macs |
| 🍏 **macOS (Intel)** | `EasyPostman-{version}-macos-x86_64.dmg` | For Intel-based Macs |
| 🪟 **Windows (Installer)** | `EasyPostman-{version}-windows-x64.exe` | Installer with auto-update |
| 🪟 **Windows (Portable)** | `EasyPostman-{version}-windows-x64-portable.zip` | No installation required |
| 🐧 **Ubuntu/Debian** | `easypostman_{version}_amd64.deb` | DEB package |
| ☕ **Cross-platform** | `easy-postman-{version}.jar` | Requires Java 17+ |

> ⚠️ **First Run Notice**
> 
> - **Windows**: If you see SmartScreen warning → Click "More info" → "Run anyway"
> - **macOS**: If "cannot be opened" → Right-click app → Select "Open" → Click "Open"
> 
> The app is completely open-source and safe. These warnings appear because we don't purchase code signing certificates.

### Gitee Mirror (China) 🌏

Due to storage limitations, Gitee mirror only provides:
- macOS (Apple Silicon) DMG
- Windows Installer and Portable ZIP

For other platforms, please use GitHub Releases.

---

## 🚀 Quick Start

### Option 1: Download Pre-built Release

1. Download the appropriate package for your platform from [Releases](https://github.com/lakernote/easy-postman/releases)
2. Install and run:
   - **macOS**: Open DMG, drag to Applications
   - **Windows Installer**: Run EXE, follow installation wizard
   - **Windows Portable**: Extract ZIP, run `EasyPostman.exe`
   - **Linux DEB**: `sudo dpkg -i easypostman_{version}_amd64.deb`
   - **JAR**: `java -jar easy-postman-{version}.jar`

### Option 2: Build from Source

```bash
# Clone repository
git clone https://github.com/lakernote/easy-postman.git
cd easy-postman

# Build and run
mvn clean package
java -jar target/easy-postman-*.jar
```

📖 **[Build Guide →](docs/BUILD.md)**

### First Steps

1. **Create a Workspace** - Choose Local (personal) or Git (team collaboration)
2. **Create a Collection** - Organize your API requests
3. **Send Your First Request** - Enter URL, configure params, click Send
4. **Set Up Environments** - Switch between dev/test/prod easily

---

## 🖼️ Screenshots

<div align="center">

### Main Interface
![Home](docs/home-en.png)

### Workspace Management
![Workspaces](docs/workspaces.png)

### Collections & API Testing
![Collections](docs/collections.png)

### Performance Testing
![Performance](docs/performance.png)

</div>

📸 **[View All Screenshots →](docs/SCREENSHOTS.md)**
---

## 🤝 Contributing

We welcome all forms of contribution! Whether it's bug reports, feature requests, or code contributions.

### Ways to Contribute

- 🐛 **Report Bugs** - Use our [bug report template](https://github.com/lakernote/easy-postman/issues/new/choose)
- ✨ **Request Features** - Share your ideas via [feature request](https://github.com/lakernote/easy-postman/issues/new/choose)
- 💻 **Submit Code** - Fork, code, and create a pull request
- 📝 **Improve Docs** - Fix typos, add examples, translate

### Automated Checks

When you submit a PR, it will automatically go through:
- ✅ Build and compilation check
- ✅ Test execution
- ✅ Code quality validation
- ✅ PR format verification

📖 **[Contributing Guide →](.github/CONTRIBUTING.md)**

---

## 📚 Documentation

- 📖 **[Feature Details](docs/FEATURES.md)** - Comprehensive feature documentation
- 🏗️ **[System Architecture](docs/ARCHITECTURE.md)** - Technical stack and architecture
- 🚀 **[Build Guide](docs/BUILD.md)** - Build from source and generate installers
- 🖼️ **[Screenshots Gallery](docs/SCREENSHOTS.md)** - All application screenshots
- 📝 **[Script API Reference](docs/SCRIPT_API_REFERENCE_zh.md)** - Pre-request and test script API
- 📝 **[Script Snippets Quick Reference](docs/SCRIPT_SNIPPETS_QUICK_REFERENCE.md)** - Built-in code snippets
- 🔐 **[Client Certificates](docs/CLIENT_CERTIFICATES.md)** - mTLS configuration
- 🐧 **[Linux Build Guide](docs/LINUX_BUILD.md)** - Building on Linux
- ❓ **[FAQ](docs/FQA.MD)** - Frequently asked questions

---

## ❓ FAQ

<details>
<summary><b>Q: Why local storage instead of cloud sync?</b></summary>

A: We value developer privacy. Local storage ensures your API data is never leaked to third parties. You can optionally use Git workspace for team collaboration while maintaining control over your data.
</details>

<details>
<summary><b>Q: How to import Postman data?</b></summary>

A: In the Collections view, click **Import** and select a Postman v2.1 JSON file. The tool will automatically convert collections, requests, and environments.
</details>

<details>
<summary><b>Q: Why does Windows/macOS show security warnings?</b></summary>

**Windows SmartScreen**: Not purchasing a code signing certificate (~$100-400/year) triggers warnings.
- **Solution**: Click "More info" → "Run anyway"
- As download count increases, warnings will gradually decrease

**macOS Gatekeeper**: Not purchasing Apple Developer certificate ($99/year) + notarization triggers warnings.
- **Solution**: Right-click the app → Select "Open"
- Or run in Terminal: `sudo xattr -rd com.apple.quarantine /Applications/EasyPostman.app`

This project is **completely open-source** and the code can be reviewed on GitHub.
</details>

<details>
<summary><b>Q: Does it support team collaboration?</b></summary>

A: ✅ **Yes!** Use **Git workspace** to:
- Share API collections and environments with your team
- Track changes with version control (commit, push, pull)
- Work across multiple devices
- Collaborate without cloud services
</details>

<details>
<summary><b>Q: Are workspaces isolated from each other?</b></summary>

A: Yes. Each workspace is completely independent with its own collections, environments, and history. Switching workspaces provides full data isolation.
</details>

<details>
<summary><b>Q: Which Git platforms are supported?</b></summary>

A: All standard Git platforms including:
- GitHub
- Gitee
- GitLab
- Bitbucket
- Self-hosted Git servers

Just provide a standard Git URL (HTTPS or SSH).
</details>

---

## 💖 Support the Project

If you find EasyPostman helpful:

- ⭐ **Star this repo** - Show your support!
- 🍴 **Fork and contribute** - Help make it better
- 📢 **Recommend to friends** - Spread the word
- 💬 **Join WeChat group** - Add **lakernote** for direct communication
- 💬 **GitHub Discussions** - [Ask questions and share ideas](https://github.com/lakernote/easy-postman/discussions)

---

## 🔗 Links

- 🌟 **GitHub**: [https://github.com/lakernote/easy-postman](https://github.com/lakernote/easy-postman)
- 🏠 **Gitee**: [https://gitee.com/lakernote/easy-postman](https://gitee.com/lakernote/easy-postman)
- 💬 **Discussions**: [https://github.com/lakernote/easy-postman/discussions](https://github.com/lakernote/easy-postman/discussions)
- 📦 **Releases**: [https://github.com/lakernote/easy-postman/releases](https://github.com/lakernote/easy-postman/releases)

---

## ⭐ Star History

[![Star History Chart](https://api.star-history.com/svg?repos=lakernote/easy-postman&type=date&legend=top-left)](https://www.star-history.com/#lakernote/easy-postman&type=date&legend=top-left)

---

## 🙏 Acknowledgements

Thanks to these awesome open-source projects:

- [FlatLaf](https://github.com/JFormDesigner/FlatLaf) - Modern Swing theme
- [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) - Syntax highlighting editor
- [OkHttp](https://github.com/square/okhttp) - HTTP client
- [Termora](https://github.com/TermoraDev/termora) - Excellent terminal emulator

---

<div align="center">

**Make API debugging easier, make performance testing more intuitive**

Made with ❤️ by [laker](https://github.com/lakernote)

</div>
