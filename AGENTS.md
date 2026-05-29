## Project Overview

EasyPostman is a Java 17 + Swing desktop API testing app. Entry point: `com.laker.postman.App`. Build tool: Maven multi-module.

---

## Module Structure

```
easy-postman-parent (root pom.xml, revision = host version)
├── easy-postman-foundation      # Lowest non-UI base layer: shared models, constants, paths, JSON, system/settings/i18n utilities
├── easy-postman-plugin-api      # Stable plugin SPI and service contracts: EasyPostmanPlugin, PluginContext, PluginDescriptor, GitPluginService, ClientCertificatePluginService
├── easy-postman-platform        # Host platform framework: custom IOC + update discovery core; startup/welcome/help/settings orchestration later
├── easy-postman-ui              # Common Swing UI base components, FontsUtil, IconUtil, NotificationUtil, EditorThemeUtil, ModernColors, ModernButtonFactory
├── easy-postman-performance-core           # Headless performance domain core: plan, runtime contracts, stats, report snapshots
├── easy-postman-performance-runtime-okhttp # OkHttp-backed performance transport runtime
├── easy-postman-plugin-runtime  # Plugin scan/load/lifecycle: PluginRuntime, PluginScanner, PluginLoader, PluginRegistry
├── easy-postman-plugins/        # Official plugins (each builds an independent JAR)
│   ├── plugin-manager           # Catalog parsing, online/offline install facade
│   ├── plugin-client-cert
│   ├── plugin-capture
│   ├── plugin-redis
│   ├── plugin-kafka
│   └── plugin-decompiler
└── easy-postman-app             # Host application; consumes plugin-registered capabilities
```

When choosing a module, use this rule set:

- Put shared non-UI foundation logic in `easy-postman-foundation`: DTOs, enums, constants, paths, JSON, system utilities, user-setting helpers, i18n mechanism, and generic parsing/formatting helpers such as Cron, JSON Path, XML, file-size, file-extension, time-display, and HTTP header constants.
- Put plugin-facing extension contracts in `easy-postman-plugin-api`: plugin SPI, service interfaces, toolbox/script/snippet contracts.
- Put host platform framework in `easy-postman-platform`: custom IOC, update discovery core, then startup, welcome/help, settings center, and theme/font application orchestration when dependencies are ready.
- Put shared Swing design-system code in `easy-postman-ui`: reusable components, UI singleton base/factory, toolbar buttons (`EditButton`, `SaveButton`, `WrapToggleButton`), form controls (`EasyComboBox`, `EasyJSpinner`, `EasyPasswordField`), fonts, icons, notification UI, editor theme helpers, semantic colors, and UI resources directly used by those components.
- Keep plugin loading, classloaders, descriptor parsing, registry, and lifecycle in `easy-postman-plugin-runtime`.
- Keep host-specific composition, app panels, menus, concrete startup wiring, and app-only services in `easy-postman-app` until each dependency is ready to migrate into `easy-postman-platform`.
- Do not put SPI code, shared UI components, or shared foundation utilities directly into `easy-postman-app`.

Reference module rules: `docs/ARCHITECTURE_MODULES_zh.md`.

---

## Build Commands

```bash
# Full build (all modules + all plugins), skip tests
mvn clean package -DskipTests

# Build only the host app (fastest iteration)
mvn -pl easy-postman-app -am -DskipTests clean package

# Build host app + one plugin
mvn -pl easy-postman-app,easy-postman-plugins/plugin-redis -am clean package -DskipTests

# Quick compile check (no jar, fast)
mvn -q -pl easy-postman-app -am -DskipTests compile

# Run tests for a specific class in headless mode
mvn -q -pl easy-postman-app -am -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

Output: `easy-postman-app/target/easy-postman-${revision}.jar`

Native installers are produced by `build/mac.sh`, `build/win-exe.bat`, `build/linux-deb.sh`, `build/linux-rpm.sh` — these call `jpackage` and reference a fixed filename `easy-postman.jar` (not the versioned one).

---

## Startup Sequence

```
App.main()
  -> configurePlatformSpecificSettings()   // Linux: FlatLaf window decorations
  -> SwingUtilities.invokeLater()
       -> SimpleThemeManager.initTheme()   // reads easy_postman_settings.properties
       -> FontManager.applyFontSettings()
       -> SplashWindow or direct SwingWorker
            -> StartupCoordinator.prepareMainFrame()
                 -> BeanFactory.init("com.laker.postman")   // scans @Component beans
                 -> PluginRuntime.initialize()               // scan, load, lifecycle
                 -> MainFrame (EDT)
  -> registerShutdownHook()
       -> PluginRuntime.shutdown() + BeanFactory.destroy()
```

---

## Custom IOC Container

The project uses its **own lightweight IOC container** (`com.laker.postman.ioc`) from `easy-postman-platform`, not Spring. Do not import Spring annotations.

| Annotation | Purpose |
|---|---|
| `@Component` | Marks a class as a managed bean (auto-scanned from `com.laker.postman`) |
| `@Autowired` | Field/constructor/method injection |
| `@PostConstruct` | Called after all fields are injected |
| `@PreDestroy` | Called on `BeanFactory.destroy()` |

Retrieve beans outside of injection: `BeanFactory.getBean(MyService.class)`.

Three-level circular dependency cache is implemented in `ApplicationContext` — if you see a circular dependency crash, check bean design rather than patching the cache.

---

## Lombok Usage

When adding or refactoring Java code, use Lombok by default for boilerplate reduction where it keeps the code clearer:
- Use `@Slf4j` instead of manually declaring logger fields.
- Use `@RequiredArgsConstructor` for required dependency constructors, especially support/service classes with `final` fields.
- Use `@Getter`, `@Setter`, `@Data`, `@Value`, and `@Builder` for simple models/value objects when appropriate.
- Use `@UtilityClass` for stateless utility classes instead of private constructors plus static methods.

Avoid Lombok only when explicit code is clearer for Swing lifecycle, side-effectful constructors, validation-heavy construction, or framework compatibility.

---

## Swing Panel Conventions

All UI panels that are logically singletons must:
1. Extend `UiSingletonPanel`
2. Be obtained via `UiSingletonFactory.getInstance(MyPanel.class)` — **never `new MyPanel()`**
3. Implement `initUI()` for component creation and `registerListeners()` for event wiring
4. Let `UiSingletonFactory` call `initializeSingletonUi()` after construction (this calls `initUI()` then `registerListeners()`)

`UiSingletonMenuBar` follows the same pattern for menu bars.

Business services and repositories must be obtained through the IOC container (`BeanFactory.getBean(...)` or constructor injection), not through `UiSingletonFactory`.

Classes that coordinate Swing tabs, dialogs, or panels but are not singleton components should live under `com.laker.postman.panel...` and use UI-oriented names (for example `RequestEditorTabs`, `OpenedRequestTabsSaver`) rather than `*Service`.

Collection services must not fetch panels directly. Use service-side models/stores such as `OpenedRequestsStore`, `CollectionTreeNodeTypes`, and the registered root-node access in `CollectionTreeRootRegistry`; the UI layer is responsible for registering the active tree root.

---

## Key Constant Files (in `easy-postman-foundation`)

- `AppConstants` — `APP_NAME`, `BASE_PACKAGE`
- `ConfigPathConstants` — all data file paths (`EASY_POSTMAN_SETTINGS`, `COLLECTIONS`, `ENVIRONMENTS`, `DEFAULT_WORKSPACE_DIR`, etc.)
- `JsonUtil` — Jackson-based JSON serialization/deserialization (supports JSON5/comments); use this instead of raw Jackson calls
- `AppRuntimeLayout` — resolves portable mode (`isPortableMode(Class<?>)`) and key directory paths (`applicationRootDirectory`, `codeSourceDirectory`); portable mode is triggered by a `.portable` marker file or the `easyPostman.portable` system property

Data root: `SystemUtil.getEasyPostmanPath()` — returns `<user.home>/EasyPostman/` in normal mode, or `<app-dir>/data/` in portable mode.

---

## Workspace & Git Sync

The app supports multiple named workspaces. Each workspace is an isolated directory (collections, environments, settings). A workspace can optionally be backed by a Git repository.

- Models: `Workspace`, `WorkspaceType` in `easy-postman-foundation`
- Workspace UI: `WorkspacePanel` and its components in `com.laker.postman.panel.workspace`
- Git operations are declared in `com.laker.postman.plugin.api.service.GitPluginService` and implemented by `GitWorkspacePluginService` in `com.laker.postman.plugin.git`
- Host-side accessor: `GitServiceAccess` (in `com.laker.postman.plugin.host`, app module) provides the built-in Git service behind the `GitPluginService` contract.
- `WorkspaceStorageUtil` (app util) handles workspace list persistence

---

## Script Execution

Pre/post scripts run through `ScriptExecutionPipeline` (`com.laker.postman.service.js`), which merges collection/folder/request-level scripts, pools Rhino contexts (`JsContextPool`), and injects polyfills.

Built-in JS libraries bundled at `easy-postman-app/src/main/resources/js-libs/`:
- `crypto-js.min.js`, `lodash.min.js`, `moment.min.js`

Plugin scripts are injected via `registerScriptApi` (alias → object factory); they become accessible as `pm.<alias>` inside scripts.

---

## Internationalisation

All user-visible strings must use `I18nUtil.getMessage(MessageKeys.SOME_KEY)`. The i18n mechanism and base `MessageKeys` live in `easy-postman-foundation`. Resource bundles should follow ownership: shared UI component text belongs with `easy-postman-ui`, app text belongs with `easy-postman-app`, and plugin text belongs with the plugin module. Never hard-code UI strings directly.

---

## Theme & Settings

- Theme is currently applied by `SimpleThemeManager` in the app (light/dark via FlatLaf, animated transitions). Startup/application of theme and font settings belongs in `easy-postman-platform` once those dependencies are extracted cleanly.
- User settings are persisted to `easy_postman_settings.properties` via `SettingManager` (static Properties file) and `UserSettingsUtil` (foundation module).
- Font size setting key: `ui_font_size` in that properties file.
- Custom FlatLaf token overrides: `easy-postman-app/src/main/resources/com/laker/postman/common/themes/EasyLightLaf.properties` and `EasyDarkLaf.properties`
- RSyntaxTextArea editor theme XMLs: `easy-postman-app/src/main/resources/themes/easypostman-light.xml` and `easypostman-dark.xml`
- Shared semantic colors for both themes: `ModernColors` in `easy-postman-ui` (`com.laker.postman.common.constants.ModernColors`)
- Font helpers, icon helpers, notification UI, editor theme helpers, and reusable Swing components belong in `easy-postman-ui`.

---

## Update Architecture

Update discovery core lives in `easy-postman-platform` under `com.laker.postman.platform.update`: `UpdateInfo`, `UpdateCheckFrequency`, `VersionChecker`, release sources, version comparison, asset resolution, changelog fetching/formatting, and Windows package/registry helpers.

Concrete update UX stays in `easy-postman-app`: `AutoUpdateManager`, `UpdateUIManager`, `UpdateDownloader`, update dialogs/notifications, browser-opening actions, installer launch, and app shutdown.

Platform update code must not import app `SettingManager`; use `UpdateSettingsProvider` and adapt it from app with `SettingManager::getUpdateSourcePreference`.

---

## Plugin System (summary)

- Each plugin is a standalone JAR with a descriptor at `META-INF/easy-postman/*.properties` (generated from the plugin's `pom.xml`).
- Plugin entry class implements `EasyPostmanPlugin`; `onLoad(PluginContext)` registers all capabilities.
- Extension points: `registerScriptApi`, `registerService`, `registerToolboxContribution`, `registerScriptCompletionContributor`, `registerSnippet`.
- Host consumes registered capabilities from `PluginRegistry`.
- **Plugin service interfaces** (`GitPluginService`, `ClientCertificatePluginService`, `RequestCollectionImportService`) live in `easy-postman-plugin-api` under `com.laker.postman.plugin.api.service`. Plugins register implementations via `context.registerService(GitPluginService.class, impl)`. Host code retrieves them through `PluginAccess.getService(Type.class)` or typed app-side accessors such as `GitServiceAccess` and `ClientCertificatePluginAccess` in `com.laker.postman.plugin.host`.
- Version model: `revision` = host release version; `plugin.platform.version` = SPI compatibility boundary. Only bump `plugin.platform.version` when plugin SPI/runtime changes are breaking.
- Catalog source of truth: `pom.xml → descriptor → release asset → catalog`. Do not hand-edit `plugin-catalog/` or the bundled fallback in `plugin-manager/src/main/resources/plugin-catalog/` independently — update both together.
- Reference runtime architecture: `docs/PLUGIN_RUNTIME_ARCHITECTURE_zh.md`.

---

## CI / GitHub Actions

| Workflow | Trigger | Purpose |
|---|---|---|
| `pr-check.yml` | PR to main/master/develop | Maven build + tests + PR validation |
| `release.yml` | Push tag | Multi-platform native installer build |
| `plugin-release.yml` | Plugin tag | Build plugin JARs, validate consistency, publish, update catalog |
| `codeql-analysis.yml` | Schedule/push | Security analysis |
| `auto-label.yml` | Issue/PR opened or edited | Auto-apply labels based on title/body keywords |
| `sync-labels.yml` | Push to main (`.github/labels.yml` changed) or manual | Sync label definitions to the repository |
| `welcome.yml` | Issue/PR opened | Post a welcome comment for first-time contributors |

---

## Skills

### Available skills

- swing-flatlaf-miglayout-principles: Use when modifying EasyPostman Swing forms that use FlatLaf and MigLayout, especially when layout refactors introduce clipped focus rings, dense spacing, border conflicts, or inconsistent form structure. (file: .codex/skills/swing-flatlaf-miglayout-principles/SKILL.md)
- fontsutil-font-usage: Use when modifying EasyPostman Swing UI fonts, especially when dialogs, labels, tables, tabs, or renderers look too large or too small, or when a change must follow the user's configured UI font size. Prefer FontsUtil.getDefaultFontWithOffset(...). (file: .codex/skills/fontsutil-font-usage/SKILL.md)
- swing-ui-test-headless-guard: Use when adding or updating EasyPostman Swing/TestNG UI tests that may run in headless CI. Reuse `AbstractSwingUiTest` instead of duplicating headless or no-display skip logic. (file: .codex/skills/swing-ui-test-headless-guard/SKILL.md)
- module-architecture-boundaries: Use when adding/refactoring modules, shared code, plugin contracts, UI utilities, i18n, settings, theme/font handling, or deciding where a class belongs. (file: .codex/skills/module-architecture-boundaries/SKILL.md)

### How to use skills

- If the request names a skill or clearly matches the description above, read the skill and follow it.
- Keep the skill body concise and use it only for repo-specific knowledge that is hard to infer from the code alone.
