package com.laker.postman.common.component.button;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;

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
            setIcon(IconUtil.createThemed(iconPath, ICON_SIZE, ICON_SIZE));
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
     * 暗色主题：使用比面板背景稍亮的颜色 (68, 71, 73)，提供微妙的层次感
     * 亮色主题：使用白色卡片背景
     */
    private Color getDefaultBackground() {
        if (ModernColors.isDarkTheme()) {
            // 暗色：比面板背景 (60,63,65) 亮 8 个色阶
            return new Color(68, 71, 73);
        }
        // 亮色：白色
        return new Color(255, 255, 255);
    }

    /**
     * 获取按钮悬停背景色 - 主题适配
     * 暗色主题：使用更亮的灰色 (78, 81, 83)，提供明显的交互反馈
     * 亮色主题：使用浅灰悬停色
     */
    private Color getHoverBackground() {
        if (ModernColors.isDarkTheme()) {
            // 暗色：比默认亮 10 个色阶
            return new Color(78, 81, 83);
        }
        // 亮色：浅灰
        return new Color(241, 245, 249);
    }

    /**
     * 获取按钮按下背景色 - 主题适配
     * 暗色主题：使用最亮的状态 (88, 91, 93)，提供清晰的按下反馈
     * 亮色主题：使用深灰按下色
     */
    private Color getPressedBackground() {
        if (ModernColors.isDarkTheme()) {
            // 暗色：最亮的按下状态
            return new Color(88, 91, 93);
        }
        // 亮色：深灰
        return new Color(226, 232, 240);
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
