package com.laker.postman.panel.sidebar;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.performance.PerformanceUiWarmup;
import com.laker.postman.panel.sidebar.cookie.CookieManagerDialog;
import com.laker.postman.panel.sidebar.global.GlobalVariablesDialog;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * 左侧标签页面板
 */
@Slf4j
public class SidebarTabPanel extends UiSingletonPanel {

    // Constants
    private static final String ICON_LABEL_NAME = "iconLabel";
    private static final String TITLE_LABEL_NAME = "titleLabel";
    @Getter
    private JTabbedPane tabbedPane;
    @Getter
    private transient List<TabInfo> tabInfos;
    private transient List<SidebarTab> visibleTabs;
    private JPanel consoleContainer;
    private SidebarBottomBar bottomBar;
    private ConsolePanel consolePanel;
    private boolean sidebarExpanded = false; // 侧边栏展开状态
    private CookieManagerDialog cookieManagerDialog; // Cookie管理器对话框实例
    private GlobalVariablesDialog globalVariablesDialog; // 全局变量对话框实例
    private int lastSelectedIndex = -1; // 记录上一次选中的索引，用于优化颜色更新

    // 字体缓存，避免重复创建
    private Font normalFont;      // PLAIN 12 - Tab文本和版本号共用
    private Font boldFont;        // BOLD 12 - Tab文本选中态和底部栏共用
    // 自适应宽高缓存
    private int calculatedExpandedTabWidth = -1; // 计算后的展开状态tab宽度
    private int calculatedCollapsedTabWidth = -1; // 计算后的收起状态tab宽度
    private int calculatedExpandedTabHeight = -1; // 计算后的展开状态tab高度
    private int calculatedCollapsedTabHeight = -1; // 计算后的收起状态tab高度

    @Override
    protected void initUI() {
        // 初始化字体缓存
        // Tab 标题使用语言感知字体（英文小一号，避免文本过长）
        normalFont = getLanguageAwareFont(Font.PLAIN);
        boldFont = getLanguageAwareFont(Font.BOLD);
        // 先读取侧边栏展开状态
        sidebarExpanded = SettingManager.isSidebarExpanded();
        setLayout(new BorderLayout());

        // 1. 创建标签页
        tabbedPane = createSidebarTabbedPane();
        reloadTabInfosFromSettings();

        // Add tabs to the JTabbedPane
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            tabbedPane.addTab(info.title, new JPanel());
            // 设置自定义 tab 组件以实现图标在上、文本在下的布局
            tabbedPane.setTabComponentAt(i, createTabComponent(visibleTabs.get(i), info.title, info.icon));
        }

        // 默认设置选中第一个标签
        if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }

        preloadInitialContent();

        // 2. 控制台日志区和底部栏
        consoleContainer = new JPanel(new BorderLayout());
        consoleContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()));
        consoleContainer.setOpaque(false);
        initBottomBar();
        consolePanel = UiSingletonFactory.getInstance(ConsolePanel.class);
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
            splitPane.setDividerSize(3);
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
            consoleContainer.add(bottomBar.leftPanel(), BorderLayout.WEST);
            consoleContainer.add(bottomBar.rightPanel(), BorderLayout.EAST);
            add(consoleContainer, BorderLayout.SOUTH);
            revalidate();
            repaint();
        }
    }

    /**
     * 初始化底部栏，包括控制台标签和版本标签
     */
    private void initBottomBar() {
        bottomBar = new SidebarBottomBar(
                sidebarExpanded,
                this::toggleSidebarExpansion,
                () -> setConsoleExpanded(true),
                this::toggleLayoutOrientation,
                this::showGlobalVariablesDialog,
                this::showCookieManagerDialog
        );
    }

    /**
     * 切换侧边栏展开/收起状态
     */
    private void toggleSidebarExpansion() {
        sidebarExpanded = !sidebarExpanded;
        SettingManager.setSidebarExpanded(sidebarExpanded);
        bottomBar.setSidebarExpanded(sidebarExpanded);
        recreateTabbedPane();
    }

    /**
     * 切换布局方向
     */
    private void toggleLayoutOrientation() {
        boolean currentVertical = SettingManager.isLayoutVertical();
        boolean newVertical = !currentVertical;
        SettingManager.setLayoutVertical(newVertical);

        bottomBar.updateLayoutToggleState(newVertical);

        // 更新所有已打开的标签页的布局
        try {
            RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
            editPanel.updateAllTabsLayout(newVertical);
        } catch (Exception e) {
            log.error("Failed to update layout for all tabs", e);
        }
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
     * 显示全局变量对话框
     */
    private void showGlobalVariablesDialog() {
        if (globalVariablesDialog == null) {
            Window window = SwingUtilities.getWindowAncestor(this);
            globalVariablesDialog = new GlobalVariablesDialog(window);
        }

        if (globalVariablesDialog.isVisible()) {
            globalVariablesDialog.toFront();
            globalVariablesDialog.requestFocus();
            return;
        }

        globalVariablesDialog.setVisible(true);
        globalVariablesDialog.toFront();
        globalVariablesDialog.requestFocus();
    }

    @Override
    public void updateUI() {
        super.updateUI();

        // Tab 标题使用语言感知字体（英文小一号）
        normalFont = getLanguageAwareFont(Font.PLAIN);
        boldFont = getLanguageAwareFont(Font.BOLD);

        resetTabLayoutCache();

        // 同步侧边栏展开状态（支持菜单栏收起展开的动态刷新）
        boolean newExpanded = SettingManager.isSidebarExpanded();
        boolean stateChanged = (this.sidebarExpanded != newExpanded);
        if (stateChanged) {
            this.sidebarExpanded = newExpanded;
            if (bottomBar != null) {
                bottomBar.setSidebarExpanded(sidebarExpanded);
            }
        }

        // 5. 重新应用自定义的 TabbedPane UI（super.updateUI() 会重置它）
        if (tabbedPane != null) {
            // 重新创建并设置自定义 UI
            recreateTabbedPaneUI();
        }

        // 6. 更新底部栏组件
        if (bottomBar != null) {
            bottomBar.refreshLocalizedText();
        }

        // 主题切换时更新 consoleContainer 的边框颜色
        if (consoleContainer != null) {
            consoleContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()));
        }

        // 7. 重新创建所有 tab 组件以更新字体和文本
        if (tabbedPane != null && tabInfos != null && visibleTabs != null) {
            int currentSelectedIndex = tabbedPane.getSelectedIndex();
            for (int i = 0; i < tabInfos.size(); i++) {
                SidebarTab sidebarTab = visibleTabs.get(i);
                TabInfo info = tabInfos.get(i);
                info.title = I18nUtil.getMessage(sidebarTab.getTitleKey());
                boolean isSelected = (i == currentSelectedIndex);
                Icon iconToUse = isSelected ? sidebarTab.getSelectedIcon() : sidebarTab.getIcon();
                tabbedPane.setTabComponentAt(i, createTabComponent(sidebarTab, info.title, iconToUse));
            }
            // 延迟执行颜色初始化，确保所有 UI 组件都创建完成
            SwingUtilities.invokeLater(() -> {
                initializeAllTabColors();
                // 再次强制刷新，确保选中状态正确显示
                tabbedPane.repaint();
            });
        }
    }

    /**
     * 重新创建 TabbedPane 的自定义 UI
     * 用于在 updateUI() 后恢复自定义的选中状态视觉效果
     */
    private void recreateTabbedPaneUI() {
        // 应用自定义 UI
        applySidebarTabbedPaneUi(tabbedPane);

        // 强制刷新 UI,确保自定义渲染立即生效
        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    @Override
    protected void registerListeners() {
        tabbedPane.addChangeListener(e -> {
            handleTabChange();
            updateTabTextColors(); // 更新所有 tab 的文字颜色
        });
        SwingUtilities.invokeLater(this::initializeAllTabColors);
        PerformanceUiWarmup.schedule();

        // 注册快捷键
        registerTabShortcuts();
    }

    public void preloadInitialContent() {
        int selectedIndex = tabbedPane != null ? tabbedPane.getSelectedIndex() : -1;
        ensureTabComponentLoaded(Math.max(selectedIndex, 0));
        initializeAllTabColors();
    }

    /**
     * 注册 Tab 快捷键
     * Ctrl/Cmd + 1-7 切换到对应的 Tab
     */
    private void registerTabShortcuts() {
        int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(); // Mac用Cmd，Windows用Ctrl

        // 为每个 Tab 注册快捷键 Ctrl/Cmd + 数字
        for (int i = 0; i < Math.min(tabInfos.size(), 9); i++) {
            final int tabIndex = i;
            int keyCode = KeyEvent.VK_1 + i; // VK_1 到 VK_9

            // 注册快捷键到输入映射
            String actionKey = "switchToTab" + (i + 1);
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(keyCode, modifier), actionKey
            );
            getActionMap().put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tabIndex < tabbedPane.getTabCount()) {
                        tabbedPane.setSelectedIndex(tabIndex);
                    }
                }
            });
        }
    }

    /**
     * 初始化所有 tab 的图标和文字颜色（在首次加载时调用，展开和收起状态都需要）
     */
    private void initializeAllTabColors() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            updateSingleTabTextColor(i, i == selectedIndex);
        }
        lastSelectedIndex = selectedIndex;
    }

    /**
     * 更新所有 tab 的图标和文字颜色（展开和收起状态都支持）
     * 优化：只更新当前和之前选中的 tab，避免遍历所有 tab
     */
    private void updateTabTextColors() {
        int selectedIndex = tabbedPane.getSelectedIndex();

        // 更新之前选中的 tab（如果存在）
        if (lastSelectedIndex >= 0 && lastSelectedIndex < tabbedPane.getTabCount()) {
            updateSingleTabTextColor(lastSelectedIndex, false);
        }

        // 更新当前选中的 tab
        if (selectedIndex >= 0 && selectedIndex < tabbedPane.getTabCount()) {
            updateSingleTabTextColor(selectedIndex, true);
        }

        // 记录当前选中的索引
        lastSelectedIndex = selectedIndex;
    }

    /**
     * 更新单个 tab 的图标和文字颜色（支持展开和收起状态）
     */
    private void updateSingleTabTextColor(int tabIndex, boolean isSelected) {
        Component tabComponent = tabbedPane.getTabComponentAt(tabIndex);
        if (!(tabComponent instanceof JPanel panel)) return;

        SidebarTab sidebarTab = getSidebarTabAt(tabIndex);
        if (sidebarTab == null) return;

        updateTabIcon(panel, sidebarTab, isSelected);
        updateTabTitle(panel, isSelected);
    }

    /**
     * 更新tab图标
     */
    private void updateTabIcon(JPanel panel, SidebarTab sidebarTab, boolean isSelected) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel label && ICON_LABEL_NAME.equals(label.getName())) {
                label.setIcon(isSelected ? sidebarTab.getSelectedIcon() : sidebarTab.getIcon());
                break;
            }
        }
    }

    /**
     * 更新tab标题
     */
    private void updateTabTitle(JPanel panel, boolean isSelected) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel label && TITLE_LABEL_NAME.equals(label.getName())) {
                if (isSelected) {
                    label.setForeground(SidebarTheme.selectedTabTitleForeground());
                    label.setFont(boldFont);
                } else {
                    label.setForeground(ModernColors.getTextSecondary());
                    label.setFont(normalFont);
                }
                break;
            }
        }
    }

    // Tab切换时才加载真正的面板内容
    private void handleTabChange() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        ensureTabComponentLoaded(selectedIndex); // 懒加载当前选中的tab内容
        SidebarTab selectedTab = getSidebarTabAt(selectedIndex);
        if (selectedTab == SidebarTab.ENVIRONMENTS) {
            Component comp = tabbedPane.getComponentAt(selectedIndex);
            if (comp instanceof EnvironmentPanel environmentPanel) {
                environmentPanel.refreshUI();
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
     * 创建侧边栏专用标签页
     */
    private JTabbedPane createSidebarTabbedPane() {
        JTabbedPane pane = new JTabbedPane(SwingConstants.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT) {
            @Override
            public void updateUI() {
                super.updateUI();
                applySidebarTabbedPaneUi(this);
            }
        };
        applySidebarTabbedPaneUi(pane);
        return pane;
    }

    private void applySidebarTabbedPaneUi(JTabbedPane pane) {
        pane.setUI(new SidebarTabbedPaneUi(
                () -> sidebarExpanded,
                this::calculateExpandedTabWidth,
                this::calculateCollapsedTabWidth,
                this::calculateExpandedTabHeight,
                this::calculateCollapsedTabHeight
        ));
    }

    /**
     * 创建自定义 Tab 组件（图标在上，文本在下）
     */
    private Component createTabComponent(SidebarTab sidebarTab, String title, Icon icon) {
        JPanel panel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                if (sidebarExpanded) {
                    // 展开状态：使用计算出的最佳宽度，基于最长文本
                    size.width = calculateExpandedTabWidth();
                } else {
                    // 收起状态：使用计算出的宽度，基于图标和边距
                    size.width = calculateCollapsedTabWidth();
                }
                return size;
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        if (sidebarExpanded) {
            // 展开状态：图标在上，文本在下
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setName(ICON_LABEL_NAME); // 标记为图标标签

            JLabel titleLabel = new JLabel(title);
            titleLabel.setName(TITLE_LABEL_NAME); // 标记为文字标签
            // 根据当前是否选中设置初始颜色
            int currentIndex = getTabIndex(sidebarTab);
            boolean isCurrentlySelected = currentIndex >= 0 && tabbedPane.getSelectedIndex() == currentIndex;

            if (isCurrentlySelected) {
                titleLabel.setFont(boldFont);
                titleLabel.setForeground(SidebarTheme.selectedTabTitleForeground());
            } else {
                titleLabel.setFont(normalFont);
                titleLabel.setForeground(ModernColors.getTextSecondary());
            }
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(Box.createVerticalStrut(SidebarTabMetrics.EXPANDED_TAB_SPACING_TOP));
            panel.add(iconLabel);
            panel.add(Box.createVerticalStrut(SidebarTabMetrics.EXPANDED_TAB_SPACING_MIDDLE));
            panel.add(titleLabel);
            panel.add(Box.createVerticalStrut(SidebarTabMetrics.EXPANDED_TAB_SPACING_BOTTOM));

            panel.setBorder(BorderFactory.createEmptyBorder(
                    SidebarTabMetrics.EXPANDED_TAB_PADDING_VERTICAL,
                    SidebarTabMetrics.EXPANDED_TAB_PADDING_HORIZONTAL,
                    SidebarTabMetrics.EXPANDED_TAB_PADDING_VERTICAL,
                    SidebarTabMetrics.EXPANDED_TAB_PADDING_HORIZONTAL
            ));
        } else {
            // 收起状态：只显示图标，居中，增加上下左右间距
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            iconLabel.setName(ICON_LABEL_NAME); // 标记为图标标签
            // 设置 tooltip 在 panel 上而非 label 上
            panel.setToolTipText(title);

            panel.add(Box.createVerticalGlue());
            panel.add(iconLabel);
            panel.add(Box.createVerticalGlue());

            panel.setBorder(BorderFactory.createEmptyBorder(
                    SidebarTabMetrics.COLLAPSED_TAB_PADDING_VERTICAL,
                    SidebarTabMetrics.COLLAPSED_TAB_PADDING_HORIZONTAL,
                    SidebarTabMetrics.COLLAPSED_TAB_PADDING_VERTICAL,
                    SidebarTabMetrics.COLLAPSED_TAB_PADDING_HORIZONTAL
            ));
        }

        // 只为 panel 添加一个鼠标监听器
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int tabIndex = getTabIndex(sidebarTab);
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
        });

        return panel;
    }

    /**
     * 根据标题获取 tab 索引
     */
    private int getTabIndex(SidebarTab sidebarTab) {
        if (visibleTabs == null) {
            return -1;
        }
        for (int i = 0; i < visibleTabs.size(); i++) {
            if (visibleTabs.get(i) == sidebarTab) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 计算展开状态下的tab宽度
     * 基于最长的tab标题文本宽度动态计算
     */
    private int calculateExpandedTabWidth() {
        if (calculatedExpandedTabWidth > 0) {
            return calculatedExpandedTabWidth;
        }

        int maxWidth = 0;
        JLabel tempLabel = new JLabel();
        tempLabel.setFont(boldFont); // 使用粗体字体计算，因为选中时会变粗

        for (TabInfo info : tabInfos) {
            tempLabel.setText(info.title);
            int textWidth = tempLabel.getPreferredSize().width;
            maxWidth = Math.max(maxWidth, textWidth);
        }

        calculatedExpandedTabWidth = SidebarTabMetrics.expandedWidth(maxWidth);

        return calculatedExpandedTabWidth;
    }

    /**
     * 计算收起状态下的tab宽度
     * 基于最宽的图标宽度动态计算
     */
    private int calculateCollapsedTabWidth() {
        if (calculatedCollapsedTabWidth > 0) {
            return calculatedCollapsedTabWidth;
        }

        int maxIconWidth = 0;
        for (TabInfo info : tabInfos) {
            if (info.icon != null) {
                maxIconWidth = Math.max(maxIconWidth, info.icon.getIconWidth());
            }
        }

        calculatedCollapsedTabWidth = SidebarTabMetrics.collapsedWidth(maxIconWidth);

        return calculatedCollapsedTabWidth;
    }

    /**
     * 计算展开状态下的tab高度
     * 基于最高的图标高度和字体高度动态计算
     */
    private int calculateExpandedTabHeight(int fontHeight) {
        if (calculatedExpandedTabHeight > 0) {
            return calculatedExpandedTabHeight;
        }

        int maxIconHeight = 0;
        for (TabInfo info : tabInfos) {
            if (info.icon != null) {
                maxIconHeight = Math.max(maxIconHeight, info.icon.getIconHeight());
            }
        }

        calculatedExpandedTabHeight = SidebarTabMetrics.expandedHeight(maxIconHeight, fontHeight);

        return calculatedExpandedTabHeight;
    }

    /**
     * 计算收起状态下的tab高度
     * 基于图标高度和上下边距
     */
    private int calculateCollapsedTabHeight() {
        if (calculatedCollapsedTabHeight > 0) {
            return calculatedCollapsedTabHeight;
        }

        int maxIconHeight = 0;
        for (TabInfo info : tabInfos) {
            if (info.icon != null) {
                maxIconHeight = Math.max(maxIconHeight, info.icon.getIconHeight());
            }
        }

        calculatedCollapsedTabHeight = SidebarTabMetrics.collapsedHeight(maxIconHeight);

        return calculatedCollapsedTabHeight;
    }

    /**
     * 更新侧边栏展开/收起状态
     * 从设置对话框调用，用于实时更新UI
     */
    public void updateSidebarExpansion() {
        boolean newExpanded = SettingManager.isSidebarExpanded();
        if (this.sidebarExpanded != newExpanded) {
            this.sidebarExpanded = newExpanded;
            resetTabLayoutCache();
            recreateTabbedPane();
        }
    }

    public void refreshSidebarConfiguration() {
        boolean newExpanded = SettingManager.isSidebarExpanded();
        boolean expansionChanged = this.sidebarExpanded != newExpanded;
        this.sidebarExpanded = newExpanded;
        if (bottomBar != null) {
            bottomBar.setSidebarExpanded(sidebarExpanded);
        }

        List<SidebarTab> configuredTabs = SidebarTabSettingsResolver.getVisibleSidebarTabs();
        boolean tabsChanged = visibleTabs == null || !visibleTabs.equals(configuredTabs);
        if (expansionChanged || tabsChanged) {
            resetTabLayoutCache();
            recreateTabbedPane();
        }
    }

    private void resetTabLayoutCache() {
        calculatedExpandedTabWidth = -1;
        calculatedCollapsedTabWidth = -1;
        calculatedExpandedTabHeight = -1;
        calculatedCollapsedTabHeight = -1;
    }

    public boolean showTab(SidebarTab sidebarTab) {
        int tabIndex = getTabIndex(sidebarTab);
        if (tabIndex < 0 || tabIndex >= tabbedPane.getTabCount()) {
            return false;
        }
        tabbedPane.setSelectedIndex(tabIndex);
        return true;
    }

    /**
     * 重新创建标签页以应用新的展开/收起状态
     */
    private void recreateTabbedPane() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        SidebarTab selectedTab = getSidebarTabAt(selectedIndex);

        // 保存所有已加载的面板
        Map<SidebarTab, Component> loadedPanels = new EnumMap<>(SidebarTab.class);
        if (visibleTabs != null) {
            for (int i = 0; i < visibleTabs.size(); i++) {
                loadedPanels.put(visibleTabs.get(i), tabbedPane.getComponentAt(i));
            }
        }

        // 移除旧的 TabbedPane
        Component consoleComp = null;
        LayoutManager layout = getLayout();
        if (layout instanceof BorderLayout borderLayout) {
            consoleComp = borderLayout.getLayoutComponent(BorderLayout.SOUTH);
            remove(tabbedPane);
        }

        // 创建新的 TabbedPane
        tabbedPane = createSidebarTabbedPane();
        reloadTabInfosFromSettings();

        // 恢复所有 tab - 重用 tabInfos 中缓存的图标
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            SidebarTab sidebarTab = visibleTabs.get(i);
            Component content = loadedPanels.getOrDefault(sidebarTab, new JPanel());
            tabbedPane.addTab(info.title, content);
            // 设置自定义 tab 组件 - icon 从 tabInfos 获取，已经缓存
            tabbedPane.setTabComponentAt(i, createTabComponent(sidebarTab, info.title, info.icon));
        }

        int restoredIndex = getTabIndex(selectedTab);
        if (restoredIndex >= 0 && restoredIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(restoredIndex);
        } else if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(Math.min(selectedIndex, tabbedPane.getTabCount() - 1));
        }

        // 重新添加组件
        add(tabbedPane, BorderLayout.CENTER);
        if (consoleComp != null) {
            add(consoleComp, BorderLayout.SOUTH);
        }

        // 重新注册监听器
        tabbedPane.addChangeListener(e -> {
            handleTabChange();
            updateTabTextColors();
        });

        // 重置 lastSelectedIndex 避免 bug
        lastSelectedIndex = -1;

        // 更新初始文字颜色
        initializeAllTabColors();

        // 刷新UI
        revalidate();
        repaint();
    }

    private void reloadTabInfosFromSettings() {
        visibleTabs = new ArrayList<>(SidebarTabSettingsResolver.getVisibleSidebarTabs());
        tabInfos = new ArrayList<>(visibleTabs.size());
        for (SidebarTab tab : visibleTabs) {
            tabInfos.add(tab.toTabInfo());
        }
    }

    private SidebarTab getSidebarTabAt(int index) {
        if (visibleTabs == null || index < 0 || index >= visibleTabs.size()) {
            return null;
        }
        return visibleTabs.get(index);
    }

    /**
     * 根据当前语言获取合适的 Tab 标题字体
     * 英文使用小一号字体，避免 Tab 标题文本过长
     * 注意：此方法仅用于 Tab 标题，底部栏组件（Console、Cookie、版本号）使用标准字体
     *
     * @param style 字体样式 (Font.PLAIN, Font.BOLD, Font.ITALIC)
     * @return Font 对象
     */
    private Font getLanguageAwareFont(int style) {
        if (I18nUtil.isChinese()) {
            // 中文使用标准字体大小
            return FontsUtil.getDefaultFont(style);
        } else {
            // 英文使用小字体
            return FontsUtil.getDefaultFontWithOffset(style, -3);
        }
    }

}
