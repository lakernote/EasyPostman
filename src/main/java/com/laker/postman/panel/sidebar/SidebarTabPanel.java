package com.laker.postman.panel.sidebar;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.TabInfo;
import com.laker.postman.panel.collections.RequestCollectionsPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import com.laker.postman.panel.toolbox.ToolboxPanel;
import com.laker.postman.panel.workspace.WorkspacePanel;
import com.laker.postman.panel.topmenu.setting.SettingManager;
import com.laker.postman.panel.sidebar.cookie.CookieManagerDialog;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

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
    private JLabel cookieLabel;
    private JLabel versionLabel;
    private ConsolePanel consolePanel;
    private boolean sidebarExpanded = false; // 侧边栏展开状态
    private CookieManagerDialog cookieManagerDialog; // Cookie管理器对话框实例

    // 支持的tab标题i18n key
    private static final String[] TAB_TITLE_KEYS = {
            MessageKeys.MENU_COLLECTIONS,
            MessageKeys.MENU_ENVIRONMENTS,
            MessageKeys.MENU_WORKSPACES,
            MessageKeys.MENU_FUNCTIONAL,
            MessageKeys.MENU_PERFORMANCE,
            MessageKeys.MENU_TOOLBOX,
            MessageKeys.MENU_HISTORY
    };
    // 支持的语言
    private static final Locale[] SUPPORTED_LOCALES = {Locale.CHINESE, Locale.ENGLISH};
    // 记录最大tab标题宽度
    private int maxTabTitleWidth = 90;

    @Override
    protected void initUI() {
        // 先读取侧边栏展开状态
        sidebarExpanded = SettingManager.isSidebarExpanded();
        setLayout(new BorderLayout());
        // 1. 创建标签页
        tabbedPane = new JTabbedPane(SwingConstants.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
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
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, new JPanel());
            tabbedPane.setTabComponentAt(i, createPostmanTabHeader(info.title, info.icon));
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

        maxTabTitleWidth = getMaxTabTitleWidth();
    }

    /**
     * 计算所有支持语言下tab标题的最大宽度
     */
    private int getMaxTabTitleWidth() {
        int maxWidth = 0;
        JLabel label = new JLabel();
        Font font = FontsUtil.getDefaultFont(Font.PLAIN, 12);
        label.setFont(font);
        for (Locale locale : SUPPORTED_LOCALES) {
            for (String key : TAB_TITLE_KEYS) {
                String text = getI18nTextForLocale(key, locale);
                FontMetrics fm = label.getFontMetrics(font);
                int width = fm.stringWidth(text);
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }
        }
        // 加padding
        return maxWidth + 10; // 10px padding
    }

    /**
     * 获取指定locale下的i18n文本
     */
    private String getI18nTextForLocale(String key, Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
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
            // 左侧放 Console
            consoleContainer.add(consoleLabel, BorderLayout.WEST);
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
        createConsoleLabel();
        createCookieLabel();
        createVersionLabel();
    }

    /**
     * 创建控制台标签
     */
    private void createConsoleLabel() {
        consoleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CONSOLE_TITLE));
        consoleLabel.setIcon(new FlatSVGIcon("icons/console.svg", 16, 16));
        consoleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 12));
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
        tabbedPane.addChangeListener(e -> handleTabChange());
        // 懒加载第一个tab
        SwingUtilities.invokeLater(() -> ensureTabComponentLoaded(0));
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
            // 展开状态：显示图标和文字，保持固定高度和最大宽度
            panel.setPreferredSize(new Dimension(maxTabTitleWidth, 60));
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setPreferredSize(new Dimension(32, 32));
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
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
}