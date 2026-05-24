# Performance JMeter Plan Model Design

## Goal

Refactor the performance execution internals toward a JMeter-style model: compile the Swing tree snapshot into an immutable `TestPlan`, then execute `ThreadGroup`, `Controller`, `Timer`, and `Sampler` elements through focused runtime classes.

## Scope

This is an internal architecture refactor. The UI, saved project format, tree node types, HTTP/WebSocket/SSE semantics, metrics, trend data, report data, and result-table behavior must remain compatible.

## Architecture

The run flow becomes:

1. `PerformanceRunControlSupport` snapshots the Swing tree as it does today.
2. `PerformanceExecutionEngine` compiles that snapshot into `PerformanceTestPlan`.
3. `PerformanceThreadGroupRunner` runs enabled thread groups according to `ThreadGroupData.ThreadMode`.
4. Each virtual user receives a `PerformanceIterationContext`.
5. `PerformancePlanExecutor` walks immutable plan elements.
6. `PerformanceSamplerExecutor` executes request samplers and records results.

`PerformanceExecutionEngine` remains the public package-level facade for existing callers. It owns cancellation resources, realtime metrics, and high-level lifecycle, but no longer contains controller traversal or thread-group scheduling details.

## Plan Model

The immutable plan model lives under `com.laker.postman.panel.performance.plan`.

- `PerformanceTestPlan`: root plan with enabled thread groups.
- `PerformanceThreadGroupPlan`: a compiled thread group with normalized `ThreadGroupData` and child elements.
- `PerformancePlanElement`: interface implemented by controllers, timers, protocol stages, assertions, and samplers.
- `PerformanceLoopController`: loop count plus nested elements.
- `PerformanceTimerElement`: timer delay data.
- `PerformanceRequestSampler`: original request node data plus compiled child elements. For WebSocket/SSE, protocol stage nodes stay nested under the sampler so existing protocol executors can still validate and execute their step sequence.
- `PerformanceAssertionElement`: immutable representation of assertion nodes used by HTTP, SSE await, and WebSocket await checks.
- `PerformanceProtocolStageElement`: immutable representation of WS/SSE stage nodes consumed directly by plan-based protocol validation and WebSocket scenario execution.

The compiler is `PerformanceTestPlanCompiler`. It reads the snapshot tree, skips disabled nodes where JMeter-style execution would skip them, normalizes `ThreadGroupData` and `LoopData`, and deep-copies mutable node data through the existing snapshot/tree-copy utilities.

Plan compilation is a one-way boundary from Swing tree snapshots into execution data. Plan elements do not expose APIs to rebuild Swing tree nodes; UI tree copying and paste behavior stay in the Swing tree support layer.

## Runtime Split

- `PerformanceThreadGroupRunner` owns FIXED, RAMP_UP, SPIKE, and STAIRS scheduling.
- `PerformanceVirtualUserCoordinator` remains the ThreadLocal owner for active user count, virtual user index, and iteration index.
- `PerformanceIterationContextFactory` creates `ExecutionVariableContext` and binds CSV row data for the current virtual user.
- `PerformancePlanExecutor` handles controllers and timers.
- `PerformanceSamplerExecutor` wraps `PerformanceRequestExecutor` and `PerformanceResultRecorder`. Request execution receives `PerformanceRequestSampler` directly; Swing tree snapshots are compiled before sampler dispatch.

## Compatibility Rules

- Existing `runJMeterTreeWithProgress(DefaultMutableTreeNode, int, BiConsumer)` remains available and compiles the incoming tree internally.
- Request IDs must be preserved during execution snapshots and regenerated only for paste, as today.
- HTTP request direct child timers still run after the HTTP sample. WebSocket request child stages continue to be interpreted by the WebSocket scenario executor.
- Disabled thread groups and disabled plan elements do not execute.
- CSV row mapping remains based on virtual user index modulo CSV row count.
- Cancellation still cancels OkHttp calls, active SSE sources, and active WebSockets.

## Testing

Add focused tests for:

- Compiling root/thread-group/loop/timer/request structures into immutable plan elements.
- Disabled nodes being skipped.
- Nested loop request estimates matching previous behavior.
- Fixed thread groups executing HTTP requests through the compiled plan.
- CSV iteration context preserving virtual user row selection.
- Existing WebSocket/SSE request executor tests continuing to pass.
