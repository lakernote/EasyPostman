# Scripts Guide

脚本入口现在按职责拆成两类：

- `build/`: 发行打包脚本，生成 DMG / EXE / DEB / RPM
- `scripts/`: 本地开发、插件验证、回归辅助脚本

推荐入口：

- 本地构建应用与插件：`./scripts/plugin-dev.sh build <plugin|all>`
- 本地验证插件安装链路：`./scripts/plugin-dev.sh prepare <plugin>`
- 保留兼容入口：`./scripts/verify-redis-plugin-macos.sh`

更多说明见：

- `docs/BUILD_zh.md`
- `docs/PLUGINS_zh.md`
