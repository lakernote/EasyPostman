---
name: module-architecture-boundaries
description: Use when adding or refactoring EasyPostman modules, shared code, plugin contracts, UI utilities, i18n, settings, theme/font handling, or deciding where a class belongs.
---

# Module Architecture Boundaries

Use this skill before adding shared code or moving code between modules. The goal is to keep EasyPostman's Maven modules small enough to reason about and strict enough that future changes do not turn into a catch-all common layer.

## Source of truth

Read `docs/ARCHITECTURE_MODULES_zh.md` first when the task is about module placement, architecture cleanup, shared UI, plugin contracts, i18n, theme, font, settings, startup, update, welcome/help, request core, collection core, API core, or performance core.

## Placement rules

1. Put non-UI base capabilities in `easy-postman-foundation`.
   Examples: shared DTOs, enums, constants, config paths, JSON helpers, system utilities, user-setting helpers, i18n mechanism, base message keys, and generic parsing/formatting helpers such as Cron, JSON Path, XML, file-size, file-extension, time-display, and HTTP header constants.

2. Put plugin extension contracts in `easy-postman-plugin-api`.
   Examples: `EasyPostmanPlugin`, `PluginContext`, `PluginDescriptor`, service interfaces, toolbox/script/snippet contracts.

3. Put request specification models in `easy-postman-request-core`.
   Examples: `HttpRequestItem`, `SavedResponse`, `HttpHeader`, `HttpParam`, `HttpFormData`, `HttpFormUrlencoded`, `CookieInfo`, auth/body/protocol enums, redirect metadata, and transport-auth metadata. Keep it UI-free and transport-implementation-free: no Swing, OkHttp, app service/panel code, plugin runtime, or concrete send/render implementation.

4. Put collection domain models and neutral import parsing in `easy-postman-collection-core`.
   Examples: `RequestGroup`, `CollectionNode`, `CollectionNodeType`, `CollectionParseResult`, collection auth parsing helpers, and Postman collection parsing. Keep it UI-free and host-free: no Swing/AWT, OkHttp, app service/panel/runtime code, platform, plugin runtime, IOC, or concrete send/render implementation.

5. Put shared Swing design-system code in `easy-postman-ui`.
   Examples: `FontsUtil`, `IconUtil`, `NotificationUtil`, `EditorThemeUtil`, `ModernColors`, reusable toolbar buttons/search/table/dialog/form controls such as `EditButton`, `SaveButton`, `WrapToggleButton`, `EasyComboBox`, `EasyJSpinner`, `EasyPasswordField`, and the icons/resources those reusable components directly reference.
   UI singleton framework classes such as `UiSingletonFactory`, `UiSingletonPanel`, `UiSingletonMenuBar`, plus Swing refresh/save helpers such as `IRefreshable` and `DebouncedSaveSupport`, also belong here.
   Generic action/control/status icons such as save, copy, paste, search, clear, cancel, close, delete, duplicate, eye, info, warning, arrows, chevrons, wrap, start, stop, send, connect, collapse, expand, more, detail, import, and export belong here. Do not duplicate the same `icons/*.svg` resource path in `easy-postman-app`, and do not make official plugins depend on app-only icon resources.

6. Put plugin loading mechanics in `easy-postman-plugin-runtime`.
   Examples: plugin scanning, descriptor parsing, classloaders, registry, lifecycle, disabled/uninstall state.

7. Put performance domain core contracts in `easy-postman-performance-core`: editable plan data, executable `plan.json`, runtime contracts, stats/report snapshots, worker assignments, and asset references. Keep concrete GUI/headless execution adapters in `easy-postman-app` until the app execution semantics can be extracted without pulling in Swing, workspace services, or app-only state.

8. Put host platform framework capabilities in `easy-postman-platform` when they can be separated from concrete app UI.
   Current examples: the custom IOC container under `com.laker.postman.ioc`, and update discovery core under `com.laker.postman.platform.update` (version comparison, update source selection, asset resolution, changelog fetching/formatting, update result models).
   Future examples: startup orchestration, welcome/help, settings center, and theme/font application orchestration.

9. Keep concrete host UI and composition in `easy-postman-app`.
   Examples: `App`, `MainFrame`, menus, app-only panels, settings pages, update dialogs, update download/install/exit flow, welcome/help pages, and concrete startup wiring that still depends on app UI.
   Keep `easy-postman-app`'s `com.laker.postman.model` narrow: only app-owned runtime HTTP exchange snapshots such as `PreparedRequest`, `HttpResponse`, and `HttpEventInfo` belong there. Domain-specific app models should live with their owner package, such as `functional.model`, `script.model`, `stream`, `snippet`, `history`, `certificate`, `variable`, `environment`, or `service.curl`.

10. Keep HTTP execution code UI-neutral even before it moves modules.
   Request preparation, URL/query helpers, request setting resolution, validation, and default request factories belong in `http.request`; `HttpTransportRuntime`, `HttpRuntimeExecutor`, scoped client providers, and compression interceptors belong in `http.runtime.transport`; OkHttp adapters belong in `http.runtime.okhttp`; TLS/certificate configuration belongs in `http.runtime.ssl`; SSE runtime callbacks belong in `http.runtime.sse`; Cookie state belongs in `http.runtime.cookie`; manual redirect execution belongs in `http.runtime.redirect`; network error mapping belongs in `http.runtime.error`; runtime settings belong in `http.runtime.config`; UI-neutral interaction ports belong in `http.runtime.interaction`; network/lifecycle observation ports belong in `http.runtime.observation`; Swing implementations belong in UI adapters such as `com.laker.postman.panel.http.runtime`. HTTP runtime, OkHttp adapters, SSL configuration, Cookie notification, download progress, response-size warnings, and WebSocket lifecycle logging must not import Swing/panel classes directly or read app `SettingManager` directly. If HTTP runtime gets its own module later, prefer a focused `easy-postman-http-runtime` over a vague `easy-postman-core`.

## Update Boundaries

- Put update discovery core in `platform`: `UpdateInfo`, `UpdateCheckFrequency`, `VersionChecker`, `VersionComparator`, `UpdateSourceSelector`, release sources, asset resolvers, changelog service/formatter, and Windows registry/package-mode helpers.
- Keep concrete update UX in `app`: `AutoUpdateManager`, `UpdateUIManager`, `UpdateDownloader`, update dialogs/notifications, manual-download/open-browser commands, install prompts, and app shutdown for installation.
- `platform` update code must not import app `SettingManager`; inject a minimal provider such as `UpdateSettingsProvider` and adapt it in app with `SettingManager::getUpdateSourcePreference`.

## I18n, Fonts, Theme

- I18n mechanism and cross-module generic labels belong in `foundation`: `I18nUtil`, `CommonI18n`, `CommonMessageKeys`, and `common-messages*`.
- I18n resources follow their owner: generic short labels such as OK, Cancel, Save, Copy, Close, Search, Success, Error, Warning, and Tip in foundation common bundles; shared UI component-specific strings in `ui` (`ui-messages*`); host strings in `app` (`messages_*`); plugin strings in each plugin.
- Font helpers and typography rules belong in `ui`; startup application of font settings belongs in `platform` once decoupled from app-specific wiring.
- Theme tokens, semantic colors, icon color strategies, RSyntaxTextArea editor theme XMLs, and reusable UI resources belong in `ui`; FlatLaf installation and theme switching belong in `platform` once decoupled from app-specific wiring. FlatLaf properties tied to app LAF classes can stay in `app` until those classes move.
- Primary-color buttons must use on-primary icon color. Icons on blue/brand buttons stay white and must not switch with the light/dark theme foreground.

## Anti-patterns

- Do not use a vague `common` module as the default destination.
- Do not create a vague `easy-postman-core` module for unrelated request, collection, runtime, and UI concerns.
- Do not put Swing code in `foundation`.
- Do not put request specification models back into `easy-postman-app`.
- Do not make `easy-postman-request-core` depend on Swing, OkHttp, app service/panel code, or plugin runtime.
- Do not put collection core models or Postman collection parsing back into `easy-postman-app`.
- Do not make `easy-postman-collection-core` depend on Swing, OkHttp, app service/panel/runtime code, platform, plugin runtime, or IOC.
- Do not make HTTP runtime/service classes depend directly on Swing/panel or app `SettingManager`; adapt UI through neutral sinks/dispatchers and settings through `HttpRuntimeSettingsProvider` so Swing, CLI tests, and future JavaFX hosts can provide separate implementations.
- Do not put UI view-state, importer scratch DTOs, script snippets, functional runner rows/results, stream message types, certificate settings rows, or history records back into `easy-postman-app/src/main/java/com/laker/postman/model`.
- Do not put plugin service contracts in `foundation`.
- Do not put shared reusable UI components directly in `app`.
- Do not make shared UI components depend on app-owned message bundles, icons, editor themes, or other app resources.
- Do not duplicate generic short labels in `ui-messages*` or plugin message bundles when `CommonMessageKeys` already owns them.
- Do not make plugins depend on `easy-postman-app`.
- Do not introduce new app-local color/font/button conventions before checking `easy-postman-ui`.
- Do not duplicate same-named `icons/*.svg` resources between `easy-postman-app` and `easy-postman-ui`; shared control icons belong in `ui`, app/domain icons stay with their owning app/plugin module. If a plugin references an icon, it must be plugin-owned or UI-owned.

## Verification

For module-boundary changes, run:

```bash
mvn -q -pl easy-postman-app -am -Dtest=ModuleArchitectureBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -pl easy-postman-platform -am -Dtest=UpdateSourceSelectorTest,PlatformDownloadUrlResolverTest,AppReleaseSelectorTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -DskipTests compile
```
