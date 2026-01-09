package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 次要按钮 - 现代化设计
 * 白色背景（亮色主题）或深色背景（暗色主题），带边框，用于次要操作（如取消、保存等）
 * 支持亮色和暗色主题自适应
 */
public class SecondaryButton extends JButton {
    private static final int ICON_SIZE = 14;

    public SecondaryButton(String text) {
        this(text, null);
    }

    public SecondaryButton(String text, String iconPath) {
        super(text);

        if (iconPath != null && !iconPath.isEmpty()) {
            FlatSVGIcon icon = new FlatSVGIcon(iconPath, ICON_SIZE, ICON_SIZE);
            // 设置图标颜色过滤器以适配主题
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
            setIcon(icon);
            setIconTextGap(4);
        }

        // 设置字体和样式
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        setForeground(ModernColors.getTextPrimary());
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

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取按钮默认背景色 - 主题适配
     */
    private Color getDefaultBackground() {
        if (isDarkTheme()) {
            // 暗色主题：使用深色背景
            return new Color(50, 52, 54);
        } else {
            // 亮色主题：使用白色背景
            return ModernColors.BG_WHITE;
        }
    }

    /**
     * 获取按钮悬停背景色 - 主题适配
     */
    private Color getHoverBackground() {
        if (isDarkTheme()) {
            // 暗色主题：稍微亮一点
            return new Color(60, 63, 65);
        } else {
            // 亮色主题：使用 HOVER_BG
            return ModernColors.HOVER_BG;
        }
    }

    /**
     * 获取按钮按下背景色 - 主题适配
     */
    private Color getPressedBackground() {
        if (isDarkTheme()) {
            // 暗色主题：更亮一点
            return new Color(70, 73, 75);
        } else {
            // 亮色主题：使用 BG_DARK
            return ModernColors.BG_DARK;
        }
    }

    /**
     * 获取按钮禁用背景色 - 主题适配
     */
    private Color getDisabledBackground() {
        if (isDarkTheme()) {
            // 暗色主题：稍暗
            return new Color(45, 47, 49);
        } else {
            // 亮色主题：使用 BG_LIGHT
            return ModernColors.BG_LIGHT;
        }
    }

    /**
     * 获取边框颜色 - 主题适配
     */
    private Color getBorderColor(boolean enabled) {
        if (!enabled) {
            return isDarkTheme() ? new Color(60, 60, 65) : ModernColors.BORDER_LIGHT;
        }
        return isDarkTheme() ? new Color(80, 83, 85) : ModernColors.BORDER_MEDIUM;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 背景颜色（主题适配）
        if (!isEnabled()) {
            g2.setColor(getDisabledBackground());
        } else if (getModel().isPressed()) {
            g2.setColor(getPressedBackground());
        } else if (getModel().isRollover()) {
            g2.setColor(getHoverBackground());
        } else {
            g2.setColor(getDefaultBackground());
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        // 边框（主题适配）
        g2.setColor(getBorderColor(isEnabled()));
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        g2.dispose();

        // 文字和图标
        super.paintComponent(g);
    }
}
