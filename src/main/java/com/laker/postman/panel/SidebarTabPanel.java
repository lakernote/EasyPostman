package com.laker.postman.panel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.panel.collections.RequestCollectionsPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.jmeter.JMeterPanel;
import com.laker.postman.panel.runner.RunnerPanel;
import com.laker.postman.util.FontUtil;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.*;
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
public class SidebarTabPanel extends BasePanel {

    private JTabbedPane tabbedPane;
    private List<TabInfo> tabInfos;
    private JPanel consoleContainer;
    private JLabel consoleLabel;
    private JPanel consolePanel;
    private JSplitPane splitPane;
    // 控制台日志区
    private JTextPane consoleLogArea;
    private StyledDocument consoleDoc;

    // 日志类型
    public enum LogType {
        INFO, ERROR, SUCCESS, WARN, DEBUG, TRACE, CUSTOM
    }

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
                () -> SingletonFactory.getInstance(RunnerPanel.class)));
        tabInfos.add(new TabInfo("Performance", new FlatSVGIcon("icons/performance.svg", 20, 20),
                () -> SingletonFactory.getInstance(JMeterPanel.class)));
        tabInfos.add(new TabInfo("History", new FlatSVGIcon("icons/history.svg", 20, 20),
                () -> SingletonFactory.getInstance(HistoryPanel.class)));
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
        consoleLogArea = new JTextPane();
        consoleLogArea.setEditable(false); // 设置为不可编辑
        consoleLogArea.setFocusable(true); // 允许获取焦点，便于复制
        consoleLogArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)); // 显示文本光标
        consoleDoc = consoleLogArea.getStyledDocument();
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
        clearBtn.addActionListener(e -> {
            try {
                consoleDoc.remove(0, consoleDoc.getLength());
            } catch (BadLocationException ex) {
                // ignore
            }
        });

        // 搜索功能
        JTextField searchField = new JTextField(10);
        JButton prevBtn = new JButton("上一个");
        JButton nextBtn = new JButton("下一个");
        prevBtn.setFocusable(false);
        nextBtn.setFocusable(false);
        searchField.setToolTipText("搜索日志内容");
        // 支持回车键触发“下一个”搜索
        searchField.addActionListener(e -> nextBtn.doClick());
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("搜索:"));
        searchPanel.add(searchField);
        searchPanel.add(prevBtn);
        searchPanel.add(nextBtn);

        // 搜索实现
        final int[] lastMatchPos = {-1};
        final String[] lastKeyword = {""};
        nextBtn.addActionListener(e -> {
            String keyword = searchField.getText();
            if (keyword.isEmpty()) return;
            String text = consoleLogArea.getText();
            int start = lastKeyword[0].equals(keyword) ? lastMatchPos[0] + 1 : 0;
            int pos = text.indexOf(keyword, start);
            if (pos == -1 && start > 0) {
                // 循环查找
                pos = text.indexOf(keyword, 0);
            }
            if (pos != -1) {
                highlightSearchResult(pos, keyword.length());
                lastMatchPos[0] = pos;
                lastKeyword[0] = keyword;
            }
        });
        prevBtn.addActionListener(e -> {
            String keyword = searchField.getText();
            if (keyword.isEmpty()) return;
            String text = consoleLogArea.getText();
            int start = lastKeyword[0].equals(keyword) ? lastMatchPos[0] - 1 : text.length();
            int pos = text.lastIndexOf(keyword, start);
            if (pos == -1 && start < text.length()) {
                // 循环查找
                pos = text.lastIndexOf(keyword);
            }
            if (pos != -1) {
                highlightSearchResult(pos, keyword.length());
                lastMatchPos[0] = pos;
                lastKeyword[0] = keyword;
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        // 右侧按钮区
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        topPanel.add(btnPanel, BorderLayout.EAST);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        consolePanel.add(topPanel, BorderLayout.NORTH);
        consolePanel.add(logScroll, BorderLayout.CENTER);
    }

    private void setConsoleExpanded(boolean expanded) {
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

    // 控制台日志追加方法
    public synchronized static void appendConsoleLog(String msg) {
        appendConsoleLog(msg, LogType.INFO);
    }

    public synchronized static void appendConsoleLog(String msg, LogType type) {
        SidebarTabPanel instance = SingletonFactory.getInstance(SidebarTabPanel.class);
        if (instance.consoleLogArea != null && instance.consoleDoc != null) {
            SwingUtilities.invokeLater(() -> {
                Style style = instance.consoleLogArea.addStyle("logStyle", null);
                switch (type) {
                    case ERROR:
                        StyleConstants.setForeground(style, new Color(220, 53, 69)); // 红色
                        StyleConstants.setBold(style, true);
                        break;
                    case SUCCESS:
                        StyleConstants.setForeground(style, new Color(40, 167, 69)); // 绿色
                        StyleConstants.setBold(style, true);
                        break;
                    case WARN:
                        StyleConstants.setForeground(style, new Color(255, 193, 7)); // 橙色
                        StyleConstants.setBold(style, true);
                        break;
                    case DEBUG:
                        StyleConstants.setForeground(style, new Color(0, 123, 255)); // 蓝色
                        StyleConstants.setBold(style, false);
                        break;
                    case TRACE:
                        StyleConstants.setForeground(style, new Color(111, 66, 193)); // 紫色
                        StyleConstants.setBold(style, false);
                        break;
                    case CUSTOM:
                        StyleConstants.setForeground(style, new Color(23, 162, 184)); // 青色
                        StyleConstants.setBold(style, false);
                        break;
                    default:
                        StyleConstants.setForeground(style, new Color(33, 37, 41)); // 深灰
                        StyleConstants.setBold(style, false);
                }
                try {
                    instance.consoleDoc.insertString(instance.consoleDoc.getLength(), msg + "\n", style);
                    instance.consoleLogArea.setCaretPosition(instance.consoleDoc.getLength());
                } catch (BadLocationException e) {
                    // ignore
                }
            });
        }
    }

    // 高亮搜索结果方法
    private void highlightSearchResult(int pos, int len) {
        try {
            consoleLogArea.getHighlighter().removeAllHighlights();
            consoleLogArea.getHighlighter().addHighlight(pos, pos + len, new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
            consoleLogArea.setCaretPosition(pos + len);
        } catch (BadLocationException ex) {
            // ignore
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

        JPanel getPanel() { // 懒加载面板
            if (panel == null) {
                panel = panelSupplier.get();
                log.info("Loaded panel for tab: {}", title);
            }
            return panel;
        }
    }
}