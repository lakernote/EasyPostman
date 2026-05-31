# Architecture Boundary Refactor Design

## Goal

Refactor EasyPostman's architecture so package names, module ownership, and SPI contracts match actual responsibilities. The refactor prioritizes dependency direction and readability over feature changes.

## Current Problems

Several non-UI execution and model classes live under `com.laker.postman.panel.performance`, so app CLI, worker, and runtime code import classes whose package name implies Swing UI ownership.

Some model and service classes depend on UI types:

- `FunctionalPersistenceService` depends on `CsvDataPanel.CsvState`.
- `SidebarTab` lives in `model` but creates icons and panels through `UiSingletonFactory`.
- `RequestItemProtocolEnum` and `MessageType` mix domain enum values with Swing icons and localized display labels.
- `HttpResponse` depends on `EasyHttpHeaders` from the HTTP service package.

The plugin SPI exposes concrete UI/editor implementation types. `ToolboxContribution` exposes `JPanel`, which is acceptable for desktop UI plugins, but `ScriptCompletionContributor` exposes RSyntaxAutoComplete `DefaultCompletionProvider`, tying the stable plugin API to one editor library.

The repository has split packages across modules, especially `com.laker.postman.model`, `com.laker.postman.util`, `com.laker.postman.common.component`, and `com.laker.postman.common.constants`. This works without JPMS, but it makes owner lookup and import review harder.

`easy-postman-plugins/plugin-manager` is consumed directly by the host app as a plugin-management library, even though its directory makes it look like a normal runtime plugin.

## Decision

Use a breaking-change refactor. Plugin SPI compatibility does not need to be preserved in this migration. Official plugins will be updated in the same change set, and `plugin.platform.version` will be bumped.

Do not create a broad `easy-postman-api-core` module in this first pass. The first pass will make package ownership and dependency direction correct inside the existing module structure. A later extraction can move stabilized request execution contracts into a dedicated API core module.

## Target Package Ownership

`com.laker.postman.panel.*` is UI-only. It may contain Swing panels, UI state, presenters, view factories, and UI adapters. It must not contain reusable runtime engines, headless adapters, persistence DTOs, or domain models.

`com.laker.postman.performance.*` owns app-side performance execution. This includes GUI/headless shared execution adapters, transport adapters, run/session classes, app-side result models, report mappers that are not Swing views, and worker/master app integration.

`com.laker.postman.functional.*` owns functional runner configuration, persistence DTOs, execution models, and non-UI functional runner logic.

`com.laker.postman.navigation.*` owns host navigation metadata such as sidebar tabs, panel factories, title keys, and icon paths. Domain model packages must not know about sidebar panels.

HTTP request preparation belongs in `com.laker.postman.http.request.*`; concrete runtime adapters belong in `com.laker.postman.http.runtime.*`. These packages must not directly show Swing dialogs or expose UI component types. UI feedback must be returned as structured outcomes or delegated through UI-layer callbacks.

`easy-postman-performance-core` remains the headless performance domain core. It owns plan data, executable run plan JSON models, runtime contracts, thread-group scheduling, worker protocol DTOs, stats, trends, and report snapshots. It must not depend on app, Swing, OkHttp, workspace services, or app settings.

## Performance Refactor

Move non-UI classes currently under `com.laker.postman.panel.performance.execution`, `runtime`, `model`, `plan`, and `report` into app-owned non-UI packages where appropriate:

- `com.laker.postman.performance.execution`
- `com.laker.postman.performance.runtime`
- `com.laker.postman.performance.model`
- `com.laker.postman.performance.plan`
- `com.laker.postman.performance.report`

Keep Swing panels, property panels, table panels, chart panels, tree views, UI controllers, and view factories under `com.laker.postman.panel.performance`.

The app CLI/runtime packages must no longer import `com.laker.postman.panel.performance.*` unless they are intentionally opening Swing UI.

Use existing `easy-postman-performance-core` types where they already represent stable core concepts. Do not force app-side classes into core if they still depend on `HttpRequestItem`, `PreparedRequest`, `HttpResponse`, `SettingManager`, script services, OkHttp, or workspace/app execution state.

## UI Dependency Cleanup

Introduce a non-UI functional CSV state DTO outside Swing panels, for example `com.laker.postman.functional.model.FunctionalCsvDataState`. `CsvDataPanel` can convert to and from this DTO, while `FunctionalPersistenceService` persists only the DTO.

Move `SidebarTab` out of `com.laker.postman.model` into a navigation/UI package. It may keep panel suppliers, icons, and title keys there because those are host navigation concerns, not domain model concerns.

Split protocol and stream-message domain data from UI metadata:

- `RequestItemProtocolEnum` keeps only protocol values and protocol predicates.
- A UI mapper such as `RequestProtocolUiMetadata` supplies icons and display labels.
- `MessageType` keeps only message type values.
- A UI mapper such as `StreamMessageUiMetadata` supplies icons and localized labels.

Move HTTP header constants used by models into foundation, or move header normalization out of `HttpResponse` into an HTTP response mapper. `HttpResponse` must not depend on app service HTTP packages.

## Plugin SPI Breaking Change

Replace `ScriptCompletionContributor` with a library-neutral SPI:

```java
@FunctionalInterface
public interface ScriptCompletionContributor {
    void contribute(ScriptCompletionSink sink);
}
```

`easy-postman-plugin-api` will define small completion DTOs and a sink interface. The app editor layer will adapt those DTOs to RSyntaxAutoComplete.

The new SPI should support the official plugin use cases currently implemented by Redis and Kafka completions:

- variable-like completions such as `pm.redis`
- function or method completions
- optional short descriptions
- optional replacement text
- optional summary text

`PluginContributionSupport` will be updated to build DTO-backed completions instead of mutating `DefaultCompletionProvider` directly.

`plugin.platform.version` will be bumped because third-party plugins using completion contributors must recompile and migrate.

`ToolboxContribution` may continue exposing `JPanel` in this pass because toolbox contributions are explicitly desktop Swing UI contributions. A future plugin-web or headless plugin model would need a different extension point.

## Plugin Manager Ownership

Document `easy-postman-plugins/plugin-manager` as a host plugin-management library special case. It is not a normal runtime plugin even though it lives under `easy-postman-plugins`.

Do not move the module in the first pass unless the package refactor is already complete and stable. Moving the module directory changes Maven paths, release scripts, and documentation; it should be a separate follow-up if still needed.

## Split Package Policy

Do not attempt to eliminate all split packages in one pass. Instead, prevent new or refactored code from adding to ambiguous split packages when a clearer owner exists.

Use owner-specific packages for moved code:

- `com.laker.postman.performance.*` for app-side performance non-UI code.
- `com.laker.postman.functional.*` for functional runner non-UI code.
- `com.laker.postman.navigation.*` for host navigation UI registry code.
- `com.laker.postman.common.*` only for truly shared UI design-system code in `easy-postman-ui`.

Where imports currently use broad or ambiguous package names, prefer explicit owner packages during the migration.

## Data Flow After Refactor

Collection request execution remains:

1. UI panel gathers form data.
2. Request preparation builds and finalizes a `PreparedRequest`.
3. Protocol dispatch selects HTTP, SSE, or WebSocket execution.
4. Transport code executes the request and maps transport response data.
5. UI presenters render request, response, stream messages, history, and test output.

Performance execution becomes:

1. UI or headless code creates a performance plan.
2. Core compiles and schedules the plan.
3. App-side performance runtime adapts samplers to EasyPostman request execution.
4. Transport adapters execute HTTP, SSE, or WebSocket samples.
5. Result collectors emit core stats and app/UI result events.
6. UI panels render events only after receiving app-side result view models.

Plugin completion becomes:

1. Plugin registers completion DTOs through `ScriptCompletionSink`.
2. Plugin runtime stores contributors as plugin-api types.
3. App editor asks contributors for neutral completion items.
4. App editor adapts completion items to RSyntaxAutoComplete.

## Error Handling

Transport and response handling code must return structured result information or throw domain-level exceptions. It must not show Swing dialogs directly.

UI warnings such as oversized download warnings are displayed by UI callers after inspecting response handling outcomes.

Headless execution must not depend on Swing classes to report errors. It should report through logs, CLI results, worker responses, or structured report fields.

Plugin completion adapter errors should be isolated per plugin contributor. A failed plugin contributor should be logged and skipped without breaking the entire editor completion provider.

## Testing Strategy

Run architecture boundary tests and full compile after each migration batch:

```bash
mvn -q -pl easy-postman-app -am -Dtest=ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -DskipTests compile
```

Add or update tests for:

- Functional CSV persistence DTO round-trip.
- Request protocol enum no longer depending on Swing/icon classes.
- Script completion contributor DTO to RSyntax adapter.
- Official Redis and Kafka plugin completion registration with the new SPI.
- Performance plan execution still compiling from GUI and headless paths.

Manual smoke checks after compile:

- Collection HTTP request sends and response renders.
- Collection SSE and WebSocket tabs still receive stream events.
- Functional runner loads saved CSV state.
- Performance GUI run records sample results.
- Headless performance run produces a JSON report.
- Redis and Kafka plugin script completions appear in the script editor.

## Acceptance Criteria

- `easy-postman-app/src/main/java/com/laker/postman/panel/performance` contains only Swing/UI classes and UI adapters.
- App CLI, worker, and headless runtime code no longer import `com.laker.postman.panel.performance.*`.
- `com.laker.postman.model` no longer imports `javax.swing`, `FlatSVGIcon`, `IconUtil`, `UiSingletonFactory`, or concrete panels.
- Persistence services do not depend on Swing panel inner classes.
- `easy-postman-plugin-api` no longer imports RSyntaxAutoComplete classes.
- Official plugins compile and use the new completion SPI.
- `plugin.platform.version` is bumped.
- `mvn -q -DskipTests compile` passes.

## Non-Goals

This pass does not introduce a new `easy-postman-api-core` module.

This pass does not move `plugin-manager` out of `easy-postman-plugins`.

This pass does not replace OkHttp, rewrite the script engine, or change collection/performance behavior intentionally.

This pass does not eliminate every split package in the repository.
