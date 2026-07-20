# 集合无头运行 CLI

EasyPostman 提供一个轻量的 Postman Collection v2.1 运行器，适合本地批量执行和 CI。它直接复用桌面端的请求准备、变量替换、前置/测试脚本和 HTTP 传输实现，不需要安装 Node.js 或 Newman。

## 1. 准备 Java 17

CLI 和桌面端使用同一个跨平台 JAR，需要 Java 17 或更高版本：

```bash
java -version
```

## 2. 获取可运行 JAR

### 方式一：下载 Release JAR

1. 打开 [GitHub Releases](https://github.com/lakernote/easy-postman/releases)。
2. 在目标版本的 Assets 中下载 `easy-postman-{版本号}.jar`。
3. 可选：将文件重命名为固定名称，后续升级时命令不需要跟着改版本号。

macOS / Linux：

```bash
mv easy-postman-6.x.x.jar easy-postman.jar
java -jar easy-postman.jar collection run --help
```

Windows PowerShell：

```powershell
Rename-Item easy-postman-6.x.x.jar easy-postman.jar
java -jar easy-postman.jar collection run --help
```

如果帮助中没有 `collection run`，说明该 Release 尚未包含此功能，请下载更新版本或使用下面的源码构建方式。

### 方式二：从源码构建

需要 JDK 17+ 和 Maven 3.6+：

```bash
git clone https://github.com/lakernote/easy-postman.git
cd easy-postman
mvn -pl easy-postman-app -am -DskipTests clean package
```

构建产物位于 `easy-postman-app/target/easy-postman-{版本号}.jar`。先确认命令可用：

```bash
java -jar easy-postman-app/target/easy-postman-*.jar \
  collection run --help
```

## 3. 运行仓库内完整示例

仓库提供了一个可以直接执行的 multipart 上传示例：

```text
docs/examples/collection-cli/
├── upload.postman_collection.json
├── postman-echo.postman_environment.json
├── users.csv
└── fixtures/
    └── sample-file.txt
```

它会读取 `users.csv` 的 `alice`、`bob` 两行数据，分别向 Postman Echo 上传一次 `sample-file.txt`，并执行状态码、上传结果和迭代变量断言。

从仓库根目录运行：

```bash
java -DCONSOLE_LOG_LEVEL=ERROR \
  -jar easy-postman-app/target/easy-postman-*.jar \
  collection run docs/examples/collection-cli/upload.postman_collection.json \
  -e docs/examples/collection-cli/postman-echo.postman_environment.json \
  -d docs/examples/collection-cli/users.csv \
  --folder "Upload API" \
  --bail \
  --out target/collection-cli-result.json
```

使用下载并重命名后的 JAR 时，只需要替换 `-jar` 后的路径：

```bash
java -DCONSOLE_LOG_LEVEL=ERROR \
  -jar /path/to/easy-postman.jar \
  collection run docs/examples/collection-cli/upload.postman_collection.json \
  -e docs/examples/collection-cli/postman-echo.postman_environment.json \
  -d docs/examples/collection-cli/users.csv \
  --bail \
  --out target/collection-cli-result.json
```

成功输出示例：

```text
Iteration 1/2
→ POST EasyPostman Collection CLI Example / Upload API / Upload fixture
  200 674ms PASS
    ✓ status is 200
    ✓ uploaded file is present
    ✓ iteration data reached the server
Iteration 2/2
...
Collection run completed: status=SUCCESS iterations=2 total=2 passed=2 failed=0 tests=6/6
```

`-DCONSOLE_LOG_LEVEL=ERROR` 只收敛框架日志，不会隐藏集合执行进度和测试结果；排查问题时可以去掉它。

## 4. 运行自己的集合

最小命令：

```bash
java -jar easy-postman.jar \
  collection run ./demo.postman_collection.json
```

常用完整命令：

```bash
java -jar easy-postman.jar \
  collection run ./demo.postman_collection.json \
  -e ./local.postman_environment.json \
  -g ./globals.postman_globals.json \
  -d ./users.csv \
  -n 2 \
  --folder UserApi \
  --working-dir ./test-assets \
  --bail \
  --out ./result.json
```

## 5. 参数

| 参数 | 说明 |
|------|------|
| `-e, --environment <file>` | Postman 环境变量文件 |
| `-g, --globals <file>` | Postman 全局变量文件 |
| `-d, --iteration-data <file>` | CSV 或 JSON 迭代数据 |
| `-n, --iteration-count <count>` | 迭代次数；未指定时，有数据文件则按数据行数，否则执行 1 次 |
| `--folder <name>` | 只运行指定文件夹；可重复传入 |
| `--working-dir <dir>` | 文件上传的相对路径根目录；默认是集合文件所在目录 |
| `--out <file>` | 写入结构化 JSON 结果报告 |
| `--bail` | 第一个请求错误或测试断言失败后停止 |
| `-h, --help` | 显示帮助 |

## 6. 文件上传

`form-data` 文件字段读取 Postman Collection 中的 `src`：

```json
{
  "body": {
    "mode": "formdata",
    "formdata": [
      {
        "key": "document",
        "type": "file",
        "src": "fixtures/sample-file.txt"
      }
    ]
  }
}
```

二进制请求体读取 `body.file.src`。相对路径默认以集合文件所在目录为基准，因此把集合和 `fixtures/` 一起提交到仓库即可直接在本地或 CI 运行。需要从其他目录取文件时使用 `--working-dir`。静态文件不存在或不可读时，CLI 会在发送请求前失败。

## 7. 迭代数据

CSV 第一行是变量名，后续每行执行一次：

```csv
userId,name
1001,Alice
1002,Bob
```

JSON 数据必须是对象数组：

```json
[
  {"userId": 1001, "name": "Alice"},
  {"userId": 1002, "name": "Bob"}
]
```

集合中的 `{{userId}}`、`{{name}}` 以及脚本中的 `pm.iterationData` 都可以读取当前行。显式指定 `-n` 且次数大于数据行数时，会循环使用数据行。

## 8. 文件夹筛选

`--folder <name>` 用于只运行 Collection 中指定文件夹及其所有子文件夹里的请求。不传该参数时运行整个 Collection。

例如集合结构：

```text
用户接口
├── 登录
├── 查询用户
└── 管理员
    └── 删除用户

订单接口
├── 创建订单
└── 查询订单
```

只运行“用户接口”及其子文件夹：

```bash
java -jar easy-postman.jar \
  collection run demo.postman_collection.json \
  --folder "用户接口"
```

同时运行多个文件夹时重复传入参数，匹配结果按“或”合并：

```bash
java -jar easy-postman.jar \
  collection run demo.postman_collection.json \
  --folder "用户接口" \
  --folder "订单接口"
```

匹配规则：

- 按文件夹名称精确、区分大小写匹配，不做模糊或部分匹配。
- 选中父文件夹后，会递归运行其所有子文件夹中的请求。
- Collection 中存在多个同名文件夹时，这些文件夹都会运行。
- 重复传入多个 `--folder` 时，只要请求属于其中任意一个文件夹就会运行。
- 文件夹名称含空格或中文时，建议使用引号。
- 没有任何请求匹配时不会静默成功，而是输出错误并返回退出码 `2`。

这适合在 CI 中按模块拆分集合，例如只运行本次改动涉及的“用户接口”。

## 9. 退出码

| 退出码 | 含义 |
|--------|------|
| `0` | 所有已执行请求和测试通过 |
| `1` | HTTP 执行、脚本或测试断言失败 |
| `2` | 命令参数或输入文件错误 |

在 shell 中可以直接读取退出码：

```bash
java -jar easy-postman.jar collection run ./demo.postman_collection.json
echo $?
```

Windows PowerShell 使用 `$LASTEXITCODE`。

## 10. GitHub Actions 示例

建议在 CI 中固定 EasyPostman 版本，避免构建结果随最新版变化。把 `EASY_POSTMAN_VERSION` 替换为已经包含 `collection run` 的 Release 版本：

```yaml
name: API collection tests

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

      - name: Download EasyPostman CLI
        env:
          EASY_POSTMAN_VERSION: "6.x.x"
        run: |
          curl -fL \
            "https://github.com/lakernote/EasyPostman/releases/download/v${EASY_POSTMAN_VERSION}/easy-postman-${EASY_POSTMAN_VERSION}.jar" \
            -o easy-postman.jar

      - name: Run collection
        run: |
          java -DCONSOLE_LOG_LEVEL=ERROR -jar easy-postman.jar \
            collection run api/upload.postman_collection.json \
            -e api/ci.postman_environment.json \
            -d api/users.csv \
            --bail \
            --out build/collection-result.json

      - name: Upload collection report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: collection-result
          path: build/collection-result.json
          if-no-files-found: ignore
```

## 11. 当前范围

这是面向 EasyPostman 的轻量运行器，不是 Newman 的完整兼容实现。目前聚焦 Postman Collection v2.1 的 HTTP 请求、集合/文件夹继承、环境与全局变量、CSV/JSON 迭代数据、前置/测试脚本、`form-data`/binary 文件上传、文件夹筛选、`--bail` 和 JSON 报告。暂不支持 Newman 的外部 reporter 生态、云端 collection URL 和全部 Newman 命令参数。
