package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * 现代化统一设置对话框
 * 集成所有设置到一个标签页界面中
 */
public class ModernSettingsDialog extends JDialog {

    // UI 常量
    private static final int MIN_WIDTH = 600;
    private static final int MIN_HEIGHT = 300;
    private static final int PREFERRED_WIDTH = 800;
    private static final int PREFERRED_HEIGHT = 550;
    private static final int TAB_FONT_SIZE = 13;
    private static final int CORNER_RADIUS = 6;
    private static final int TAB_MARGIN = 2;
    private static final int TAB_PADDING = 4;

    private final JTabbedPane tabbedPane;

    /**
     * 获取主题适配的背景色
     */
    private Color getBackgroundColor() {
        return ModernColors.getBackgroundColor();
    }

    /**
     * 获取主题适配的标签页背景色
     */
    private Color getTabBackgroundColor() {
        return ModernColors.getCardBackgroundColor();
    }

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
        return ModernColors.getHoverBackgroundColor();
    }


    public ModernSettingsDialog(Window parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_TITLE), ModalityType.APPLICATION_MODAL);
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
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        addWindowListener(new WindowAdapter() {
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
    }

    private void addSettingTabs() {
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_TITLE),
                new UISettingsPanelModern());
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TITLE),
                new RequestSettingsPanelModern());
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TITLE),
                new ProxySettingsPanelModern());
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_TITLE),
                new AutoUpdateSettingsPanel());
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_TITLE),
                new PerformanceSettingsPanelModern());
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.CERT_TITLE),
                new ClientCertificateSettingsPanelModern(this));
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.SETTINGS_SHORTCUTS_TITLE),
                new ShortcutSettingsPanel());
    }

    private void setupMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(getBackgroundColor());
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
        pane.setBackground(getTabBackgroundColor());
        pane.setForeground(getTextColor());
        pane.setFont(new Font(pane.getFont().getName(), Font.PLAIN, TAB_FONT_SIZE));
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
            tabAreaInsets = new Insets(5, 5, 5, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabInsets = new Insets(8, 12, 8, 12);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isSelected) {
                    g2.setColor(ModernColors.PRIMARY);
                    g2.fillRoundRect(x + TAB_MARGIN, y + TAB_MARGIN,
                            w - TAB_PADDING, h - TAB_PADDING, CORNER_RADIUS, CORNER_RADIUS);
                } else if (getRolloverTab() == tabIndex) {
                    g2.setColor(getHoverBackgroundColor());
                    g2.fillRoundRect(x + TAB_MARGIN, y + TAB_MARGIN,
                            w - TAB_PADDING, h - TAB_PADDING, CORNER_RADIUS, CORNER_RADIUS);
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

                g2.setColor(isSelected ? Color.WHITE : getTextColor());
                g2.setFont(isSelected ? font.deriveFont(Font.BOLD) : font);
                g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            // 不绘制内容区边框
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
        }
    }
}