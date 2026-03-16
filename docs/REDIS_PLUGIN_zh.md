# EasyPostman Redis 插件落地说明

## 1. 当前状态

Redis 相关能力现在已经按真正插件边界拆开：

- 宿主 `easy-postman` 不再打包 Redis 类、Redis 文案、Redis 图标
- Redis 插件独立产物：
  - `plugins/plugin-redis/target/easy-postman-<version>-plugin-redis.jar`
- 插件自身包含：
  - Redis 工具箱面板
  - `pm.plugin("redis")` / `pm.redis` 兼容脚本 API
  - 自动补全、Snippet、插件图标、插件 descriptor
- 插件运行时支持：
  - 按需安装
  - 启用 / 禁用
  - 最低 / 最高宿主版本兼容判断

## 2. 本地验证目标

本地验证建议至少覆盖 4 项：

1. 宿主 jar 不包含 Redis 内容
2. Redis 插件 jar 包含自己的类、资源和 descriptor
3. 插件市场可以通过本地 `file://catalog.json` 安装 Redis 插件
4. 安装后重启，Redis 工具箱、脚本 API、Snippet 都可用

## 3. macOS 本地验证脚本

仓库已经加了脚本：

```bash
./scripts/verify-redis-plugin-macos.sh
```

默认就是 `prepare`，会自动完成这些事：

- 执行 `mvn -DskipTests clean package`
- 校验宿主 / 插件 jar 边界
- 生成隔离的本地验证目录
- 生成本地 `catalog.json`
- 生成 macOS 可双击的 `.command` 辅助脚本

### 3.1 一键准备

```bash
./scripts/verify-redis-plugin-macos.sh prepare
```

生成目录：

```text
temp/redis-plugin-verify/
├── artifacts/
│   ├── easy-postman.jar
│   └── easy-postman-plugin-redis.jar
├── data/
├── catalog.json
├── easy-postman-plugin-redis.sha256.txt
├── launch-clean.command
├── install-redis.command
├── launch-with-redis.command
└── reset-verify.command
```

### 3.2 验证路径 A：市场安装

1. 启动一个“干净”宿主：

```bash
./scripts/verify-redis-plugin-macos.sh run-clean
```

也可以直接双击：

```text
temp/redis-plugin-verify/launch-clean.command
```

2. 打开 EasyPostman：
   - `Top Menu -> Plugin Manager -> Marketplace`

3. 验证点：
   - Catalog URL 已自动带上本地 `file://.../catalog.json`
   - 市场里能看到 `Redis Plugin`
   - 状态应显示 `Available`

4. 点击安装

5. 关闭并重新启动：

```bash
./scripts/verify-redis-plugin-macos.sh run-with-plugin
```

或者双击：

```text
temp/redis-plugin-verify/launch-with-redis.command
```

### 3.3 验证路径 B：直接安装

```bash
./scripts/verify-redis-plugin-macos.sh install-direct
./scripts/verify-redis-plugin-macos.sh run-with-plugin
```

也可以直接双击：

```text
temp/redis-plugin-verify/install-redis.command
temp/redis-plugin-verify/launch-with-redis.command
```

### 3.4 重置验证环境

```bash
./scripts/verify-redis-plugin-macos.sh reset
```

或者双击：

```text
temp/redis-plugin-verify/reset-verify.command
```

## 4. 安装后你应该看到什么

### 4.1 插件管理器

- Installed 里存在 `plugin-redis`
- 状态是：
  - `Loaded`：当前已加载
  - `Disabled`：被禁用，重启后不加载
  - `Incompatible`：宿主版本不满足插件 descriptor 限制

### 4.2 Toolbox

左侧 Toolbox 里应出现：

- `Redis`

并且图标来自插件自身资源，不再依赖宿主 jar。

### 4.3 Script

脚本里应可用：

```javascript
var redis = pm.plugin('redis');
```

兼容入口也会保留：

```javascript
pm.redis
```

### 4.4 Snippet / Completion

应能看到 Redis 相关内容：

- `Redis 查询与断言`
- `Redis 写入与断言`
- `pm.plugin("redis")`

## 5. 如何验证 Redis 功能本身

如果你本机已经有 Redis，可直接连 `127.0.0.1:6379`。

### 5.1 如果你用 Homebrew

```bash
brew services start redis
redis-cli ping
```

### 5.2 如果你用 Docker

```bash
docker run --name easy-postman-redis -p 6379:6379 -d redis:7-alpine
docker exec -it easy-postman-redis redis-cli ping
```

### 5.3 在插件里做最小验证

建议在 Redis 面板里做这组操作：

1. 连接 `127.0.0.1:6379`
2. 执行 `SET`：
   - key: `easy-postman:test`
   - value: `{"status":"ok"}`
3. 再执行 `GET`
4. 确认结果正常回显

## 6. 发布到 GitHub 后如何使用

当前运行时支持的方式是：

- 插件包放 GitHub Releases
- `catalog.json` 放 GitHub Pages
- 客户端通过 Plugin Manager 加载 `catalog.json`

### 6.1 发布插件包到 GitHub Releases

本地构建：

```bash
mvn -DskipTests clean package
```

上传这个文件到 Release：

```text
plugins/plugin-redis/target/easy-postman-<version>-plugin-redis.jar
```

### 6.2 计算 sha256

```bash
shasum -a 256 plugins/plugin-redis/target/easy-postman-<version>-plugin-redis.jar
```

### 6.3 准备 catalog.json

可以参考：

- `docs/plugin-market/catalog.sample.json`

Redis 插件条目示例：

```json
{
  "id": "plugin-redis",
  "name": "Redis Plugin",
  "version": "4.3.55",
  "description": "Redis toolbox panel, pm.redis script API, completions and snippets.",
  "downloadUrl": "https://github.com/<owner>/<repo>/releases/download/v4.3.55/easy-postman-4.3.55-plugin-redis.jar",
  "homepage": "https://github.com/<owner>/<repo>",
  "sha256": "<replace-with-real-sha256>"
}
```

### 6.4 把 catalog.json 放到 GitHub Pages

例如：

```text
https://<owner>.github.io/<repo>/catalog.json
```

### 6.5 客户端如何使用

在 EasyPostman：

1. 打开 `Plugin Manager`
2. 切到 `Marketplace`
3. 填入 GitHub Pages 上的 `catalog.json` URL
4. 点击加载
5. 选择 `Redis Plugin`
6. 点击安装
7. 重启应用

## 7. GitHub 使用建议

推荐固定这三个地址：

1. 插件主页：
   - GitHub 仓库首页
2. 插件下载：
   - GitHub Releases 资产下载地址
3. 插件市场目录：
   - GitHub Pages `catalog.json`

这样整套链路都可以免费托管。

## 8. 当前限制

Redis 插件这条线已经收尾，但还有两件事不在这份文档范围内：

1. Kafka / ES / InfluxDB 还没拆成独立插件
2. GitHub Actions 还没自动发布 Redis 插件和自动更新 catalog

如果下一步继续推进，优先建议是：

1. 先做 `easy-postman-plugin-kafka`
2. 再补 GitHub Actions 自动发布插件资产与 catalog
