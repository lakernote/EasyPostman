package com.laker.postman.panel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.AbstractBasePanel;
import com.laker.postman.common.SingletonPanelFactory;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.panel.batch.BatchRunPanel;
import com.laker.postman.panel.collections.RequestCollectionsPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.stress.RequestStressTestPanel;
import com.laker.postman.util.FontUtil;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 左侧标签页面板，包含集合、环境变量、压测、批量执行四个标签页
 */
@Slf4j
public class SidebarTabPanel extends AbstractBasePanel {

    private JTabbedPane tabbedPane;
    private List<TabInfo> tabInfos;
    private JPanel consoleContainer;
    private boolean isConsoleExpanded = false;
    private JLabel consoleLabel;
    private JPanel consolePanel;
    private JSplitPane splitPane;


    // 控制台日志区
    private JTextArea consoleLogArea;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        // 1. 创建标签页
        tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabInfos = new ArrayList<>();
        tabInfos.add(new TabInfo("集合", new FlatSVGIcon("icons/collections.svg", 20, 20),
                () -> SingletonPanelFactory.getInstance(RequestCollectionsPanel.class)));
        tabInfos.add(new TabInfo("环境", new FlatSVGIcon("icons/env.svg", 20, 20),
                () -> SingletonPanelFactory.getInstance(EnvironmentPanel.class)));
        tabInfos.add(new TabInfo("批量", new FlatSVGIcon("icons/batch.svg", 20, 20),
                BatchRunPanel::new));
        tabInfos.add(new TabInfo("压测", new FlatSVGIcon("icons/stress.svg", 20, 20),
                RequestStressTestPanel::new));
        // 新增“历史”Tab，将原consolePanel内容迁移到此Tab
        tabInfos.add(new TabInfo("历史", new FlatSVGIcon("icons/history.svg", 20, 20),
                () -> SingletonPanelFactory.getInstance(HistoryPanel.class)));
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, new JPanel()); // 占位面板，实际内容在切换时加载 先用空面板占位，后续可以懒加载真正的内容面板（如ensureTabComponentLoaded方法所做的），提升性能和启动速度。
            tabbedPane.setTabComponentAt(i, createPostmanTabHeader(info.title, info.icon));
        }
        tabbedPane.setSelectedIndex(0);

        // 2. 控制台日志区
        consoleContainer = new JPanel(new BorderLayout());
        consoleContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        consoleContainer.setOpaque(false);
        createConsoleLabel();
        createConsolePanel();
        setConsoleExpanded(false);
    }

    private void createConsolePanel() {
        // 展开时的 consolePanel，包含日志和关闭按钮
        consolePanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Console");
        consoleLogArea = new JTextArea();
        consoleLogArea.setEditable(false); // 设置为不可编辑
        consoleLogArea.setLineWrap(true); // 自动换行
        consoleLogArea.setWrapStyleWord(true); // 换行时按单词换行
        consoleLogArea.setFocusable(true); // 允许获取焦点，便于复制
        consoleLogArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)); // 显示文本光标
        JScrollPane logScroll = new JScrollPane(consoleLogArea);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JButton closeBtn = new JButton();
        closeBtn.setIcon(IconFontSwing.buildIcon(FontAwesome.TIMES, 16, new Color(80, 80, 80)));
        closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        closeBtn.setBackground(Colors.PANEL_BACKGROUND);
        closeBtn.addActionListener(e -> setConsoleExpanded(false));

        // 新增清空按钮
        JButton clearBtn = new JButton();
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setBorder(BorderFactory.createEmptyBorder());
        clearBtn.setBackground(Colors.PANEL_BACKGROUND);
        clearBtn.setToolTipText("清空日志");
        clearBtn.addActionListener(e -> consoleLogArea.setText(""));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(title, BorderLayout.WEST);
        // 右侧按钮区
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        topPanel.add(btnPanel, BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        consolePanel.add(topPanel, BorderLayout.NORTH);
        consolePanel.add(logScroll, BorderLayout.CENTER);
    }

    private void setConsoleExpanded(boolean expanded) {
        isConsoleExpanded = expanded;
        removeAll();
        if (expanded) {
            consoleContainer.removeAll();
            consoleContainer.add(consolePanel, BorderLayout.CENTER);
            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPane, consoleContainer);
            splitPane.setDividerSize(2); // 分割条的大小
            splitPane.setBorder(null);
            splitPane.setOneTouchExpandable(true);
            splitPane.setResizeWeight(1.0); // 让下方panel（console）初始高度最小
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
        consoleLabel.setFocusable(true); // 关键：让label可聚焦
        consoleLabel.setEnabled(true);
        consoleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { // 用mousePressed替换mouseClicked
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

    // Tab切换处理逻辑，便于维护
    private void handleTabChange() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        ensureTabComponentLoaded(selectedIndex);
        String selectedTitle = tabbedPane.getTitleAt(selectedIndex);
        switch (selectedTitle) {
            case "压测":
                break;
            case "集合":
                break;
            default:
                break;
        }
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

    // 控制台日志追加方法
    public static void appendConsoleLog(String msg) {
        SidebarTabPanel instance = SingletonPanelFactory.getInstance(SidebarTabPanel.class);
        if (instance != null && instance.consoleLogArea != null) {
            SwingUtilities.invokeLater(() -> {
                instance.consoleLogArea.append(msg + "\n");
                instance.consoleLogArea.setCaretPosition(instance.consoleLogArea.getText().length());
            });
        }
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