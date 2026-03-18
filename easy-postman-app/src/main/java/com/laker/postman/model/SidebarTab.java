package com.laker.postman.model;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.panel.collections.RequestCollectionsPanel;
import com.laker.postman.panel.env.EnvironmentPanel;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.history.HistoryPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import com.laker.postman.panel.toolbox.ToolboxPanel;
import com.laker.postman.panel.workspace.WorkspacePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 侧边栏 Tab 枚举
 * 集中管理所有 Tab 的配置信息，避免重复代码
 */
@Getter
public enum SidebarTab {
    COLLECTIONS(
            MessageKeys.MENU_COLLECTIONS,
            "icons/collections.svg",
            () -> SingletonFactory.getInstance(RequestCollectionsPanel.class)
    ),
    ENVIRONMENTS(
            MessageKeys.MENU_ENVIRONMENTS,
            "icons/environments.svg",
            () -> SingletonFactory.getInstance(EnvironmentPanel.class)
    ),
    WORKSPACES(
            MessageKeys.MENU_WORKSPACES,
            "icons/workspace.svg",
            () -> SingletonFactory.getInstance(WorkspacePanel.class)
    ),
    FUNCTIONAL(
            MessageKeys.MENU_FUNCTIONAL,
            "icons/functional.svg",
            () -> SingletonFactory.getInstance(FunctionalPanel.class)
    ),
    PERFORMANCE(
            MessageKeys.MENU_PERFORMANCE,
            "icons/performance.svg",
            () -> SingletonFactory.getInstance(PerformancePanel.class)
    ),
    TOOLBOX(
            MessageKeys.MENU_TOOLBOX,
            "icons/tools.svg",
            () -> SingletonFactory.getInstance(ToolboxPanel.class)
    ),
    HISTORY(
            MessageKeys.MENU_HISTORY,
            "icons/history.svg",
            () -> SingletonFactory.getInstance(HistoryPanel.class)
    );

    private final String titleKey;
    private final String iconPath;
    private final Supplier<JPanel> panelSupplier;

    // 懒加载缓存
    private FlatSVGIcon icon;
    private FlatSVGIcon selectedIcon;  // 选中状态的图标（带颜色）
    private String title;
    private JPanel panel;

    SidebarTab(String titleKey, String iconPath, Supplier<JPanel> panelSupplier) {
        this.titleKey = titleKey;
        this.iconPath = iconPath;
        this.panelSupplier = panelSupplier;
    }

    /**
     * 获取标题（懒加载，支持国际化）
     */
    public String getTitle() {
        if (title == null) {
            title = I18nUtil.getMessage(titleKey);
        }
        return title;
    }

    /**
     * 获取当前语言下的标题，不使用缓存，便于设置页和运行时刷新。
     */
    public String getDisplayTitle() {
        return I18nUtil.getMessage(titleKey);
    }

    /**
     * 获取图标（懒加载，避免重复创建）- 普通状态
     */
    public Icon getIcon() {
        if (icon == null) {
            icon = IconUtil.createThemed(iconPath, IconUtil.SIZE_TAB, IconUtil.SIZE_TAB);
        }
        return icon;
    }

    /**
     * 获取选中状态的图标（懒加载，带主题色）
     * 使用 ModernColors.PRIMARY 确保颜色一致性
     */
    public Icon getSelectedIcon() {
        if (selectedIcon == null) {
            selectedIcon = IconUtil.createPrimary(iconPath, IconUtil.SIZE_TAB, IconUtil.SIZE_TAB);
        }
        return selectedIcon;
    }

    /**
     * 获取面板（懒加载）
     */
    public JPanel getPanel() {
        if (panel == null) {
            panel = panelSupplier.get();
        }
        return panel;
    }

    /**
     * 转换为 TabInfo 对象（为了兼容现有代码）
     */
    public TabInfo toTabInfo() {
        return new TabInfo(getDisplayTitle(), getIcon(), this::getPanel);
    }

    public static SidebarTab fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (SidebarTab tab : values()) {
            if (tab.name().equalsIgnoreCase(name.trim())) {
                return tab;
            }
        }
        return null;
    }

    public static List<SidebarTab> resolveOrderedTabs(List<String> orderedNames) {
        List<SidebarTab> resolvedTabs = new ArrayList<>();
        Set<SidebarTab> addedTabs = new HashSet<>();

        if (orderedNames != null) {
            for (String orderedName : orderedNames) {
                SidebarTab tab = fromName(orderedName);
                if (tab != null && addedTabs.add(tab)) {
                    resolvedTabs.add(tab);
                }
            }
        }

        for (SidebarTab tab : values()) {
            if (addedTabs.add(tab)) {
                resolvedTabs.add(tab);
            }
        }

        return resolvedTabs;
    }

    public static List<SidebarTab> resolveVisibleTabs(List<String> orderedNames, Set<String> hiddenNames) {
        List<SidebarTab> orderedTabs = resolveOrderedTabs(orderedNames);
        Set<String> normalizedHiddenNames = new HashSet<>();
        if (hiddenNames != null) {
            for (String hiddenName : hiddenNames) {
                if (hiddenName != null && !hiddenName.isBlank()) {
                    normalizedHiddenNames.add(hiddenName.trim().toUpperCase());
                }
            }
        }

        List<SidebarTab> visibleTabs = new ArrayList<>();
        for (SidebarTab tab : orderedTabs) {
            if (!normalizedHiddenNames.contains(tab.name())) {
                visibleTabs.add(tab);
            }
        }

        return visibleTabs.isEmpty() ? new ArrayList<>(List.of(values())) : visibleTabs;
    }

}
