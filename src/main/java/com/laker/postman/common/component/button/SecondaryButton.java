package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 次要按钮 - 现代化设计
 * 完全支持亮色和暗色主题自适应
 * 用于次要操作（如取消、保存等）
 *
 * @author laker
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
        // 文本颜色会根据主题自动适配
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
     * 获取按钮默认背景色 - 主题适配
     * 使用 ModernColors 的卡片背景色
     */
    private Color getDefaultBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    /**
     * 获取按钮悬停背景色 - 主题适配
     * 使用 ModernColors 的悬停背景色
     */
    private Color getHoverBackground() {
        return ModernColors.getHoverBackgroundColor();
    }

    /**
     * 获取按钮按下背景色 - 主题适配
     * 使用 ModernColors 的按钮按下颜色
     */
    private Color getPressedBackground() {
        return ModernColors.getButtonPressedColor();
    }

    /**
     * 获取按钮禁用背景色 - 主题适配
     * 使用 ModernColors 的主背景色（禁用时与背景接近）
     */
    private Color getDisabledBackground() {
        return ModernColors.getBackgroundColor();
    }

    /**
     * 获取边框颜色 - 主题适配
     * 使用 ModernColors 的边框颜色方法
     */
    private Color getBorderColor(boolean enabled) {
        if (!enabled) {
            // 禁用状态：使用浅边框
            return ModernColors.getBorderLightColor();
        }
        // 启用状态：使用中等边框
        return ModernColors.getBorderMediumColor();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 根据状态设置文本颜色
        if (!isEnabled()) {
            // 禁用状态：使用提示色（更暗淡）
            setForeground(ModernColors.getTextHint());
        } else {
            // 启用状态：使用主文本颜色
            setForeground(ModernColors.getTextPrimary());
        }

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
