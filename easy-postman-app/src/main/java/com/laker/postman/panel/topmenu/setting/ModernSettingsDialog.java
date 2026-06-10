package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;


/**
 * 现代化统一设置对话框
 * 集成所有设置到一个标签页界面中
 */
public class ModernSettingsDialog extends JDialog {

    // UI 常量
    private static final int MIN_WIDTH = 600;
    private static final int MIN_HEIGHT = 300;
    private static final int PREFERRED_WIDTH = 960;
    private static final int PREFERRED_HEIGHT = 700;
    private static final int SIDE_TAB_WIDTH = 176;

    private final JTabbedPane tabbedPane;
    private final SettingsContributionRegistry contributionRegistry;

    /**
     * 获取主题适配的文本颜色
     */
    private Color getTextColor() {
        return ModernColors.getTextPrimary();
    }

    /**
     * 获取主题适配的悬停背景色
     */
    private Color getHoverBackgroundColor() {
        return ModernColors.getTabHoverBackgroundColor();
    }

    private Color getNavigationSelectedBackgroundColor() {
        return ModernColors.getSelectionBackgroundColor();
    }

    private Color getNavigationSelectedForegroundColor() {
        return ModernColors.getTextPrimary();
    }


    public ModernSettingsDialog(Window parent) {
        this(parent, SettingsContributionRegistry.defaultRegistry());
    }

    ModernSettingsDialog(Window parent, SettingsContributionRegistry contributionRegistry) {
        super(parent, I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_TITLE), ModalityType.APPLICATION_MODAL);
        this.contributionRegistry = Objects.requireNonNull(contributionRegistry, "contributionRegistry");
        this.tabbedPane = createModernTabbedPane();
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        configureDialog();
        addSettingTabs();
        setupMainPanel();
        setupIcon();
    }

    private void configureDialog() {
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                resetSelectedTabScrollPosition();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        // Add ESC key handling to close dialog
        JRootPane rootPane = getRootPane();
        rootPane.registerKeyboardAction(
                e -> handleWindowClosing(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        tabbedPane.addChangeListener(e -> resetSelectedTabScrollPosition());
    }

    private void addSettingTabs() {
        SettingsContributionContext context = new SettingsContributionContext(this);
        for (SettingsContribution contribution : contributionRegistry.contributions()) {
            tabbedPane.addTab(
                    contribution.resolveTitle(),
                    contribution.createPanel(context)
            );
        }
    }

    private void setupMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private void setupIcon() {
        try {
            setIconImage(Toolkit.getDefaultToolkit()
                    .getImage(getClass().getResource("/icons/icon.png")));
        } catch (Exception e) {
            // Icon loading failed, continue without icon
        }
    }

    /**
     * 创建简洁的标签页
     */
    private JTabbedPane createModernTabbedPane() {
        JTabbedPane pane = new JTabbedPane(SwingConstants.LEFT);
        ToolWindowSurfaceStyle.applyDialogSurface(pane);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setForeground(getTextColor());
        pane.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        pane.setUI(new SimpleTabbedPaneUI());
        return pane;
    }

    /**
     * 简洁的标签页 UI
     */
    private class SimpleTabbedPaneUI extends BasicTabbedPaneUI {

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(12, 0, 12, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabInsets = new Insets(8, 24, 8, 16);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            int tabAreaWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
            g.setColor(ModernColors.getDialogChromeBackgroundColor());
            g.fillRect(0, 0, tabAreaWidth, tabPane.getHeight());
            super.paintTabArea(g, tabPlacement, selectedIndex);
            // 左侧导航的竖线属于整个 tabArea，而不是某个 tab，避免选中项重绘时出现断线。
            paintTabAreaDivider(g, tabAreaWidth);
        }

        private void paintTabAreaDivider(Graphics g, int tabAreaWidth) {
            if (tabAreaWidth <= 0) {
                return;
            }
            g.setColor(ModernColors.getBorderLightColor());
            int x = tabAreaWidth - 1;
            g.drawLine(x, 0, x, tabPane.getHeight());
        }

        @Override
        protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
            return Math.max(SIDE_TAB_WIDTH, super.calculateTabWidth(tabPlacement, tabIndex, metrics));
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int tabAreaWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                if (isSelected) {
                    g2.setColor(getNavigationSelectedBackgroundColor());
                    // 左侧导航选中态要贴齐固定分割线，避免 BasicTabbedPaneUI 的 tab rect 留白造成视觉错位。
                    g2.fillRect(0, y, Math.max(0, tabAreaWidth - 1), h);
                } else if (getRolloverTab() == tabIndex) {
                    g2.setColor(getHoverBackgroundColor());
                    g2.fillRect(0, y, Math.max(0, tabAreaWidth - 1), h);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
            // 不绘制边框
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                           int tabIndex, Rectangle iconRect, Rectangle textRect,
                                           boolean isSelected) {
            // 不绘制焦点指示器
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                 int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                g2.setColor(isSelected ? getNavigationSelectedForegroundColor() : getTextColor());
                Font textFont = isSelected ? font.deriveFont(Font.BOLD) : font;
                g2.setFont(textFont);
                FontMetrics actualMetrics = g2.getFontMetrics();
                int tabAreaWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                int textWidth = actualMetrics.stringWidth(title);
                int textX = Math.max(8, (tabAreaWidth - 1 - textWidth) / 2);
                int textY = textRect.y + actualMetrics.getAscent();
                if (rects != null && tabIndex >= 0 && tabIndex < rects.length) {
                    Rectangle tabRect = rects[tabIndex];
                    textY = tabRect.y + (tabRect.height - actualMetrics.getHeight()) / 2 + actualMetrics.getAscent();
                }
                g2.drawString(title, textX, textY);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            // 左侧导航分割线由 tabArea 固定绘制，避免选中项重绘时出现断线。
        }
    }

    /**
     * 处理窗口关闭事件
     */
    private void handleWindowClosing() {
        if (checkCurrentPanelUnsavedChanges() && checkAllPanelsUnsavedChanges()) {
            dispose();
        }
    }

    /**
     * 检查当前选中面板的未保存更改
     */
    private boolean checkCurrentPanelUnsavedChanges() {
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof ModernSettingsPanel panel) {
            return !panel.hasUnsavedChanges() || panel.confirmDiscardChanges();
        }
        return true;
    }

    /**
     * 检查所有面板的未保存更改
     */
    private boolean checkAllPanelsUnsavedChanges() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (component instanceof ModernSettingsPanel panel && panel.hasUnsavedChanges()) {
                tabbedPane.setSelectedIndex(i);
                if (!panel.confirmDiscardChanges()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 显示设置对话框
     *
     * @param parent 父窗口
     */
    public static void showSettings(Window parent) {
        showSettings(parent, 0);
    }

    /**
     * 显示设置对话框并打开指定的标签页
     *
     * @param parent   父窗口
     * @param tabIndex 要打开的标签页索引
     */
    public static void showSettings(Window parent, int tabIndex) {
        ModernSettingsDialog dialog = new ModernSettingsDialog(parent);
        dialog.selectTab(tabIndex);
        dialog.setVisible(true);
    }

    /**
     * 选择指定的标签页
     *
     * @param tabIndex 标签页索引
     */
    private void selectTab(int tabIndex) {
        if (tabIndex >= 0 && tabIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(tabIndex);
            resetSelectedTabScrollPosition();
        }
    }

    private void resetSelectedTabScrollPosition() {
        Component component = tabbedPane.getSelectedComponent();
        if (component instanceof ModernSettingsPanel panel) {
            panel.resetScrollPositionToTop();
        }
    }
}
