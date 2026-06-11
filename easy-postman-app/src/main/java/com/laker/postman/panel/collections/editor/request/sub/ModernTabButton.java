package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代化的Tab按钮组件
 * 支持：
 * 1. 底部高亮指示器
 * 2. 悬停效果
 * 3. 平滑动画
 * 4. 主题自适应（使用TabbedPane主题颜色）
 */
public class ModernTabButton extends JButton {

    // 常量定义
    private static final int INDICATOR_HEIGHT = 3;
    private static final int HOVER_ANIMATION_DELAY = 10;
    private static final float HOVER_ALPHA_STEP = 0.1f;

    private final int tabIndex;
    private int selectedTabIndex = -1;

    // 悬停效果
    private float hoverAlpha = 0.0f;
    private Timer hoverAnimationTimer;

    public ModernTabButton(String text, int tabIndex) {
        super(text);
        this.tabIndex = tabIndex;
        initializeStyle();
        setupHoverEffect();
    }

    /**
     * 初始化按钮样式
     */
    private void initializeStyle() {
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        setForeground(ModernTabButtonTheme.foreground(true));

        // 设置内边距
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 16));

        // 设置光标
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * 设置悬停效果
     */
    private void setupHoverEffect() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    startHoverAnimation(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                startHoverAnimation(false);
            }
        });
    }

    /**
     * 启动悬停动画
     *
     * @param fadeIn true为淡入，false为淡出
     */
    private void startHoverAnimation(boolean fadeIn) {
        if (hoverAnimationTimer != null && hoverAnimationTimer.isRunning()) {
            hoverAnimationTimer.stop();
        }

        hoverAnimationTimer = new Timer(HOVER_ANIMATION_DELAY, e -> {
            if (fadeIn) {
                hoverAlpha = Math.min(1.0f, hoverAlpha + HOVER_ALPHA_STEP);
                if (hoverAlpha >= 1.0f) {
                    hoverAnimationTimer.stop();
                }
            } else {
                hoverAlpha = Math.max(0.0f, hoverAlpha - HOVER_ALPHA_STEP);
                if (hoverAlpha <= 0.0f) {
                    hoverAnimationTimer.stop();
                }
            }
            repaint();
        });
        hoverAnimationTimer.start();
    }

    /**
     * 更新选中的tab索引
     */
    public void updateSelectedIndex(int selectedIndex) {
        this.selectedTabIndex = selectedIndex;
        repaint();
    }

    /**
     * 判断是否被选中（匹配 JToggleButton 接口）
     */
    @Override
    public boolean isSelected() {
        return selectedTabIndex == tabIndex;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 绘制悬停背景
        if (hoverAlpha > 0 && !isSelected()) {
            Color hoverBg = ModernTabButtonTheme.hoverBackground(hoverAlpha);
            g2d.setColor(hoverBg);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight() - INDICATOR_HEIGHT, 4, 4);
        }

        // 绘制选中背景
        if (isSelected()) {
            Color selectedBg = ModernTabButtonTheme.selectedBackground();
            g2d.setColor(selectedBg);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight() - INDICATOR_HEIGHT, 4, 4);
        }

        g2d.dispose();

        // 绘制文本
        super.paintComponent(g);

        // 绘制底部指示器（使用TabbedPane主题颜色）
        if (isSelected()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color indicatorColor = ModernTabButtonTheme.indicator(isEnabled());
            g2.setColor(indicatorColor);
            g2.fillRoundRect(4, getHeight() - INDICATOR_HEIGHT,
                    getWidth() - 8, INDICATOR_HEIGHT, 2, 2);
            g2.dispose();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            hoverAlpha = 0.0f;
            if (hoverAnimationTimer != null) {
                hoverAnimationTimer.stop();
            }
        }
        setForeground(ModernTabButtonTheme.foreground(enabled));
    }

    /**
     * 清理资源，防止内存泄漏
     * 当按钮从容器中移除时应该调用此方法
     */
    public void cleanup() {
        if (hoverAnimationTimer != null && hoverAnimationTimer.isRunning()) {
            hoverAnimationTimer.stop();
        }
        hoverAnimationTimer = null;
    }

    @Override
    public void removeNotify() {
        // 组件从容器中移除时自动清理资源
        cleanup();
        super.removeNotify();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // 确保有足够的高度
        size.height = Math.max(size.height, 36);
        return size;
    }
}
