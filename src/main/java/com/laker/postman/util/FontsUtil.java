package com.laker.postman.util;

import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;

/**
 * 字体工具类，提供系统默认字体，保留完整的字体降级链以支持 emoji 等特殊字符
 */
@UtilityClass
public class FontsUtil {

    /**
     * 获取默认字体，从 UIManager 派生以保留降级链，支持 emoji 等特殊字符
     *
     * @param style 字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @param size  字体大小
     * @return Font 对象，从系统默认字体派生
     */
    public static Font getDefaultFont(int style, int size) {
        // 从 UIManager 获取默认字体，使用 deriveFont 派生，保留降级链
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont == null) {
            // 如果 UIManager 中没有，使用系统默认字体
            baseFont = new JLabel().getFont();
        }
        // 使用 deriveFont 派生新字体，保留原字体的所有属性和降级链
        return baseFont.deriveFont(style, (float) size);
    }

    /**
     * 获取默认字体，使用用户设置的字体大小
     * 从 UIManager 派生以保留降级链，支持 emoji 等特殊字符
     *
     * @param style 字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @return Font 对象，使用用户设置的字体大小
     */
    public static Font getDefaultFont(int style) {
        // 使用用户设置的字体大小
        int fontSize = SettingManager.getUiFontSize();
        return getDefaultFont(style, fontSize);
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
        int fontSize = SettingManager.getUiFontSize() + offset;
        // 确保字体大小在合理范围内（最小 8，最大 32）
        fontSize = Math.max(8, Math.min(32, fontSize));
        return getDefaultFont(style, fontSize);
    }
}

