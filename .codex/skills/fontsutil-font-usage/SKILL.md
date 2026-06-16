---
name: fontsutil-font-usage
description: Use when modifying EasyPostman Swing UI fonts, especially when dialogs, labels, tables, tabs, or renderers look too large or too small, or when a change needs to stay consistent with the user's configured UI font size.
---

# FontsUtil Font Usage

Use this skill when changing Swing font sizes in this repo. The goal is to keep UI text aligned with the user's configured base font size instead of freezing sizes with hard-coded `deriveFont(..., 15f)` style values.

## When to use

- A dialog or panel looks too large or too small after the user changes global font size
- A patch is about title, subtitle, label, table, list, tab, or badge font sizing
- You see hard-coded point sizes such as `13f`, `15f`, `18f`, `22f`
- You are touching renderers or custom components that should track the repo's UI font setting

## Core rules

1. If size changes, prefer `FontsUtil.getDefaultFontWithOffset(style, offset)`.
2. If size should stay at the user default, use `FontsUtil.getDefaultFont(style)`.
3. Avoid absolute point sizes for normal app UI unless there is a strong visual reason.
4. Keep offsets modest for standard dialogs.
   Typical ranges in this repo:
   - body text: `0` or `-1`
   - secondary/meta text: `-1` or `-2`
   - section/list titles: `+1`
   - dialog/detail titles: `+1` or `+2`
5. If only weight changes and size should remain exactly as-is, `font.deriveFont(Font.BOLD)` is acceptable.
6. Do not solve localized English/Chinese text overflow by shrinking fonts first. Fix layout constraints, short i18n keys, wrapping, dynamic measurement, or tooltip-backed truncation before changing typography.

## Repo-specific guidance

- For module placement, `FontsUtil` and reusable typography helpers belong in `easy-postman-ui`; startup-time application of font settings is app/platform orchestration. See `docs/ARCHITECTURE_MODULES_zh.md`.
- `FontsUtil` lives at:
  `easy-postman-ui/src/main/java/com/laker/postman/util/FontsUtil.java`
- `FontsUtil` reads the configured `ui_font_size`, clamps it, and derives from UI defaults.
- For this repo, using `FontsUtil` is the correct way to respect user-configured font size and keep fallback chains intact.

## Anti-patterns

- `label.getFont().deriveFont(Font.BOLD, 18f)` for regular dialog headers
- Mixing `FontsUtil` and hard-coded point sizes in the same panel without a reason
- Fixing a "font too big" complaint by picking another absolute size
- Reducing the UI font offset because an English button, tab, or label does not fit a Chinese-sized container

## Verification

1. Check the changed view with the user's current UI font size.
2. Confirm titles, meta text, and body text still have a clear visual hierarchy.
3. If the change is in app code, rebuild with `mvn -q -pl easy-postman-app -am -DskipTests compile`.
