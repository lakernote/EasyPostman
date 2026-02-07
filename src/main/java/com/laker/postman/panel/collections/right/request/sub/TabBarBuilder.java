package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Tab栏构建器
 * 负责根据协议类型创建Tab按钮和面板
 */
public class TabBarBuilder {

    // Tab索引常量（与ResponsePanel保持一致）
    private static final int TAB_INDEX_LOG = 5;

    private TabBarBuilder() {
        // 工具类，禁止实例化
    }

    /**
     * Tab配置信息
     */
    public static class TabConfig {
        public final String[] tabNames;
        public final JButton[] tabButtons;
        public final boolean[] initialVisibility;

        public TabConfig(String[] tabNames, JButton[] tabButtons, boolean[] initialVisibility) {
            this.tabNames = tabNames;
            this.tabButtons = tabButtons;
            this.initialVisibility = initialVisibility;
        }
    }

    /**
     * 为WebSocket协议创建Tab配置
     */
    public static TabConfig createWebSocketTabs() {
        return createSSETabs();
    }

    /**
     * 为SSE协议创建Tab配置
     */
    public static TabConfig createSSETabs() {
        String[] names = new String[]{
                I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG),
                I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS)
        };
        return createTabConfig(names, null);
    }

    /**
     * 为HTTP协议创建Tab配置
     */
    public static TabConfig createHttpTabs() {
        String[] names = new String[]{
                I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_BODY),
                I18nUtil.getMessage(MessageKeys.TAB_RESPONSE_HEADERS),
                I18nUtil.getMessage(MessageKeys.TAB_TESTS),
                I18nUtil.getMessage(MessageKeys.TAB_NETWORK_LOG),
                I18nUtil.getMessage(MessageKeys.TAB_TIMING),
                I18nUtil.getMessage(MessageKeys.MENU_FILE_LOG)
        };

        // HTTP模式下，默认隐藏日志tab
        boolean[] visibility = createAllVisibleArray(names.length);
        visibility[TAB_INDEX_LOG] = false; // 隐藏日志tab

        return createTabConfig(names, visibility);
    }

    /**
     * 创建Tab配置
     *
     * @param names      tab名称数组
     * @param visibility 可见性数组，null表示全部可见
     * @return Tab配置对象
     */
    private static TabConfig createTabConfig(String[] names, boolean[] visibility) {
        JButton[] buttons = new JButton[names.length];
        boolean[] vis = visibility != null ? visibility : createAllVisibleArray(names.length);
        return new TabConfig(names, buttons, vis);
    }

    /**
     * 创建全部可见的布尔数组
     */
    private static boolean[] createAllVisibleArray(int length) {
        boolean[] result = new boolean[length];
        java.util.Arrays.fill(result, true);
        return result;
    }

    /**
     * 将Tab按钮添加到tabBar面板
     *
     * @param tabBar     目标面板
     * @param buttons    按钮数组
     * @param visibility 可见性数组
     */
    public static void addButtonsToTabBar(JPanel tabBar, JButton[] buttons, boolean[] visibility) {
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                buttons[i].setVisible(visibility[i]);
                tabBar.add(buttons[i]);
            }
        }
    }

    /**
     * 为所有tab按钮绑定点击事件
     *
     * @param buttons       按钮数组
     * @param tabNames      tab名称数组
     * @param cardPanel     卡片面板
     * @param onTabSelected tab选中时的回调
     */
    public static void bindTabActions(JButton[] buttons, String[] tabNames,
                                      JPanel cardPanel, Consumer<Integer> onTabSelected) {
        for (int i = 0; i < buttons.length; i++) {
            final int tabIndex = i;
            buttons[i].addActionListener(e -> {
                CardLayout cl = (CardLayout) cardPanel.getLayout();
                cl.show(cardPanel, tabNames[tabIndex]);
                onTabSelected.accept(tabIndex);
            });
        }
    }
}
