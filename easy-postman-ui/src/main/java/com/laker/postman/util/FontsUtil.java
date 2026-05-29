package com.laker.postman.util;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * 字体工具类，提供系统默认字体，保留完整的字体降级链以支持 emoji 等特殊字符
 */
@UtilityClass
public class FontsUtil {

    private static final int MIN_FONT_SIZE = 8;
    private static final int MAX_FONT_SIZE = 32;
    private static final int DEFAULT_FONT_SIZE = 13;
    private static final int MIN_CONFIGURED_FONT_SIZE = 10;
    private static final int MAX_CONFIGURED_FONT_SIZE = 24;
    private static final String UI_FONT_SIZE_KEY = "ui_font_size";

    private static int getBaseFontSize() {
        Integer configured = readConfiguredFontSize();
        if (configured != null) {
            return configured;
        }
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont == null) {
            baseFont = new JLabel().getFont();
        }
        return baseFont == null ? DEFAULT_FONT_SIZE : baseFont.getSize();
    }

    private static Integer readConfiguredFontSize() {
        File settingsFile = new File(com.laker.postman.common.constants.ConfigPathConstants.EASY_POSTMAN_SETTINGS);
        if (!settingsFile.exists()) {
            return null;
        }
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(settingsFile)) {
            properties.load(inputStream);
            String configured = properties.getProperty(UI_FONT_SIZE_KEY);
            if (configured == null || configured.isBlank()) {
                return null;
            }
            int parsed = Integer.parseInt(configured.trim());
            return Math.max(MIN_CONFIGURED_FONT_SIZE, Math.min(MAX_CONFIGURED_FONT_SIZE, parsed));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取默认字体，从 UIManager 派生以保留降级链，支持 emoji 等特殊字符
     *
     * @param style 字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param size  字体大小
     * @return Font 对象，从系统默认字体派生
     */
    private static Font getDefaultFont(int style, int size) {
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont == null) {
            baseFont = new JLabel().getFont();
        }
        return baseFont.deriveFont(style, size);
    }

    /**
     * 获取默认字体，使用用户设置的字体大小
     * 从 UIManager 派生以保留降级链，支持 emoji 等特殊字符
     *
     * @param style 字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @return Font 对象，使用用户设置的字体大小
     */
    public static Font getDefaultFont(int style) {
        return getDefaultFont(style, getBaseFontSize());
    }

    /**
     * 获取默认字体，使用相对于用户设置字体大小的偏移
     * 例如：如果用户设置字体为 14，offset 为 -2，则返回 12 号字体
     *
     * @param style  字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param offset 相对于用户设置字体大小的偏移量（可正可负）
     * @return Font 对象
     */
    public static Font getDefaultFontWithOffset(int style, int offset) {
        int fontSize = getBaseFontSize() + offset;
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize));
        return getDefaultFont(style, fontSize);
    }
}
