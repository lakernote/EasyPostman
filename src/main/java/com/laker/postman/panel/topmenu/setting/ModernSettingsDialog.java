package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 现代化统一设置对话框
 * 集成所有设置到一个标签页界面中
 */
public class ModernSettingsDialog extends JDialog {
    private JTabbedPane tabbedPane;

    public ModernSettingsDialog(Window parent) {
        super(parent, I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_TITLE), ModalityType.APPLICATION_MODAL);
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(1000, 700));
        setPreferredSize(new Dimension(1100, 750));

        // 主容器
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(ModernColors.BG_LIGHT);

        // 创建现代化标签页
        tabbedPane = createModernTabbedPane();

        // 添加各个设置面板
        tabbedPane.addTab(
                I18nUtil.getMessage(MessageKeys.SETTINGS_GENERAL_TITLE),
                new UISettingsPanelModern()
        );

        tabbedPane.addTab(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TITLE),
                new RequestSettingsPanelModern()
        );

        tabbedPane.addTab(
                I18nUtil.getMessage(MessageKeys.SETTINGS_PROXY_TITLE),
                new ProxySettingsPanelModern()
        );

        tabbedPane.addTab(
                I18nUtil.getMessage(MessageKeys.SETTINGS_AUTO_UPDATE_TITLE),
                new SystemSettingsPanelModern()
        );

        tabbedPane.addTab(
                I18nUtil.getMessage(MessageKeys.SETTINGS_JMETER_TITLE),
                new PerformanceSettingsPanelModern()
        );

        tabbedPane.addTab(
                I18nUtil.getMessage(MessageKeys.CERT_TITLE),
                new ClientCertificateSettingsPanelModern(this)
        );

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        setContentPane(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        try {
            setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/icon.png")));
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 创建现代化标签页
     */
    private JTabbedPane createModernTabbedPane() {
        JTabbedPane pane = new JTabbedPane(JTabbedPane.LEFT);
        pane.setBackground(ModernColors.BG_WHITE);
        pane.setForeground(ModernColors.TEXT_PRIMARY);
        pane.setFont(new Font(pane.getFont().getName(), Font.PLAIN, 14));
        pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // 自定义标签页UI
        pane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(10, 12, 10, 0);
                contentBorderInsets = new Insets(0, 1, 0, 0);
                tabInsets = new Insets(12, 20, 12, 20);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isSelected) {
                    // 绘制外部阴影（多层）
                    for (int i = 3; i > 0; i--) {
                        int alpha = 10 + (i * 5);
                        g2.setColor(new Color(0, 0, 0, alpha));
                        g2.fillRoundRect(x + 4 + i, y + 2 + i, w - 8, h - 4, 10, 10);
                    }

                    // 主背景 - 蓝色
                    g2.setColor(ModernColors.PRIMARY);
                    g2.fillRoundRect(x + 4, y + 2, w - 8, h - 4, 10, 10);

                    // 顶部微妙的高光效果（内发光）
                    GradientPaint topGlow = new GradientPaint(
                            x + 4, y + 2,
                            new Color(255, 255, 255, 35),
                            x + 4, y + 2 + (h - 4) / 3,
                            new Color(255, 255, 255, 0)
                    );
                    g2.setPaint(topGlow);
                    g2.fillRoundRect(x + 4, y + 2, w - 8, (h - 4) / 2, 10, 10);


                } else if (getRolloverTab() == tabIndex) {
                    // 悬停的标签 - 添加微妙阴影
                    g2.setColor(new Color(0, 0, 0, 8));
                    g2.fillRoundRect(x + 5, y + 3, w - 8, h - 4, 10, 10);

                    g2.setColor(ModernColors.HOVER_BG);
                    g2.fillRoundRect(x + 4, y + 2, w - 8, h - 4, 10, 10);

                    // 悬停时的左侧细线提示
                    g2.setColor(new Color(ModernColors.PRIMARY.getRed(),
                            ModernColors.PRIMARY.getGreen(),
                            ModernColors.PRIMARY.getBlue(), 60));
                    g2.fillRoundRect(x + 5, y + 4, 2, h - 8, 2, 2);
                }
                // 普通标签不绘制背景

                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                // 不绘制任何边框
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                               int tabIndex, Rectangle iconRect, Rectangle textRect,
                                               boolean isSelected) {
                // 不绘制焦点指示器（虚线框）
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                     int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);

                if (isSelected) {
                    // 选中标签 - 白色文字，带微妙阴影增强可读性
                    g2.setColor(new Color(0, 0, 0, 30));
                    g2.setFont(font.deriveFont(Font.BOLD, 14f));
                    g2.drawString(title, textRect.x, textRect.y + metrics.getAscent() + 1);

                    g2.setColor(ModernColors.TEXT_INVERSE);
                    g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
                } else {
                    g2.setColor(ModernColors.TEXT_PRIMARY);
                    g2.setFont(font.deriveFont(Font.PLAIN, 13f));
                    g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
                }

                g2.dispose();
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // 不绘制内容区边框和分隔线
            }
        });

        return pane;
    }


    /**
     * 处理窗口关闭事件
     */
    private void handleWindowClosing() {
        // 检查当前选中的面板是否有未保存的更改
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof ModernSettingsPanel) {
            ModernSettingsPanel panel = (ModernSettingsPanel) selectedComponent;
            if (panel.hasUnsavedChanges()) {
                if (!panel.confirmDiscardChanges()) {
                    return; // 用户选择不关闭
                }
            }
        }

        // 检查所有标签页是否有未保存的更改
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (component instanceof ModernSettingsPanel) {
                ModernSettingsPanel panel = (ModernSettingsPanel) component;
                if (panel.hasUnsavedChanges()) {
                    // 切换到有未保存更改的标签页
                    tabbedPane.setSelectedIndex(i);
                    if (!panel.confirmDiscardChanges()) {
                        return; // 用户选择不关闭
                    }
                }
            }
        }

        dispose();
    }

    /**
     * 显示设置对话框
     */
    public static void showSettings(Window parent) {
        ModernSettingsDialog dialog = new ModernSettingsDialog(parent);
        dialog.setVisible(true);
    }

    /**
     * 显示设置对话框并打开指定的标签页
     */
    public static void showSettings(Window parent, int tabIndex) {
        ModernSettingsDialog dialog = new ModernSettingsDialog(parent);
        if (tabIndex >= 0 && tabIndex < dialog.tabbedPane.getTabCount()) {
            dialog.tabbedPane.setSelectedIndex(tabIndex);
        }
        dialog.setVisible(true);
    }
}

