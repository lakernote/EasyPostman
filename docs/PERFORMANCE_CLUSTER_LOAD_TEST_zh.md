# 集群压测使用指南

本文面向使用者，说明如何把 GUI 中配置好的性能测试计划分发到多台服务器执行。底层行为类似 JMeter 的 master/worker：master 只负责分发、轮询、停止和汇总，真正的请求压力由各 worker 进程产生。

## 核心概念

- `plan.json`：从 GUI 导出的可执行压测计划。它包含环境变量、全局变量、执行设置、线程组、请求、脚本、断言、CSV inline 数据和外部资产引用。
- master：GUI 远程模式或 `performance master run` 命令。master 不直接压目标服务，只把计划和分片 assignment 发给 worker。
- worker：服务器上的 `performance worker` 进程。worker 只在收到 master 分配的 assignment 后执行，避免多台机器各自完整跑一份计划导致总并发放大。
- 总并发：GUI 中每个 Thread Group 配置的用户数是全局总并发，不是每台 worker 的并发。比如 100 用户、2 台 worker，默认分成 50/50；101 用户、2 台 worker，默认分成 51/50。
- CSV 分片：CSV Data Set 会跟随虚拟用户全局区间分片，避免所有 worker 都从第 0 行开始读。CSV 行数少于虚拟用户数时，会按全局用户编号循环复用。

## 什么时候使用集群压测

适合：

- 单机 CPU、网络、文件描述符或目标连接数不足，需要多台机器共同施压。
- 需要在无桌面服务器上长期跑压测。
- GUI 负责编辑、观察和停止，worker 在专用压测机上执行。

不适合：

- 只需要几百并发的快速验证，单机 GUI 或 CLI 更简单。
- 需要严格的跨 worker 全局 P99 原始样本合并。目前 master 聚合的是各 worker 的报告结果，失败/慢请求明细按 worker 有界保留后拉回。

## 准备工作

1. 所有机器使用相同版本的 EasyPostman JAR 或安装包。
2. 每台 worker 机器能访问目标 API。
3. master 能访问每台 worker 的监听端口。
4. 如果计划引用本地文件，例如 file-source CSV、multipart 文件，请把文件提前放到每台 worker 的相同路径。GUI 不会自动上传本地文件。
5. 如果 CSV 是在 GUI 中手工创建或导入并内嵌到计划里的，不需要额外上传文件。

## 启动 worker

在每台压测机上启动 worker：

```bash
java -jar easy-postman-5.5.28.jar \
  performance worker \
  --host 0.0.0.0 \
  --port 19090
```

`--progress-interval <seconds>` 只控制 worker 控制台多久打印一次进度，例如 users、total、success、failed、qps。它不影响 master 轮询频率，也不影响压测请求执行。默认就是 1 秒，因此一般可以不写。

关闭控制台进度输出：

```bash
java -jar easy-postman-5.5.28.jar \
  performance worker \
  --host 0.0.0.0 \
  --port 19090 \
  --no-progress
```

本机验证两个 worker：

```bash
java -jar easy-postman-5.5.28.jar performance worker --host 127.0.0.1 --port 19090
java -jar easy-postman-5.5.28.jar performance worker --host 127.0.0.1 --port 19091
```

## GUI 远程执行

1. 在性能测试页面配置一个或多个 Thread Group。
2. 顶部勾选 `远程`。
3. 在 `Workers` 输入框填写 worker 列表，支持逗号或空白分隔：

```text
10.0.0.11:19090,10.0.0.12:19090
```

4. 点击开始。GUI 会作为 master 生成分片 assignment，并把当前计划发送给各 worker。
5. 点击停止时，GUI 会向所有 worker 发送 stop。
6. 结束后，GUI 会拉取每个 worker 的最终 report 并聚合到“报表”；失败/慢请求明细会在合适的收尾阶段拉回到“结果表”。

## CLI master 执行

先从 GUI 导出 `plan.json`，再运行：

```bash
java -jar easy-postman-5.5.28.jar \
  performance master run \
  --plan /path/to/plan.json \
  --workers 10.0.0.11:19090,10.0.0.12:19090 \
  --out /tmp/easy-postman-master-result.json
```

单机无 GUI 执行同一份计划：

```bash
java -jar easy-postman-5.5.28.jar \
  performance run \
  --plan /path/to/plan.json \
  --out /tmp/easy-postman-result.json
```

## 实时刷新如何工作

单机 `performance run` 和 `performance master run` 都会在控制台打印进度。指定 `--out` 后，输出文件会先变为 `PENDING`，运行中通过同目录临时文件加原子替换持续更新为 `RUNNING` 快照，结束后替换为完整最终报告；失败时也会写入本次 `FAILED` 报告，避免残留上一次结果。两种命令的最终 JSON 均为顶层 `PerformanceJsonReport`，可统一使用 `.summary.totalRequests` 等路径读取。

GUI 远程模式运行时，每 1 秒向每个 worker 查询一次状态：

- 关闭“实时刷新”且关闭“启用趋势”时：请求 `/api/performance/v1/runs/{runId}?report=false`，只拉轻量状态，包含活跃用户数、总用户数、请求数、失败数、QPS 和运行状态。
- 开启“实时刷新”或“启用趋势”时：请求 `/api/performance/v1/runs/{runId}`，worker 会构建当前聚合 report 快照返回。实时报表用它刷新“报表”页；趋势图用它读取 HTTP/WS/SSE 的协议级累计计数。
- “启用趋势”使用同一轮状态数据做趋势采样，不额外增加 worker 请求次数；趋势采样间隔取用户设置，最低 1 秒。WS/SSE 的发送消息/秒、接收消息/秒必须依赖聚合 report，否则轻量 status 只能得到总请求数，无法区分协议消息计数。
- 运行结束后：master 拉取 `/result` 获取最终报告，再拉取 `/details` 获取失败/慢请求明细。

因此，worker 数越多，master 每秒控制面请求数约为 `worker 数量`。开启实时报表或趋势时，每个响应体更大、worker 需要构建聚合 report 快照，但不会返回失败明细或响应体列表；短压测或极限吞吐压测建议按需关闭实时报表，只看趋势和最终报表。

## worker 内存和性能边界

worker 不保存所有请求明细，避免高 QPS 时内存无限增长：

- 聚合统计使用按 API / 协议维度的统计对象，不按请求数线性保存完整响应。
- efficient mode 开启时，结果明细只保留失败请求和慢请求，最多保留 1000 条。
- efficient mode 关闭时，结果明细按“性能结果表行数限制”保留，超过后丢弃最旧明细。
- 响应体会按预览策略裁剪，结果表渲染不需要的字段会被简化。
- worker 完成后的 run 状态默认保留 30 分钟，过期后会自动清理，避免 `/result` 和 `/details` 长期占用内存。

建议：

- 大流量或长时间压测开启 efficient mode。
- 不需要运行中报表时关闭“实时刷新”。
- 结果表行数不要设置过大，失败排查通常看最近失败和慢请求即可。
- 每台 worker 使用独立压测机，避免 GUI、目标服务和 worker 抢同一台机器资源。

## 控制面接口

| Method | Path | 说明 |
|---|---|---|
| `GET` | `/api/performance/v1/health` | worker 探活。 |
| `POST` | `/api/performance/v1/runs` | 提交一次运行，body 包含完整 `plan` 和该 worker 的 `assignment`。 |
| `GET` | `/api/performance/v1/runs/{runId}?report=false` | 轻量状态轮询，关闭实时报表和趋势时使用。 |
| `GET` | `/api/performance/v1/runs/{runId}` | 带运行中聚合 report 的状态轮询，开启实时报表或趋势时使用。 |
| `POST` | `/api/performance/v1/runs/{runId}/stop` | 停止指定运行。 |
| `GET` | `/api/performance/v1/runs/{runId}/result` | 拉取最终 JSON report。 |
| `GET` | `/api/performance/v1/runs/{runId}/details` | 拉取失败/慢请求明细，用于 GUI 结果表。 |

控制面请求不计入压测 report 的请求数和 QPS。

## 常见问题

### 为什么两个 worker 不一定比一个 worker 快？

分布式压测只解决“施压端瓶颈”。如果瓶颈在目标服务、网络回环、master/worker 同机竞争 CPU、短压测初始化开销、脚本执行或 HTTP 连接设置上，多 worker 不一定更快。建议至少跑 30 秒以上，并把 worker 放到独立机器上测试。

### `plan.json` 上传时间算不算压测时间？

不算。master 发送 `plan + assignment` 属于控制面分发，不计入最终 report 的 `elapsedTimeMs` 和 QPS。worker 控制台的运行耗时从 worker 异步执行开始计算，可能包含 worker 内部 plan 编译、脚本池初始化和分片应用。

### worker 是否需要打开 GUI？

不需要。`performance worker`、`performance run` 和 `performance master run` 都会走 headless 启动路径，不创建 Swing 主窗口。
