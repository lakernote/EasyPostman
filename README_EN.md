[中文](../README.md) | English

# EasyPostman

> 🚀 An open-source API debugging tool inspired by Postman, optimized for developers. Simple UI, powerful features.

EasyPostman aims to provide a local API debugging experience comparable to Postman, supporting environment variables, batch requests, stress testing, and more to help developers efficiently test and manage APIs.

---

## Quick Start

1. Clone the repo: `git clone https://github.com/your-repo/easy-postman.git`
2. Build with JDK 17+: `mvn clean package`
3. Run the app: `java -jar target/easy-postman.jar`

Or use the packaging scripts (see "Packaging" below).

---

## Features

- 🚦 Supports common HTTP methods (GET/POST/PUT/DELETE, etc.)
- 🌏 Environment variable management for easy switching
- 🕑 Auto-saved request history for review and reuse
- 📦 Batch requests & stress testing for various scenarios
- 📝 Syntax highlighting request editor (based on RSyntaxTextArea, in development)
- 🌐 Multi-language support (Simplified Chinese, English, in development)
- 💾 Local data storage for privacy and security
- 🖥️ Modern UI theme, dark mode supported

---

## Tech Stack

- Java 17
- Swing desktop GUI
- jlink & jpackage for packaging
- RSyntaxTextArea for code highlighting
- FlatLaf modern UI theme
- jIconFont-Swing for font icons

---

## Screenshots

|      Icon      |   Collection Management   |   Batch Request   |
|:-------------:|:------------------------:|:----------------:|
| ![Icon](../docs/EasyPostman-1024.png) | ![Collections](../docs/collections.png) | ![Batch](../docs/batch.png) |

|  Environments  |   History   |   Stress Test   |
|:-------------:|:-----------:|:---------------:|
| ![Envs](../docs/environments.png) | ![History](../docs/history.png) | ![Stress](../docs/stresstest.png) |

---

## Packaging

- **Mac**: Run `build/mac.sh` (JDK 17+ required)
- **Windows**: Install [wix3](https://github.com/wixtoolset/wix3) then run `build/win.bat`

---

## Community & Contribution

- Issues and PRs are welcome!
- QQ/WeChat groups (ask in Issues for details)
- Follow [GitHub](https://github.com/your-repo/easy-postman) for updates

---

## License

This project is open-sourced under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0) license.

