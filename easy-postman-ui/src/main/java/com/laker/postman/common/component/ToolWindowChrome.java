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
    public static final int DIVIDER_SIZE = 5;
    public static final int INNER_DIVIDER_SIZE = 5;
    static final String CHROME_BACKGROUND_PROPERTY = "EasyPostman.toolWindowChrome.background";
    static final String CHROME_ROUNDED_PROPERTY = "EasyPostman.toolWindowChrome.rounded";
    static final String CHROME_SPLIT_PROPERTY = "EasyPostman.toolWindowChrome.split";
    private static final int OUTER_VERTICAL_GAP = 4;
    private static final int OUTER_HORIZONTAL_GAP = 6;
    private static final int INNER_GAP = 0;
    private static final Insets DEFAULT_CONTENT_INSETS = new Insets(8, 10, 8, 10);

    private ToolWindowChrome() {
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
        return createSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right, dividerLocation);
    }

    public static JSplitPane createVerticalSplitPane(Component top, Component bottom, int dividerLocation) {
        return createSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom, dividerLocation);
    }

    public static JSplitPane createHorizontalInnerSplitPane(Component left, Component right, int dividerLocation) {
        return createInnerSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right, dividerLocation);
    }

    public static JSplitPane createVerticalInnerSplitPane(Component top, Component bottom, int dividerLocation) {
        return createInnerSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom, dividerLocation);
    }

    public static JSplitPane createHorizontalCardSplitPane(Component leftContent, Component rightContent,
                                                          int dividerLocation) {
        return createHorizontalSplitPane(
                wrapLeftInsetToolWindow(leftContent),
                wrapRightToolWindow(rightContent),
                dividerLocation
        );
    }

    public static JSplitPane createVerticalCardSplitPane(Component topContent, Component bottomContent,
                                                        int dividerLocation) {
        return createVerticalSplitPane(
                wrapTopToolWindow(topContent),
                wrapBottomToolWindow(bottomContent),
                dividerLocation
        );
    }

    public static JSplitPane createVerticalStackedCardSplitPane(Component topToolWindow, Component bottomContent,
                                                               int dividerLocation) {
        return createVerticalSplitPane(
                topToolWindow,
                wrapBottomToolWindow(bottomContent),
                dividerLocation
        );
    }

    private static JSplitPane createSplitPane(int orientation, Component first, Component second, int dividerLocation) {
        JSplitPane splitPane = new ToolWindowSplitPane(orientation, first, second);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(dividerLocation);
        splitPane.setDividerSize(DIVIDER_SIZE);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        if (divider != null) {
            divider.setBorder(BorderFactory.createEmptyBorder());
        }
        return splitPane;
    }

    private static JSplitPane createInnerSplitPane(int orientation, Component first, Component second,
                                                  int dividerLocation) {
        // 内部 split 只用于一个圆角卡片内的二级分区：不再套新的圆角卡片。
        JSplitPane splitPane = new ToolWindowInnerSplitPane(orientation, first, second);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(dividerLocation);
        splitPane.setDividerSize(INNER_DIVIDER_SIZE);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        if (divider != null) {
            divider.setBorder(BorderFactory.createEmptyBorder());
        }
        return splitPane;
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
        private ToolWindowSplitPane(int orientation, Component first, Component second) {
            super(orientation, first, second);
            putClientProperty(CHROME_SPLIT_PROPERTY, Boolean.TRUE);
            setOpaque(true);
            refreshBackground();
        }

        @Override
        public void updateUI() {
            setUI(new BorderlessSplitPaneUi());
            refreshBackground();
        }

        private void refreshBackground() {
            setBackground(ModernColors.getBackgroundColor());
        }
    }

    private static final class ToolWindowInnerSplitPane extends JSplitPane {
        private ToolWindowInnerSplitPane(int orientation, Component first, Component second) {
            super(orientation, first, second);
            putClientProperty(CHROME_SPLIT_PROPERTY, Boolean.TRUE);
            setOpaque(true);
            refreshBackground();
        }

        @Override
        public void updateUI() {
            setUI(new InnerDividerSplitPaneUi());
            refreshBackground();
        }

        private void refreshBackground() {
            setBackground(ModernColors.getCardBackgroundColor());
        }
    }

    private static final class BorderlessSplitPaneUi extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                {
                    setBorder(BorderFactory.createEmptyBorder());
                    setBackground(ModernColors.getBackgroundColor());
                }

                @Override
                public void paint(Graphics g) {
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
                        g2.setColor(ModernColors.getBackgroundColor());
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    } finally {
                        g2.dispose();
                    }
                }
            };
        }
    }
}
