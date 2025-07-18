[中文](README.md) | English

# EasyPostman

> 🚀 An open-source API debugging and stress testing tool inspired by Postman and a simplified JMeter, optimized for
> developers with a clean UI and powerful features.

EasyPostman aims to provide developers with a local API debugging experience comparable to Postman, and integrates batch
requests and stress testing capabilities similar to a simplified JMeter. It supports advanced features such as
environment variables, batch requests, and stress testing to help efficiently test and manage APIs.

- 🌟 GitHub: [https://github.com/lakernote/easy-postman](https://github.com/lakernote/easy-postman)
- 🏠 Gitee: [https://gitee.com/lakernote/easy-postman](https://gitee.com/lakernote/easy-postman)
- 📦 Download: [https://gitee.com/lakernote/easy-postman/releases](https://gitee.com/lakernote/easy-postman/releases)
    - 🍏 Mac: EasyPostman-latest.dmg
    - 🪟 Windows: EasyPostman-latest.msi
- 💬 WeChat: **lakernote**

---

## ✨ Features

- 🚦 Supports common HTTP methods (GET/POST/PUT/DELETE, etc.)
- 📡 Supports SSE (Server-Sent Events) and WebSocket protocols
- 🌏 Environment variable management for easy switching
- 🕑 Auto-saved request history for review and reuse
- 📦 Batch requests & stress testing (simplified JMeter), supports report, result tree, and trend chart visualization
- 📝 Syntax highlighting request editor
- 🌐 Multi-language support (Simplified Chinese, English, in development)
- 💾 Local data storage for privacy and security
- 📂 Import/Export Postman v2.1, curl format
- 📊 Visualized response results, supports JSON/XML
- 🔍 Configurable request parameters, headers, cookies, etc.
- 📂 File upload and download support
- 📑 Request scripts (Pre-request Script, Tests)
- 🔗 Request chaining support
- 🧪 Detailed network request event monitoring and analysis

---

## 🖼️ Screenshots

|                                Preview                                |                                  Preview                                   |
|:----------------------------------------------------------------:|:---------------------------------------------------------------------:|
|                     ![icon](docs/icon.png)                      |                     ![welcome](docs/welcome.png)                     |
|              ![collections](docs/collections.png)              |             ![collections-import](docs/collections-import.png)             |
|            ![environments](docs/environments.png)            |                  ![functional](docs/functional.png)                  |
|              ![functional_1](docs/functional_1.png)              |                ![functional_2](docs/functional_2.png)                |
|                 ![history](docs/history.png)                 |              ![history-timeline](docs/history-timeline.png)              |
|            ![history-events](docs/history-events.png)            |                  ![networklog](docs/networklog.png)                  |
|              ![performance](docs/performance.png)              |            ![performance-report](docs/performance-report.png)            |
|    ![performance-resultTree](docs/performance-resultTree.png)    |              ![performance-trend](docs/performance-trend.png)              |
| ![performance-threadgroup-fixed](docs/performance-threadgroup-fixed.png) | ![performance-threadgroup-rampup](docs/performance-threadgroup-rampup.png) |
| ![performance-threadgroup-spike](docs/performance-threadgroup-spike.png) | ![performance-threadgroup-stairs](docs/performance-threadgroup-stairs.png) |
|              ![script-pre](docs/script-pre.png)              |                ![script-post](docs/script-post.png)                |
|            ![script-snippets](docs/script-snippets.png)            |                                                                       |

---

## 🚀 Quick Start

1. ⬇️ Clone the repo: `git clone https://gitee.com/lakernote/easy-postman.git`
2. 🛠️ Build with JDK 17+: `mvn clean package`
3. ▶️ Run the app: `App.java` or `java -jar target/easy-postman.jar`

---

## 📋 Feature Modules in Detail

### API Debugging (Collections)
- Supports multi-level directory management for API collections
- Rich request methods: GET, POST, PUT, DELETE, etc.
- Multiple request body formats: form data, JSON, XML, files, etc.
- Automatic saving of request history for tracing and reuse

### Environment Variables (Environments)
- Supports multiple environment configurations and switching
- Separate management of global variables and environment variables
- Variable reference using `{{variable}}` syntax

### Performance Testing (Performance)
- Multiple thread group types: fixed, ramp-up, stair-step, spike
- Visual test reports
- Result tree showing detailed information for each request
- Trend charts for analyzing request performance metrics

### History Records (History)
- Timeline view for historical requests
- Detailed request event analysis, showing the complete request chain
- Network log recording and analysis

### Request Scripts (Scripts)
- Pre-request Script support
- Tests script support
- Built-in code snippet library for quick insertion of common scripts

---

## Packaging

> Packaging EasyPostman requires JDK 17+ and uses Maven.

- **Mac**: Run `build/mac.sh`
- **Windows**: Install [wix3](https://github.com/wixtoolset/wix3) then run `build/win.bat`

---

## Community & Contribution

- Issues and PRs are welcome!
- QQ/WeChat groups (ask in Issues for details) or add WeChat **lakernote**
- Follow [GitHub](https://github.com/lakernote/easy-postman) for updates

---

## License

This project is open-sourced under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) license.
