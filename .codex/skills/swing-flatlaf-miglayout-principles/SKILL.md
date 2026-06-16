---
name: swing-flatlaf-miglayout-principles
description: Use when modifying EasyPostman Swing forms, tool-window panels, localized English/Chinese text layouts, or FlatLaf/MigLayout layouts, especially when refactors introduce clipped focus rings, truncated text, clipped status badges, dense spacing, border conflicts, rounded-card corner leaks, or inconsistent form structure.
---

# Swing FlatLaf MigLayout Principles

Use this skill when editing Swing form layouts in this repo. The goal is not just fixing one bug, but following stable layout principles that work well with `FlatLaf` and `MigLayout`.

## When to use

- A `JTextField`, `JPasswordField`, or `JComboBox` looks fine until focused
- Focus/highlight borders are clipped or hidden
- A list item, sidebar row, badge, tab, or localized label is truncated or spills out of its container
- A refactor added borders, cards, sections, or sub-panels and spacing became unstable
- A rounded tool-window card or panel corner looks square because an opaque child paints over the chrome
- A dense toolbar/form row in MigLayout starts to look cramped or visually inconsistent
- A Swing form in EasyPostman needs to be reorganized without regressing FlatLaf behavior

## Core principles

1. Preserve focus visibility first.
   In this repo, form controls must keep the full FlatLaf focus ring visible on all sides.

2. Prefer layout fixes over padding hacks.
   If the issue appears only on focus, suspect layout constraints or border interaction before adding more empty space.

3. Avoid decorative borders around dense input areas.
   Dense forms usually behave better with separators, spacing, and simple line borders than with `TitledBorder`.

4. Keep form hierarchy shallow.
   Extra wrapper panels often make focus rendering and spacing harder to reason about.

5. Follow existing repo form patterns.
   Reuse the simpler top-bar/form-row patterns already used in other toolbox panels where possible.

6. Treat localized text as variable-width UI.
   English status strings and Chinese labels can have very different widths. Do not rely on one locale fitting a fixed row.

## Preferred fix order

1. Check whether the affected container uses `MigLayout`.
   If yes, evaluate whether `visualPadding` is the real cause.

2. Add `novisualpadding` to the relevant `MigLayout` containers first.
   Apply it to the form layout and the immediate child panels that place focusable controls.

3. If clipping remains, simplify borders around the focused components.
   Prefer:
   - `EmptyBorder`
   - a plain `LineBorder`
   - separators between sections

4. Only then tune insets/gaps.
   Padding should refine the layout, not compensate for the wrong layout model.

5. If a refactor introduced titles inside borders, remove the titles first.
   If grouping is still needed, use borderless sections plus separators, or a plain line border without title text.

6. For truncated text or badges in list renderers, inspect row/column constraints before changing fonts.
   Long labels in `[pref!]`, `shrink 0`, or same-row title/status layouts can force clipping inside fixed-width sidebars.

## Narrow list and badge layouts

- In fixed-width sidebars, avoid putting a variable-length title and a variable-length status badge in the same row.
  Prefer:
  - row 1: title spans all columns with `span 2, growx, wmin 0, wrap`
  - row 2: version/meta in a growing column, compact status badge in a right-aligned `pref` column
- Keep `wmin 0` on labels that may need to shrink or ellipsize. Without it, MigLayout can preserve the label's preferred width and push siblings out.
- Use `shrink 0` only for controls or badges whose displayed text is intentionally short. Never apply it to long localized status strings.
- If a status must be semantically long, add a short list-specific i18n key for the badge and keep the full text in the details panel or tooltip.
- Do not fix repeated clipping by reducing font sizes. Use `FontsUtil` only to preserve hierarchy after the layout constraints are correct.
- Recheck both installed and marketplace renderers when they share the same list pattern; otherwise the bug usually comes back in the sibling view.

## Localized text width

- Treat every user-visible string as variable width across Chinese and English. Never tune a container only against the current screenshot locale.
- For compact UI surfaces such as toolbar buttons, tabs, sidebars, badges, table headers, and custom-painted summaries, decide which text is primary and which text may truncate before changing layout.
- Use separate short i18n keys for compact commands or tabs when the full label is too long. Keep the full phrase in tooltips, detail panels, or accessible text instead of forcing one key to serve every surface.
- Prefer layout capacity over font-size reduction: adjust `growx`, `wmin 0`, column constraints, dynamic preferred sizes, or custom `FontMetrics` measurement before considering typography.
- For custom painting, measure translated labels with the actual `FontsUtil` font and allocate label/value areas dynamically. Avoid fixed label widths chosen from one locale.
- Do not ellipsize critical labels or command text first. Ellipsize secondary values, metadata, long hostnames, certificate names, and paths; provide a tooltip or detail view with the full value.
- Buttons with localized text should use familiar icons plus concise labels when space is tight. Icon-only buttons need tooltips and accessible names.
- When fixing localized truncation, add a test at the source of the layout rule: assert the longest English and Chinese labels fit or that truncation has a full-value tooltip. Prefer width-budget tests over screenshot assertions.

## Rounded tool-window card chrome

- `RoundedToolWindowPanel` owns the rounded card shape; child panels should not be treated as the outer card chrome.
- A rounded parent alone is not enough if a direct child is opaque and touches the card edge. Components such as `JSplitPane`, `JScrollPane`, `JTable`, and large content panels can repaint independently and fill a rectangular area unless repaint is routed through the rounded parent or the child is kept away from the edge.
- For top-level tool windows, prefer `ToolWindowChrome` / `AppToolWindowChrome` wrappers (`wrapToolWindow`, `wrapInsetToolWindow`, `createHorizontalCardSplitPane`, `createVerticalCardSplitPane`) instead of manually applying `applyCard` to the root panel.
- If content is meant to sit inside a rounded card, add an inset wrapper (`createInsetContent`, `wrapInsetToolWindow`, or the side-specific wrapper) when controls or opaque scroll/split/table surfaces would otherwise touch the rounded corners.
- Inside an already rounded card, avoid stacking more opaque `applyCard` panels at the outer edge just to get the same background. Use transparent section panels or inner split panes where the parent card should remain visually continuous.
- If a rounded corner becomes square only after interaction, resize, scrolling, or split-pane dragging, suspect descendant repaint bypassing the parent clip before changing colors or arc values. The fix belongs in the rounded chrome or wrapper hierarchy, not in per-panel paint hacks.

## IDEA-like tool-window separators

- IntelliJ IDEA tool-window separators are usually quiet 1px lines, not thick gray bands. Match that visual weight unless the user explicitly wants a larger gutter.
- For toolbar-to-content boundaries, use a bottom/top `MatteBorder` with `ModernColors.getTabSeparatorColor()`. Avoid `getDividerBorderColor()` for these header separators unless a deliberately stronger boundary is needed.
- For request/response or other resizable `JSplitPane` dividers, keep the divider's hit area wide enough for dragging, but paint the visible separator as a centered 1px line:
  - fill the divider with the surrounding card/background color
  - draw one line with `ModernColors.getTabSeparatorColor()`
  - orient the line from `splitPane.getOrientation()`
- For editor tab strips that need an IDEA-like baseline, prefer `TABBED_PANE_HAS_FULL_BORDER=false` and `TABBED_PANE_SHOW_CONTENT_SEPARATOR=true` with `TabbedPane.contentSeparatorHeight=1`; do not add an extra hand-drawn border on top of FlatLaf's content separator.
- For inner request tabs or detail tabs whose content should flow as one card, explicitly disable the content separator instead of stacking another border.
- For stacked bottom tool windows such as Console, keep the status bar outside the resizable tool-window split and use `ToolWindowChrome`/`AppToolWindowChrome` stacked-card APIs so the top/bottom gap matches the left/right tool-window gap.
- If the tool-window split belongs inside a tab content area, keep the `JTabbedPane` component identity stable. Prefer a small stable host component that owns the real content and swaps only its internal layout; do not repeatedly replace tab components with temporary `JSplitPane` wrappers.
- Do not let a persistent scrollbar become a fake divider. Use `VERTICAL_SCROLLBAR_AS_NEEDED` unless always-visible scrollbars are a functional requirement.
- Regression tests should lock the behavior at the source:
  - assert the tab pane FlatLaf client properties
  - assert toggling the tool window does not replace the tab component identity when a stable host is needed
  - inspect toolbar `MatteBorder` color/insets
  - paint split-pane dividers into a `BufferedImage` and assert only the centered pixel line uses the separator color

## Repo-specific guidance

- For module placement before changing shared UI, read `docs/ARCHITECTURE_MODULES_zh.md` and keep reusable Swing components, colors, fonts, icons, notifications, and editor theme helpers in `easy-postman-ui`.
- Kafka top connection form is the known reference case:
  `easy-postman-plugins/plugin-kafka/src/main/java/com/laker/postman/plugin/kafka/connection/ui/KafkaConnectionPanel.java`
- The correct direction there was:
  - keep the layout in MigLayout
  - add `novisualpadding`
  - avoid titled section borders
  - reintroduce only plain borders after focus behavior is stable
- If the user says "it is probably MigLayout", take that seriously and verify MigLayout constraints before touching theme or component code.
- In dense connection toolbars, `JSpinner`/`EasyJSpinner` can make MigLayout's baseline calculation inflate a row even when the spinner preferred height was manually compacted. If a row mixes text fields, combo boxes, buttons, and spinners, constrain the row height explicitly, for example `[" + ConnectionToolbarUi.FORM_CONTROL_HEIGHT + "!]`, then verify with the real FlatLaf theme and saved UI font size.
- For multi-row connection forms, reuse the same column specification across rows when fields are expected to line up. Do not switch from the first row's custom columns, such as narrow `port`/`DB` label widths, to generic `connectionFieldColumns(...)` on the second row; labels like username/password will visibly drift.

## Theme entry points in this repo

When the task is about light/dark theme colors instead of layout, start from these files instead of scattering hard-coded colors in panel code:

- Shared semantic colors for both themes:
  `easy-postman-ui/src/main/java/com/laker/postman/common/constants/ModernColors.java`
- FlatLaf light theme tokens and component defaults:
  `easy-postman-app/src/main/resources/com/laker/postman/common/themes/EasyLightLaf.properties`
- FlatLaf dark theme tokens and component defaults:
  `easy-postman-app/src/main/resources/com/laker/postman/common/themes/EasyDarkLaf.properties`
- RSyntaxTextArea editor theme for light mode:
  `easy-postman-app/src/main/resources/themes/easypostman-light.xml`
- RSyntaxTextArea editor theme for dark mode:
  `easy-postman-app/src/main/resources/themes/easypostman-dark.xml`

Use the files with this intent:

1. `ModernColors.java`
   Use for shared semantic brand colors such as primary blue, hover blue, status colors, and colors referenced directly by custom Swing painting or custom components.
2. `EasyLightLaf.properties` / `EasyDarkLaf.properties`
   Use for FlatLaf UI defaults such as `Component.accentColor`, `Table.selectionBackground`, `Tree.selectionBackground`, `TabbedPane.*`, borders, backgrounds, and hover states.
3. `easypostman-light.xml` / `easypostman-dark.xml`
   Use only for editor syntax colors inside `RSyntaxTextArea`. These files are loaded by `EditorThemeUtil`, not by FlatLaf itself.

Preferred order when adjusting theme:

1. If the change is a shared accent or semantic status color, start with `ModernColors.java`.
2. If the change is a standard Swing/FlatLaf control state, start with `EasyLightLaf.properties` or `EasyDarkLaf.properties`.
3. If the change is only about code editor token colors, caret, selection, or current-line styling, change the `easypostman-*.xml` files.
4. Do not hard-code new colors directly in panels before checking whether one of the files above is the right source of truth.

## Design heuristics for this repo

- Top tool panels should prefer:
  - one clear outer border strategy
  - simple horizontal/vertical separators
  - compact but not crowded form rows
- Section grouping should be visual, not heavy-handed.
- If a grouped layout costs too much vertical space or breaks focus rendering, simplify it.
- Sidebars with action/status rows should favor scanability over cramming: let the main label breathe, then place status/meta on the next row.

## Anti-patterns

- Stacking `EmptyBorder` repeatedly without changing MigLayout behavior
- Keeping `TitledBorder` on dense editable forms after focus clipping appears
- Solving a layout bug by moving controls farther apart without understanding the container behavior
- Adding more nested panels than the visual structure actually needs
- Combining long title text and long status text in a fixed-width list row with both components preserving preferred width
- Adding absolute point-size font tweaks to make one English string fit
- Reusing one long i18n key for compact tabs, buttons, toolbars, and full detail labels

## Verification

After the fix, verify all of the following:

1. Focus a text field in the affected row.
2. Focus a combo box in the same area.
3. Confirm the highlight ring is visible on top, bottom, left, and right.
4. Confirm spacing still looks intentional when the control is unfocused.
5. For truncation fixes, verify the longest English and Chinese labels/statuses in the affected fixed-width container.
6. For buttons, tabs, and custom-painted text, verify any truncated value has a tooltip or detail path with the full text.
7. Rebuild with `mvn -q -DskipTests compile`.
