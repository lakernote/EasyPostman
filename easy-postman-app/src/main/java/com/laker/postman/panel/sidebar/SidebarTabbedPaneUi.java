package com.laker.postman.panel.sidebar;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * 侧边栏专用 TabbedPane 绘制策略。
 */
final class SidebarTabbedPaneUi extends BasicTabbedPaneUI {
    private final BooleanSupplier sidebarExpandedSupplier;
    private final IntSupplier expandedTabWidthSupplier;
    private final IntSupplier collapsedTabWidthSupplier;
    private final IntUnaryOperator expandedTabHeightSupplier;
    private final IntSupplier collapsedTabHeightSupplier;

    SidebarTabbedPaneUi(BooleanSupplier sidebarExpandedSupplier,
                        IntSupplier expandedTabWidthSupplier,
                        IntSupplier collapsedTabWidthSupplier,
                        IntUnaryOperator expandedTabHeightSupplier,
                        IntSupplier collapsedTabHeightSupplier) {
        this.sidebarExpandedSupplier = sidebarExpandedSupplier;
        this.expandedTabWidthSupplier = expandedTabWidthSupplier;
        this.collapsedTabWidthSupplier = collapsedTabWidthSupplier;
        this.expandedTabHeightSupplier = expandedTabHeightSupplier;
        this.collapsedTabHeightSupplier = collapsedTabHeightSupplier;
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabAreaInsets = new Insets(8, 6, 8, 0);
        contentBorderInsets = new Insets(0, 1, 0, 0);
        tabInsets = new Insets(2, 2, 2, 2);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        if (!isSelected) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            enableHighQualityRendering(g2);
            paintSelectedTabBackground(g2, x, y, w, h);
            paintSelectedTabIndicator(g2, x, y, h);
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
        // 侧边栏 tab 不绘制边框，避免和左侧选中指示条抢视觉层级。
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                       int tabIndex, Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
        // 侧边栏 tab 的 focus 态由选中背景表达，不额外绘制虚线框。
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        if (tabPlacement == SwingConstants.LEFT && tabPane != null) {
            int height = tabPane.getHeight();
            int tabAreaWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);

            g.setColor(ModernColors.getDividerBorderColor());
            g.drawLine(tabAreaWidth, 0, tabAreaWidth, height);
        }
    }

    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        return sidebarExpandedSupplier.getAsBoolean()
                ? expandedTabWidthSupplier.getAsInt()
                : collapsedTabWidthSupplier.getAsInt();
    }

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        return sidebarExpandedSupplier.getAsBoolean()
                ? expandedTabHeightSupplier.applyAsInt(fontHeight)
                : collapsedTabHeightSupplier.getAsInt();
    }

    @Override
    protected JButton createScrollButton(int direction) {
        return new SidebarScrollButton(direction);
    }

    private void enableHighQualityRendering(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private void paintSelectedTabBackground(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(SidebarTheme.selectedTabBackground());
        g2.fillRect(x, y, w, h);
    }

    private void paintSelectedTabIndicator(Graphics2D g2, int x, int y, int h) {
        int leftMargin = 2;
        int verticalMargin = 6;
        int indicatorWidth = 4;
        int indicatorRadius = 2;

        int indicatorX = x + leftMargin;
        int indicatorY = y + verticalMargin;
        int indicatorHeight = h - verticalMargin * 2;

        g2.setPaint(SidebarTheme.selectedTabIndicatorPaint(indicatorHeight));
        g2.translate(indicatorX, indicatorY);
        g2.fillRoundRect(0, 0, indicatorWidth, indicatorHeight, indicatorRadius, indicatorRadius);
        g2.translate(-indicatorX, -indicatorY);
    }

    private static final class SidebarScrollButton extends BasicArrowButton implements UIResource {
        private final int direction;

        private SidebarScrollButton(int direction) {
            super(direction,
                    UIManager.getColor("TabbedPane.selected"),
                    UIManager.getColor("TabbedPane.shadow"),
                    UIManager.getColor("TabbedPane.darkShadow"),
                    UIManager.getColor("TabbedPane.highlight"));
            this.direction = direction;
            setFocusable(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(0, 0, 0, 0));
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                ButtonModel model = getModel();
                boolean hovered = model.isRollover() && isEnabled();
                boolean pressed = (model.isPressed() || model.isArmed()) && isEnabled();
                boolean enabled = isEnabled();

                int inset = 2;
                int width = Math.max(0, getWidth() - inset * 2);
                int height = Math.max(0, getHeight() - inset * 2);

                if (hovered || pressed) {
                    Color background = pressed ? ModernColors.getButtonPressedColor() : ModernColors.getHoverBackgroundColor();
                    g2.setColor(withAlpha(background, 230));
                    g2.fillRoundRect(inset, inset, width, height, 8, 8);
                }

                Color arrowColor;
                if (!enabled) {
                    arrowColor = ModernColors.getTextDisabled();
                } else if (pressed || hovered) {
                    arrowColor = ModernColors.getPrimary();
                } else {
                    arrowColor = ModernColors.getTextSecondary();
                }

                g2.setColor(arrowColor);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                paintChevron(g2, direction, getWidth(), getHeight());
            } finally {
                g2.dispose();
            }
        }

        private void paintChevron(Graphics2D g2, int direction, int width, int height) {
            int midX = width / 2;
            int midY = height / 2;
            int size = 4;
            Path2D.Float chevron = new Path2D.Float();

            switch (direction) {
                case SwingConstants.NORTH -> {
                    chevron.moveTo(midX - size, midY + 2);
                    chevron.lineTo(midX, midY - size);
                    chevron.lineTo(midX + size, midY + 2);
                }
                case SwingConstants.SOUTH -> {
                    chevron.moveTo(midX - size, midY - 2);
                    chevron.lineTo(midX, midY + size);
                    chevron.lineTo(midX + size, midY - 2);
                }
                case SwingConstants.WEST -> {
                    chevron.moveTo(midX + 2, midY - size);
                    chevron.lineTo(midX - size, midY);
                    chevron.lineTo(midX + 2, midY + size);
                }
                default -> {
                    chevron.moveTo(midX - 2, midY - size);
                    chevron.lineTo(midX + size, midY);
                    chevron.lineTo(midX - 2, midY + size);
                }
            }

            g2.draw(chevron);
        }

        private Color withAlpha(Color color, int alpha) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        }
    }
}
