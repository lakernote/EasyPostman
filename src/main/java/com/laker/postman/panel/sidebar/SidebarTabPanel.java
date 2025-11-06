package com.laker.postman.panel.sidebar;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.TabInfo;
import com.laker.postman.panel.collections.RequestCollectionsPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import com.laker.postman.panel.sidebar.cookie.CookieManagerDialog;
import com.laker.postman.panel.toolbox.ToolboxPanel;
import com.laker.postman.panel.workspace.WorkspacePanel;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static com.laker.postman.util.MessageKeys.MENU_FUNCTIONAL;

/**
 * 左侧标签页面板
 */
@Slf4j
public class SidebarTabPanel extends SingletonBasePanel {

    @Getter
    private JTabbedPane tabbedPane;
    @Getter
    private transient List<TabInfo> tabInfos;
    private JPanel consoleContainer;
    private JLabel consoleLabel;
    private JLabel sidebarToggleLabel; // 侧边栏展开/收起按钮
    private JLabel cookieLabel;
    private JLabel versionLabel;
    private ConsolePanel consolePanel;
    private boolean sidebarExpanded = false; // 侧边栏展开状态
    private CookieManagerDialog cookieManagerDialog; // Cookie管理器对话框实例

    @Override
    protected void initUI() {
        // 先读取侧边栏展开状态
        sidebarExpanded = SettingManager.isSidebarExpanded();
        setLayout(new BorderLayout());
        // 1. 创建标签页
        tabbedPane = createModernTabbedPane();
        tabInfos = new ArrayList<>();
        tabInfos.add(new TabInfo(I18nUtil.getMessage(MessageKeys.MENU_COLLECTIONS), new FlatSVGIcon("icons/collections.svg", 20, 20),
                () -> SingletonFactory.getInstance(RequestCollectionsPanel.class)));
        tabInfos.add(new TabInfo(I18nUtil.getMessage(MessageKeys.MENU_ENVIRONMENTS), new FlatSVGIcon("icons/environments.svg", 20, 20),
                () -> SingletonFactory.getInstance(EnvironmentPanel.class)));
        tabInfos.add(new TabInfo(I18nUtil.getMessage(MessageKeys.MENU_WORKSPACES), new FlatSVGIcon("icons/workspace.svg", 20, 20),
                () -> SingletonFactory.getInstance(WorkspacePanel.class)));
        tabInfos.add(new TabInfo(I18nUtil.getMessage(MENU_FUNCTIONAL), new FlatSVGIcon("icons/functional.svg", 20, 20),
                () -> SingletonFactory.getInstance(FunctionalPanel.class)));
        tabInfos.add(new TabInfo(I18nUtil.getMessage(MessageKeys.MENU_PERFORMANCE), new FlatSVGIcon("icons/performance.svg", 20, 20),
                () -> SingletonFactory.getInstance(PerformancePanel.class)));
        tabInfos.add(new TabInfo(I18nUtil.getMessage(MessageKeys.MENU_TOOLBOX), new FlatSVGIcon("icons/tools.svg", 20, 20),
                () -> SingletonFactory.getInstance(ToolboxPanel.class)));
        tabInfos.add(new TabInfo(I18nUtil.getMessage(MessageKeys.MENU_HISTORY), new FlatSVGIcon("icons/history.svg", 20, 20),
                () -> SingletonFactory.getInstance(HistoryPanel.class)));

        // Add tabs to the JTabbedPane
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, new JPanel());
            // 设置自定义 tab 组件以实现图标在上、文本在下的布局
            tabbedPane.setTabComponentAt(i, createTabComponent(info.title, info.icon));
        }

        // 默认设置选中第一个标签
        tabbedPane.setSelectedIndex(0);

        // 2. 控制台日志区和底部栏
        consoleContainer = new JPanel(new BorderLayout());
        consoleContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        consoleContainer.setOpaque(false);
        initBottomBar();
        consolePanel = SingletonFactory.getInstance(ConsolePanel.class);
        // 注册关闭按钮事件
        consolePanel.setCloseAction(e -> setConsoleExpanded(false));
        setConsoleExpanded(false);
    }

    private void setConsoleExpanded(boolean expanded) {
        JSplitPane splitPane;
        removeAll();
        if (expanded) {
            consoleContainer.removeAll();
            consoleContainer.add(consolePanel, BorderLayout.CENTER);
            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, consoleContainer);
            splitPane.setDividerSize(2);
            splitPane.setBorder(null);
            splitPane.setOneTouchExpandable(true);
            splitPane.setResizeWeight(1.0);
            splitPane.setMinimumSize(new Dimension(0, 10));
            tabbedPane.setMinimumSize(new Dimension(0, 30));
            consoleContainer.setMinimumSize(new Dimension(0, 30));
            add(splitPane, BorderLayout.CENTER);
            revalidate();
            repaint();
            SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(splitPane.getHeight() - 300));
        } else {
            add(tabbedPane, BorderLayout.CENTER);
            consoleContainer.removeAll();
            // 左侧放 SidebarToggle 和 Console
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            leftPanel.setOpaque(false);
            leftPanel.add(sidebarToggleLabel);
            leftPanel.add(consoleLabel);
            consoleContainer.add(leftPanel, BorderLayout.WEST);
            // 右侧放 Cookies 和 Version
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            rightPanel.setOpaque(false);
            rightPanel.add(cookieLabel);
            rightPanel.add(versionLabel);
            consoleContainer.add(rightPanel, BorderLayout.EAST);
            add(consoleContainer, BorderLayout.SOUTH);
            revalidate();
            repaint();
        }
    }

    /**
     * 初始化底部栏，包括控制台标签和版本标签
     */
    private void initBottomBar() {
        createSidebarToggleLabel();
        createConsoleLabel();
        createCookieLabel();
        createVersionLabel();
    }

    /**
     * 创建侧边栏展开/收起按钮
     */
    private void createSidebarToggleLabel() {
        sidebarToggleLabel = new JLabel(new FlatSVGIcon("icons/sidebar-toggle.svg", 20, 20));
        sidebarToggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sidebarToggleLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        sidebarToggleLabel.setFocusable(true);
        sidebarToggleLabel.setEnabled(true);
        sidebarToggleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                toggleSidebarExpansion();
            }
        });
    }

    /**
     * 切换侧边栏展开/收起状态
     */
    private void toggleSidebarExpansion() {
        sidebarExpanded = !sidebarExpanded;
        SettingManager.setSidebarExpanded(sidebarExpanded);
        recreateTabbedPane();
    }

    /**
     * 创建控制台标签
     */
    private void createConsoleLabel() {
        consoleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CONSOLE_TITLE));
        consoleLabel.setIcon(new FlatSVGIcon("icons/console.svg", 16, 16));
        consoleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
        consoleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        consoleLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        consoleLabel.setFocusable(true); // 让label可聚焦
        consoleLabel.setEnabled(true); // 确保label可用
        consoleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setConsoleExpanded(true);
            }
        });
    }

    /**
     * 创建 Cookie 标签
     */
    private void createCookieLabel() {
        cookieLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COOKIES_TITLE));
        cookieLabel.setIcon(new FlatSVGIcon("icons/cookie.svg", 16, 16));
        cookieLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
        cookieLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cookieLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        cookieLabel.setFocusable(true);
        cookieLabel.setEnabled(true);
        cookieLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showCookieManagerDialog();
            }
        });
    }

    /**
     * 显示 Cookie 管理器对话框
     */
    private void showCookieManagerDialog() {
        // 如果对话框已存在且可见，则将其置于前台
        if (cookieManagerDialog != null && cookieManagerDialog.isVisible()) {
            cookieManagerDialog.toFront();
            cookieManagerDialog.requestFocus();
            return;
        }

        // 创建新对话框
        Window window = SwingUtilities.getWindowAncestor(this);
        cookieManagerDialog = new CookieManagerDialog(window);
        cookieManagerDialog.setVisible(true);
    }

    /**
     * 创建版本号标签
     */
    private void createVersionLabel() {
        versionLabel = new JLabel(SystemUtil.getCurrentVersion());
        versionLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        versionLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 12));
    }

    @Override
    protected void registerListeners() {
        tabbedPane.addChangeListener(e -> {
            handleTabChange();
            updateTabTextColors(); // 更新所有 tab 的文字颜色
        });
        // 懒加载第一个tab
        SwingUtilities.invokeLater(() -> {
            ensureTabComponentLoaded(0);
            updateTabTextColors(); // 初始化时更新颜色
        });
    }

    /**
     * 更新所有 tab 的文字颜色（仅在展开状态下）
     */
    private void updateTabTextColors() {
        if (!sidebarExpanded) return;

        int selectedIndex = tabbedPane.getSelectedIndex();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            updateSingleTabTextColor(i, i == selectedIndex);
        }
    }

    /**
     * 更新单个 tab 的文字颜色
     */
    private void updateSingleTabTextColor(int tabIndex, boolean isSelected) {
        Component tabComponent = tabbedPane.getTabComponentAt(tabIndex);
        if (!(tabComponent instanceof JPanel panel)) return;

        // 查找文字标签并更新颜色
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel label && label.getText() != null && !label.getText().isEmpty()) {
                if (isSelected) {
                    label.setForeground(ModernColors.PRIMARY);
                    label.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
                } else {
                    label.setForeground(ModernColors.TEXT_SECONDARY);
                    label.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
                }
                break;
            }
        }
    }

    // Tab切换时才加载真正的面板内容
    private void handleTabChange() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        ensureTabComponentLoaded(selectedIndex); // 懒加载当前选中的tab内容
        // Check if the selected tab is the environment tab
        if (selectedIndex >= 0 && selectedIndex < tabInfos.size()) {
            TabInfo info = tabInfos.get(selectedIndex);
            if (info.title.equals(I18nUtil.getMessage(MessageKeys.MENU_ENVIRONMENTS))) {
                Component comp = tabbedPane.getComponentAt(selectedIndex);
                if (comp instanceof EnvironmentPanel environmentPanel) {
                    environmentPanel.refreshUI();
                }
            }
        }
    }

    private void ensureTabComponentLoaded(int index) {
        if (index < 0 || index >= tabInfos.size()) return;
        TabInfo info = tabInfos.get(index);
        Component comp = tabbedPane.getComponentAt(index);
        if (comp == null || comp.getClass() == JPanel.class) {
            JPanel realPanel = info.getPanel(); // 懒加载真正的面板内容
            tabbedPane.setComponentAt(index, realPanel);
        }
    }

    /**
     * 创建现代化标签页
     */
    private JTabbedPane createModernTabbedPane() {
        JTabbedPane pane = new JTabbedPane(SwingConstants.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        pane.setForeground(ModernColors.TEXT_PRIMARY);
        pane.setFont(new Font(pane.getFont().getName(), Font.PLAIN, 14));

        // 自定义标签页UI
        pane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                // 增加 tab 区域的上下边距，让 tab 之间有更多空间
                tabAreaInsets = new Insets(12, 8, 12, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
                // tab 之间的间距
                tabInsets = new Insets(2, 2, 2, 2);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isSelected) {
                    // 选中状态：只绘制左侧蓝色指示条
                    int margin = 4;
                    int indicatorWidth = 3;
                    int indicatorRadius = 2;

                    g2.setColor(ModernColors.PRIMARY);
                    g2.fillRoundRect(x + margin, y + margin, indicatorWidth, h - margin * 2, indicatorRadius, indicatorRadius);
                }
                // 不绘制悬停效果，避免卡顿

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
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // 不绘制内容区边框和分隔线
            }
        });

        return pane;
    }

    /**
     * 创建自定义 Tab 组件（图标在上，文本在下）
     */
    private Component createTabComponent(String title, Icon icon) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // 创建鼠标监听器，用于处理 tab 切换
        MouseAdapter tabClickListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int tabIndex = getTabIndexByTitle(title);
                if (tabIndex >= 0) {
                    tabbedPane.setSelectedIndex(tabIndex);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setCursor(Cursor.getDefaultCursor());
            }
        };

        if (sidebarExpanded) {
            // 展开状态：图标在上，文本在下
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel titleLabel = new JLabel(title);
            // 根据当前是否选中设置初始颜色
            int currentIndex = getTabIndexByTitle(title);
            boolean isCurrentlySelected = currentIndex >= 0 && tabbedPane.getSelectedIndex() == currentIndex;

            if (isCurrentlySelected) {
                titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
                titleLabel.setForeground(ModernColors.PRIMARY);
            } else {
                titleLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
                titleLabel.setForeground(ModernColors.TEXT_SECONDARY);
            }
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(Box.createVerticalStrut(6));
            panel.add(iconLabel);
            panel.add(Box.createVerticalStrut(6));
            panel.add(titleLabel);
            panel.add(Box.createVerticalStrut(6));

            panel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

            // 为图标和标题添加点击事件
            iconLabel.addMouseListener(tabClickListener);
            titleLabel.addMouseListener(tabClickListener);
        } else {
            // 收起状态：只显示图标，居中，增加上下左右间距
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setToolTipText(title); // 悬停显示标题

            panel.add(Box.createVerticalGlue());
            panel.add(iconLabel);
            panel.add(Box.createVerticalGlue());

            // 增加边距，尤其是上下间距，让图标之间不会挨在一起
            panel.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));

            // 为图标添加点击事件
            iconLabel.addMouseListener(tabClickListener);
        }

        // 为整个 panel 也添加鼠标监听器，确保点击任何地方都能触发
        panel.addMouseListener(tabClickListener);

        return panel;
    }

    /**
     * 根据标题获取 tab 索引
     */
    private int getTabIndexByTitle(String title) {
        for (int i = 0; i < tabInfos.size(); i++) {
            if (tabInfos.get(i).title.equals(title)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 更新侧边栏展开/收起状态
     * 从设置对话框调用，用于实时更新UI
     */
    public void updateSidebarExpansion() {
        boolean newExpanded = SettingManager.isSidebarExpanded();
        if (this.sidebarExpanded != newExpanded) {
            this.sidebarExpanded = newExpanded;
            // 重新创建 TabbedPane 以应用新的展开状态
            recreateTabbedPane();
        }
    }

    /**
     * 重新创建标签页以应用新的展开/收起状态
     */
    private void recreateTabbedPane() {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // 保存所有已加载的面板
        List<Component> loadedPanels = new ArrayList<>();
        for (int i = 0; i < tabInfos.size(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            loadedPanels.add(comp);
        }

        // 移除旧的 TabbedPane
        Component consoleComp = null;
        LayoutManager layout = getLayout();
        if (layout instanceof BorderLayout borderLayout) {
            consoleComp = borderLayout.getLayoutComponent(BorderLayout.SOUTH);
            remove(tabbedPane);
        }

        // 创建新的 TabbedPane
        tabbedPane = createModernTabbedPane();

        // 恢复所有 tab
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, loadedPanels.get(i));
            // 设置自定义 tab 组件以实现图标在上、文本在下的布局
            tabbedPane.setTabComponentAt(i, createTabComponent(info.title, info.icon));
        }

        // 恢复选中的索引
        if (selectedIndex >= 0 && selectedIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(selectedIndex);
        }

        // 重新添加组件
        add(tabbedPane, BorderLayout.CENTER);
        if (consoleComp != null) {
            add(consoleComp, BorderLayout.SOUTH);
        }

        // 重新注册监听器
        tabbedPane.addChangeListener(e -> {
            handleTabChange();
            updateTabTextColors(); // 更新所有 tab 的文字颜色
        });

        // 更新初始文字颜色
        updateTabTextColors();

        // 刷新UI
        revalidate();
        repaint();
    }
}