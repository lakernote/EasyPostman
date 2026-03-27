## Project Overview

EasyPostman is a Java 17 + Swing desktop API testing app. Entry point: `com.laker.postman.App`. Build tool: Maven multi-module.

---

## Module Structure

```
easy-postman-parent (root pom.xml, revision = host version)
├── easy-postman-plugin-api      # Stable plugin SPI: EasyPostmanPlugin, PluginContext, PluginDescriptor
├── easy-postman-plugin-bridge   # Shared bridge contracts, models, utils (ConfigPathConstants, AppConstants, I18nUtil, MessageKeys, SystemUtil, UserSettingsUtil)
├── easy-postman-plugin-ui       # Shared Swing UI base components, FontsUtil, IconUtil, NotificationUtil
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

When adding shared non-UI logic accessible by both host and plugins, put it in `easy-postman-plugin-bridge`. When adding shared UI utilities, put them in `easy-postman-plugin-ui`. Do not put bridge/SPI code directly into `easy-postman-app`.

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

The project uses its **own lightweight IOC container** (`com.laker.postman.ioc`), not Spring. Do not import Spring annotations.

| Annotation | Purpose |
|---|---|
| `@Component` | Marks a class as a managed bean (auto-scanned from `com.laker.postman`) |
| `@Autowired` | Field/constructor/method injection |
| `@PostConstruct` | Called after all fields are injected |
| `@PreDestroy` | Called on `BeanFactory.destroy()` |

Retrieve beans outside of injection: `BeanFactory.getBean(MyService.class)`.

Three-level circular dependency cache is implemented in `ApplicationContext` — if you see a circular dependency crash, check bean design rather than patching the cache.

---

## Swing Panel Conventions

All UI panels that are logically singletons must:
1. Extend `SingletonBasePanel`
2. Be obtained via `SingletonFactory.getInstance(MyPanel.class)` — **never `new MyPanel()`**
3. Implement `initUI()` for component creation and `registerListeners()` for event wiring
4. Call `safeInit()` after obtaining the instance (this calls `initUI()` then `registerListeners()`)

`SingletonBaseMenuBar` follows the same pattern for menu bars.

---

## Key Constant Files (in `easy-postman-plugin-bridge`)

- `AppConstants` — `APP_NAME`, `BASE_PACKAGE`
- `ConfigPathConstants` — all data file paths (`EASY_POSTMAN_SETTINGS`, `COLLECTIONS`, `ENVIRONMENTS`, `DEFAULT_WORKSPACE_DIR`, etc.)

Data root: `SystemUtil.getEasyPostmanPath()` — returns `<user.home>/EasyPostman/` in normal mode, or `<app-dir>/data/` in portable mode.

---

## Internationalisation

All user-visible strings must use `I18nUtil.getMessage(MessageKeys.SOME_KEY)`. Keys are defined as constants in `MessageKeys` (bridge module). Translations live in `easy-postman-app/src/main/resources/messages_en.properties` and `messages_zh.properties`. Never hard-code UI strings directly.

---

## Theme & Settings

- Theme is managed by `SimpleThemeManager` (light/dark via FlatLaf, animated transitions).
- User settings are persisted to `easy_postman_settings.properties` via `SettingManager` (static Properties file) and `UserSettingsUtil` (bridge module).
- Font size setting key: `ui_font_size` in that properties file.

---

## Plugin System (summary)

- Each plugin is a standalone JAR with a descriptor at `META-INF/easy-postman/*.properties` (generated from the plugin's `pom.xml`).
- Plugin entry class implements `EasyPostmanPlugin`; `onLoad(PluginContext)` registers all capabilities.
- Extension points: `registerScriptApi`, `registerService`, `registerToolboxContribution`, `registerScriptCompletionContributor`, `registerSnippet`.
- Host consumes registered capabilities from `PluginRegistry`.
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

---

## Skills

### Available skills

- swing-flatlaf-miglayout-principles: Use when modifying EasyPostman Swing forms that use FlatLaf and MigLayout, especially when layout refactors introduce clipped focus rings, dense spacing, border conflicts, or inconsistent form structure. (file: /Users/lonli2/IdeaProjects-laker/easy-postman-github/.codex/skills/swing-flatlaf-miglayout-principles/SKILL.md)
- fontsutil-font-usage: Use when modifying EasyPostman Swing UI fonts, especially when dialogs, labels, tables, tabs, or renderers look too large or too small, or when a change must follow the user's configured UI font size. Prefer FontsUtil.getDefaultFontWithOffset(...). (file: /Users/lonli2/IdeaProjects-laker/easy-postman-github/.codex/skills/fontsutil-font-usage/SKILL.md)
- swing-ui-test-headless-guard: Use when adding or updating EasyPostman Swing/TestNG UI tests that may run in headless CI. Reuse `AbstractSwingUiTest` instead of duplicating headless or no-display skip logic. (file: /Users/lonli2/IdeaProjects-laker/easy-postman-github/.codex/skills/swing-ui-test-headless-guard/SKILL.md)

### How to use skills

- If the request names a skill or clearly matches the description above, read the skill and follow it.
- Keep the skill body concise and use it only for repo-specific knowledge that is hard to infer from the code alone.
