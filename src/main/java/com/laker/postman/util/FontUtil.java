package com.laker.postman.util;

import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.Enumeration;

/**
 * 字体工具类，统一根据操作系统选择合适的字体。
 */
public class FontUtil {
    /**
     * 获取适合当前操作系统的默认字体
     *
     * @param style 字体样式 PLAIN 普通 | BOLD 加粗 | ITALIC 倾斜 | BOLD_ITALIC 加粗倾斜
     * @param size  字体大小 数值越大字体越大
     * @return Font
     */
    public static Font getDefaultFont(int style, int size) {
        if (SystemInfo.isMacOS) {
            // 优先 PingFang SC，找不到则降级
            String[] macFonts = {"PingFang SC", "苹方-简", "Hiragino Sans GB", "Heiti SC", "微软雅黑", "SansSerif"};
            Font font = getFont(style, size, macFonts);
            if (font != null) return font;
        } else if (SystemInfo.isWindows) {
            // 优先 Microsoft YaHei UI，找不到则降级
            String[] winFonts = {"微软雅黑", "Microsoft YaHei UI", "宋体", "SansSerif"};
            Font font = getFont(style, size, winFonts);
            if (font != null) return font;
        }
        return new Font("SansSerif", style, size);
    }

    public static void setupGlobalFont(Font font) {
        // 设置全局字体，覆盖所有 UI 组件的默认字体
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) { // 遍历所有 UIManager 键
            Object key = keys.nextElement(); // 获取当前键
            Object value = UIManager.get(key); // 获取当前键对应的值
            if (value instanceof FontUIResource) { // 如果值是 FontUIResource 类型
                UIManager.put(key, new FontUIResource(font)); // 替换为新的 FontUIResource
            }
        }
        UIManager.put("Menu.font", font); // 设置菜单字体
        UIManager.put("MenuItem.font", font); // 设置菜单项字体
        UIManager.put("MenuBar.font", font); // 设置菜单栏字体
        UIManager.put("PopupMenu.font", font); // 设置弹出菜单字体
        UIManager.put("ToolTip.font", font); // 设置工具提示字体
        UIManager.put("Button.font", font); // 设置按钮字体
        UIManager.put("Label.font", font); // 设置标签字体
        UIManager.put("TextField.font", font); // 设置文本字段字体
        UIManager.put("TextArea.font", font); // 设置文本区域字体
        UIManager.put("CheckBoxMenuItem.font", font); // 设置复选框菜单项字体
        UIManager.put("RadioButtonMenuItem.font", font); // 设置单选按钮菜单项字体
        UIManager.put("OptionPane.font", font); // 设置选项面板字体
    }

    /**
     * 获取指定样式和大小的字体，优先使用给定的字体列表中的字体。
     *
     * @param style    字体样式
     * @param size     字体大小
     * @param macFonts macOS 字体列表
     * @return Font
     */
    private static Font getFont(int style, int size, String[] macFonts) {
        for (String fontName : macFonts) {
            Font font = new Font(fontName, style, size);
            // 检查字体是否可用
            if (font.getFamily().equals(fontName) || font.canDisplay('测')) {
                return font;
            }
        }
        return null;
    }
}