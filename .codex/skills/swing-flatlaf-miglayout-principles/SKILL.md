---
name: swing-flatlaf-miglayout-principles
description: Use when modifying EasyPostman Swing forms that use FlatLaf and MigLayout, especially when layout refactors introduce clipped focus rings, dense spacing, border conflicts, or inconsistent form structure.
---

# Swing FlatLaf MigLayout Principles

Use this skill when editing Swing form layouts in this repo. The goal is not just fixing one bug, but following stable layout principles that work well with `FlatLaf` and `MigLayout`.

## When to use

- A `JTextField`, `JPasswordField`, or `JComboBox` looks fine until focused
- Focus/highlight borders are clipped or hidden
- A refactor added borders, cards, sections, or sub-panels and spacing became unstable
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

## Repo-specific guidance

- Kafka top connection form is the known reference case:
  `src/main/java/com/laker/postman/panel/toolbox/kafka/connection/ui/KafkaConnectionPanel.java`
- The correct direction there was:
  - keep the layout in MigLayout
  - add `novisualpadding`
  - avoid titled section borders
  - reintroduce only plain borders after focus behavior is stable
- If the user says "it is probably MigLayout", take that seriously and verify MigLayout constraints before touching theme or component code.

## Theme entry points in this repo

When the task is about light/dark theme colors instead of layout, start from these files instead of scattering hard-coded colors in panel code:

- Shared semantic colors for both themes:
  `easy-postman-plugin-ui/src/main/java/com/laker/postman/common/constants/ModernColors.java`
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

## Anti-patterns

- Stacking `EmptyBorder` repeatedly without changing MigLayout behavior
- Keeping `TitledBorder` on dense editable forms after focus clipping appears
- Solving a layout bug by moving controls farther apart without understanding the container behavior
- Adding more nested panels than the visual structure actually needs

## Verification

After the fix, verify all of the following:

1. Focus a text field in the affected row.
2. Focus a combo box in the same area.
3. Confirm the highlight ring is visible on top, bottom, left, and right.
4. Confirm spacing still looks intentional when the control is unfocused.
5. Rebuild with `mvn -q -DskipTests compile`.
