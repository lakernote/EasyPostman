
package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 主按钮 - 现代化设计
 * 蓝色背景，白色文字，用于主要操作（如发送、连接等）
 */
public class PrimaryButton extends JButton {
    private static final int ICON_SIZE = 14;

    // 缓存颜色，避免每次 paintComponent 都查询 ClientProperty
    private Color cachedBaseColor = ModernColors.PRIMARY;
    private Color cachedHoverColor = ModernColors.PRIMARY_DARK;
    private Color cachedPressColor = ModernColors.PRIMARY_DARKER;
    private boolean colorsInitialized = false;

    public PrimaryButton(String text) {
        this(text, null);
    }

    public PrimaryButton(String text, String iconPath) {
        super(text);

        if (iconPath != null && !iconPath.isEmpty()) {
            setIcon(new FlatSVGIcon(iconPath, ICON_SIZE, ICON_SIZE));
            setIconTextGap(4);
        }

        // 设置字体和样式
        setFont(FontsUtil.getDefaultFont(Font.BOLD));
        setForeground(ModernColors.TEXT_INVERSE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(6, 12, 6, 12));

        // 悬停动画
        getModel().addChangeListener(e -> {
            if (isEnabled()) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 检查是否需要重新读取颜色
        Boolean shouldReload = (Boolean) getClientProperty("colorsInitialized");
        if (shouldReload != null && !shouldReload) {
            colorsInitialized = false;
        }

        // 只在第一次或颜色变更时读取 ClientProperty
        if (!colorsInitialized) {
            Color baseColor = (Color) getClientProperty("baseColor");
            Color hoverColor = (Color) getClientProperty("hoverColor");
            Color pressColor = (Color) getClientProperty("pressColor");

            if (baseColor != null) cachedBaseColor = baseColor;
            if (hoverColor != null) cachedHoverColor = hoverColor;
            if (pressColor != null) cachedPressColor = pressColor;
            colorsInitialized = true;
        }

        // 背景颜色
        if (!isEnabled()) {
            g2.setColor(ModernColors.TEXT_DISABLED);
        } else if (getModel().isPressed()) {
            g2.setColor(cachedPressColor);
        } else if (getModel().isRollover()) {
            g2.setColor(cachedHoverColor);
        } else {
            g2.setColor(cachedBaseColor);
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.dispose();

        // 文字和图标
        super.paintComponent(g);
    }
}

