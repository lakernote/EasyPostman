---
name: swing-intellij-dialog-style
description: Use when modifying EasyPostman Swing dialogs, popups, wizards, or modal flows to better match IntelliJ IDEA or FlatLaf settings-style visual hierarchy, especially when read-only panels, editable inputs, steps, scroll panes, footers, or action choices are visually unclear.
---

# Swing IntelliJ Dialog Style

Use this skill for EasyPostman Swing dialogs that should feel closer to IntelliJ IDEA: quiet, dense, readable, and clearly separated by behavior rather than decoration.

## Use With

- Read `swing-flatlaf-miglayout-principles` first when the change touches form layout, focus rings, clipping, nested panels, scroll panes, or section borders.
- Read `fontsutil-font-usage` when changing any label, title, option, table, renderer, or dialog font size.
- Keep reusable controls, icons, fonts, and colors in `easy-postman-ui`; keep host dialog composition in `easy-postman-app`.

## Core Style

- Prefer dialog-surface backgrounds plus thin separators over large banner headers or heavy cards.
- Treat dialog chrome as separate from app frame/tool-window chrome:
  - Use `ModernColors.getDialogChromeBackgroundColor()` through `ToolWindowSurfaceStyle.applyDialogWindowChrome(...)`, `applyDialogSurface(...)`, `applyDialogSection(...)`, and `applyDialogFooter(...)`.
  - Do not use or change `ModernColors.getWindowChromeBackgroundColor()` for dialogs; that belongs to the main frame/menu/title chrome.
  - Light dialog chrome is expected to read near `247,248,249`; dark dialog chrome near `25,26,28`.
- Keep the split of responsibility clear:
  - FlatLaf `.properties` and `ModernColors` own theme tokens and standard component defaults.
  - `ToolWindowSurfaceStyle` owns reusable styling for custom composite dialog surfaces, scroll panes, list blank areas, headers, and footers.
  - Individual dialog classes should only express layout or renderer behavior that is local to that dialog.
- Keep headers compact: small icon or no icon, title at `FontsUtil.getDefaultFontWithOffset(Font.BOLD, +1)` or lower, secondary metadata at `-1`.
- Use one outer border strategy per content region. For IDEA-like sections, use a light line border plus modest inner padding such as `8,10,8,10`.
- Avoid nested card-in-card layouts. Use rows, split content, or thin `MatteBorder` separators.
- Use `ModernButtonFactory` for dialog footer buttons so Cancel/OK/Apply/primary actions match settings dialogs.
- Use `ToolWindowSurfaceStyle.applyDialogFooter(...)` for the footer and `applyDialogSurface(...)` for outer containers.
- Use `ToolWindowSurfaceStyle.applyDialogSection(...)` for bordered non-edit sections instead of `applyCard(...)` or `applySectionCard(...)`.
- Use `ToolWindowSurfaceStyle.applyTextComponentDialogSurface(...)` for read-only text viewers inside dialogs.

## Read-Only vs Editable

Make editable controls visibly different from read-only output.

- Read-only output areas:
  - `JEditorPane` or viewer components should usually use dialog-surface background, no strong input border, and secondary text where appropriate.
  - Add bottom padding inside scroll content so text does not collide with separators or fixed bottom sections.
  - Keep scrollbars narrow and low-contrast when the content is informational.
- Non-edit dialog content:
  - Do not paint large white cards just to group content. Match the dialog chrome background and use separators or `applyDialogSection(...)` when grouping is necessary.
  - Dialog `JList` blank areas and read-only list rows should use `ToolWindowSurfaceStyle.applyDialogList(...)` or `applyDialogListScrollPane(...)`; do not restyle them with `applyListCard(...)` inside dialogs.
  - Reserve white/light input color for actual editable controls such as `JTextField`, `JTextArea`, `JPasswordField`, `JComboBox`, and editable table cells.
- Editable inputs:
  - Use input background, visible medium border, and inner padding.
  - Add focus feedback, usually primary-color border on focus.
  - If the surrounding area contains mostly read-only content, label editable sections with a small edit icon such as `icons/edit.svg`.
  - Preserve FlatLaf focus visibility; do not hide focus rings with wrapper borders.

Typical editable text-area pattern:

```java
ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
textArea.setBorder(new EmptyBorder(5, 8, 5, 8));
scrollPane.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(focused ? ModernColors.getPrimary() : ModernColors.getBorderMediumColor()),
        BorderFactory.createEmptyBorder(1, 1, 1, 1)
));
```

## Wizard/Step Areas

- Keep step indicators compact and secondary to the content.
- Use small circular badges, muted inactive labels, and a clear active state.
- Do not let the step strip consume the vertical space needed by the main content.
- If current progress is not actionable, keep it visually quieter than form inputs and action choices.

## Scroll And Fixed Sections

- When a scrollable read-only area sits above a fixed editable section, separate them with a thin `MatteBorder` and add top padding to the fixed section.
- Add bottom padding to the scrollable content so the final line is not visually clipped by the separator.
- Use a modest scrollbar width, for example `8px`, for compact informational panes.
- Avoid making scrollbars or empty gutters the strongest visual element in the dialog.
- If a scroll pane contains an editable text area, style the scroll pane as an input border; if it contains read-only content, use dialog-surface scroll styling plus a subtle separator/border only when needed.
- Use `JSplitPane` only when the user needs to resize sections. For fixed two-column dialog layouts, prefer `MigLayout` columns and gaps so the split divider does not become a visible artifact.

## Action Choices

- For radio-button strategy sections, use a light bordered section with compact title, radio label, and muted description.
- Keep option descriptions smaller with `FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -3)` or similar.
- Dangerous options can use `ModernColors.getError()` for the radio label, but avoid large warning blocks unless the action is destructive and easy to trigger.

## I18n And Icons

- User-visible text must use `I18nUtil.getMessage(...)` or the appropriate shared i18n helper.
- Remove decorative emoji from dialog status text; use structure, color, and icons instead.
- Use UI-owned generic icons such as `icons/edit.svg` through `IconUtil.createThemed(...)`.

## Verification

- Compile the app module: `mvn -q -pl easy-postman-app -am -DskipTests compile`.
- Run focused existing tests when a component has one, for example `StepIndicatorTest`.
- For editable fields, manually reason or visually verify that the default state, focused state, and disabled state are distinguishable.
- Check Chinese and English labels for truncation and excessive fixed-width assumptions.
