package com.laker.postman.common.component;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

/**
 * Lightweight response loading overlay with an IDE-style indeterminate spinner.
 */
public class LoadingOverlay extends JComponent {
    private static final int SEGMENT_COUNT = 12;
    private static final int INNER_RADIUS = 7;
    private static final int OUTER_RADIUS = 14;
    private static final float SPINNER_STROKE = 2.2f;
    private static final int ANIMATION_DELAY_MS = 80;

    private final Timer animationTimer;
    private int activeSegment;
    private boolean isVisible = false;

    public LoadingOverlay() {
        setOpaque(false);
        setVisible(false);
        setFocusable(false);
        setEnabled(false);

        animationTimer = new Timer(ANIMATION_DELAY_MS, e -> {
            activeSegment = (activeSegment + 1) % SEGMENT_COUNT;
            repaint();
        });
    }

    /**
     * 显示加载遮罩
     */
    public void showLoading() {
        this.isVisible = true;
        setVisible(true);
        activeSegment = 0;
        animationTimer.start();
        repaint();
    }

    /**
     * 显示加载遮罩。执行状态由 spinner 和取消按钮表达，不再绘制居中文案。
     */
    public void showLoading(String message) {
        showLoading();
    }

    /**
     * 隐藏加载遮罩
     */
    public void hideLoading() {
        this.isVisible = false;
        animationTimer.stop();
        setVisible(false);
    }

    /**
     * 检查是否正在显示
     */
    public boolean isLoading() {
        return isVisible;
    }

    /**
     * 保留旧接口兼容调用方，当前遮罩不展示文案。
     */
    public void setMessage(String message) {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!isVisible) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // 启用抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 绘制半透明背景遮罩 - 主题适配
            Color overlayColor = LoadingOverlayTheme.overlay();
            g2d.setColor(overlayColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            drawSpinner(g2d, centerX, centerY);
        } finally {
            g2d.dispose();
        }
    }

    @Override
    public boolean contains(int x, int y) {
        // loading overlay 只负责视觉提示，不应该抢占底层组件的鼠标命中。
        // 这样即使正在执行请求，用户仍然可以切换 response tab 或查看旧内容。
        return false;
    }

    private void drawSpinner(Graphics2D g2d, int centerX, int centerY) {
        Stroke oldStroke = g2d.getStroke();
        try {
            g2d.setStroke(new BasicStroke(SPINNER_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < SEGMENT_COUNT; i++) {
                int fadeIndex = Math.floorMod(activeSegment - i, SEGMENT_COUNT);
                g2d.setColor(LoadingOverlayTheme.spinnerSegment(fadeIndex, SEGMENT_COUNT));

                double angle = Math.toRadians((360.0 / SEGMENT_COUNT) * i - 90.0);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                g2d.draw(new Line2D.Double(
                        centerX + INNER_RADIUS * cos,
                        centerY + INNER_RADIUS * sin,
                        centerX + OUTER_RADIUS * cos,
                        centerY + OUTER_RADIUS * sin
                ));
            }
        } finally {
            g2d.setStroke(oldStroke);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        if (parent != null) {
            return parent.getSize();
        }
        return super.getPreferredSize();
    }
}
