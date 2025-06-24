package com.laker.postman.panel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.panel.collections.RequestCollectionsPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.jmeter.JMeterPanel;
import com.laker.postman.util.FontUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 左侧标签页面板，包含集合、环境变量、压测、批量执行四个标签页
 */
@Slf4j
public class SidebarTabPanel extends BasePanel {

    private JTabbedPane tabbedPane;
    private List<TabInfo> tabInfos;


    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        // 1. 创建标签页
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabInfos = new ArrayList<>();
        tabInfos.add(new TabInfo("集合", new FlatSVGIcon("icons/collections.svg", 20, 20),
                () -> SingletonFactory.getInstance(RequestCollectionsPanel.class)));
        tabInfos.add(new TabInfo("环境", new FlatSVGIcon("icons/env.svg", 20, 20),
                () -> SingletonFactory.getInstance(EnvironmentPanel.class)));
        tabInfos.add(new TabInfo("Jmeter", new FlatSVGIcon("icons/jmeter.svg", 20, 20),
                () -> SingletonFactory.getInstance(JMeterPanel.class)));
        tabInfos.add(new TabInfo("历史", new FlatSVGIcon("icons/history.svg", 20, 20),
                () -> SingletonFactory.getInstance(HistoryPanel.class)));
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, new JPanel()); // 占位面板，实际内容在切换时加载 先用空面板占位，后续可以懒加载真正的内容面板（如ensureTabComponentLoaded方法所做的），提升性能和启动速度。
            tabbedPane.setTabComponentAt(i, createPostmanTabHeader(info.title, info.icon));
        }
        tabbedPane.setSelectedIndex(0);
        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    protected void registerListeners() {
        tabbedPane.addChangeListener(e -> handleTabChange());
        // 懒加载第一个tab
        SwingUtilities.invokeLater(() -> ensureTabComponentLoaded(0));
    }

    /**
     * 点击标签页时才加载对应的组件 lazy loading 懒加载
     */
    private void handleTabChange() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        ensureTabComponentLoaded(selectedIndex);
    }

    private void ensureTabComponentLoaded(int index) {
        if (index < 0 || index >= tabInfos.size()) return;
        TabInfo info = tabInfos.get(index);
        Component comp = tabbedPane.getComponentAt(index);
        if (comp == null || comp.getClass() == JPanel.class) {
            JPanel realPanel = info.getPanel();
            tabbedPane.setComponentAt(index, realPanel);
        }
    }

    /**
     * 创建模仿Postman风格的Tab头部（图标在上，文本在下）
     */
    private Component createPostmanTabHeader(String title, Icon icon) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));  //
        panel.setPreferredSize(new Dimension(60, 60)); // 设置合适的宽高
        panel.setOpaque(false); // 设置透明背景
        JLabel iconLabel = new JLabel(icon); // 使用传入的图标
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 图标居中对齐
        iconLabel.setPreferredSize(new Dimension(28, 28));
        JLabel titleLabel = new JLabel(title); // 使用传入的标题
        titleLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 12));
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

        JPanel getPanel() {
            if (panel == null) {
                panel = panelSupplier.get();
                log.info("Loaded panel for tab: {}", title);
            }
            return panel;
        }
    }
}