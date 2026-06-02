package com.laker.postman.panel.sidebar;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.performance.PerformanceUiWarmup;
import com.laker.postman.panel.sidebar.cookie.CookieManagerDialog;
import com.laker.postman.panel.sidebar.global.GlobalVariablesDialog;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


/**
 * 左侧标签页面板
 */
@Slf4j
public class SidebarTabPanel extends UiSingletonPanel {

    @Getter
    private JTabbedPane tabbedPane;
    private transient List<TabInfo> tabInfos;
    private transient List<SidebarTab> visibleTabs;
    private SidebarBottomBar bottomBar;
    private SidebarConsoleArea consoleArea;
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
    private SidebarTabComponentFactory tabComponentFactory;

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
            tabbedPane.addTab(info.getTitle(), new JPanel());
            // 设置自定义 tab 组件以实现图标在上、文本在下的布局
            tabbedPane.setTabComponentAt(i, tabComponentFactory().create(visibleTabs.get(i), info.getTitle(), info.getIcon()));
        }

        // 默认设置选中第一个标签
        if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(0);
        }

        preloadInitialContent();

        bottomBar = createBottomBar();
        consoleArea = new SidebarConsoleArea(this, bottomBar);
        consoleArea.setTabbedPane(tabbedPane);
    }

    private SidebarBottomBar createBottomBar() {
        return new SidebarBottomBar(
                sidebarExpanded,
                this::toggleSidebarExpansion,
                this::expandConsoleArea,
                this::toggleLayoutOrientation,
                this::showGlobalVariablesDialog,
                this::showCookieManagerDialog
        );
    }

    private void expandConsoleArea() {
        if (consoleArea != null) {
            consoleArea.expand();
        }
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

        if (consoleArea != null) {
            consoleArea.refreshTheme();
        }

        // 7. 重新创建所有 tab 组件以更新字体和文本
        if (tabbedPane != null && tabInfos != null && visibleTabs != null) {
            int currentSelectedIndex = tabbedPane.getSelectedIndex();
            for (int i = 0; i < tabInfos.size(); i++) {
                SidebarTab sidebarTab = visibleTabs.get(i);
                TabInfo info = tabInfos.get(i);
                info.setTitle(I18nUtil.getMessage(sidebarTab.getTitleKey()));
                boolean isSelected = (i == currentSelectedIndex);
                Icon iconToUse = isSelected ? sidebarTab.getSelectedIcon() : sidebarTab.getIcon();
                tabbedPane.setTabComponentAt(i, tabComponentFactory().create(sidebarTab, info.getTitle(), iconToUse));
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

    private void preloadInitialContent() {
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
        SidebarTab sidebarTab = getSidebarTabAt(tabIndex);
        if (sidebarTab == null) return;

        tabComponentFactory().updateSelection(tabComponent, sidebarTab, isSelected);
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

    private SidebarTabComponentFactory tabComponentFactory() {
        if (tabComponentFactory == null) {
            tabComponentFactory = new SidebarTabComponentFactory(
                    tabbedPane,
                    () -> sidebarExpanded,
                    this::findTabIndex,
                    this::calculateExpandedTabWidth,
                    this::calculateCollapsedTabWidth,
                    () -> normalFont,
                    () -> boldFont
            );
        }
        return tabComponentFactory;
    }

    private int findTabIndex(SidebarTab sidebarTab) {
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
            tempLabel.setText(info.getTitle());
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
            if (info.getIcon() != null) {
                maxIconWidth = Math.max(maxIconWidth, info.getIcon().getIconWidth());
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
            if (info.getIcon() != null) {
                maxIconHeight = Math.max(maxIconHeight, info.getIcon().getIconHeight());
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
            if (info.getIcon() != null) {
                maxIconHeight = Math.max(maxIconHeight, info.getIcon().getIconHeight());
            }
        }

        calculatedCollapsedTabHeight = SidebarTabMetrics.collapsedHeight(maxIconHeight);

        return calculatedCollapsedTabHeight;
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
        int tabIndex = findTabIndex(sidebarTab);
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

        // 创建新的 TabbedPane
        tabbedPane = createSidebarTabbedPane();
        tabComponentFactory = null;
        reloadTabInfosFromSettings();

        // 恢复所有 tab - 重用 tabInfos 中缓存的图标
        for (int i = 0; i < tabInfos.size(); i++) {
            TabInfo info = tabInfos.get(i);
            SidebarTab sidebarTab = visibleTabs.get(i);
            Component content = loadedPanels.getOrDefault(sidebarTab, new JPanel());
            tabbedPane.addTab(info.getTitle(), content);
            // 设置自定义 tab 组件 - icon 从 tabInfos 获取，已经缓存
            tabbedPane.setTabComponentAt(i, tabComponentFactory().create(sidebarTab, info.getTitle(), info.getIcon()));
        }

        int restoredIndex = findTabIndex(selectedTab);
        if (restoredIndex >= 0 && restoredIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(restoredIndex);
        } else if (tabbedPane.getTabCount() > 0) {
            tabbedPane.setSelectedIndex(Math.min(selectedIndex, tabbedPane.getTabCount() - 1));
        }

        if (consoleArea != null) {
            consoleArea.setTabbedPane(tabbedPane);
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
