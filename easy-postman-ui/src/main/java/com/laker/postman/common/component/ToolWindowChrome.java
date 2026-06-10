package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

/**
 * 大块工具窗口的统一外壳，负责 IDEA 风格的外层背景、圆角卡片和 split 间距。
 */
public final class ToolWindowChrome {
    public static final int DEFAULT_SIDE_WIDTH = 310;
    public static final int DIVIDER_SIZE = 4;
    public static final int INNER_DIVIDER_SIZE = 3;
    static final int DRAG_GAP_DIVIDER_SIZE = 5;
    static final String CHROME_BACKGROUND_PROPERTY = "EasyPostman.toolWindowChrome.background";
    static final String CHROME_ROUNDED_PROPERTY = "EasyPostman.toolWindowChrome.rounded";
    static final String CHROME_SPLIT_PROPERTY = "EasyPostman.toolWindowChrome.split";
    private static final int OUTER_VERTICAL_GAP = 4;
    private static final int OUTER_HORIZONTAL_GAP = 6;
    private static final int INNER_GAP = 1;
    private static final int DRAG_GAP_INNER_GAP = 0;
    private static final Insets DEFAULT_CONTENT_INSETS = new Insets(8, 10, 8, 10);

    private ToolWindowChrome() {
    }

    enum SplitDividerStyle {
        /**
         * Stable shared default used by plugins and generic tool windows.
         */
        DEFAULT,

        /**
         * Explicit app opt-in: a 5px background-colored drag gap without line or handle.
         */
        DRAG_GAP
    }

    public static JComponent wrapLeftToolWindow(Component content) {
        return wrapToolWindow(content, new Insets(OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP,
                OUTER_VERTICAL_GAP, INNER_GAP));
    }

    public static JComponent wrapLeftInsetToolWindow(Component content) {
        return wrapLeftToolWindow(createInsetContent(content));
    }

    public static JComponent wrapRightToolWindow(Component content) {
        return wrapToolWindow(content, new Insets(OUTER_VERTICAL_GAP, INNER_GAP,
                OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP));
    }

    public static JComponent wrapTopToolWindow(Component content) {
        return wrapToolWindow(content, new Insets(OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP,
                INNER_GAP, OUTER_HORIZONTAL_GAP));
    }

    public static JComponent wrapBottomToolWindow(Component content) {
        return wrapToolWindow(content, new Insets(INNER_GAP, OUTER_HORIZONTAL_GAP,
                OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP));
    }

    public static JComponent wrapToolWindow(Component content) {
        return wrapToolWindow(content, new Insets(OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP,
                OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP));
    }

    public static JComponent wrapInsetToolWindow(Component content) {
        return wrapToolWindow(createInsetContent(content));
    }

    public static JComponent createInsetContent(Component content) {
        return createInsetContent(content, DEFAULT_CONTENT_INSETS);
    }

    public static JComponent createInsetContent(Component content, Insets contentInsets) {
        JPanel wrapper = new ContentInsetPanel(contentInsets);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    public static JComponent wrapDialogToolWindow(Component content) {
        JPanel wrapper = new BackgroundPanel();
        wrapper.add(wrapToolWindow(content), BorderLayout.CENTER);
        return wrapper;
    }

    public static JComponent wrapDialogInsetToolWindow(Component content) {
        return wrapDialogToolWindow(createInsetContent(content));
    }

    public static JComponent wrapToolWindow(Component content, Insets outerGap) {
        JPanel wrapper = new BackgroundPanel();
        wrapper.setBorder(new EmptyBorder(outerGap));
        wrapper.add(new RoundedToolWindowPanel(content), BorderLayout.CENTER);
        return wrapper;
    }

    public static JSplitPane createHorizontalSplitPane(Component left, Component right, int dividerLocation) {
        return createHorizontalSplitPane(left, right, dividerLocation, SplitDividerStyle.DEFAULT);
    }

    static JSplitPane createHorizontalSplitPane(Component left, Component right, int dividerLocation,
                                                SplitDividerStyle dividerStyle) {
        return createSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right, dividerLocation, dividerStyle);
    }

    public static JSplitPane createVerticalSplitPane(Component top, Component bottom, int dividerLocation) {
        return createVerticalSplitPane(top, bottom, dividerLocation, SplitDividerStyle.DEFAULT);
    }

    static JSplitPane createVerticalSplitPane(Component top, Component bottom, int dividerLocation,
                                              SplitDividerStyle dividerStyle) {
        return createSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom, dividerLocation, dividerStyle);
    }

    public static JSplitPane createHorizontalInnerSplitPane(Component left, Component right, int dividerLocation) {
        return createHorizontalInnerSplitPane(left, right, dividerLocation, SplitDividerStyle.DEFAULT);
    }

    static JSplitPane createHorizontalInnerSplitPane(Component left, Component right, int dividerLocation,
                                                    SplitDividerStyle dividerStyle) {
        return createInnerSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right, dividerLocation, dividerStyle);
    }

    public static JSplitPane createVerticalInnerSplitPane(Component top, Component bottom, int dividerLocation) {
        return createVerticalInnerSplitPane(top, bottom, dividerLocation, SplitDividerStyle.DEFAULT);
    }

    static JSplitPane createVerticalInnerSplitPane(Component top, Component bottom, int dividerLocation,
                                                  SplitDividerStyle dividerStyle) {
        return createInnerSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom, dividerLocation, dividerStyle);
    }

    public static JSplitPane createHorizontalCardSplitPane(Component leftContent, Component rightContent,
                                                          int dividerLocation) {
        return createHorizontalCardSplitPane(leftContent, rightContent, dividerLocation, SplitDividerStyle.DEFAULT);
    }

    static JSplitPane createHorizontalCardSplitPane(Component leftContent, Component rightContent,
                                                   int dividerLocation, SplitDividerStyle dividerStyle) {
        int innerGap = innerGap(dividerStyle);
        return createHorizontalSplitPane(
                wrapLeftInsetToolWindow(leftContent, innerGap),
                wrapRightToolWindow(rightContent, innerGap),
                dividerLocation,
                dividerStyle
        );
    }

    public static JSplitPane createVerticalCardSplitPane(Component topContent, Component bottomContent,
                                                        int dividerLocation) {
        return createVerticalCardSplitPane(topContent, bottomContent, dividerLocation, SplitDividerStyle.DEFAULT);
    }

    static JSplitPane createVerticalCardSplitPane(Component topContent, Component bottomContent,
                                                 int dividerLocation, SplitDividerStyle dividerStyle) {
        int innerGap = innerGap(dividerStyle);
        return createVerticalSplitPane(
                wrapTopToolWindow(topContent, innerGap),
                wrapBottomToolWindow(bottomContent, innerGap),
                dividerLocation,
                dividerStyle
        );
    }

    public static JSplitPane createVerticalStackedCardSplitPane(Component topToolWindow, Component bottomContent,
                                                               int dividerLocation) {
        return createVerticalStackedCardSplitPane(topToolWindow, bottomContent, dividerLocation,
                SplitDividerStyle.DEFAULT);
    }

    static JSplitPane createVerticalStackedCardSplitPane(Component topToolWindow, Component bottomContent,
                                                        int dividerLocation, SplitDividerStyle dividerStyle) {
        return createVerticalSplitPane(
                topToolWindow,
                wrapBottomToolWindow(bottomContent, innerGap(dividerStyle)),
                dividerLocation,
                dividerStyle
        );
    }

    private static JComponent wrapLeftInsetToolWindow(Component content, int innerGap) {
        return wrapToolWindow(createInsetContent(content), new Insets(OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP,
                OUTER_VERTICAL_GAP, innerGap));
    }

    private static JComponent wrapRightToolWindow(Component content, int innerGap) {
        return wrapToolWindow(content, new Insets(OUTER_VERTICAL_GAP, innerGap,
                OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP));
    }

    private static JComponent wrapTopToolWindow(Component content, int innerGap) {
        return wrapToolWindow(content, new Insets(OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP,
                innerGap, OUTER_HORIZONTAL_GAP));
    }

    private static JComponent wrapBottomToolWindow(Component content, int innerGap) {
        return wrapToolWindow(content, new Insets(innerGap, OUTER_HORIZONTAL_GAP,
                OUTER_VERTICAL_GAP, OUTER_HORIZONTAL_GAP));
    }

    private static JSplitPane createSplitPane(int orientation, Component first, Component second, int dividerLocation,
                                              SplitDividerStyle dividerStyle) {
        JSplitPane splitPane = new ToolWindowSplitPane(orientation, first, second, normalizeStyle(dividerStyle));
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(dividerLocation);
        splitPane.setDividerSize(dividerSize(dividerStyle, false));
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        if (divider != null) {
            divider.setBorder(BorderFactory.createEmptyBorder());
        }
        return splitPane;
    }

    private static JSplitPane createInnerSplitPane(int orientation, Component first, Component second,
                                                  int dividerLocation, SplitDividerStyle dividerStyle) {
        // 内部 split 只用于一个圆角卡片内的二级分区：不再套新的圆角卡片。
        JSplitPane splitPane = new ToolWindowInnerSplitPane(orientation, first, second, normalizeStyle(dividerStyle));
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(dividerLocation);
        splitPane.setDividerSize(dividerSize(dividerStyle, true));
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        if (divider != null) {
            divider.setBorder(BorderFactory.createEmptyBorder());
        }
        return splitPane;
    }

    private static SplitDividerStyle normalizeStyle(SplitDividerStyle dividerStyle) {
        return dividerStyle == null ? SplitDividerStyle.DEFAULT : dividerStyle;
    }

    private static int dividerSize(SplitDividerStyle dividerStyle, boolean inner) {
        if (normalizeStyle(dividerStyle) == SplitDividerStyle.DRAG_GAP) {
            return DRAG_GAP_DIVIDER_SIZE;
        }
        return inner ? INNER_DIVIDER_SIZE : DIVIDER_SIZE;
    }

    private static int innerGap(SplitDividerStyle dividerStyle) {
        return normalizeStyle(dividerStyle) == SplitDividerStyle.DRAG_GAP ? DRAG_GAP_INNER_GAP : INNER_GAP;
    }

    private static final class BackgroundPanel extends JPanel {
        private BackgroundPanel() {
            super(new BorderLayout());
            putClientProperty(CHROME_BACKGROUND_PROPERTY, Boolean.TRUE);
            setOpaque(true);
            refreshBackground();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            refreshBackground();
        }

        private void refreshBackground() {
            setBackground(ModernColors.getBackgroundColor());
        }
    }

    private static final class ContentInsetPanel extends JPanel {
        private ContentInsetPanel(Insets contentInsets) {
            super(new BorderLayout());
            setBorder(new EmptyBorder(contentInsets));
            setOpaque(true);
            refreshBackground();
        }

        @Override
        public void updateUI() {
            super.updateUI();
            refreshBackground();
        }

        private void refreshBackground() {
            setBackground(ModernColors.getCardBackgroundColor());
        }
    }

    private static final class ToolWindowSplitPane extends JSplitPane {
        private final SplitDividerStyle dividerStyle;

        private ToolWindowSplitPane(int orientation, Component first, Component second,
                                    SplitDividerStyle dividerStyle) {
            super(orientation, first, second);
            this.dividerStyle = dividerStyle;
            putClientProperty(CHROME_SPLIT_PROPERTY, Boolean.TRUE);
            setOpaque(true);
            updateUI();
            refreshBackground();
        }

        @Override
        public void updateUI() {
            setUI(new BorderlessSplitPaneUi(dividerStyle));
            refreshBackground();
        }

        private void refreshBackground() {
            setBackground(ModernColors.getBackgroundColor());
        }
    }

    private static final class ToolWindowInnerSplitPane extends JSplitPane {
        private final SplitDividerStyle dividerStyle;

        private ToolWindowInnerSplitPane(int orientation, Component first, Component second,
                                         SplitDividerStyle dividerStyle) {
            super(orientation, first, second);
            this.dividerStyle = dividerStyle;
            putClientProperty(CHROME_SPLIT_PROPERTY, Boolean.TRUE);
            setOpaque(true);
            updateUI();
            refreshBackground();
        }

        @Override
        public void updateUI() {
            setUI(new InnerDividerSplitPaneUi(dividerStyle));
            refreshBackground();
        }

        private void refreshBackground() {
            setBackground(ModernColors.getCardBackgroundColor());
        }
    }

    private static final class BorderlessSplitPaneUi extends BasicSplitPaneUI {
        private final SplitDividerStyle dividerStyle;

        private BorderlessSplitPaneUi(SplitDividerStyle dividerStyle) {
            this.dividerStyle = dividerStyle;
        }

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                {
                    setBorder(BorderFactory.createEmptyBorder());
                    setBackground(ModernColors.getBackgroundColor());
                }

                @Override
                public void paint(Graphics g) {
                    if (dividerStyle != SplitDividerStyle.DRAG_GAP) {
                        return;
                    }
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        g2.setColor(ModernColors.getBackgroundColor());
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    } finally {
                        g2.dispose();
                    }
                }
            };
        }
    }

    private static final class InnerDividerSplitPaneUi extends BasicSplitPaneUI {
        private final SplitDividerStyle dividerStyle;

        private InnerDividerSplitPaneUi(SplitDividerStyle dividerStyle) {
            this.dividerStyle = dividerStyle;
        }

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                {
                    setBorder(BorderFactory.createEmptyBorder());
                    setBackground(ModernColors.getCardBackgroundColor());
                }

                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        if (dividerStyle == SplitDividerStyle.DRAG_GAP) {
                            g2.setColor(ModernColors.getBackgroundColor());
                            g2.fillRect(0, 0, getWidth(), getHeight());
                            return;
                        }
                        g2.setColor(ModernColors.getCardBackgroundColor());
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.setColor(ModernColors.getBorderLightColor());
                        if (splitPane != null && splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                            int x = Math.max(0, getWidth() / 2);
                            g2.drawLine(x, 0, x, getHeight());
                        } else {
                            int y = Math.max(0, getHeight() / 2);
                            g2.drawLine(0, y, getWidth(), y);
                        }
                    } finally {
                        g2.dispose();
                    }
                }
            };
        }
    }
}
