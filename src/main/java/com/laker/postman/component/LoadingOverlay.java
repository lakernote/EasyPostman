package com.laker.postman.component;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

/**
 * 现代化的加载遮罩组件，显示扁平风格的转圈动画
 * 类似Postman的加载效果，完全支持亮色/暗色主题自适应
 */
public class LoadingOverlay extends JComponent {
    private static final int SPINNER_SIZE = 40;
    private static final int SPINNER_THICKNESS = 4;

    private final Timer animationTimer;
    private int currentAngle = 0;
    private String message;
    private boolean isVisible = false;

    public LoadingOverlay() {
        setOpaque(false);
        setVisible(false);
        this.message = I18nUtil.getMessage(MessageKeys.STATUS_SENDING_REQUEST);

        // 创建动画定时器，每30ms旋转6度，实现平滑动画
        animationTimer = new Timer(30, e -> {
            currentAngle = (currentAngle + 6) % 360;
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
        currentAngle = 0;
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

            // 计算转圈位置（居中）
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            // 绘制转圈动画（使用圆环）
            drawSpinner(g2d, centerX, centerY - 20);

            // 绘制加载文字
            if (message != null && !message.isEmpty()) {
                drawMessage(g2d, centerX, centerY + 35);
            }
        } finally {
            g2d.dispose();
        }
    }

    /**
     * 获取遮罩层颜色 - 主题适配
     * 亮色主题：半透明白色
     * 暗色主题：半透明深灰色
     */
    private Color getOverlayColor() {
        if (ModernColors.isDarkTheme()) {
            // 暗色主题：使用半透明的背景色
            Color bg = ModernColors.getBackgroundColor();
            return new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 230);
        } else {
            // 亮色主题：半透明白色
            return new Color(255, 255, 255, 230);
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
     * 绘制转圈动画
     */
    private void drawSpinner(Graphics2D g2d, int centerX, int centerY) {
        // 创建圆环形状
        int outerRadius = SPINNER_SIZE / 2;
        int innerRadius = outerRadius - SPINNER_THICKNESS;

        // 外圆
        Ellipse2D outer = new Ellipse2D.Double(
            (double) centerX - outerRadius,
            (double) centerY - outerRadius,
            SPINNER_SIZE,
            SPINNER_SIZE
        );

        // 内圆
        Ellipse2D inner = new Ellipse2D.Double(
            (double) centerX - innerRadius,
            (double) centerY - innerRadius,
            (double) innerRadius * 2,
            (double) innerRadius * 2
        );

        // 创建圆环区域
        Area ring = new Area(outer);
        ring.subtract(new Area(inner));

        // 绘制底层圆环 - 主题适配
        g2d.setColor(getSpinnerBackgroundColor());
        g2d.fill(ring);

        // 绘制旋转的弧形（270度，留90度间隙）
        Arc2D.Double arc = new Arc2D.Double(
            (double) centerX - outerRadius,
            (double) centerY - outerRadius,
            SPINNER_SIZE,
            SPINNER_SIZE,
            currentAngle,
            270,
            Arc2D.PIE
        );

        // 创建弧形区域
        Area arcArea = new Area(arc);
        arcArea.intersect(ring);

        // 绘制主色部分
        g2d.setColor(getSpinnerColor());
        g2d.fill(arcArea);
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

