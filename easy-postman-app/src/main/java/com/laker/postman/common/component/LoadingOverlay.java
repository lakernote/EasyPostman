package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * 现代化的加载遮罩组件，显示科技感的多层动画效果
 * 包含：多层旋转圆环、脉冲效果、粒子动画
 * 完全支持亮色/暗色主题自适应
 */
public class LoadingOverlay extends JComponent {
    // 外层圆环
    private static final int OUTER_RING_SIZE = 60;
    private static final int OUTER_RING_THICKNESS = 3;

    // 中层圆环
    private static final int MIDDLE_RING_SIZE = 44;
    private static final int MIDDLE_RING_THICKNESS = 2;

    // 内层圆环
    private static final int INNER_RING_SIZE = 32;
    private static final int INNER_RING_THICKNESS = 2;

    // 中心脉冲
    private static final int PULSE_SIZE = 12;

    // 粒子数量
    private static final int PARTICLE_COUNT = 8;
    private static final int PARTICLE_SIZE = 4;

    private final Timer animationTimer;
    private int outerAngle = 0;      // 外层角度
    private int middleAngle = 0;     // 中层角度
    private int innerAngle = 0;      // 内层角度
    private float pulsePhase = 0;    // 脉冲相位
    private String message;
    private boolean isVisible = false;

    public LoadingOverlay() {
        setOpaque(false);
        setVisible(false);
        this.message = I18nUtil.getMessage(MessageKeys.STATUS_SENDING_REQUEST);

        // 创建动画定时器，每25ms更新一次，实现流畅的多层动画
        animationTimer = new Timer(25, e -> {
            // 外层：顺时针慢速旋转
            outerAngle = (outerAngle + 3) % 360;

            // 中层：逆时针中速旋转
            middleAngle = (middleAngle - 5) % 360;

            // 内层：顺时针快速旋转
            innerAngle = (innerAngle + 8) % 360;

            // 脉冲：连续循环
            pulsePhase = (pulsePhase + 0.08f) % (2 * (float) Math.PI);

            repaint();
        });
    }

    /**
     * 显示加载遮罩
     */
    public void showLoading() {
        showLoading(I18nUtil.getMessage(MessageKeys.STATUS_SENDING_REQUEST));
    }

    /**
     * 显示加载遮罩并自定义消息
     */
    public void showLoading(String message) {
        this.message = message;
        this.isVisible = true;
        setVisible(true);

        // 重置所有动画状态
        outerAngle = 0;
        middleAngle = 0;
        innerAngle = 0;
        pulsePhase = 0;

        animationTimer.start();
        repaint();
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
     * 更新加载消息
     */
    public void setMessage(String message) {
        this.message = message;
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
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 绘制半透明背景遮罩 - 主题适配
            Color overlayColor = getOverlayColor();
            g2d.setColor(overlayColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 计算中心位置
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            // 绘制多层科技感动画
            drawTechSpinner(g2d, centerX, centerY - 20);

            // 绘制加载文字
            if (message != null && !message.isEmpty()) {
                drawMessage(g2d, centerX, centerY + 50);
            }
        } finally {
            g2d.dispose();
        }
    }

    /**
     * 获取遮罩层颜色 - 主题适配
     * 亮色主题：#f5f7fa
     * 暗色主题：半透明深灰色
     */
    private Color getOverlayColor() {
        if (ModernColors.isDarkTheme()) {
            // 暗色主题：使用半透明的背景色
            Color bg = ModernColors.getBackgroundColor();
            return new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 230);
        } else {
            // 亮色主题：#f5f7fa (245, 247, 250) 带透明度
            return new Color(245, 247, 250, 230);
        }
    }

    /**
     * 获取转圈主色 - 主题适配
     */
    private Color getSpinnerColor() {
        return ModernColors.PRIMARY;
    }

    /**
     * 获取转圈背景色 - 主题适配
     */
    private Color getSpinnerBackgroundColor() {
        if (ModernColors.isDarkTheme()) {
            // 暗色主题：稍亮的灰色
            return new Color(100, 100, 100);
        } else {
            // 亮色主题：浅灰色
            return new Color(220, 220, 220);
        }
    }

    /**
     * 绘制科技感的多层转圈动画
     */
    private void drawTechSpinner(Graphics2D g2d, int centerX, int centerY) {
        // 1. 绘制外层圆环（慢速顺时针）
        drawRing(g2d, centerX, centerY, OUTER_RING_SIZE, OUTER_RING_THICKNESS,
                outerAngle, 240, 0.4f);

        // 2. 绘制粒子效果（围绕外层圆环）
        drawParticles(g2d, centerX, centerY, OUTER_RING_SIZE / 2 + 6);

        // 3. 绘制中层圆环（中速逆时针）
        drawRing(g2d, centerX, centerY, MIDDLE_RING_SIZE, MIDDLE_RING_THICKNESS,
                middleAngle, 200, 0.6f);

        // 4. 绘制内层圆环（快速顺时针）
        drawRing(g2d, centerX, centerY, INNER_RING_SIZE, INNER_RING_THICKNESS,
                innerAngle, 180, 0.8f);

        // 5. 绘制中心脉冲效果
        drawPulse(g2d, centerX, centerY);
    }

    /**
     * 绘制单个圆环
     */
    private void drawRing(Graphics2D g2d, int centerX, int centerY,
                          int size, int thickness, int angle, int arcLength, float alpha) {
        int radius = size / 2;

        // 绘制背景圆环（淡色）
        Color bgColor = getSpinnerBackgroundColor();
        g2d.setColor(new Color(bgColor.getRed(), bgColor.getGreen(),
                bgColor.getBlue(), (int) (50 * alpha)));
        g2d.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawOval(centerX - radius, centerY - radius, size, size);

        // 绘制主色弧形（带渐变效果）
        Color primaryColor = getSpinnerColor();

        // 起始颜色（主色）
        Color startColor = new Color(
                primaryColor.getRed(),
                primaryColor.getGreen(),
                primaryColor.getBlue(),
                (int) (255 * alpha)
        );

        // 结束颜色（透明）
        Color endColor = new Color(
                primaryColor.getRed(),
                primaryColor.getGreen(),
                primaryColor.getBlue(),
                0
        );

        // 计算渐变的起点和终点
        double startRad = Math.toRadians(angle);
        double endRad = Math.toRadians(angle + arcLength);

        int x1 = (int) (centerX + radius * Math.cos(startRad));
        int y1 = (int) (centerY + radius * Math.sin(startRad));
        int x2 = (int) (centerX + radius * Math.cos(endRad));
        int y2 = (int) (centerY + radius * Math.sin(endRad));

        GradientPaint gradient = new GradientPaint(x1, y1, startColor, x2, y2, endColor);
        g2d.setPaint(gradient);

        Arc2D arc = new Arc2D.Double(
                centerX - radius, centerY - radius,
                size, size,
                angle, arcLength,
                Arc2D.OPEN
        );

        g2d.draw(arc);
    }

    /**
     * 绘制粒子效果
     */
    private void drawParticles(Graphics2D g2d, int centerX, int centerY, int orbitRadius) {
        Color primaryColor = getSpinnerColor();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            // 计算粒子位置（均匀分布在圆周上）
            double angle = Math.toRadians((360.0 / PARTICLE_COUNT) * i + outerAngle);
            int x = (int) (centerX + orbitRadius * Math.cos(angle));
            int y = (int) (centerY + orbitRadius * Math.sin(angle));

            // 粒子的透明度随位置变化（产生拖尾效果）
            float alpha = 0.3f + 0.7f * (float) Math.sin(angle + pulsePhase);
            alpha = Math.max(0.2f, Math.min(1.0f, alpha));

            Color particleColor = new Color(
                    primaryColor.getRed(),
                    primaryColor.getGreen(),
                    primaryColor.getBlue(),
                    (int) (255 * alpha)
            );

            g2d.setColor(particleColor);
            g2d.fillOval(x - PARTICLE_SIZE / 2, y - PARTICLE_SIZE / 2,
                    PARTICLE_SIZE, PARTICLE_SIZE);
        }
    }

    /**
     * 绘制中心脉冲效果
     */
    private void drawPulse(Graphics2D g2d, int centerX, int centerY) {
        Color primaryColor = getSpinnerColor();

        // 脉冲大小随相位变化
        float scale = 0.7f + 0.3f * (float) Math.sin(pulsePhase);
        int currentSize = (int) (PULSE_SIZE * scale);

        // 透明度也随相位变化
        float alpha = 0.4f + 0.4f * (float) Math.sin(pulsePhase);

        // 绘制外圈光晕
        int glowSize = (int) (currentSize * 1.8f);
        Color glowColor = new Color(
                primaryColor.getRed(),
                primaryColor.getGreen(),
                primaryColor.getBlue(),
                (int) (100 * alpha)
        );

        RadialGradientPaint glowPaint = new RadialGradientPaint(
                centerX, centerY,
                glowSize / 2.0f,
                new float[]{0.0f, 1.0f},
                new Color[]{glowColor, new Color(primaryColor.getRed(), primaryColor.getGreen(),
                        primaryColor.getBlue(), 0)}
        );

        g2d.setPaint(glowPaint);
        g2d.fillOval(centerX - glowSize / 2, centerY - glowSize / 2, glowSize, glowSize);

        // 绘制实心圆
        Color solidColor = new Color(
                primaryColor.getRed(),
                primaryColor.getGreen(),
                primaryColor.getBlue(),
                (int) (255 * (0.8f + 0.2f * alpha))
        );

        g2d.setColor(solidColor);
        g2d.fillOval(centerX - currentSize / 2, centerY - currentSize / 2,
                currentSize, currentSize);
    }

    /**
     * 绘制加载消息
     */
    private void drawMessage(Graphics2D g2d, int centerX, int centerY) {
        // 使用主题适配的文本颜色
        g2d.setColor(ModernColors.getTextSecondary());

        // 使用 FontsUtil 获取字体
        g2d.setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        FontMetrics fm = g2d.getFontMetrics();
        int messageWidth = fm.stringWidth(message);

        g2d.drawString(message, centerX - messageWidth / 2, centerY);
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

