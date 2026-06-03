package com.laker.postman.util;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class I18nGovernanceTest {

    @Test
    public void shouldExposeSupportedLocalesAndFallbackLocale() {
        assertEquals(I18nUtil.fallbackLocale(), Locale.ENGLISH);
        assertEquals(I18nUtil.supportedLocales(), List.of(Locale.ENGLISH, Locale.CHINESE));
        assertEquals(I18nUtil.normalizeSupportedLocale(Locale.SIMPLIFIED_CHINESE), Locale.CHINESE);
        assertEquals(I18nUtil.normalizeSupportedLocale(Locale.FRENCH), Locale.ENGLISH);
    }

    @Test
    public void pluginBundleLookupShouldRegisterBundleForDiagnostics() {
        I18nUtil.getMessage(
                "plugin-test-messages",
                I18nGovernanceTest.class.getClassLoader(),
                "hello",
                "Codex"
        );

        assertTrue(I18nBundleRegistry.registeredBundles().stream()
                .anyMatch(bundle -> "plugin-test-messages".equals(bundle.ownerId())
                        && "plugin-test-messages".equals(bundle.bundleName())));
    }

    @Test
    public void bundleLookupShouldFallbackToEnglishLocaleBundle() throws Exception {
        Locale originalLocale = forceCurrentLocale(Locale.CHINESE);
        try {
            assertEquals(
                    I18nUtil.getMessage(
                            "fallback-test-messages",
                            I18nGovernanceTest.class.getClassLoader(),
                            "only.en"
                    ),
                    "English fallback"
            );
        } finally {
            forceCurrentLocale(originalLocale);
        }
    }

    @Test
    public void diagnosticsShouldFindMissingAndDuplicateKeys() throws Exception {
        Path resourcesRoot = Files.createTempDirectory("i18n-diagnostics");
        Files.writeString(resourcesRoot.resolve("demo-messages_en.properties"),
                """
                hello=Hello
                english.only=English only
                duplicated=First
                duplicated=Second
                """);
        Files.writeString(resourcesRoot.resolve("demo-messages_zh.properties"),
                """
                hello=你好
                """);

        I18nDiagnosticsReport report = I18nBundleDiagnostics.scanResourceDirectory(
                resourcesRoot,
                List.of("demo-messages")
        );

        assertTrue(report.hasIssues());
        assertTrue(report.missingKeys().stream()
                .anyMatch(key -> "demo-messages".equals(key.bundleName())
                        && Locale.CHINESE.equals(key.locale())
                        && "english.only".equals(key.key())));
        assertTrue(report.duplicateKeys().stream()
                .anyMatch(key -> key.resourceFile().endsWith("demo-messages_en.properties")
                        && "duplicated".equals(key.key())
                        && key.lineNumbers().equals(List.of(3, 4))));
    }

    @Test
    public void diagnosticsShouldPassWhenLocaleKeysMatchFallbackAndNoDuplicates() throws Exception {
        Path resourcesRoot = Files.createTempDirectory("i18n-diagnostics-clean");
        Files.writeString(resourcesRoot.resolve("clean-messages.properties"),
                """
                hello=Hello
                goodbye=Goodbye
                """);
        Files.writeString(resourcesRoot.resolve("clean-messages_zh.properties"),
                """
                hello=你好
                goodbye=再见
                """);

        I18nDiagnosticsReport report = I18nBundleDiagnostics.scanResourceDirectory(
                resourcesRoot,
                List.of("clean-messages")
        );

        assertFalse(report.hasIssues());
    }

    private static Locale forceCurrentLocale(Locale locale) throws Exception {
        Field currentLocaleField = I18nUtil.class.getDeclaredField("currentLocale");
        currentLocaleField.setAccessible(true);
        Locale original = (Locale) currentLocaleField.get(null);
        currentLocaleField.set(null, locale);
        return original;
    }
}
