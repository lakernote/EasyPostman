package com.laker.postman.util;

import com.laker.postman.common.component.EditorFontProperties;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class EditorFontManager {

    public static final String FALLBACK_FONT_CLIENT_PROPERTY = EditorFontProperties.FALLBACK_FONT_CLIENT_PROPERTY;
    private static final String JETBRAINS_MONO = "JetBrains Mono";
    private static final String PINGFANG_SC = "PingFang SC";
    private static final String MICROSOFT_YAHEI_UI = "Microsoft YaHei UI";
    private static final String MICROSOFT_YAHEI = "Microsoft YaHei";
    private static final String NOTO_SANS_CJK_SC = "Noto Sans CJK SC";
    private static final String NOTO_SANS_CJK = "Noto Sans CJK";
    private static final String NOTO_SANS_SC = "Noto Sans SC";
    private static final String SOURCE_HAN_SANS_SC = "Source Han Sans SC";
    private static final String HIRAGINO_SANS_GB = "Hiragino Sans GB";
    private static final String WENQUANYI_MICRO_HEI = "WenQuanYi Micro Hei";
    private static final List<String> WINDOWS_EDITOR_FONT_CANDIDATES = List.of(
            JETBRAINS_MONO,
            "Cascadia Code",
            "Consolas",
            Font.MONOSPACED
    );
    private static final List<String> MAC_EDITOR_FONT_CANDIDATES = List.of(
            JETBRAINS_MONO,
            "Menlo",
            "Monaco",
            "SF Mono",
            Font.MONOSPACED
    );
    private static final List<String> LINUX_EDITOR_FONT_CANDIDATES = List.of(
            JETBRAINS_MONO,
            "Noto Sans Mono",
            "DejaVu Sans Mono",
            "Ubuntu Mono",
            "Liberation Mono",
            "Droid Sans Mono",
            Font.MONOSPACED
    );
    private static final List<String> GENERIC_EDITOR_FONT_CANDIDATES = List.of(
            JETBRAINS_MONO,
            "Cascadia Code",
            "Consolas",
            "Menlo",
            "Monaco",
            "Noto Sans Mono",
            "DejaVu Sans Mono",
            "Ubuntu Mono",
            Font.MONOSPACED
    );
    private static final List<String> WINDOWS_EDITOR_FALLBACK_FONT_CANDIDATES = List.of(
            MICROSOFT_YAHEI_UI,
            MICROSOFT_YAHEI,
            "SimHei",
            "SimSun",
            NOTO_SANS_CJK_SC,
            NOTO_SANS_CJK,
            NOTO_SANS_SC,
            SOURCE_HAN_SANS_SC,
            WENQUANYI_MICRO_HEI,
            PINGFANG_SC,
            HIRAGINO_SANS_GB,
            Font.SANS_SERIF
    );
    private static final List<String> MAC_EDITOR_FALLBACK_FONT_CANDIDATES = List.of(
            PINGFANG_SC,
            HIRAGINO_SANS_GB,
            NOTO_SANS_CJK_SC,
            NOTO_SANS_CJK,
            NOTO_SANS_SC,
            SOURCE_HAN_SANS_SC,
            MICROSOFT_YAHEI_UI,
            MICROSOFT_YAHEI,
            "SimHei",
            "SimSun",
            WENQUANYI_MICRO_HEI,
            Font.SANS_SERIF
    );
    private static final List<String> LINUX_EDITOR_FALLBACK_FONT_CANDIDATES = List.of(
            NOTO_SANS_CJK_SC,
            NOTO_SANS_CJK,
            NOTO_SANS_SC,
            SOURCE_HAN_SANS_SC,
            WENQUANYI_MICRO_HEI,
            MICROSOFT_YAHEI_UI,
            MICROSOFT_YAHEI,
            "SimHei",
            "SimSun",
            PINGFANG_SC,
            HIRAGINO_SANS_GB,
            Font.SANS_SERIF
    );
    private static volatile List<String> cachedAvailableFontFamilyNames;

    public static Font getConfiguredEditorFont() {
        String fontName = SettingManager.getEditorFontName();
        String family = fontName == null || fontName.isBlank()
                ? getDefaultEditorFontFamily()
                : fontName.trim();
        return new Font(family, Font.PLAIN, SettingManager.getEditorFontSize());
    }

    public static String getDefaultEditorFontFamily() {
        return resolveDefaultEditorFontFamily(availableFontFamilyNames(), System.getProperty("os.name", ""));
    }

    public static Font getConfiguredEditorFallbackFont() {
        String fallbackName = SettingManager.getEditorFontFallbackName();
        String family = fallbackName == null || fallbackName.isBlank()
                ? getDefaultEditorFallbackFontFamily()
                : fallbackName.trim();
        return new Font(family, Font.PLAIN, SettingManager.getEditorFontSize());
    }

    public static String getDefaultEditorFallbackFontFamily() {
        return resolveDefaultEditorFallbackFontFamily(availableFontFamilyNames(), System.getProperty("os.name", ""));
    }

    public static void applyConfiguredEditorFont(RSyntaxTextArea area) {
        if (area == null) {
            return;
        }
        area.setFont(getConfiguredEditorFont());
        Font fallbackFont = getConfiguredEditorFallbackFont();
        if (fallbackFont == null) {
            area.putClientProperty(FALLBACK_FONT_CLIENT_PROPERTY, null);
        } else {
            area.putClientProperty(FALLBACK_FONT_CLIENT_PROPERTY, fallbackFont);
        }
    }

    static String resolveDefaultEditorFontFamily(Collection<String> availableFamilyNames, String osName) {
        return resolveFirstAvailableFont(
                availableFamilyNames,
                resolveEditorFontCandidates(osName),
                Font.MONOSPACED
        );
    }

    private static List<String> availableFontFamilyNames() {
        List<String> cachedFamilies = cachedAvailableFontFamilyNames;
        if (cachedFamilies != null) {
            return cachedFamilies;
        }
        synchronized (EditorFontManager.class) {
            cachedFamilies = cachedAvailableFontFamilyNames;
            if (cachedFamilies == null) {
                cachedFamilies = List.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
                cachedAvailableFontFamilyNames = cachedFamilies;
            }
            return cachedFamilies;
        }
    }

    static String resolveDefaultEditorFallbackFontFamily(Collection<String> availableFamilyNames, String osName) {
        return resolveFirstAvailableFont(
                availableFamilyNames,
                resolveEditorFallbackFontCandidates(osName),
                Font.SANS_SERIF
        );
    }

    private static String resolveFirstAvailableFont(Collection<String> availableFamilyNames,
                                                    List<String> candidates,
                                                    String logicalFallback) {
        Set<String> availableFamilies = availableFamilyNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return candidates.stream()
                .filter(candidate -> logicalFallback.equals(candidate)
                        || availableFamilies.contains(candidate.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(logicalFallback);
    }

    private static List<String> resolveEditorFontCandidates(String osName) {
        return switch (resolveOperatingSystem(osName)) {
            case WINDOWS -> WINDOWS_EDITOR_FONT_CANDIDATES;
            case MACOS -> MAC_EDITOR_FONT_CANDIDATES;
            case LINUX -> LINUX_EDITOR_FONT_CANDIDATES;
            case OTHER -> GENERIC_EDITOR_FONT_CANDIDATES;
        };
    }

    private static List<String> resolveEditorFallbackFontCandidates(String osName) {
        return switch (resolveOperatingSystem(osName)) {
            case WINDOWS -> WINDOWS_EDITOR_FALLBACK_FONT_CANDIDATES;
            case MACOS -> MAC_EDITOR_FALLBACK_FONT_CANDIDATES;
            case LINUX, OTHER -> LINUX_EDITOR_FALLBACK_FONT_CANDIDATES;
        };
    }

    private static OperatingSystemFamily resolveOperatingSystem(String osName) {
        String normalizedOsName = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (normalizedOsName.contains("mac") || normalizedOsName.contains("darwin")) {
            return OperatingSystemFamily.MACOS;
        }
        if (normalizedOsName.startsWith("win")) {
            return OperatingSystemFamily.WINDOWS;
        }
        if (normalizedOsName.contains("linux")) {
            return OperatingSystemFamily.LINUX;
        }
        return OperatingSystemFamily.OTHER;
    }

    private enum OperatingSystemFamily {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }
}
