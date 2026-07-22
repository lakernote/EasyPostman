# EasyPostman Collection CLI

`collection run` 用于在开发机、CI Runner 或独立服务器上，按集合和文件夹无界面运行 EasyPostman 原生 workspace。

它只负责集合运行，不读取 `functional_config.json`，也不接受 Postman Collection / Environment 作为运行输入。

```bash
java -jar easy-postman.jar collection run <workspace-directory> [options]
```

## 1. Workspace 文件

最小目录：

```text
api-workspace/
├── collections.json          # 必需：集合、文件夹、请求、脚本和断言
├── environments.json         # 可选：环境列表和活动环境
├── data/                     # 可选：CSV / JSON 迭代数据
│   └── users.csv
└── fixtures/                 # 可选：上传附件
    └── avatar.png
```

CLI 行为：

- 请求配置只来自 workspace 根目录的 `collections.json`。
- 自动读取同目录的 `environments.json`。
- 应用级 `global_variables.json` 从 EasyPostman 数据目录读取。
- `-d` 指定 CSV / JSON 多轮数据，相对路径从 workspace 根目录解析。
- 相对上传路径默认从 workspace 根目录解析。
- CLI 只读 workspace 配置，不会写回集合和环境。

## 2. 最短用法

运行 workspace 中全部集合：

```bash
java -jar easy-postman.jar collection run /srv/api-workspace
```

选择集合、文件夹和环境：

```bash
java -jar easy-postman.jar collection run /srv/api-workspace \
  -c "Order API" \
  --folder "Smoke" \
  -e "CI"
```

在 Git workspace 根目录：

```bash
java -jar /opt/easy-postman/easy-postman.jar collection run .
```

当前目录包含 `collections.json` 时可以省略 `.`，但 CI 脚本建议显式写出目录。

安装过桌面端的本机还可以传已登记 workspace 的名称或 ID；全新 CI Runner 应直接传目录，不依赖 `workspaces.json`。

## 3. 集合与文件夹选择

不传 `-c` 时运行全部根集合：

```bash
java -jar easy-postman.jar collection run "$WORKSPACE_DIR"
```

`-c` 可按集合名称或 ID 选择，并可重复：

```bash
java -jar easy-postman.jar collection run "$WORKSPACE_DIR" \
  -c "User API" \
  -c "Order API"
```

`--folder` 按文件夹名称选择该文件夹及其子文件夹，可重复：

```bash
java -jar easy-postman.jar collection run "$WORKSPACE_DIR" \
  -c "Order API" \
  --folder "Smoke" \
  --folder "Regression"
```

集合或文件夹不存在时退出码为 `2`，并列出可用集合或未匹配的文件夹。

## 4. CSV / JSON 多轮驱动

### 4.1 CSV

`data/users.csv`：

```csv
user,role
alice,admin
bob,user
```

运行：

```bash
java -jar easy-postman.jar collection run "$WORKSPACE_DIR" \
  -d data/users.csv
```

默认执行两轮。请求中使用 `{{user}}`，脚本中使用：

```javascript
pm.iterationData.get('user');
pm.variables.get('user');
pm.info.iteration;
pm.info.iterationCount;
```

### 4.2 JSON

JSON 必须是对象数组：

```json
[
  {"user": "alice", "role": "admin"},
  {"user": "bob", "role": "user"}
]
```

```bash
java -jar easy-postman.jar collection run "$WORKSPACE_DIR" \
  -d data/users.json
```

### 4.3 `-n` 规则

| 数据 | 参数 | 实际行为 |
|---|---|---|
| 无数据 | 不传 `-n` | 1 轮 |
| 2 行 | 不传 `-n` | 2 轮 |
| 2 行 | `-n 1` | 只使用第 1 行 |
| 2 行 | `-n 5` | 依次使用第 1、2、1、2、1 行 |
| 无数据 | `-n 3` | 3 轮空迭代数据 |

## 5. 普通 workspace 复制到 CI

普通 workspace 不要求是 Git 仓库。GUI 机器保存后，应传递完整目录，而不是只复制 `collections.json`。

开发机打包：

```bash
tar -C /Users/me/EasyPostman/workspaces \
  -czf order-api-workspace.tar.gz \
  order-api
```

CI 解压并运行：

```bash
mkdir -p "$CI_PROJECT_DIR/api-workspace"
tar -xzf order-api-workspace.tar.gz \
  -C "$CI_PROJECT_DIR/api-workspace" \
  --strip-components=1

java -jar "$CI_PROJECT_DIR/tools/easy-postman.jar" \
  collection run "$CI_PROJECT_DIR/api-workspace" \
  -e "CI" \
  -c "Order API" \
  --folder "Smoke" \
  -d data/ci-orders.csv \
  --bail \
  --out "$CI_PROJECT_DIR/target/collection-result.json"
```

也可以通过 CI Artifact、SCP、rsync、Docker volume 或 Kubernetes volume 交付 workspace。

## 6. Git workspace 在 CI 中运行

推荐让 GUI workspace 目录本身就是 Git 仓库：

1. 开发机 clone API workspace 仓库。
2. EasyPostman 中把 Git workspace 路径指向该目录。
3. 使用 GUI 维护集合、环境和非敏感附件。
4. 审查并提交 `collections.json`、`environments.json`、`data/`、`fixtures/`。
5. push 后由 CI 使用干净 checkout。
6. CI 在仓库根目录执行 `collection run .`。

不要提交真实 Token、Cookie、私钥、生产数据或运行报告。

### GitHub Actions

```yaml
name: EasyPostman Collection Test

on:
  push:
  pull_request:

jobs:
  collection-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Run collection
        run: |
          mkdir -p target
          java -jar tools/easy-postman.jar \
            collection run . \
            -e "CI" \
            -c "Order API" \
            --folder "Smoke" \
            -d data/ci-orders.csv \
            --bail \
            --out target/collection-result.json

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: collection-result
          path: target/collection-result.json
```

业务代码和 workspace 分仓时，checkout 到一个目录后直接传该目录：

```bash
git clone "$API_WORKSPACE_REPOSITORY" "$CI_PROJECT_DIR/api-workspace"

java -jar "$CI_PROJECT_DIR/tools/easy-postman.jar" \
  collection run "$CI_PROJECT_DIR/api-workspace"
```

CI 不需要登记 workspace，也不需要 GUI 机器保持在线。

## 7. 环境、全局变量和 Secret

未传 `-e` 时优先使用 `active: true` 的环境；否则使用第一个环境。CI 建议固定 `-e "CI"`。

`global_variables.json` 是应用级文件。CI 可指定隔离数据目录：

```bash
java -DeasyPostman.data.dir="$RUNNER_TEMP/easy-postman-data" \
  -jar tools/easy-postman.jar \
  collection run . \
  -e "CI"
```

Secret 不应提交到 workspace。可以在 CI 临时 checkout 中用 `jq` 注入环境副本：

```bash
jq --arg token "$API_TOKEN" '
  map(
    if .name == "CI" then
      .variableList |= map(
        if .key == "apiToken" then .value = $token else . end
      )
    else . end
  )
' environments.json > environments.json.tmp

mv environments.json.tmp environments.json
```

CLI 不会自动把任意 shell 环境变量映射为 EasyPostman 变量。

## 8. 文件上传

环境变量可保存 workspace 相对路径：

```text
fixtures/avatar.png
```

默认以 workspace 根目录解析。附件位于其他挂载目录时：

```bash
java -jar easy-postman.jar collection run "$WORKSPACE_DIR" \
  --working-dir "$CI_PROJECT_DIR/test-assets"
```

路径为空、仍含 `{{...}}`、文件不存在或不可读时会在发送前失败。

## 9. 参数与退出码

| 参数 | 说明 |
|---|---|
| `[workspace]` | workspace 名称、ID 或目录；CI 推荐目录或 `.` |
| `-w, --workspace` | workspace 的显式写法，不能与位置参数同时使用 |
| `-c, --collection` | 选择集合，可重复 |
| `--folder` | 选择文件夹，可重复 |
| `-e, --environment` | 环境名称或 ID |
| `-d, --iteration-data` | workspace 相对或绝对 CSV / JSON 路径 |
| `-n, --iteration-count` | 正整数总轮数 |
| `--working-dir` | 上传文件根目录 |
| `--out` | JSON 报告路径 |
| `--bail` | 首次请求、脚本或断言失败后停止 |
| `-h, --help` | 帮助 |

退出码：`0` 全部成功；`1` 请求、脚本或断言失败；`2` 参数或 workspace 数据无效。

报告 schema 为 `2.1`，`selectionMode` 固定为 `COLLECTIONS`。

## 10. 仓库示例

示例 workspace：[`docs/examples/collection-cli`](examples/collection-cli/)

```bash
java -jar easy-postman-app/target/easy-postman-*.jar \
  collection run docs/examples/collection-cli \
  -c "EasyPostman CLI Example" \
  --folder "Upload API" \
  -d users.csv \
  --bail \
  --out target/collection-run-result.json
```

该命令执行两轮、发送两个请求并完成六个断言。

功能测试 CLI 请参阅 [`FUNCTIONAL_CLI_zh.md`](FUNCTIONAL_CLI_zh.md)。
