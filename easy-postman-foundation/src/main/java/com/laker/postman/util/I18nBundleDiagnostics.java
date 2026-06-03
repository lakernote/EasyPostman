package com.laker.postman.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Slf4j
@UtilityClass
public class I18nBundleDiagnostics {

    public static I18nDiagnosticsReport scanRegisteredBundles() {
        List<I18nMissingKey> missingKeys = new ArrayList<>();
        for (I18nBundleDescriptor bundle : I18nBundleRegistry.registeredBundles()) {
            Set<String> fallbackKeys = loadBundleKeys(bundle.bundleName(), I18nUtil.fallbackLocale(), bundle.classLoader(), true);
            if (fallbackKeys.isEmpty()) {
                continue;
            }
            for (Locale locale : I18nUtil.supportedLocales()) {
                if (I18nUtil.fallbackLocale().getLanguage().equals(locale.getLanguage())) {
                    continue;
                }
                Set<String> localeKeys = loadBundleKeys(bundle.bundleName(), locale, bundle.classLoader(), false);
                addMissingKeys(missingKeys, bundle.ownerId(), bundle.bundleName(), locale, fallbackKeys, localeKeys);
            }
        }
        return new I18nDiagnosticsReport(missingKeys, List.of());
    }

    public static I18nDiagnosticsReport scanResourceDirectory(Path resourcesRoot, Collection<String> bundleNames) {
        List<I18nMissingKey> missingKeys = new ArrayList<>();
        List<I18nDuplicateKey> duplicateKeys = new ArrayList<>();
        for (String bundleName : bundleNames) {
            Set<String> fallbackKeys = loadBundleKeys(resourcesRoot, bundleName, I18nUtil.fallbackLocale(), true);
            for (Locale locale : I18nUtil.supportedLocales()) {
                if (I18nUtil.fallbackLocale().getLanguage().equals(locale.getLanguage())) {
                    continue;
                }
                Set<String> localeKeys = loadBundleKeys(resourcesRoot, bundleName, locale, false);
                addMissingKeys(missingKeys, bundleName, bundleName, locale, fallbackKeys, localeKeys);
            }
            for (Path candidate : bundleResourceCandidates(resourcesRoot, bundleName)) {
                if (Files.exists(candidate)) {
                    duplicateKeys.addAll(findDuplicateKeys(candidate));
                }
            }
        }
        return new I18nDiagnosticsReport(missingKeys, duplicateKeys);
    }

    public static List<I18nDuplicateKey> findDuplicateKeys(Path propertyFile) {
        Map<String, List<Integer>> occurrences = new LinkedHashMap<>();
        try {
            List<String> lines = Files.readAllLines(propertyFile, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String key = parsePropertyKey(lines.get(i));
                if (key != null && !key.isBlank()) {
                    occurrences.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i + 1);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to inspect duplicate i18n keys in {}", propertyFile, e);
            return List.of();
        }

        List<I18nDuplicateKey> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : occurrences.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicates.add(new I18nDuplicateKey(propertyFile, entry.getKey(), entry.getValue()));
            }
        }
        return duplicates;
    }

    private static void addMissingKeys(List<I18nMissingKey> missingKeys,
                                       String ownerId,
                                       String bundleName,
                                       Locale locale,
                                       Set<String> fallbackKeys,
                                       Set<String> localeKeys) {
        for (String key : fallbackKeys) {
            if (!localeKeys.contains(key)) {
                missingKeys.add(new I18nMissingKey(ownerId, bundleName, locale, key));
            }
        }
    }

    private static Set<String> loadBundleKeys(String bundleName,
                                              Locale locale,
                                              ClassLoader classLoader,
                                              boolean includeBaseFallback) {
        for (String resourceName : bundleResourceNames(bundleName, locale, includeBaseFallback)) {
            try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
                if (inputStream != null) {
                    return loadKeys(inputStream);
                }
            } catch (IOException e) {
                log.warn("Failed to read i18n bundle resource {}", resourceName, e);
            }
        }
        return Set.of();
    }

    private static Set<String> loadBundleKeys(Path resourcesRoot,
                                              String bundleName,
                                              Locale locale,
                                              boolean includeBaseFallback) {
        for (String resourceName : bundleResourceNames(bundleName, locale, includeBaseFallback)) {
            Path path = resourcesRoot.resolve(resourceName);
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    return loadKeys(reader);
                } catch (IOException e) {
                    log.warn("Failed to read i18n bundle file {}", path, e);
                }
            }
        }
        return Set.of();
    }

    private static Set<String> loadKeys(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return new LinkedHashSet<>(properties.stringPropertyNames());
    }

    private static Set<String> loadKeys(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        return new LinkedHashSet<>(properties.stringPropertyNames());
    }

    private static List<Path> bundleResourceCandidates(Path resourcesRoot, String bundleName) {
        List<Path> candidates = new ArrayList<>();
        for (Locale locale : I18nUtil.supportedLocales()) {
            for (String resourceName : bundleResourceNames(bundleName, locale, true)) {
                Path path = resourcesRoot.resolve(resourceName);
                if (!candidates.contains(path)) {
                    candidates.add(path);
                }
            }
        }
        return candidates;
    }

    private static List<String> bundleResourceNames(String bundleName, Locale locale, boolean includeBaseFallback) {
        String baseName = bundleName.replace('.', '/');
        List<String> names = new ArrayList<>();
        if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isBlank()) {
            if (locale.getCountry() != null && !locale.getCountry().isBlank()) {
                names.add(baseName + "_" + locale.getLanguage() + "_" + locale.getCountry() + ".properties");
            }
            names.add(baseName + "_" + locale.getLanguage() + ".properties");
        }
        if (includeBaseFallback) {
            names.add(baseName + ".properties");
        }
        return names;
    }

    private static String parsePropertyKey(String line) {
        String trimmed = line.stripLeading();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
            return null;
        }

        int separatorIndex = -1;
        boolean escaped = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '=' || ch == ':' || Character.isWhitespace(ch)) {
                separatorIndex = i;
                break;
            }
        }

        String key = separatorIndex >= 0 ? trimmed.substring(0, separatorIndex) : trimmed;
        return key.trim();
    }
}
