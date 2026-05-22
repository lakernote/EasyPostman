# JMeter-Aligned Performance Module Design

## Goal

Evolve EasyPostman's performance module toward JMeter's component model while preserving the current EasyPostman execution engine, Collection integration, workspace persistence, and result reporting. This phase does not implement JMX import or export.

## Scope

In scope:

- Introduce a JMeter-like internal performance plan model.
- Add first-class concepts for common HTTP load-testing components: Thread Group, Loop Controller, HTTP Request Sampler, Header Manager, Cookie Manager, CSV Data Set Config, Constant Timer, Response Assertion, and Listener.
- Keep Collection requests as reusable request assets referenced by performance request nodes.
- Add a runtime resolver that combines Collection-derived request data with performance-plan scoped overrides.
- Improve the performance tree naming, add-menu grouping, and property panel structure so the UI is easier to understand for users familiar with JMeter.

Out of scope:

- JMX import.
- JMX export.
- Embedding or executing Apache JMeter Engine.
- Large package/class renames in the first phase.
- Rewriting `PerformanceExecutionEngine`.
- Full compatibility with every JMeter component or script/runtime behavior.

## Current System

The current performance module already has a JMeter-like tree under `easy-postman-app/src/main/java/com/laker/postman/panel/performance`:

- `PerformancePanel` assembles the Swing UI, result panels, persistence, and execution lifecycle.
- `JMeterTreeNode` and `NodeType` represent the tree node data.
- `PerformanceExecutionEngine` executes enabled thread groups and request nodes.
- `ThreadGroupData`, `LoopData`, `TimerData`, and `AssertionData` store existing component configuration.
- `PerformancePersistenceService` stores the performance plan per workspace in `performance_config.json`.
- Request nodes store a `HttpRequestItem` copy, while persistence stores only the Collection request id and reloads the latest Collection request on load.
- `PreparedRequestBuilder` applies Collection group inheritance before building a `PreparedRequest`.
- `CsvDataPanel` and `IterationDataRuntimeSupport` already provide performance CSV iteration data.

This is a strong base. The main gap is that component boundaries are not yet explicit enough to support a maintainable JMeter-style model and future JMX mapping.

## Architecture

The first phase adds a JMeter-like plan model around the existing tree and execution engine:

```text
Collection request assets
  + Performance plan scoped components
  -> Performance runtime resolver
  -> PreparedRequest / execution node data
  -> existing PerformanceExecutionEngine
```

The existing `JMeterTreeNode` and `NodeType` remain in place during this phase. A new model layer is introduced and mapped to/from the existing tree data, so implementation can move in small steps without destabilizing the UI, persistence, and result reporting paths.

Recommended package:

```text
easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan
```

This package stays in the app module because the first phase is host UI/runtime behavior, not shared plugin SPI.

## New Model

Create a JMeter-like internal model:

```java
public final class PerformanceTestPlan {
    private PerformancePlanNode root;
    private PerformancePlanSettings settings;
}

public final class PerformancePlanNode {
    private String id;
    private String name;
    private PerformanceElementType type;
    private boolean enabled;
    private Object data;
    private List<PerformancePlanNode> children;
}

public enum PerformanceElementType {
    TEST_PLAN,
    THREAD_GROUP,
    LOOP_CONTROLLER,
    HTTP_SAMPLER,
    HEADER_MANAGER,
    COOKIE_MANAGER,
    CSV_DATA_SET,
    CONSTANT_TIMER,
    RESPONSE_ASSERTION,
    SUMMARY_REPORT,
    VIEW_RESULTS_TREE,
    TREND_LISTENER,
    SSE_SAMPLER,
    WEBSOCKET_SAMPLER,
    WEBSOCKET_SEND,
    WEBSOCKET_AWAIT,
    WEBSOCKET_CLOSE
}
```

The model should avoid depending on Swing classes. Swing tree nodes can be converted through a mapper:

```text
DefaultMutableTreeNode + JMeterTreeNode
  <-> PerformancePlanNode
```

## Component Data

Add explicit data classes for common JMeter-style components:

- `ThreadGroupElementData`: wraps or replaces `ThreadGroupData` over time.
- `LoopControllerData`: wraps or replaces `LoopData` over time.
- `HttpSamplerData`: references the Collection request id and stores request-node-local execution metadata.
- `HeaderManagerData`: stores enabled headers scoped to the performance plan.
- `CookieManagerData`: stores load-test cookie behavior, separate from the global Cookie Jar.
- `CsvDataSetData`: stores CSV source name, variable names, rows, sharing behavior, and iteration behavior.
- `ConstantTimerData`: wraps or replaces `TimerData`.
- `ResponseAssertionData`: wraps or replaces `AssertionData` with clearer JMeter-style assertion fields.
- `ListenerData`: stores listener type and display settings for Summary Report, View Results Tree, and Trend.

Existing data classes can be reused internally during the first implementation step. The new class names provide the target shape and make future JMX mapping easier.

## Collection Boundary

Collection remains the source of reusable request assets:

- URL, method, body, query params, form data, request settings.
- Request-level auth, scripts, and headers.
- Folder-level inherited auth, scripts, variables, and headers.
- Environment and global variable resolution.

Performance Plan stores runtime load-test configuration:

- Header Manager overrides.
- Cookie Manager behavior.
- CSV Data Set values.
- Timers.
- Assertions.
- Listener choices.
- Thread scheduling and controllers.

Performance components must not write back into Collection requests by default. This prevents a load-test-specific header, cookie setting, or CSV data source from polluting normal API debugging.

## Runtime Resolution

Add a runtime resolver used by performance execution before each request is sent:

```text
HttpRequestItem from Collection
  -> PreparedRequestBuilder applies Collection inheritance
  -> PerformanceRuntimeResolver applies plan-scoped config
  -> PreparedRequest used by PerformanceRequestExecutor
```

Recommended class:

```java
final class PerformanceRuntimeResolver {
    PreparedRequest resolve(
        JMeterTreeNode requestNode,
        PerformanceExecutionContext context
    );
}
```

The first implementation may adapt around `PerformanceRequestExecutor` and `PreparedRequestBuilder` instead of replacing their signatures. The resolver should be introduced behind a narrow interface so execution behavior can be migrated gradually.

## Header Manager

Header Manager is a Performance Plan scoped config element.

Rules:

- It does not mutate `HttpRequestItem.headersList`.
- It applies only during performance execution.
- It can live under a Thread Group, Loop Controller, or HTTP Sampler.
- Lower or nearer Header Managers override higher or outer Header Managers with the same case-insensitive key.
- Performance Header Manager overrides Collection inherited headers with the same key.

Effective order:

```text
Collection inherited headers
  -> Thread Group Header Manager
  -> Controller Header Manager
  -> HTTP Sampler Header Manager
  -> request-node-local overrides
```

## Cookie Manager

Cookie Manager is separated from the global debug Cookie Jar.

Rules:

- Performance execution uses a load-test cookie context.
- Cookie state must not be written into the global `CookieService` by default.
- The first phase should support an isolated per-virtual-user cookie context.
- The first phase should support clearing cookies at the beginning of each virtual user run.
- Static Cookie Manager entries may be supported if they can be represented safely without mutating global cookies.

This avoids concurrency and contamination problems during load testing.

## CSV Data Set

CSV Data Set is a Performance Plan scoped data source.

Rules:

- It does not write variables into Collection.
- It feeds `IterationDataRuntimeSupport` and `IterationDataVariableService`.
- The first phase can continue using `CsvDataPanel.CsvState` as the underlying state.
- UI naming should become `CSV Data Set Config`.
- Runtime behavior should remain compatible with the current `pm.iterationData` and `{{variable}}` support.

## UI Design

Tree naming and menus should become JMeter-like without losing EasyPostman's usability.

Tree menu grouping:

- Threads: Add Thread Group.
- Samplers: Add HTTP Request.
- Logic Controllers: Add Loop Controller.
- Config Elements: Add Header Manager, Cookie Manager, CSV Data Set Config.
- Timers: Add Constant Timer.
- Assertions: Add Response Assertion.
- Listeners: Add Summary Report, View Results Tree, Trend View.

Property panels should show:

- Component summary and scope.
- Core fields first.
- Advanced fields collapsed or visually secondary.
- Compatibility or future-JMX notes only where useful.

The request property view continues to reuse the existing request editor. It should also show that the request comes from Collection and should support refreshing from Collection.

## Persistence

Maintain backward compatibility with existing `performance_config.json`.

Recommended persistence strategy:

- Keep reading existing version `1.0`.
- Add a new version when the new plan model is persisted.
- During load, convert legacy `JMeterTreeNode` JSON into the new plan model and then into the Swing tree.
- During save, persist enough data to reconstruct the new component scopes.
- Existing workspaces should continue loading without user action.

## Error Handling

Runtime resolver errors should fail narrowly:

- Invalid disabled Header Manager rows are ignored.
- Invalid enabled Header Manager rows are recorded as request execution failures only when they prevent request construction.
- Missing Collection request ids keep the current behavior: warn and remove or skip the node during refresh/load.
- CSV with no rows behaves like no iteration data.
- Cookie Manager context creation failure fails the run startup with a clear UI message.

Configuration validation should happen in property panels before save where possible.

## Testing

Model tests:

- `PerformancePlanNode` preserves id, name, type, enabled state, data, and children.
- Legacy `JMeterTreeNode` converts to `PerformancePlanNode` and back without losing existing Thread Group, Loop, Request, Timer, Assertion, SSE, and WebSocket data.

Header Manager tests:

- Collection inherited headers are preserved when no Header Manager exists.
- Thread Group Header Manager applies to child samplers.
- Sampler-level Header Manager overrides Thread Group and Collection headers case-insensitively.
- Disabled Header Manager rows are ignored.

Cookie Manager tests:

- Performance cookie context does not modify `CookieService` global cookies.
- Per-virtual-user cookie contexts are isolated.
- Clear-on-start behavior removes prior virtual-user cookies.

CSV Data Set tests:

- Existing CSV state still exposes variables through `{{variable}}` and `pm.iterationData`.
- Empty CSV state does not fail request execution.
- CSV Data Set node persistence round-trips.

UI tests:

- Swing UI tests that instantiate panels must extend `AbstractSwingUiTest` and skip cleanly in headless/no-display environments.
- Tree menu visibility should be covered for common node types.
- Property panels should not require a real display in non-visual tests.

Regression tests:

- `mvn -q -pl easy-postman-app -am -DskipTests compile`
- Focused TestNG tests for new plan model, mappers, and runtime resolver.

## Implementation Phases

Phase 1: Model and mapping foundation.

- Add the JMeter-like plan model.
- Add mapper between existing tree nodes and plan nodes.
- Add tests for legacy conversion.

Phase 2: Runtime-scoped config components.

- Add Header Manager, Cookie Manager, and CSV Data Set data classes.
- Add runtime resolver behavior for headers, cookies, and CSV.
- Add focused tests for scope and precedence.

Phase 3: UI alignment.

- Reorganize add menus using JMeter component categories.
- Add or update property panels for Header Manager, Cookie Manager, and CSV Data Set Config.
- Update node titles and icons while keeping current visual style.

Phase 4: Persistence compatibility.

- Persist new component scopes.
- Load legacy and new plan files.
- Verify workspace switching still saves to the correct workspace.

Future phase:

- Add JMX import/export using the new plan model as the mapping boundary.

## Approval Criteria

The first phase is successful when:

- Existing performance plans continue to load and run.
- New JMeter-like plan model exists and is covered by tests.
- Header Manager, Cookie Manager, and CSV Data Set are modeled as performance-plan scoped components.
- Collection requests are not mutated by performance-only runtime config.
- The UI presents common performance components using JMeter-style categories.
- JMX import/export remains absent from the UI and code path in this phase.
