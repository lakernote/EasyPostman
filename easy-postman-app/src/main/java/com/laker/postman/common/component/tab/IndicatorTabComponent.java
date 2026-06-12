package com.laker.postman.common.component.tab;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import com.laker.postman.common.component.tab.state.IndicatorPosition;
import com.laker.postman.common.component.tab.state.TabIndicatorType;
import com.laker.postman.common.component.tab.state.TabState;
import com.laker.postman.util.FontsUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 带指示器的 Tab 头组件
 *
 * <p>支持在文字前或文字后显示状态指示器（圆点 / 数字角标），
 * 行为类似 Postman 的 Tab 标签。
 *
 * <p>布局规则（逻辑顺序）：
 * <pre>
 * | 内边距 | [指示器]? | 间距 | 文字 | 间距 | [指示器]? | 内边距 |
 * </pre>
 *
 * <ul>
 *   <li>指示器尺寸与位置无关，避免 UI 抖动</li>
 *   <li>TAB_SAFE_MARGIN 用于补偿 JTabbedPane 的内部裁剪</li>
 *   <li>所有尺寸单位均为像素</li>
 * </ul>
 */
@Slf4j
public class IndicatorTabComponent extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * 圆点指示器的直径（像素）
     * 适用于 GREEN_DOT / RED_DOT
     */
    private static final int INDICATOR_DIAMETER = 6;

    /**
     * 数字角标的左右内边距总和（像素）
     * 每边 2px，共 4px
     */
    private static final int NUMBER_HORIZONTAL_PADDING = 4;

    /**
     * 指示器与文字之间的水平间距（像素）
     * 对"文字前指示器"和"文字后指示器"均生效
     */
    private static final int INDICATOR_SPACING = 7;

    /**
     * 文字左右内边距（像素）
     * 当前为 0，表示文字紧贴 Tab 边缘
     */
    private static final int LABEL_HORIZONTAL_PADDING = 0;

    /**
     * 文字上下内边距（像素）
     * 决定 Tab 的整体高度
     */
    private static final int LABEL_VERTICAL_PADDING = 4;

    /**
     * 默认安全边距（像素）
     *
     * <p>用于补偿 JTabbedPane 内部裁剪、insets、focus 边框等不可见占用，
     * 防止指示器被“吃掉一半”。
     * 实际使用时会通过 getSafeMargin() 动态计算。
     */
    private static final int DEFAULT_SAFE_MARGIN = 3;

    /**
     * Tab 的状态模型
     *
     * <p>包含：
     * <ul>
     *   <li>标题文本</li>
     *   <li>指示器类型（无 / 绿点 / 红点 / 数字）</li>
     *   <li>指示器位置（文字前 / 文字后）</li>
     *   <li>数字角标值</li>
     * </ul>
     */
    private final TabState state;

    /**
     * 当前 Tab 使用的字体
     *
     * <p>由 FontsUtil 统一管理，保证全局字体一致性
     */
    private final Font font;

    /**
     * 创建一个带指示器的 Tab 组件
     *
     * @param title Tab 标题
     */
    public IndicatorTabComponent(String title) {
        this(new TabState(title));
    }

    /**
     * 创建一个带指示器的 Tab 组件
     *
     * @param state Tab 状态模型
     */
    public IndicatorTabComponent(TabState state) {
        this.state = state;
        this.font = FontsUtil.getDefaultFont(Font.PLAIN);
        setOpaque(false);
        updatePreferredSize();
    }

    /**
     * 设置是否显示指示器（绿点）
     *
     * @param show true 显示绿点，false 隐藏指示器
     */
    public void setShowIndicator(boolean show) {
        if (show) {
            if (state.getIndicatorType() == TabIndicatorType.NONE) {
                state.setIndicator(TabIndicatorType.DEFAULT_DOT);
            }
        } else {
            state.clearIndicator();
        }
        updatePreferredSize();
        repaint();
    }

    /**
     * 设置数字角标
     *
     * @param number 角标数字
     */
    public void setNumber(int number) {
        state.setNumber(number);
        updatePreferredSize();
        repaint();
    }

    /**
     * 设置指示器相对于文字的位置
     *
     * @param position BEFORE_TEXT 或 AFTER_TEXT
     */
    public void setIndicatorPosition(IndicatorPosition position) {
        state.setIndicatorPosition(position);
        updatePreferredSize();
        repaint();
    }

    /**
     * 根据当前状态重新计算组件首选大小
     */
    private void updatePreferredSize() {
        FontMetrics fm = getFontMetrics(font);
        int textWidth = fm.stringWidth(state.getTitle());
        int safeMargin = getSafeMargin();
        int width = textWidth + LABEL_HORIZONTAL_PADDING * 2 + safeMargin * 2;


        if (state.getIndicatorType() != TabIndicatorType.NONE) {
            width += indicatorWidth(fm) + INDICATOR_SPACING;
        }

        int height = fm.getHeight() + LABEL_VERTICAL_PADDING * 2;
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        revalidate();
    }


    /**
     * 返回当前指示器本身的宽度（像素），不含间距
     *
     * @param fm FontMetrics，用于动态计算数字角标宽度；传 null 时使用默认值
     * @return 指示器宽度
     */
    private int indicatorWidth(FontMetrics fm) {
        return switch (state.getIndicatorType()) {
            case DEFAULT_DOT, GREEN_DOT, BLUE_DOT, RED_DOT -> INDICATOR_DIAMETER;
            case NUMBER -> {
                // 动态计算：取显示文本宽度+内边距和字体高度的较大值
                String displayText = getDisplayText(state.getNumber());
                int textWidth = fm.stringWidth(displayText);
                int textHeight = fm.getHeight();
                yield Math.max(textWidth + NUMBER_HORIZONTAL_PADDING, textHeight);
            }
            default -> 0;
        };
    }

    /**
     * 获取数字角标的显示文本
     * 超过99显示".."，否则显示实际数字
     *
     * @param number 原始数字
     * @return 显示文本
     */
    private String getDisplayText(int number) {
        return number > 99 ? "··" : String.valueOf(number);
    }

    /**
     * 动态计算安全边距
     *
     * <p>自动检测父容器（JTabbedPane）的 insets 和边框，
     * 返回最大安全边距值，确保内容不被裁剪。
     *
     * @return 安全边距（像素）
     */
    private int getSafeMargin() {
        int margin = DEFAULT_SAFE_MARGIN;

        // 获取父容器的 insets
        if (getParent() != null) {
            margin = Math.max(margin, Math.max(getParent().getInsets().left, getParent().getInsets().right));
        }

        // 获取组件自身的边框 insets
        if (getBorder() != null) {
            margin = Math.max(margin, Math.max(getBorder().getBorderInsets(this).left, 
                                                getBorder().getBorderInsets(this).right));
        }

        return margin;
    }

    /**
     * 绘制组件
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setFont(font);
            g2.setColor(getForeground());

            FontMetrics fm = g2.getFontMetrics();
            int textHeight = fm.getAscent();
            int totalHeight = getHeight();
            
            // 安全边距，与 updatePreferredSize() 保持一致
            int safeMargin = getSafeMargin();

            int textX;
            int textY = (totalHeight - fm.getHeight()) / 2 + textHeight;

            if (state.getIndicatorType() == TabIndicatorType.NONE) {
                textX = LABEL_HORIZONTAL_PADDING + safeMargin;
                g2.drawString(state.getTitle(), textX, textY);
                return;
            }

            int textWidth = fm.stringWidth(state.getTitle());

            int indicatorWidth = indicatorWidth(fm);
            int indicatorY = (totalHeight - INDICATOR_DIAMETER) / 2;

            if (state.getIndicatorPosition() == IndicatorPosition.AFTER_TEXT) {
                // 文字 → 指示器
                textX = LABEL_HORIZONTAL_PADDING + safeMargin;
                int indicatorX = textX + textWidth + INDICATOR_SPACING;
                g2.drawString(state.getTitle(), textX, textY);
                drawIndicator(g2, indicatorX, indicatorY, state);

            } else {
                // 指示器 → 文字
                int indicatorX = LABEL_HORIZONTAL_PADDING + safeMargin;
                textX = indicatorX + indicatorWidth + INDICATOR_SPACING;
                drawIndicator(g2, indicatorX, indicatorY, state);
                g2.drawString(state.getTitle(), textX, textY);
            }

        } finally {
            g2.dispose();
        }
    }

    /**
     * 绘制当前类型的指示器
     */
    private void drawIndicator(Graphics2D g2, int x, int y, TabState state) {
        switch (state.getIndicatorType()) {
        	case DEFAULT_DOT -> drawDot(g2, x, y,IndicatorTabTheme.indicator());
            case GREEN_DOT -> drawDot(g2, x, y,Color.GREEN);
            case RED_DOT -> drawDot(g2, x, y, Color.RED);
            case BLUE_DOT -> drawDot(g2, x, y, Color.BLUE);
            case NUMBER -> drawNumber(g2, x, state.getNumber());
            default -> throw new IllegalArgumentException("Unexpected value: " + state.getIndicatorType());
        }
    }

    /**
     * 绘制圆点指示器
     */
    private void drawDot(Graphics2D g2, int x, int y, Color color) {
        Color oldColor = g2.getColor();
        g2.setColor(color);
        g2.fillOval(x, y, INDICATOR_DIAMETER, INDICATOR_DIAMETER);
        g2.setColor(IndicatorTabTheme.indicatorBorder());
        g2.drawOval(x, y, INDICATOR_DIAMETER, INDICATOR_DIAMETER);
        g2.setColor(oldColor);
    }

    /**
     * 绘制数字角标（圆形）
     */
    private void drawNumber(Graphics2D g2, int x, int number) {
        String displayText = getDisplayText(number);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(displayText);
        int textHeight = fm.getHeight();
        
        // 圆形直径：与 indicatorWidth() 保持一致
        int diameter = Math.max(textWidth + NUMBER_HORIZONTAL_PADDING, textHeight);
        int radius = diameter / 2;
        
        // 计算垂直居中位置
        int centerY = getHeight() / 2;
        int circleY = centerY - radius;
        
        // 绘制圆形背景
        g2.setColor(IndicatorTabTheme.indicator());
        g2.fillOval(x, circleY, diameter, diameter);
        g2.setColor(IndicatorTabTheme.indicatorBorder());
        g2.drawOval(x, circleY, diameter, diameter);
        
        // 文字居中绘制
        int textX = x + (diameter - textWidth) / 2;
        int textY = circleY + (diameter - fm.getHeight()) / 2 + fm.getAscent();
       
        g2.setColor(Color.WHITE);
        g2.drawString(displayText, textX, textY);
    }
}