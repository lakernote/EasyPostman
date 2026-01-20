package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 主按钮 - 现代化设计
 * 蓝色背景，白色文字，用于主要操作（如发送、连接等）
 * 支持亮色和暗色主题自适应
 * 添加按下动画效果，提供丝滑的交互体验
 */
public class PrimaryButton extends JButton {
    private static final int ICON_SIZE = 14;

    // 缓存颜色，避免每次 paintComponent 都查询 ClientProperty
    private Color cachedBaseColor = ModernColors.PRIMARY;
    private Color cachedHoverColor = ModernColors.PRIMARY_DARK;
    private Color cachedPressColor = ModernColors.PRIMARY_DARKER;
    private boolean colorsInitialized = false;

    // 按下动画状态
    private float pressScale = 1.0f;
    private Timer pressAnimationTimer;

    public PrimaryButton(String text) {
        this(text, null);
    }

    public PrimaryButton(String text, String iconPath) {
        super(text);

        if (iconPath != null && !iconPath.isEmpty()) {
            FlatSVGIcon icon = new FlatSVGIcon(iconPath, ICON_SIZE, ICON_SIZE);
            // 设置图标颜色过滤器为白色，与按钮文字颜色一致
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> Color.WHITE));
            setIcon(icon);
            setIconTextGap(4);
        }

        // 设置字体和样式
        setFont(FontsUtil.getDefaultFont(Font.BOLD));
        // 文字颜色始终为白色（在蓝色背景上）
        setForeground(Color.WHITE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(6, 12, 6, 12));

        // 添加鼠标监听器实现按下动画
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    animatePress(true);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    animatePress(false);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) {
                    animatePress(false);
                }
            }
        });

        // 悬停动画
        getModel().addChangeListener(e -> {
            if (isEnabled()) {
                repaint();
            }
        });
    }

    /**
     * 按下动画 - 轻微缩放效果
     */
    private void animatePress(boolean pressed) {
        if (pressAnimationTimer != null && pressAnimationTimer.isRunning()) {
            pressAnimationTimer.stop();
        }

        float targetScale = pressed ? 0.96f : 1.0f;
        float step = pressed ? -0.02f : 0.02f;

        pressAnimationTimer = new Timer(10, e -> {
            if ((step > 0 && pressScale >= targetScale) || (step < 0 && pressScale <= targetScale)) {
                pressScale = targetScale;
                ((Timer) e.getSource()).stop();
            } else {
                pressScale += step;
            }
            repaint();
        });
        pressAnimationTimer.start();
    }

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取禁用状态的背景色 - 主题适配
     */
    private Color getDisabledBackground() {
        if (isDarkTheme()) {
            // 暗色主题：使用深灰色
            return new Color(60, 60, 65);
        } else {
            // 亮色主题：使用 TEXT_DISABLED
            return ModernColors.getTextDisabled();
        }
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

        // 应用按下缩放效果
        if (pressScale != 1.0f) {
            int w = getWidth();
            int h = getHeight();
            int offsetX = (int) ((w - w * pressScale) / 2);
            int offsetY = (int) ((h - h * pressScale) / 2);
            g2.translate(offsetX, offsetY);
            g2.scale(pressScale, pressScale);
        }

        // 背景颜色（主题适配禁用状态）
        if (!isEnabled()) {
            g2.setColor(getDisabledBackground());
        } else if (getModel().isPressed()) {
            g2.setColor(cachedPressColor);
        } else if (getModel().isRollover()) {
            g2.setColor(cachedHoverColor);
        } else {
            g2.setColor(cachedBaseColor);
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        // 文字和图标
        super.paintComponent(g2);

        g2.dispose();
    }
}

