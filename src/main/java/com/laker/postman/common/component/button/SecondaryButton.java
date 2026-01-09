package com.laker.postman.common.component.button;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 次要按钮 - 现代化设计
 * 白色背景，带边框，用于次要操作（如取消、保存等）
 */
public class SecondaryButton extends JButton {
    private static final int ICON_SIZE = 14;

    public SecondaryButton(String text) {
        this(text, null);
    }

    public SecondaryButton(String text, String iconPath) {
        super(text);

        if (iconPath != null && !iconPath.isEmpty()) {
            setIcon(new FlatSVGIcon(iconPath, ICON_SIZE, ICON_SIZE));
            setIconTextGap(4);
        }

        // 设置字体和样式
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        setForeground(ModernColors.TEXT_PRIMARY);
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

        // 背景颜色
        if (!isEnabled()) {
            g2.setColor(ModernColors.BG_LIGHT);
        } else if (getModel().isPressed()) {
            g2.setColor(ModernColors.BG_DARK);
        } else if (getModel().isRollover()) {
            g2.setColor(ModernColors.HOVER_BG);
        } else {
            g2.setColor(ModernColors.BG_WHITE);
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        // 边框
        g2.setColor(isEnabled() ? ModernColors.BORDER_MEDIUM : ModernColors.BORDER_LIGHT);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        g2.dispose();

        // 文字和图标
        super.paintComponent(g);
    }
}

