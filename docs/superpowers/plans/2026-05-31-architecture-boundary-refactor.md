# Architecture Boundary Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在允许破坏兼容的前提下，把压测、collection 请求生命周期、service/model、plugin SPI 的职责边界调到“包名表达职责、依赖单向、UI 只在 UI 层”的状态。

**Architecture:** 先补架构边界测试，再按最小可编译批次迁移。`easy-postman-performance-core` 继续保存可复用的 headless plan/runtime/report 契约；`easy-postman-app/com.laker.postman.performance.*` 保存依赖 app 服务的 headless host 实现；`easy-postman-app/com.laker.postman.panel.*` 只保留 Swing 面板、控制器、树适配和 UI metadata。

**Tech Stack:** Java 17, Maven multi-module, TestNG, Swing/FlatLaf/RSyntax 只允许出现在 app UI 或 `easy-postman-ui` 适配层，插件 SPI 使用中立 DTO。

---

## Reference Inputs

- 设计规格：`docs/superpowers/specs/2026-05-31-architecture-boundary-refactor-design.md`
- 中文设计规格：`docs/superpowers/specs/2026-05-31-architecture-boundary-refactor-design_zh.md`
- 模块规则：`docs/ARCHITECTURE_MODULES_zh.md`
- 插件运行时规则：`docs/PLUGIN_RUNTIME_ARCHITECTURE_zh.md`
- 现有边界测试：`easy-postman-app/src/test/java/com/laker/postman/architecture/ModuleArchitectureBoundaryTest.java`

## Non-Negotiable Rules

- 不保留旧包 shim。旧 `com.laker.postman.panel.performance.execution/runtime/model/plan/report` 引用必须清零。
- `easy-postman-app/src/main/java/com/laker/postman/model/**` 不允许 import Swing/AWT/FlatLaf/IconUtil/UiSingletonFactory/panel。
- `easy-postman-plugin-api` 不允许 import `org.fife.ui.autocomplete.*`。
- `easy-postman-app/src/main/java/com/laker/postman/service/**` 不允许依赖具体 Swing panel 的内嵌 DTO。
- 所有移动优先用 `git mv` 保留历史；手工内容修改用 `apply_patch`。
- 每个批次都必须能编译或有明确的预期失败测试作为下一步输入。

## Task 1: Add Boundary Tests First

- [ ] Update `easy-postman-app/src/test/java/com/laker/postman/architecture/ModuleArchitectureBoundaryTest.java`.
- [ ] Add helper methods for source scanning:
  - `private static List<String> sourceContainsViolations(Path file, List<String> forbiddenPatterns)`
  - `private static List<String> sourcePackageViolations(Path sourceRoot, List<String> forbiddenPatterns)`
- [ ] Add `appModelPackageStaysHeadless()`:
  - Scan `easy-postman-app/src/main/java/com/laker/postman/model`.
  - Forbid `javax.swing`, `java.awt`, `com.formdev`, `org.fife`, `UiSingletonFactory`, `IconUtil`, `com.laker.postman.panel.`.
- [ ] Add `serviceLayerDoesNotDependOnSwingPanels()`:
  - Scan `easy-postman-app/src/main/java/com/laker/postman/service`.
  - Forbid at minimum `com.laker.postman.common.component.CsvDataPanel`.
  - Also forbid `CsvDataPanel.CsvState`.
- [ ] Add `pluginApiDoesNotExposeEditorImplementationTypes()`:
  - Scan `easy-postman-plugin-api/src/main/java`.
  - Forbid `org.fife.ui.autocomplete`.
  - Do not forbid `javax.swing` globally in this test because `ToolboxContribution` still intentionally exposes `JPanel`.
- [ ] Add `performanceHeadlessPackagesStayOutOfPanelNamespace()`:
  - Assert these paths do not exist after migration:
    - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/execution`
    - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/runtime`
    - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/model`
    - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan`
    - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/report`
  - Scan `easy-postman-app/src/main/java/com/laker/postman/performance` and forbid `import com.laker.postman.panel.performance.execution.`, `runtime.`, `model.`, `plan.`, `report.`.
- [ ] Run the new guard test and confirm it fails before code migration:

```bash
mvn -q -pl easy-postman-app -am -Dtest=ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] Commit:

```bash
git add easy-postman-app/src/test/java/com/laker/postman/architecture/ModuleArchitectureBoundaryTest.java
git commit -m "test: add architecture boundary guards"
```

## Task 2: Decouple Functional CSV Persistence From UI Panel

- [ ] Add `easy-postman-app/src/main/java/com/laker/postman/functional/model/FunctionalCsvDataState.java`.
- [ ] Implement it as an immutable value object with defensive copies:
  - `String sourceName`
  - `List<String> headers`
  - `List<Map<String, String>> rows`
  - getters return copies.
- [ ] Update `easy-postman-app/src/main/java/com/laker/postman/common/component/CsvDataPanel.java`:
  - Keep inner `CsvState` as the UI component snapshot type because performance CSV UI also reuses it.
  - Do not make `CsvDataPanel` depend on `com.laker.postman.functional.model`.
- [ ] Update `easy-postman-app/src/main/java/com/laker/postman/service/FunctionalPersistenceService.java`:
  - Replace `CsvDataPanel.CsvState` import and method signatures with `FunctionalCsvDataState`.
  - `serializeCsvState(...)` and `deserializeCsvState(...)` use the neutral DTO.
- [ ] Update `easy-postman-app/src/main/java/com/laker/postman/panel/functional/FunctionalPanel.java`:
  - Convert `CsvDataPanel.CsvState` to `FunctionalCsvDataState` before calling persistence save APIs.
  - Convert `FunctionalCsvDataState` back to `CsvDataPanel.CsvState` before restoring the UI.
- [ ] Update all references found by:

```bash
rg -n "CsvDataPanel\\.CsvState|CsvState|exportState\\(|restoreState\\(" easy-postman-app/src/main/java easy-postman-app/src/test/java
```

- [ ] Update tests:
  - `easy-postman-app/src/test/java/com/laker/postman/common/component/CsvDataPanelTest.java`
  - `easy-postman-app/src/test/java/com/laker/postman/panel/functional/FunctionalPanelSaveTest.java`
  - `easy-postman-app/src/test/java/com/laker/postman/service/FunctionalPersistenceServiceTest.java`
- [ ] Run focused tests:

```bash
mvn -q -pl easy-postman-app -am -Dtest=CsvDataPanelTest,FunctionalPanelSaveTest,FunctionalPersistenceServiceTest,PersistenceServiceBoundaryTest,ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

- [ ] Commit:

```bash
git add easy-postman-app/src/main/java/com/laker/postman/functional/model/FunctionalCsvDataState.java easy-postman-app/src/main/java/com/laker/postman/panel/functional/FunctionalPanel.java easy-postman-app/src/main/java/com/laker/postman/service/FunctionalPersistenceService.java easy-postman-app/src/test/java
git commit -m "refactor: decouple functional csv state from ui panel"
```

## Task 3: Move SidebarTab Out Of Domain Model

- [ ] Move `easy-postman-app/src/main/java/com/laker/postman/model/SidebarTab.java` to `easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/SidebarTab.java`.
- [ ] Change package declaration to `com.laker.postman.panel.sidebar`.
- [ ] Update imports in:
  - `easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/SidebarTabPanel.java`
  - `easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/SidebarTabMetrics.java`
  - all tests currently importing `com.laker.postman.model.SidebarTab`.
- [ ] Move `easy-postman-app/src/test/java/com/laker/postman/model/SidebarTabTest.java` to `easy-postman-app/src/test/java/com/laker/postman/panel/sidebar/SidebarTabTest.java`.
- [ ] Verify no `model.SidebarTab` references remain:

```bash
rg -n "model\\.SidebarTab|com\\.laker\\.postman\\.model\\.SidebarTab|SidebarTab" easy-postman-app/src/main/java easy-postman-app/src/test/java
```

- [ ] Run focused tests:

```bash
mvn -q -pl easy-postman-app -am -Dtest=SidebarTabTest,SidebarTabMetricsTest,SidebarTabPanelThemeTest,ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

- [ ] Commit:

```bash
git add easy-postman-app/src/main/java/com/laker/postman/panel/sidebar easy-postman-app/src/test/java/com/laker/postman/panel/sidebar easy-postman-app/src/test/java/com/laker/postman/model
git commit -m "refactor: move sidebar tab metadata to sidebar ui package"
```

## Task 4: Split Domain Enums From UI Metadata

- [ ] Move `easy-postman-app/src/main/java/com/laker/postman/model/TabInfo.java` to `easy-postman-app/src/main/java/com/laker/postman/panel/sidebar/TabInfo.java`:
  - Change package to `com.laker.postman.panel.sidebar`.
  - Update `SidebarTab` and `SidebarTabPanel` imports.
  - Do not change behavior; this is sidebar UI metadata.
- [ ] Update `easy-postman-app/src/main/java/com/laker/postman/model/RequestItemProtocolEnum.java`:
  - Keep only protocol identity and helpers: `protocol`, `isWebSocketProtocol()`, `isHttpProtocol()`, `isSseProtocol()`.
  - Remove `FlatSVGIcon`, `IconUtil`, `Icon`, and icon fields.
- [ ] Add `easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/RequestProtocolUiMetadata.java`:
  - `static Icon iconFor(RequestItemProtocolEnum protocol)`.
  - Owns `http.svg`, `websocket.svg`, `sse.svg`, `save-response.svg` mapping.
- [ ] Update UI call sites found by:

```bash
rg -n "RequestItemProtocolEnum|\\.getIcon\\(\\)" easy-postman-app/src/main/java/com/laker/postman
```

- [ ] Update `easy-postman-app/src/main/java/com/laker/postman/model/MessageType.java`:
  - Keep enum constants only.
  - Remove `display`, `icon`, `I18nUtil`, `MessageKeys`, `IconUtil`, `FlatSVGIcon`.
- [ ] Add `easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request/StreamMessageUiMetadata.java`:
  - `static String display(MessageType type)`
  - `static Icon icon(MessageType type)`
  - Owns WebSocket/SSE stream message icon and i18n mapping.
- [ ] Update call sites found by:

```bash
rg -n "MessageType\\.[A-Z_]+\\.display|MessageType\\.[A-Z_]+\\.icon|\\.display|\\.icon" easy-postman-app/src/main/java/com/laker/postman/panel/collections easy-postman-app/src/test/java
```

- [ ] Update tests:
  - `easy-postman-app/src/test/java/com/laker/postman/panel/collections/editor/request/sub/RequestStreamUiHelperTest.java`
  - add or update a model enum test that asserts no UI dependency by behavior, not icons.
- [ ] Run focused tests:

```bash
mvn -q -pl easy-postman-app -am -Dtest=RequestStreamUiHelperTest,ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

- [ ] Commit:

```bash
git add easy-postman-app/src/main/java/com/laker/postman/model easy-postman-app/src/main/java/com/laker/postman/panel/sidebar easy-postman-app/src/main/java/com/laker/postman/panel/collections/editor/request easy-postman-app/src/main/java/com/laker/postman/common/component/tab easy-postman-app/src/test/java
git commit -m "refactor: split model enums from ui metadata"
```

## Task 5: Remove Model Dependency On service.http Constants

- [ ] Update `easy-postman-foundation/src/main/java/com/laker/postman/util/HttpHeaderConstants.java`:
  - Add constants:
    - `CONTENT_ENCODING = "Content-Encoding"`
    - `CONTENT_LENGTH = "Content-Length"`
    - `EASY_CONTENT_LENGTH = "Easy-Content-Length"`
    - `EASY_CONTENT_ENCODING = "Easy-Content-Encoding"`
- [ ] Replace all `EasyHttpHeaders` references with `HttpHeaderConstants`:

```bash
rg -n "EasyHttpHeaders" easy-postman-app/src/main/java easy-postman-app/src/test/java
```

- [ ] Delete `easy-postman-app/src/main/java/com/laker/postman/service/http/EasyHttpHeaders.java` after all references are replaced.
- [ ] Add a `modelPackageDoesNotDependOnServiceLayer()` guard in `ModuleArchitectureBoundaryTest`:
  - Scan `easy-postman-app/src/main/java/com/laker/postman/model`.
  - Forbid `com.laker.postman.service.`.
- [ ] Run focused tests:

```bash
mvn -q -pl easy-postman-app -am -Dtest=OkHttpResponseHandlerTest,ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] Commit:

```bash
git add easy-postman-foundation/src/main/java/com/laker/postman/util/HttpHeaderConstants.java easy-postman-app/src/main/java/com/laker/postman/model/HttpResponse.java easy-postman-app/src/main/java/com/laker/postman/service/http easy-postman-app/src/test/java easy-postman-app/src/test/java/com/laker/postman/architecture/ModuleArchitectureBoundaryTest.java
git commit -m "refactor: move easy http header constants to foundation"
```

## Task 6: Break Script Completion SPI Dependency On RSyntax

- [ ] Add plugin-api DTO and sink types:
  - `easy-postman-plugin-api/src/main/java/com/laker/postman/plugin/api/ScriptCompletionKind.java`
  - `easy-postman-plugin-api/src/main/java/com/laker/postman/plugin/api/ScriptCompletionItem.java`
  - `easy-postman-plugin-api/src/main/java/com/laker/postman/plugin/api/ScriptCompletionSink.java`
- [ ] Define `ScriptCompletionKind` values:
  - `BASIC`
  - `SHORTHAND`
- [ ] Define `ScriptCompletionItem` fields:
  - `ScriptCompletionKind kind`
  - `String inputText`
  - `String replacementText`
  - `String shortDescription`
- [ ] Define `ScriptCompletionSink` with neutral convenience methods:
  - `void add(ScriptCompletionItem item)`
  - `default void basic(String inputText, String shortDescription)`
  - `default void shorthand(String inputText, String replacementText, String shortDescription)`
- [ ] Update `easy-postman-plugin-api/src/main/java/com/laker/postman/plugin/api/ScriptCompletionContributor.java`:
  - Replace `void contribute(DefaultCompletionProvider provider)` with `void contribute(ScriptCompletionSink sink)`.
- [ ] Update `easy-postman-plugin-api/src/main/java/com/laker/postman/plugin/api/PluginContributionSupport.java`:
  - Remove RSyntax imports.
  - `addScriptApiCompletions(ScriptCompletionSink sink, ...)` contributes neutral items.
  - `addShorthandCompletion(ScriptCompletionSink sink, ...)` contributes neutral items.
  - Keep `JPanel` usage for toolbox unchanged.
- [ ] Update app adapter in `easy-postman-app/src/main/java/com/laker/postman/common/component/editor/ScriptSnippetManager.java`:
  - Build a `ScriptCompletionSink` that converts neutral items to `BasicCompletion` or `ShorthandCompletion`.
  - Keep all RSyntax imports inside app UI/editor code.
- [ ] Update official plugins:
  - `easy-postman-plugins/plugin-redis/src/main/java/com/laker/postman/plugin/redis/RedisPlugin.java`
  - `easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/KafkaPlugin.java`
- [ ] Bump root `pom.xml`:
  - `<plugin.platform.version>2.0.0</plugin.platform.version>` to `<plugin.platform.version>3.0.0</plugin.platform.version>`.
- [ ] Verify no RSyntax type leaks from plugin-api:

```bash
rg -n "org\\.fife\\.ui\\.autocomplete|DefaultCompletionProvider|BasicCompletion|ShorthandCompletion" easy-postman-plugin-api/src/main/java
```

- [ ] Run focused tests and compile plugin path:

```bash
mvn -q -pl easy-postman-app,easy-postman-plugin-api,easy-postman-plugin-runtime,easy-postman-plugins/plugin-redis,easy-postman-plugins/plugin-kafka -am -Dtest=ScriptSnippetManagerTest,ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

- [ ] Commit:

```bash
git add pom.xml easy-postman-plugin-api/src/main/java easy-postman-app/src/main/java/com/laker/postman/common/component/editor/ScriptSnippetManager.java easy-postman-plugins/plugin-redis/src/main/java easy-postman-plugins/plugin-kafka/src/main/java
git commit -m "refactor: make script completion spi editor neutral"
```

## Task 7: Move Headless Performance Code Out Of panel.performance

- [ ] Move source packages:
  - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/execution` to `easy-postman-app/src/main/java/com/laker/postman/performance/execution`
  - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/runtime` to `easy-postman-app/src/main/java/com/laker/postman/performance/runtime`
  - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/model` to `easy-postman-app/src/main/java/com/laker/postman/performance/model`
  - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/plan` to `easy-postman-app/src/main/java/com/laker/postman/performance/plan`
  - `easy-postman-app/src/main/java/com/laker/postman/panel/performance/report` to `easy-postman-app/src/main/java/com/laker/postman/performance/report`
- [ ] Move corresponding test packages:
  - `easy-postman-app/src/test/java/com/laker/postman/panel/performance/execution` to `easy-postman-app/src/test/java/com/laker/postman/performance/execution`
  - `easy-postman-app/src/test/java/com/laker/postman/panel/performance/runtime` to `easy-postman-app/src/test/java/com/laker/postman/performance/runtime`
  - `easy-postman-app/src/test/java/com/laker/postman/panel/performance/model` to `easy-postman-app/src/test/java/com/laker/postman/performance/model`
  - `easy-postman-app/src/test/java/com/laker/postman/panel/performance/plan` to `easy-postman-app/src/test/java/com/laker/postman/performance/plan`
  - `easy-postman-app/src/test/java/com/laker/postman/panel/performance/report` to `easy-postman-app/src/test/java/com/laker/postman/performance/report`
- [ ] Update package declarations in moved files:
  - `com.laker.postman.panel.performance.execution` to `com.laker.postman.performance.execution`
  - `com.laker.postman.panel.performance.runtime` to `com.laker.postman.performance.runtime`
  - `com.laker.postman.panel.performance.model` to `com.laker.postman.performance.model`
  - `com.laker.postman.panel.performance.plan` to `com.laker.postman.performance.plan`
  - `com.laker.postman.panel.performance.report` to `com.laker.postman.performance.report`
- [ ] Update imports across app, worker/master CLI, and tests:

```bash
rg -n "com\\.laker\\.postman\\.panel\\.performance\\.(execution|runtime|model|plan|report)" easy-postman-app/src/main/java easy-postman-app/src/test/java easy-postman-performance-core/src/test/java
```

- [ ] Keep these package families under `com.laker.postman.panel.performance` because they are UI/Swing adapters:
  - `assertion`, `component`, `config`, `control`, `controller`, `extractor`, `result` panels, `threadgroup`, `timer`, `tree`
  - root helpers that import Swing tree/component types.
- [ ] Split `easy-postman-app/src/main/java/com/laker/postman/panel/performance/result` by file content:
  - Move non-UI result aggregation/mapping classes to `com.laker.postman.performance.result`.
  - Keep Swing panels such as `PerformanceReportPanel`, `PerformanceResultTablePanel`, `PerformanceTrendPanel` in `panel.performance.result`.
  - Use this scan to classify:

```bash
rg -n "javax\\.swing|java\\.awt|com\\.formdev|net\\.miginfocom|JPanel|JTable|JLabel|SwingUtilities|DefaultMutableTreeNode|TreePath" easy-postman-app/src/main/java/com/laker/postman/panel/performance/result
```

- [ ] Add a second guard in `ModuleArchitectureBoundaryTest` after result split:
  - `com.laker.postman.performance.**` may depend on `easy-postman-performance-core`, `model`, `service`, and foundation.
  - `com.laker.postman.performance.**` must not import `javax.swing`, `java.awt`, `com.formdev`, `net.miginfocom`, or `com.laker.postman.panel.`.
  - Exception: if a remaining host adapter still needs `DefaultMutableTreeNode`, keep it under `panel.performance.tree` and do not move it.
- [ ] Run focused performance tests by class names:

```bash
mvn -q -pl easy-postman-app -am -Dtest=BoundedTextAccumulatorTest,DefaultPerformanceNetworkRuntimeTest,HttpSamplerExecutorTest,PerformanceAssertionRunnerTest,PerformanceExtractorRunnerTest,PerformanceProtocolStageValidatorTest,PerformanceRequestExecutorTest,PerformanceRequestPostProcessorTest,PerformanceRequestTransportExecutorDispatchTest,PerformanceResponseCapturePlanTest,SseSampleExecutorTest,WebSocketScenarioExecutorTest,PerformanceSampleResultTest,ResultNodeInfoTest,PerformanceCorePlanAdapterTest,PerformancePlanContractsTest,PerformancePlanDocumentCompilerTest,PerformancePlanStorageTest,PerformanceRunPlanFactoryTest,PerformanceReportMarkdownBuilderTest,PerformanceReportTableSchemaTest,PerformanceExecutionEngineNetworkRuntimeTest,PerformanceIterationContextFactoryTest,PerformancePlanScriptUsageDetectorTest,PerformanceResultSinkListenerAdapterTest,PerformanceRunSessionTest,PerformanceRunCliCommandTest,PerformanceMasterRunCommandTest,DefaultPerformanceWorkerRunExecutorTest,PerformanceWorkerServerTest,ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

- [ ] Run compile after import churn:

```bash
mvn -q -pl easy-postman-app -am -DskipTests compile
```

- [ ] Commit:

```bash
git add easy-postman-app/src/main/java/com/laker/postman/performance easy-postman-app/src/main/java/com/laker/postman/panel/performance easy-postman-app/src/test/java/com/laker/postman/performance easy-postman-app/src/test/java/com/laker/postman/panel/performance easy-postman-app/src/test/java/com/laker/postman/architecture/ModuleArchitectureBoundaryTest.java
git commit -m "refactor: move headless performance code out of panel package"
```

## Task 8: Document plugin-manager Ownership Exception

- [x] Update `docs/ARCHITECTURE_MODULES_zh.md`:
  - State that `easy-postman-plugins/plugin-manager` is a host plugin-management library packaged under the official plugin aggregation directory for release/build convenience.
  - State it is not loaded as an ordinary runtime plugin by end users.
- [x] Update `docs/PLUGIN_RUNTIME_ARCHITECTURE_zh.md` and `docs/PLUGINS_zh.md` with the same ownership note where runtime and catalog/install flows are described.
- [ ] Add a short note to `AGENTS.md` only if the local module instructions need to be changed for future agents.
- [x] Run lightweight docs guard requested for this task:

```bash
git diff --check
```

- [ ] Commit:

```bash
git add docs/ARCHITECTURE_MODULES_zh.md docs/PLUGIN_RUNTIME_ARCHITECTURE_zh.md docs/PLUGINS_zh.md docs/superpowers/plans/2026-05-31-architecture-boundary-refactor.md
git commit -m "docs: document plugin manager ownership exception"
```

## Task 9: Final Verification

- [ ] Run boundary tests:

```bash
mvn -q -pl easy-postman-app -am -Dtest=ModuleArchitectureBoundaryTest,PersistenceServiceBoundaryTest,ServiceLayerBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

- [ ] Run focused app behavior tests touched by the refactor:

```bash
mvn -q -pl easy-postman-app -am -Dtest=CsvDataPanelTest,FunctionalPanelSaveTest,FunctionalPersistenceServiceTest,SidebarTabTest,SidebarTabMetricsTest,RequestStreamUiHelperTest,ScriptSnippetManagerTest,OkHttpResponseHandlerTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

- [ ] Run focused performance tests:

```bash
mvn -q -pl easy-postman-app -am -Dtest=PerformanceRunCliCommandTest,PerformanceMasterRunCommandTest,DefaultPerformanceWorkerRunExecutorTest,PerformanceWorkerServerTest,PerformanceRequestExecutorTest,PerformancePlanExecutorTest,PerformanceExecutionEngineTest,PerformanceReportMarkdownBuilderTest,PerformanceResultCollectorTest,PerformanceResultDisplayMapperTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

- [ ] Run full compile across host and official plugins:

```bash
mvn -q -DskipTests compile
```

- [ ] Run final status checks:

```bash
rg -n "com\\.laker\\.postman\\.panel\\.performance\\.(execution|runtime|model|plan|report)" easy-postman-app/src/main/java easy-postman-app/src/test/java
rg -n "org\\.fife\\.ui\\.autocomplete" easy-postman-plugin-api/src/main/java
rg -n "CsvDataPanel\\.CsvState|com\\.laker\\.postman\\.model\\.SidebarTab|EasyHttpHeaders" easy-postman-app/src/main/java easy-postman-app/src/test/java
git status --short
```

## Expected End State

- `panel.performance` 只表达 Swing UI、树交互、属性面板、结果面板、UI 控制器。
- `performance.execution/runtime/model/plan/report/result` 表达 headless host 实现，不反向依赖 `panel.performance`。
- `model` 包不再包含 icon、panel supplier、Swing 组件、service 常量。
- `FunctionalPersistenceService` 使用中立 DTO 持久化 CSV 状态。
- 插件补全 SPI 不再暴露 RSyntax 类型；RSyntax 只在 app editor adapter 中出现。
- `plugin.platform.version` 已升到 `3.0.0`，官方 Redis/Kafka 插件已适配新 SPI。
- 文档明确 `plugin-manager` 是 host plugin-management library 的目录特例。
