package com.laker.postman.util;

import javax.swing.*;
import java.awt.*;

/**
 * 字体工具类，提供系统默认字体，保留完整的字体降级链以支持 emoji 等特殊字符
 */
public class EasyPostManFontUtil {
    // 私有构造函数，防止实例化工具类
    private EasyPostManFontUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

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
}