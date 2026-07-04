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
    private static final List<String> DEFAULT_EDITOR_FONT_CANDIDATES = List.of(
            "JetBrains Mono",
            "Cascadia Code",
            "Consolas",
            "Menlo",
            "Monaco",
            Font.MONOSPACED
    );
    private static final List<String> WINDOWS_EDITOR_FALLBACK_FONT_CANDIDATES = List.of(
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "SimHei",
            "SimSun",
            "Noto Sans CJK SC",
            "Noto Sans CJK",
            "Noto Sans SC",
            "Source Han Sans SC",
            "WenQuanYi Micro Hei",
            "PingFang SC",
            "Hiragino Sans GB",
            Font.SANS_SERIF
    );
    private static final List<String> MAC_EDITOR_FALLBACK_FONT_CANDIDATES = List.of(
            "PingFang SC",
            "Hiragino Sans GB",
            "Noto Sans CJK SC",
            "Noto Sans CJK",
            "Noto Sans SC",
            "Source Han Sans SC",
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "SimHei",
            "SimSun",
            "WenQuanYi Micro Hei",
            Font.SANS_SERIF
    );
    private static final List<String> LINUX_EDITOR_FALLBACK_FONT_CANDIDATES = List.of(
            "Noto Sans CJK SC",
            "Noto Sans CJK",
            "Noto Sans SC",
            "Source Han Sans SC",
            "WenQuanYi Micro Hei",
            "Microsoft YaHei UI",
            "Microsoft YaHei",
            "SimHei",
            "SimSun",
            "PingFang SC",
            "Hiragino Sans GB",
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
        return resolveDefaultEditorFontFamily();
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

    private static String resolveDefaultEditorFontFamily() {
        Set<String> availableFamilies = availableFontFamilyNames().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return DEFAULT_EDITOR_FONT_CANDIDATES.stream()
                .filter(candidate -> availableFamilies.contains(candidate.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(Font.MONOSPACED);
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

    static String resolveDefaultEditorFallbackFontFamily(Collection<String> availableFamilyNames) {
        return resolveDefaultEditorFallbackFontFamily(availableFamilyNames, System.getProperty("os.name", ""));
    }

    static String resolveDefaultEditorFallbackFontFamily(Collection<String> availableFamilyNames, String osName) {
        Set<String> availableFamilies = availableFamilyNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return resolveEditorFallbackFontCandidates(osName).stream()
                .filter(candidate -> Font.SANS_SERIF.equals(candidate)
                        || availableFamilies.contains(candidate.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(Font.SANS_SERIF);
    }

    private static List<String> resolveEditorFallbackFontCandidates(String osName) {
        String normalizedOsName = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (normalizedOsName.contains("win")) {
            return WINDOWS_EDITOR_FALLBACK_FONT_CANDIDATES;
        }
        if (normalizedOsName.contains("mac") || normalizedOsName.contains("darwin")) {
            return MAC_EDITOR_FALLBACK_FONT_CANDIDATES;
        }
        return LINUX_EDITOR_FALLBACK_FONT_CANDIDATES;
    }
}
