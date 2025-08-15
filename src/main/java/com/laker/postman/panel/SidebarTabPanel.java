package com.laker.postman.panel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.panel.SingletonBasePanel;
import com.laker.postman.panel.collections.RequestCollectionsPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import com.laker.postman.util.FontUtil;
import com.laker.postman.util.UserSettingsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 左侧标签页面板
 */
@Slf4j
public class SidebarTabPanel extends SingletonBasePanel {

    private static final String TAB_TOGGLE = "Toggle";
    private JTabbedPane tabbedPane;
    private List<TabInfo> tabInfos;
    private JPanel consoleContainer;
    private JLabel consoleLabel;
    private ConsolePanel consolePanel;
    private JSplitPane splitPane;
    private boolean sidebarExpanded = false; // 侧边栏展开状态
    private int lastSelectedTabIndex = 0; // 记录上一个被选中的标签索引

    @Override
    protected void initUI() {
        // 先读取侧边栏展开状态
        sidebarExpanded = UserSettingsUtil.isSidebarExpanded();
        setLayout(new BorderLayout());
        // 1. 创建标签页
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabInfos = new ArrayList<>();
        tabInfos.add(new TabInfo("Collections", new FlatSVGIcon("icons/collections.svg", 20, 20),
                () -> SingletonFactory.getInstance(RequestCollectionsPanel.class)));
        tabInfos.add(new TabInfo("Environments", new FlatSVGIcon("icons/environments.svg", 20, 20),
                () -> SingletonFactory.getInstance(EnvironmentPanel.class)));
        tabInfos.add(new TabInfo("Functional", new FlatSVGIcon("icons/functional.svg", 20, 20),
                () -> SingletonFactory.getInstance(FunctionalPanel.class)));
        tabInfos.add(new TabInfo("Performance", new FlatSVGIcon("icons/performance.svg", 20, 20),
                () -> SingletonFactory.getInstance(PerformancePanel.class)));
        tabInfos.add(new TabInfo("History", new FlatSVGIcon("icons/history.svg", 20, 20),
                () -> SingletonFactory.getInstance(HistoryPanel.class)));
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, new JPanel());
            tabbedPane.setTabComponentAt(i, createPostmanTabHeader(info.title, info.icon));
        }
        // 在tabbedPane最后面增加展开、收起菜单标签
        tabbedPane.addTab(TAB_TOGGLE, new JPanel()); // 添加一个空的tab作为占位符
        int toggleTabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(toggleTabIndex, createToggleTabHeader());

        // 默认设置选中第一个标签
        tabbedPane.setSelectedIndex(0);

        // 2. 控制台日志区
        consoleContainer = new JPanel(new BorderLayout());
        consoleContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        consoleContainer.setOpaque(false);
        createConsoleLabel();
        consolePanel = SingletonFactory.getInstance(ConsolePanel.class);
        // 注册关闭按钮事件
        consolePanel.setCloseAction(e -> setConsoleExpanded(false));
        setConsoleExpanded(false);
    }

    private void setConsoleExpanded(boolean expanded) {
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
            consoleContainer.add(consoleLabel, BorderLayout.CENTER);
            add(consoleContainer, BorderLayout.SOUTH);
            revalidate();
            repaint();
        }
    }

    private void createConsoleLabel() {
        consoleLabel = new JLabel("Console");
        consoleLabel.setIcon(new FlatSVGIcon("icons/console.svg", 16, 16));
        consoleLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 12));
        consoleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        consoleLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        consoleLabel.setFocusable(true); // 让label可聚焦
        consoleLabel.setEnabled(true); // 确保label可用
        consoleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setConsoleExpanded(true);
            }
        });
    }

    @Override
    protected void registerListeners() {
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            int toggleTabIndex = tabbedPane.getTabCount() - 1;
            if (selectedIndex == toggleTabIndex) {
                // 切换侧边栏状态
                toggleSidebar();
                // 重新选择之前的标签，避免选中切换按钮标签
                SwingUtilities.invokeLater(() -> {
                    if (tabbedPane.getTabCount() > 1 && lastSelectedTabIndex >= 0 && lastSelectedTabIndex < toggleTabIndex) {
                        tabbedPane.setSelectedIndex(lastSelectedTabIndex);
                    }
                });
            } else {
                // 记录上一个被选中的标签索引（不包括toggle标签）
                lastSelectedTabIndex = selectedIndex;
                handleTabChange();
            }
        });
        // 懒加载第一个tab
        SwingUtilities.invokeLater(() -> ensureTabComponentLoaded(0));
    }

    // Tab切换时才加载真正的面板内容
    private void handleTabChange() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        ensureTabComponentLoaded(selectedIndex);
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
     * 创建模仿Postman风格的Tab头部（图标在上，文本在下）
     */
    private Component createPostmanTabHeader(String title, Icon icon) {
        return createTabHeader(title, icon, sidebarExpanded);
    }

    /**
     * 创建标签头部，支持展开和收起状态
     */
    private Component createTabHeader(String title, Icon icon, boolean expanded) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // 创建鼠标监听器，用于处理tab切换
        MouseAdapter tabClickListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 查找这个标签在tabInfos中的索引
                for (int i = 0; i < tabInfos.size(); i++) {
                    TabInfo info = tabInfos.get(i);
                    if (info.title.equals(title)) {
                        tabbedPane.setSelectedIndex(i);
                        break;
                    }
                }
            }
        };

        if (expanded) {
            // 展开状态：显示图标和文字，保持固定高度
            panel.setPreferredSize(new Dimension(81, 60));
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setPreferredSize(new Dimension(32, 32));
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(iconLabel);
            panel.add(Box.createVerticalStrut(2));
            panel.add(titleLabel);

            // 为图标和标题都添加点击事件
            iconLabel.addMouseListener(tabClickListener);
            titleLabel.addMouseListener(tabClickListener);
        } else {
            // 收起状态：只显示图标，但保持与展开状态相同的高度
            panel.setPreferredSize(new Dimension(30, 60)); // 保持高度60不变
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setPreferredSize(new Dimension(20, 20));
            iconLabel.setToolTipText(title); // 悬停显示标题
            panel.add(Box.createVerticalGlue());
            panel.add(iconLabel);
            panel.add(Box.createVerticalGlue());

            // 为收起状态下的图标添加点击事件，确保点击图标能触发tab切换
            iconLabel.addMouseListener(tabClickListener);
        }

        // 为常规标签也添加鼠标监听器，确保点击响应
        panel.addMouseListener(tabClickListener);

        panel.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2)); // 恢复原来的边距
        return panel;
    }

    /**
     * 创建展开/收起侧边栏的标签头部
     */
    private JPanel createToggleTabHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // 根据当前状态显示不同的图标和大小
        Icon toggleIcon = sidebarExpanded ?
                new FlatSVGIcon("icons/collapse.svg", 20, 20) :
                new FlatSVGIcon("icons/expand.svg", 20, 20); // 收起状态下也保持20x20

        // 创建鼠标监听器，用于处理切换操作
        MouseAdapter toggleClickListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                toggleSidebar();
            }
        };

        if (sidebarExpanded) {
            panel.setPreferredSize(new Dimension(81, 30));
            JLabel iconLabel = new JLabel(toggleIcon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setPreferredSize(new Dimension(32, 32));
            panel.add(Box.createVerticalGlue());
            panel.add(iconLabel);
            panel.add(Box.createVerticalGlue());

            // 为图标添加点击事件
            iconLabel.addMouseListener(toggleClickListener);
        } else {
            panel.setPreferredSize(new Dimension(30, 30));
            JLabel iconLabel = new JLabel(toggleIcon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setPreferredSize(new Dimension(20, 20)); // 保持图标20x20大小不变
            panel.add(Box.createVerticalGlue());
            panel.add(iconLabel);
            panel.add(Box.createVerticalGlue());

            // 为收起状态下的图标添加点击事件
            iconLabel.addMouseListener(toggleClickListener);
        }

        // 添加鼠标监听器来处理切换操作
        panel.addMouseListener(toggleClickListener);

        panel.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2)); // 恢复原来的边距
        return panel;
    }

    /**
     * 切换侧边栏的展开和收起状态
     */
    private void toggleSidebar() {
        sidebarExpanded = !sidebarExpanded;
        UserSettingsUtil.saveSidebarExpanded(sidebarExpanded); // 保存状态
        updateTabHeaders();
    }

    /**
     * 更新所有标签头部的显示状态
     */
    private void updateTabHeaders() {
        // 更新所有常规标签的头部
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.setTabComponentAt(i, createTabHeader(info.title, info.icon, sidebarExpanded));
        }

        // 更新切换按钮的头部
        int toggleTabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setTabComponentAt(toggleTabIndex, createToggleTabHeader());

        revalidate(); // 重新布局
        repaint(); // 重绘组件
    }

    // Tab元数据结构，便于维护和扩展
    private static class TabInfo {
        String title;
        Icon icon;
        Supplier<JPanel> panelSupplier; // 用于懒加载面板
        JPanel panel;

        TabInfo(String title, Icon icon, Supplier<JPanel> panelSupplier) {
            this.title = title;
            this.icon = icon;
            this.panelSupplier = panelSupplier;
        }

        JPanel getPanel() { // 懒加载面板
            if (panel == null) {
                panel = panelSupplier.get();
                log.info("Loaded panel for tab: {}", title);
            }
            return panel;
        }
    }
}