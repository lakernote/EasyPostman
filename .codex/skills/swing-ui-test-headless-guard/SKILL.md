---
name: swing-ui-test-headless-guard
description: Use when adding or updating EasyPostman Swing/TestNG UI tests that may run in headless CI. Extract and reuse the shared skip logic from AbstractSwingUiTest instead of duplicating DISPLAY/headless checks in each test.
---

# Swing UI Test Headless Guard

Use this skill for Swing UI tests under `easy-postman-app/src/test/java`.

## Default pattern

1. Put shared no-display skip logic in:
   `easy-postman-app/src/test/java/com/laker/postman/test/AbstractSwingUiTest.java`
2. Make Swing/UI tests extend that base class.
3. Use `@BeforeClass` + `SkipException` to skip tests when:
   - `GraphicsEnvironment.isHeadless()` is `true`
   - or Linux has neither `DISPLAY` nor `WAYLAND_DISPLAY`

## Guardrails

- Keep the base class narrow. Only put environment-skip behavior there unless multiple UI tests clearly need more.
- Do not add this base class to non-UI tests.
- Prefer `SkipException` over brittle per-test conditionals.
- After changes, verify with:
  - `mvn -q -pl easy-postman-app -am -DskipTests compile`
  - `mvn -q -pl easy-postman-app -am -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test`
