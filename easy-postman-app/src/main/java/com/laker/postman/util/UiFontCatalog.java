package com.laker.postman.util;

import lombok.experimental.UtilityClass;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

@UtilityClass
public class UiFontCatalog {

    private static final int INSPECTION_FONT_SIZE = 13;
    private static final String CJK_SAMPLE = "界面字体汉字";
    private static final String EMOJI_SAMPLE = "😀🚀✨";
    private static final ExecutorService FONT_LOAD_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ui-font-catalog-loader");
        thread.setDaemon(true);
        return thread;
    });
    private static final FontOptionCache FONT_OPTION_CACHE = new FontOptionCache();

    public enum FontSupport {
        FULL(true, true),
        NO_EMOJI(true, false),
        NO_CJK(false, false);

        private final boolean supportsCjk;
        private final boolean supportsEmoji;

        FontSupport(boolean supportsCjk, boolean supportsEmoji) {
            this.supportsCjk = supportsCjk;
            this.supportsEmoji = supportsEmoji;
        }

        public boolean isUiSafe() {
            return supportsCjk;
        }
    }

    public record FontOption(String family, FontSupport support) {
    }

    public static List<FontOption> getAvailableFontOptions() {
        return FONT_OPTION_CACHE.getOrLoad(UiFontCatalog::loadAvailableFontOptions);
    }

    public static CompletableFuture<List<FontOption>> getAvailableFontOptionsAsync() {
        return FONT_OPTION_CACHE.getOrLoadAsync(UiFontCatalog::loadAvailableFontOptions);
    }

    public static List<FontOption> getCachedAvailableFontOptions() {
        return FONT_OPTION_CACHE.getCachedOptions();
    }

    private static List<FontOption> loadAvailableFontOptions() {
        return buildFontOptions(
                List.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()),
                UiFontCatalog::inspectFamily
        );
    }

    public static List<String> mergeFamiliesForCombo(String currentFamily, Collection<String> loadedFamilies) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (currentFamily != null && !currentFamily.isBlank()) {
            merged.put(currentFamily.trim().toLowerCase(Locale.ROOT), currentFamily.trim());
        }
        for (String family : normalizeFamilies(loadedFamilies)) {
            merged.putIfAbsent(family.toLowerCase(Locale.ROOT), family);
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)))
                .toList();
    }

    static List<String> normalizeFamilies(Collection<String> familyNames) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (String familyName : familyNames) {
            if (familyName == null) {
                continue;
            }
            String trimmed = familyName.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalized.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }

        return normalized.values().stream()
                .sorted(Comparator.comparing(name -> name.toLowerCase(Locale.ROOT)))
                .toList();
    }

    static List<FontOption> buildFontOptions(Collection<String> familyNames,
                                             Function<String, FontSupport> supportInspector) {
        return normalizeFamilies(familyNames).stream()
                .map(family -> new FontOption(family, supportInspector.apply(family)))
                .toList();
    }

    public static FontSupport inspectFamily(String familyName) {
        if (familyName == null || familyName.isBlank()) {
            return FontSupport.FULL;
        }

        Font font = new Font(familyName, Font.PLAIN, INSPECTION_FONT_SIZE);
        boolean supportsCjk = font.canDisplayUpTo(CJK_SAMPLE) == -1;
        if (!supportsCjk) {
            return FontSupport.NO_CJK;
        }

        boolean supportsEmoji = font.canDisplayUpTo(EMOJI_SAMPLE) == -1;
        return supportsEmoji ? FontSupport.FULL : FontSupport.NO_EMOJI;
    }

    static final class FontOptionCache {
        private volatile List<FontOption> cachedOptions;
        private volatile CompletableFuture<List<FontOption>> loadingFuture;

        synchronized List<FontOption> getOrLoad(Supplier<List<FontOption>> loader) {
            if (cachedOptions == null) {
                cachedOptions = loader.get();
            }
            return cachedOptions;
        }

        synchronized CompletableFuture<List<FontOption>> getOrLoadAsync(Supplier<List<FontOption>> loader) {
            if (cachedOptions != null) {
                return CompletableFuture.completedFuture(cachedOptions);
            }
            if (loadingFuture != null) {
                return loadingFuture;
            }

            loadingFuture = CompletableFuture.supplyAsync(loader, FONT_LOAD_EXECUTOR)
                    .whenComplete((options, throwable) -> {
                        synchronized (this) {
                            if (throwable == null) {
                                cachedOptions = options;
                            }
                            loadingFuture = null;
                        }
                    });
            return loadingFuture;
        }

        synchronized List<FontOption> getCachedOptions() {
            return cachedOptions;
        }
    }
}
