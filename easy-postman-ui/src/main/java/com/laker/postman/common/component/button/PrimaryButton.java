package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 主按钮 - 现代化设计
 * 蓝色背景，白色文字，用于主要操作（如发送、连接等）
 * 支持亮色和暗色主题自适应
 */
public class PrimaryButton extends JButton {
    private static final int ICON_SIZE = 14;
    private static final String BASE_COLOR_PROPERTY = "baseColor";
    private static final String HOVER_COLOR_PROPERTY = "hoverColor";
    private static final String PRESS_COLOR_PROPERTY = "pressColor";
    private static final String COLORS_INITIALIZED_PROPERTY = "colorsInitialized";

    public PrimaryButton(String text) {
        this(text, null);
    }

    public PrimaryButton(String text, String iconPath) {
        super(text);

        if (iconPath != null && !iconPath.isEmpty()) {
            setIcon(IconUtil.createOnPrimary(iconPath, ICON_SIZE, ICON_SIZE));
            setIconTextGap(4);
        }

        // 设置字体和样式
        setFont(FontsUtil.getDefaultFont(Font.BOLD));
        putClientProperty(FlatClientProperties.STYLE_CLASS, ModernButtonFactory.PRIMARY_STYLE_CLASS);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);
        addPropertyChangeListener(event -> {
            String name = event.getPropertyName();
            if (BASE_COLOR_PROPERTY.equals(name)
                    || HOVER_COLOR_PROPERTY.equals(name)
                    || PRESS_COLOR_PROPERTY.equals(name)
                    || COLORS_INITIALIZED_PROPERTY.equals(name)) {
                applyCustomColorStyle();
            }
        });
    }

    private void applyCustomColorStyle() {
        Color baseColor = clientColor(BASE_COLOR_PROPERTY);
        Color hoverColor = clientColor(HOVER_COLOR_PROPERTY);
        Color pressColor = clientColor(PRESS_COLOR_PROPERTY);
        if (baseColor == null && hoverColor == null && pressColor == null) {
            putClientProperty(FlatClientProperties.STYLE, null);
            return;
        }

        Color resolvedBase = baseColor != null ? baseColor : ModernColors.getPrimary();
        Color resolvedHover = hoverColor != null ? hoverColor : ModernColors.getPrimaryLight();
        Color resolvedPress = pressColor != null ? pressColor : ModernColors.getPrimaryDarker();
        putClientProperty(FlatClientProperties.STYLE, String.join("; ",
                "arc: 8",
                "borderWidth: 1",
                "margin: 4,12,4,12",
                "background: " + toStyleColor(resolvedBase),
                "hoverBackground: " + toStyleColor(resolvedHover),
                "pressedBackground: " + toStyleColor(resolvedPress),
                "borderColor: " + toStyleColor(resolvedBase),
                "hoverBorderColor: " + toStyleColor(resolvedHover),
                "pressedBorderColor: " + toStyleColor(resolvedPress),
                "focusedBorderColor: " + toStyleColor(resolvedHover)
        ));
        revalidate();
        repaint();
    }

    private Color clientColor(String propertyName) {
        Object value = getClientProperty(propertyName);
        return value instanceof Color color ? color : null;
    }

    private static String toStyleColor(Color color) {
        if (color.getAlpha() == 255) {
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        }
        return String.format("#%02x%02x%02x%02x",
                color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
    }
}
