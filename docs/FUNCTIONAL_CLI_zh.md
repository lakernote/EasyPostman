# EasyPostman Functional CLI

`functional run` 用于在开发机、CI Runner 或独立服务器上，无界面复现 EasyPostman GUI“功能测试”面板保存的请求选择、执行顺序和 CSV 多轮数据。

CLI 以完整 workspace 目录为运行单位。普通 workspace 应把整个目录复制到 CI；Git workspace 应在 CI checkout 后直接运行仓库目录，不要只复制 `functional_config.json`。

```bash
java -jar easy-postman.jar functional run <workspace-directory> [options]
```

`<workspace-directory>` 直接指定 workspace 所在目录，例如：

```bash
java -jar easy-postman.jar functional run /srv/api-workspace
```

## 下载最新 JAR

打开任一发布页，在最新 `v*` 版本的 Assets 中下载 `easy-postman-{版本号}.jar`：

- [GitHub 最新 Release](https://github.com/lakernote/easy-postman/releases/latest)
- [Gitee Releases（国内镜像）](https://gitee.com/lakernote/easy-postman/releases)

Gitee 附件同步可能稍晚；如果最新版本中暂时没有主 JAR，请使用 GitHub 下载地址。运行 CLI 需要 Java 17 或更高版本。

它不提供集合或文件夹筛选参数；执行范围只由 `functional_config.json` 决定。

## 1. Workspace 文件

标准 workspace 目录包含 4 个 JSON 文件：

```text
api-workspace/
├── collections.json          # 必需：请求内容、脚本和断言
├── environments.json         # 环境列表和活动环境
├── functional_config.json    # 必需：已选请求 ID、顺序和可选内嵌 CSV
└── performance_config.json   # 性能测试配置，functional run 不读取
```

文件职责：

- `functional_config.json` 决定运行哪些请求、执行顺序和默认迭代数据。
- `collections.json` 是请求内容的唯一事实来源。
- `requestItemId` 用于从最新 `collections.json` 查找请求。
- `environments.json` 提供环境变量。
- `performance_config.json` 属于标准 workspace，但不参与 `functional run`。
- 应用级 `global_variables.json` 从 EasyPostman 数据目录读取。
- CLI 只读 workspace，不会写回 GUI 配置。

## 2. 最短用法

```bash
java -jar easy-postman.jar functional run /srv/api-workspace
```

选择 CI 环境并生成报告：

```bash
java -jar easy-postman.jar functional run /srv/api-workspace \
  -e "CI" \
  --bail \
  --out target/functional-result.json
```

在 Git workspace 根目录：

```bash
java -jar /opt/easy-postman/easy-postman.jar functional run .
```

## 3. `functional_config.json`

GUI 功能测试面板保存的结构示例：

```json
{
  "version": "1.0",
  "rows": [
    {"selected": true, "requestItemId": "request-login"},
    {"selected": false, "requestItemId": "request-debug"}
  ],
  "csvState": {
    "sourceName": "users.csv",
    "headers": ["user", "role"],
    "rows": [
      {"user": "alice", "role": "admin"},
      {"user": "bob", "role": "user"}
    ]
  }
}
```

运行规则：

- 只执行 `selected: true` 的行。
- 按 `rows` 的保存顺序执行。
- 每个 `requestItemId` 必须能在 `collections.json` 中找到。
- 没有选中请求或请求 ID 已失效时退出码为 `2`，避免 CI 静默漏跑。
- `csvState.sourceName` 只是 GUI 展示来源，不会在运行时重新读取该文件。
- 真正的默认多轮数据是 `csvState.rows` 中内嵌的内容。

GUI 中刷新功能测试面板并保存，可以同步已删除或移动的请求 ID。

## 4. CSV / JSON 多轮驱动

### 4.1 使用内嵌 CSV

如果 `csvState.rows` 有两行：

```bash
java -jar easy-postman.jar functional run "$WORKSPACE_DIR"
```

默认执行两轮，无需再传 `-d`。

请求中使用 `{{user}}`，脚本中使用：

```javascript
pm.iterationData.get('user');
pm.variables.get('user');
pm.info.iteration;
pm.info.iterationCount;
```

将 `functional_config.json` 提交到 Git 等于提交其中的 CSV 行，因此不要内嵌真实密码、Token 或生产数据。

### 4.2 外部数据覆盖内嵌数据

CI 保留 GUI 请求选择，但使用 CI 专用数据：

```bash
java -jar easy-postman.jar functional run "$WORKSPACE_DIR" \
  -d /opt/easy-postman-data/ci-users.csv
```

此时请求列表仍来自 `functional_config.json`，迭代数据完全来自外部文件。相对 `-d` 路径从 workspace 根目录解析。

JSON 必须是对象数组：

```json
[
  {"user": "alice", "role": "admin"},
  {"user": "bob", "role": "user"}
]
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

GUI 机器保存后，应复制完整 workspace，包括 `collections.json` 和 `functional_config.json`。

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
  functional run "$CI_PROJECT_DIR/api-workspace" \
  -e "CI" \
  --bail \
  --out "$CI_PROJECT_DIR/target/functional-result.json"
```

如果 CI 使用专用数据，则把运行命令改为：

```bash
java -jar "$CI_PROJECT_DIR/tools/easy-postman.jar" \
  functional run "$CI_PROJECT_DIR/api-workspace" \
  -e "CI" \
  -d "$CI_PROJECT_DIR/test-data/ci-users.csv" \
  --bail \
  --out "$CI_PROJECT_DIR/target/functional-result.json"
```

workspace 也可以通过 CI Artifact、SCP、rsync、Docker volume 或 Kubernetes volume 交付。

## 6. Git workspace 在 CI 中运行

推荐维护流程：

1. 开发机 clone API workspace 仓库。
2. EasyPostman 中把 Git workspace 路径指向该目录。
3. GUI 中维护集合、环境、功能测试勾选和 CSV。
4. 保存后审查并提交 workspace 的 4 个 JSON 文件。
5. commit 并 push。
6. CI 每个 Job 使用干净 checkout。
7. 在仓库根目录运行 `functional run .`。

这样 CI 不依赖 GUI 机器在线，功能测试选择和请求内容也能随 Git 版本一起审查。

建议提交：

```text
collections.json
functional_config.json
environments.json
performance_config.json
```

不要提交真实 Token、Cookie、私钥、生产数据或运行报告。

### GitHub Actions

```yaml
name: EasyPostman Functional Test

on:
  push:
  pull_request:

jobs:
  functional-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Run functional tests
        run: |
          mkdir -p target
          java -jar tools/easy-postman.jar \
            functional run . \
            -e "CI" \
            --bail \
            --out target/functional-result.json

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: functional-result
          path: target/functional-result.json
```

业务代码和 API workspace 分仓：

```bash
git clone "$API_WORKSPACE_REPOSITORY" "$CI_PROJECT_DIR/api-workspace"

java -jar "$CI_PROJECT_DIR/tools/easy-postman.jar" \
  functional run "$CI_PROJECT_DIR/api-workspace" \
  -e "CI"
```

## 7. 环境、全局变量和 Secret

未传 `-e` 时优先使用 `active: true` 的环境；否则使用第一个环境。CI 建议固定 `-e "CI"`。

隔离应用级全局变量目录：

```bash
java -DeasyPostman.data.dir="$RUNNER_TEMP/easy-postman-data" \
  -jar tools/easy-postman.jar \
  functional run . \
  -e "CI"
```

Secret 不应写入 Git workspace。可以在 CI 临时 checkout 中用 `jq` 注入：

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

上传附件不属于标准 workspace 文件。附件由 CI 单独挂载时：

```bash
java -jar easy-postman.jar functional run "$WORKSPACE_DIR" \
  --working-dir "$CI_PROJECT_DIR/test-assets"
```

路径为空、仍含 `{{...}}`、文件不存在或不可读时会在发送前失败。

## 9. 参数、报告和退出码

| 参数 | 说明 |
|---|---|
| `[workspace-directory]` | workspace 所在目录；Git workspace 根目录可写 `.` |
| `-w, --workspace` | workspace 目录的显式写法，不能与位置参数同时使用 |
| `-e, --environment` | 环境名称或 ID |
| `-d, --iteration-data` | 覆盖内嵌数据的 CSV / JSON |
| `-n, --iteration-count` | 正整数总轮数 |
| `--working-dir` | 上传文件根目录 |
| `--out` | JSON 报告路径 |
| `--bail` | 首次请求、脚本或断言失败后停止 |
| `-h, --help` | 帮助 |

`functional run` 不接受 `-c` 或 `--folder`。

退出码：`0` 全部成功；`1` 请求、脚本或断言失败；`2` 参数、workspace 或功能配置无效。

报告 schema 为 `2.1`，其中：

- `selectionMode` 固定为 `FUNCTIONAL_CONFIG`。
- `iterationDataSource` 表示内嵌 CSV 来源、外部数据绝对路径或 `<none>`。
- 报告还包含 workspace、环境、轮数、请求结果和断言明细。

## 10. 仓库示例

示例 workspace：[`docs/examples/functional-cli`](examples/functional-cli/)

```bash
java -jar easy-postman-app/target/easy-postman-*.jar \
  functional run docs/examples/functional-cli \
  --bail \
  --out target/functional-run-result.json
```

该命令读取 `functional_config.json` 中的选中请求和两行内嵌 CSV，执行两轮、发送两个请求并完成四个断言。

集合 CLI 请参阅 [`COLLECTION_CLI_zh.md`](COLLECTION_CLI_zh.md)。
