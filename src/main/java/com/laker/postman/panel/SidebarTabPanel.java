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

    private JTabbedPane tabbedPane;
    private List<TabInfo> tabInfos;
    private JPanel consoleContainer;
    private JLabel consoleLabel;
    private ConsolePanel consolePanel;
    private JSplitPane splitPane;

    @Override
    protected void initUI() {
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
        tabbedPane.addChangeListener(e -> handleTabChange());
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));  //
        panel.setPreferredSize(new Dimension(81, 60)); // 设置合适的宽高
        panel.setOpaque(false); // 设置透明背景
        JLabel iconLabel = new JLabel(icon); // 使用传入的图标
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 图标居中对齐
        iconLabel.setPreferredSize(new Dimension(32, 32));
        JLabel titleLabel = new JLabel(title); // 使用传入的标题
        titleLabel.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 文本居中对齐
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(2)); // 图标和文本之间的间距
        panel.add(titleLabel);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        return panel;
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