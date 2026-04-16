# Build Scripts

`build/` 目录只负责发行打包，不放插件开发和验证脚本。

当前脚本：

- `mac.sh`: macOS DMG
- `win-exe.bat`: Windows EXE / portable
- `linux-deb.sh`: Debian / Ubuntu DEB（产物架构跟随当前打包机器，例如 `amd64` / `arm64`）
- `linux-rpm.sh`: RPM（产物架构跟随当前打包机器）

统一约定：

- Maven 先产出带版本号的 jar：
  `easy-postman-app/target/easy-postman-${version}.jar`
- 打包脚本再复制成固定名：
  `easy-postman.jar`
- `jpackage` 和安装包内部始终引用固定文件名

这样做的好处：

- 打包脚本不需要跟着版本号改
- 安装包内部 classpath 固定
- 自动更新和替换主 jar 更简单

如果你要做的是：

- 发行包构建：看 `build/`
- 插件本地开发/安装验证：看 `scripts/plugin-dev.sh`
- 插件架构与使用说明：看 `docs/PLUGINS_zh.md`
