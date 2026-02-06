package com.laker.postman.common.component.tab;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.util.FontsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 带指示器的Tab头组件
 * 支持在Tab标题后显示状态指示器（绿点），用于标识该Tab是否有内容
 * 类似 Postman 的 标签效果
 */
@Slf4j
public class IndicatorTabComponent extends JPanel {
    private static final int INDICATOR_DIAMETER = 6; // 指示器直径
    private static final int INDICATOR_SPACING = 6; // 指示器与文字之间的间距
    private static final int LABEL_HORIZONTAL_PADDING = 0; // Label 左右内边距
    private static final int LABEL_VERTICAL_PADDING = 4; // Label 上下内边距

    private final String title;
    private boolean showIndicator = false; // 是否显示指示器
    private final Font font;

    /**
     * 创建一个带指示器的Tab组件
     *
     * @param title Tab标题
     */
    public IndicatorTabComponent(String title) {
        this.title = title;
        this.font = FontsUtil.getDefaultFont(Font.PLAIN);
        setOpaque(false);
        // 设置初始大小
        updatePreferredSize();
    }

    /**
     * 设置是否显示指示器
     *
     * @param show true 显示绿点，false 隐藏绿点
     */
    public void setShowIndicator(boolean show) {
        if (this.showIndicator != show) {
            this.showIndicator = show;
            // 重新计算大小，根据绿点显示状态动态调整宽度
            updatePreferredSize();
            repaint();
        }
    }

    /**
     * 更新组件的首选大小
     * 根据是否显示指示器动态计算宽度
     */
    private void updatePreferredSize() {
        FontMetrics fm = getFontMetrics(font);
        int textWidth = fm.stringWidth(title);
        int width = textWidth + LABEL_HORIZONTAL_PADDING * 2;

        // 只有在显示指示器时才增加指示器的宽度
        if (showIndicator) {
            width += INDICATOR_DIAMETER + INDICATOR_SPACING;
        }

        int height = fm.getHeight() + LABEL_VERTICAL_PADDING * 2;
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // 启用抗锯齿
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 设置字体和颜色
            g2.setFont(font);
            g2.setColor(getForeground());

            // 计算文字尺寸
            FontMetrics fm = g2.getFontMetrics();
            int textHeight = fm.getAscent();

            // 组件的总高度
            int totalHeight = getHeight();

            // 文字从左边开始绘制（加上padding）
            int textX = LABEL_HORIZONTAL_PADDING;

            // 文字垂直居中
            int textY = (totalHeight - fm.getHeight()) / 2 + textHeight;

            // 绘制文字
            g2.drawString(title, textX, textY);

            // 如果需要显示指示器
            if (showIndicator) {
                int textWidth = fm.stringWidth(title);
                // 指示器在文字右侧
                int indicatorX = textX + textWidth + INDICATOR_SPACING;
                int indicatorY = (totalHeight - INDICATOR_DIAMETER) / 2;

                // 绘制绿色圆点
                g2.setColor(getIndicatorColor());
                g2.fillOval(indicatorX, indicatorY, INDICATOR_DIAMETER, INDICATOR_DIAMETER);

                // 添加一个微妙的边框使其更明显
                g2.setColor(getIndicatorBorderColor());
                g2.drawOval(indicatorX, indicatorY, INDICATOR_DIAMETER, INDICATOR_DIAMETER);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取主题适配的指示器颜色
     */
    private Color getIndicatorColor() {
        // 绿色在两种主题下都适用，暗色主题使用更亮的绿色
        if (isDarkTheme()) {
            return new Color(76, 175, 80); // Material Green 500
        } else {
            return new Color(56, 142, 60); // Material Green 700
        }
    }

    /**
     * 获取主题适配的指示器边框颜色
     */
    private Color getIndicatorBorderColor() {
        // 边框颜色比填充颜色深一点
        if (isDarkTheme()) {
            return new Color(56, 142, 60, 100);
        } else {
            return new Color(27, 94, 32, 100);
        }
    }

}
