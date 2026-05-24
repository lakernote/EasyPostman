# 性能压测模块设计

本文档说明 `com.laker.postman.panel.performance` 下压测功能的当前设计边界，重点覆盖 UI、执行模型、线程组模型、指标采样和结果刷新。

## 设计目标

- `PerformancePanel` 只负责组装 Swing 视图、绑定事件和协调生命周期，不承载压测执行细节。
- 压测执行和 UI 刷新解耦：请求执行在线程组 worker 中完成，统计快照在专用后台线程中生成，Swing 组件只在 EDT 更新。
- 线程模型集中管理：所有性能压测后台线程通过统一工厂命名和设置 daemon，便于调试、关闭和日志排查。
- 指标数据增量采样：总报表使用累计统计，趋势图使用采样窗口统计，WebSocket/SSE 长连接使用实时计数补充窗口内没有完成样本的情况。
- 高并发结果显示保持有界：结果表通过队列和 Swing Timer 分批刷新，避免每个请求都直接触发 EDT 更新。

## 主要组件

### UI 编排

- `PerformancePanel`：模块入口，创建树、属性面板、结果页、执行引擎、统计协调器和定时器。
- `PerformancePanelViewFactory`：集中创建 Swing 组件，降低 `PerformancePanel` 的 UI 构建代码量。
- `PerformanceTreeSupport` / `PerformanceTreeInteractionSupport`：管理树节点结构、右键菜单、节点选择和请求结构同步。
- `PerformancePropertyPanelSupport`：保存当前属性面板数据，屏蔽线程组、断言、定时器、SSE、WebSocket 配置面板的细节。
- `PerformanceRunControlSupport`：处理启动、停止、按钮状态、最终 flush 和完成通知。

### 执行模型

- `PerformanceTestPlanCompiler`：将执行快照从 Swing `DefaultMutableTreeNode` 编译成不可变的 JMeter 风格执行计划。计划由 `PerformanceTestPlan`、`PerformanceThreadGroupPlan`、`PerformanceLoopController`、`PerformanceTimerElement`、`PerformanceRequestSampler`、断言元素和协议阶段元素组成。
- `PerformanceExecutionEngine`：执行门面，负责运行生命周期、实时指标、网络取消资源和旧树入口兼容。它不再直接遍历 Swing tree。
- `PerformanceThreadGroupRunner`：执行启用的线程组，并根据 FIXED、RAMP_UP、SPIKE、STAIRS 调度虚拟用户 worker。
- `PerformancePlanExecutor`：执行线程组内的控制器模型，按顺序处理 Loop、Timer 和 Request Sampler。
- `PerformanceSamplerExecutor`：把 request sampler 交给 `PerformanceRequestExecutor`，并通过 `PerformanceResultRecorder` 记录结果。热路径直接消费 plan model；旧的 tree-based request 入口只作为兼容层，会先编译成 request sampler。
- `PerformanceIterationContextFactory`：为每次虚拟用户迭代创建 `ExecutionVariableContext`，设置迭代编号，并按虚拟用户编号绑定 CSV 行。
- `PerformanceThreadGroupPlanner`：基于编译后的 plan 计算线程数和预估请求量。旧的 tree-based 方法保留，但内部会先编译 plan。
- `PerformanceVirtualUserCoordinator`：维护虚拟用户上下文，包括 active user 数、虚拟用户编号、当前迭代编号和进度回调。

## 线程模型

### 线程类型

- `PerformanceRun-*`：一次压测运行的顶层后台线程，由 `PerformanceRunControlSupport` 创建。
- `PerformanceThreadGroup-*`：根节点下每个启用线程组的调度入口。
- `PerformanceFixedWorker-*`：固定线程模式 worker。
- `PerformanceRampScheduler-*` / `PerformanceRampWorker-*`：递增模式的调度线程和 worker。
- `PerformanceSpikeScheduler-*` / `PerformanceSpikeWorker-*`：尖刺模式的调度线程和 worker。
- `PerformanceStairsScheduler-*` / `PerformanceStairsWorker-*`：阶梯模式的调度线程和 worker。
- `PerformanceTimer-*`：趋势图采样和报表刷新定时触发器。
- `PerformanceMetrics-*`：统计快照和趋势窗口计算线程。
- `PerformanceStopFlush-*`：停止压测后的最终 UI flush 协调线程。

所有这些线程都通过 `PerformanceThreadFactory` 创建，统一设置线程名、daemon 属性和未捕获异常日志。

### 停止语义

停止压测时按以下顺序处理：

1. 将 running 状态置为 false，并 interrupt 顶层运行线程。
2. 取消 OkHttp 请求、SSE EventSource 和 WebSocket。
3. 停止采样/刷新定时器。
4. 异步等待短暂统计收敛，然后在 EDT 上 flush 结果表、采样最后一次趋势数据、刷新报表。
5. 恢复 OkHttp 默认连接池和 dispatcher 配置。

`PerformanceTimerManager.stopAll()` 在非 EDT 调用时会等待 scheduler 关闭；如果从 EDT 调用，只取消任务并关闭 scheduler，不等待 termination，避免停止按钮卡住界面。

## 指标模型

### 累计统计

`PerformanceStatsCollector` 使用同步方法维护累计统计：

- 按协议和 API 聚合请求数、成功数、失败数。
- 使用直方图计算耗时分位数，避免保存所有请求耗时。
- WebSocket/SSE 额外记录消息数、匹配数、首消息延迟和主要完成原因。

`snapshot()` 生成报表快照，不保留单请求结果。

### 趋势窗口

`sampleTrendSnapshot(...)` 读取当前窗口内的增量统计，然后重置窗口计数。HTTP 的 QPS、失败率和耗时来自已完成请求；WebSocket/SSE 的实时消息速率、首消息延迟和活跃会话时长来自 `PerformanceRealtimeMetrics`，用于补齐长连接运行中尚未结束的样本。

### UI 刷新

`PerformanceStatisticsCoordinator` 负责连接统计数据和 UI：

- `refreshReport()` 先切到 EDT 检查当前是否在报表 Tab，未查看时报表刷新直接跳过。
- 报表快照和趋势窗口计算提交到单线程 `PerformanceMetrics-*`，串行执行，避免占用 common pool。
- 所有 Swing 组件更新通过 `SwingUtilities.invokeLater` 回到 EDT。

## 结果表刷新

`PerformanceResultTablePanel` 不直接在请求线程更新表格：

- 请求线程只把 `ResultNodeInfo` 放入 `pendingQueue`。
- Swing Timer 每 200ms 在 EDT 上批量拉取最多 2000 条结果并追加到 TableModel。
- 停止或完成时调用 `flushPendingResults()`，确保队列中的结果最终显示。
- 搜索使用 300ms 防抖，避免每次输入都对大结果集做过滤。

## 扩展新线程模式

新增线程组模式时建议按以下步骤处理：

1. 在 `ThreadGroupData.ThreadMode` 中增加模式及配置字段，并在 `normalize()` 中定义边界。
2. 在 `ThreadGroupPropertyPanel` 中增加配置 UI 和预览数据。
3. 在 `PerformanceThreadGroupPlanner` 中增加最大线程数和请求量估算。
4. 在 `PerformanceThreadGroupRunner` 中增加调度方法，worker 生命周期交给 `PerformanceVirtualUserCoordinator`。
5. 增加针对估算、调度边界、停止语义的单元测试。

## 维护原则

- Swing 组件只在 EDT 读写。
- 压测执行只能消费 `PerformanceTestPlanCompiler` 生成的执行计划，不直接依赖可变 UI 树。
- 执行线程不直接操作 UI，只通过进度回调、结果队列和统计协调器传递状态。
- 线程创建不要直接 `new Thread(...)`，使用 `PerformanceThreadFactory`。
- 指标计算不要使用 `CompletableFuture.runAsync(...)` 默认线程池，使用模块内专用执行器。
- 高并发路径不保存不必要的明细对象；需要展示明细时由高效模式控制保留范围。
