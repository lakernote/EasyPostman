package com.laker.postman.util;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;

@UtilityClass
public class JComponentUtils {

    /**
     * 超出宽度显示省略号（...）。默认icon/padding宽度为30px，最小宽度为200px。
     *
     * @param text      原始文本
     * @param component 用于计算宽度的组件（如JList、JLabel等）
     * @return 适配宽度的文本，超出部分自动省略并加...，否则原文
     */
    public static String ellipsisText(String text, JComponent component) {
        return ellipsisText(text, component, 30, 200);
    }

    /**
     * 超出宽度显示省略号（...），可自定义icon/padding宽度和最小宽度。
     *
     * @param text          原始文本
     * @param component     用于计算宽度的组件（如JList、JLabel等）
     * @param reservedWidth 预留宽度（如icon和padding，单位px）在计算文本最大宽度时，会减去30像素，剩下的空间才用于显示文本
     * @param minWidth      最小宽度（单位px），表示即使组件宽度为0，也假定有200像素可用来显示文本。
     * @return 适配宽度的文本，超出部分自动省略并加...，否则原文
     */
    public static String ellipsisText(String text, JComponent component, int reservedWidth, int minWidth) {
        if (text == null) return null;
        FontMetrics fm = component.getFontMetrics(component.getFont()); // 获取字体度量
        int maxWidth = component.getWidth() > 0 ? component.getWidth() - reservedWidth : minWidth; // 预留icon和padding
        if (maxWidth <= 0) return text;
        String ellipsis = "...";
        int width = fm.stringWidth(text);
        if (width <= maxWidth) return text;
        for (int i = text.length() - 1; i > 0; i--) {
            String sub = text.substring(0, i) + ellipsis;
            if (fm.stringWidth(sub) <= maxWidth) {
                return sub;
            }
        }
        return text;
    }
}