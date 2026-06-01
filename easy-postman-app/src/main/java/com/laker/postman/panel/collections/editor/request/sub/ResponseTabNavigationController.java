package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.EasyComboBox;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 响应区域 Tab 导航控制器。
 * <p>
 * 负责按钮选中态、横向布局下拉框、HTTP/SSE 动态标签切换；ResponsePanel 只保留响应内容编排。
 */
@RequiredArgsConstructor
final class ResponseTabNavigationController {
    private static final int TAB_INDEX_RESPONSE_BODY = 0;
    private static final int TAB_INDEX_LOG = 5;

    private final JPanel topResponseBar;
    private final JPanel tabBar;
    private final Component statusBar;
    private final JPanel cardPanel;
    private final JButton[] tabButtons;
    private final String[] tabNames;

    private EasyComboBox<String> tabComboBox;
    private int selectedTabIndex;
    private boolean horizontalLayout;

    void initializeFirstVisibleTab() {
        for (int i = 0; i < tabButtons.length; i++) {
            if (tabButtons[i].isVisible() && tabButtons[i] instanceof ModernTabButton modernTabButton) {
                modernTabButton.updateSelectedIndex(i);
                selectedTabIndex = i;
                break;
            }
        }
    }

    void bindTabActions() {
        TabBarBuilder.bindTabActions(tabButtons, tabNames, cardPanel, this::onTabSelected);
    }

    void installInitialLayout(boolean horizontalLayout) {
        this.horizontalLayout = horizontalLayout;
        if (horizontalLayout) {
            topResponseBar.remove(tabBar);
            topResponseBar.add(createComboPanel(), BorderLayout.WEST);
        }
    }

    void setEnabled(boolean enabled) {
        for (JButton button : tabButtons) {
            button.setEnabled(enabled);
        }
        if (tabComboBox != null) {
            tabComboBox.setEnabled(enabled);
        }
    }

    boolean enabledStateMatches(boolean enabled) {
        return tabButtons.length > 0
                && tabButtons[0].isEnabled() == enabled
                && (tabComboBox == null || tabComboBox.isEnabled() == enabled);
    }

    void switchHttpOrSse(String type) {
        if ("http".equals(type)) {
            showHttpTabs();
            return;
        }
        showSseTabs();
    }

    void switchToTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= tabButtons.length) {
            return;
        }
        if (tabButtons[tabIndex].isVisible() && tabButtons[tabIndex].isEnabled()) {
            tabButtons[tabIndex].doClick();
        }
    }

    void updateLayoutOrientation(boolean vertical) {
        boolean newHorizontalLayout = !vertical;
        if (horizontalLayout == newHorizontalLayout) {
            return;
        }
        horizontalLayout = newHorizontalLayout;

        topResponseBar.removeAll();
        if (horizontalLayout) {
            topResponseBar.add(createComboPanel(), BorderLayout.WEST);
        } else {
            topResponseBar.add(tabBar, BorderLayout.WEST);
        }
        topResponseBar.add(statusBar, BorderLayout.EAST);
        topResponseBar.revalidate();
        topResponseBar.repaint();
    }

    private void onTabSelected(int tabIndex) {
        selectedTabIndex = tabIndex;
        for (JButton button : tabButtons) {
            if (button instanceof ModernTabButton modernTabButton) {
                modernTabButton.updateSelectedIndex(selectedTabIndex);
            }
        }
        syncComboBoxSelection();
    }

    private void showHttpTabs() {
        tabButtons[TAB_INDEX_RESPONSE_BODY].setVisible(true);
        tabButtons[TAB_INDEX_LOG].setVisible(false);
        refreshTabSelector();
        selectVisibleTabIfNeeded(TAB_INDEX_RESPONSE_BODY);
    }

    private void showSseTabs() {
        tabButtons[TAB_INDEX_RESPONSE_BODY].setVisible(false);
        tabButtons[TAB_INDEX_LOG].setVisible(true);
        refreshTabSelector();
        selectVisibleTabIfNeeded(TAB_INDEX_LOG);
    }

    private void selectVisibleTabIfNeeded(int fallbackTabIndex) {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabButtons.length && tabButtons[selectedTabIndex].isVisible()) {
            showCard(selectedTabIndex);
            return;
        }
        if (fallbackTabIndex >= 0 && fallbackTabIndex < tabButtons.length && tabButtons[fallbackTabIndex].isVisible()) {
            if (selectedTabIndex == fallbackTabIndex) {
                showCard(fallbackTabIndex);
                return;
            }
            tabButtons[fallbackTabIndex].doClick();
        }
    }

    private JPanel createComboPanel() {
        ensureComboBox();
        refreshTabSelector();
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        comboPanel.add(tabComboBox);
        return comboPanel;
    }

    private void ensureComboBox() {
        if (tabComboBox != null) {
            return;
        }
        tabComboBox = new EasyComboBox<>(visibleTabNames(), EasyComboBox.WidthMode.DYNAMIC);
        tabComboBox.setSelectedIndex(visibleTabIndex(selectedTabIndex));
        tabComboBox.setEnabled(tabButtons.length > 0 && tabButtons[0].isEnabled());
        tabComboBox.addActionListener(e -> {
            int selectedVisibleIndex = tabComboBox.getSelectedIndex();
            int actualIndex = actualTabIndex(selectedVisibleIndex);
            if (actualIndex != selectedTabIndex) {
                selectedTabIndex = actualIndex;
                showCard(actualIndex);
            }
        });
    }

    private void refreshTabSelector() {
        if (tabComboBox == null) {
            return;
        }
        String[] visibleNames = visibleTabNames();
        tabComboBox.removeAllItems();
        for (String name : visibleNames) {
            tabComboBox.addItem(name);
        }
        int visibleIndex = Math.min(visibleTabIndex(selectedTabIndex), Math.max(visibleNames.length - 1, 0));
        if (visibleNames.length > 0) {
            tabComboBox.setSelectedIndex(visibleIndex);
        }
        tabComboBox.setEnabled(tabButtons.length > 0 && tabButtons[0].isEnabled());
    }

    private void syncComboBoxSelection() {
        if (tabComboBox == null) {
            return;
        }
        int visibleIndex = visibleTabIndex(selectedTabIndex);
        if (visibleIndex >= 0 && visibleIndex < tabComboBox.getItemCount()) {
            tabComboBox.setSelectedIndex(visibleIndex);
        }
    }

    private String[] visibleTabNames() {
        List<String> visibleNames = new ArrayList<>();
        for (int i = 0; i < tabButtons.length; i++) {
            if (tabButtons[i].isVisible()) {
                visibleNames.add(tabNames[i]);
            }
        }
        return visibleNames.toArray(new String[0]);
    }

    private int visibleTabIndex(int actualIndex) {
        int visibleIndex = 0;
        for (int i = 0; i < actualIndex && i < tabButtons.length; i++) {
            if (tabButtons[i].isVisible()) {
                visibleIndex++;
            }
        }
        return visibleIndex;
    }

    private int actualTabIndex(int visibleIndex) {
        int count = 0;
        for (int i = 0; i < tabButtons.length; i++) {
            if (tabButtons[i].isVisible()) {
                if (count == visibleIndex) {
                    return i;
                }
                count++;
            }
        }
        return 0;
    }

    private void showCard(int tabIndex) {
        CardLayout cardLayout = (CardLayout) cardPanel.getLayout();
        cardLayout.show(cardPanel, tabNames[tabIndex]);
    }
}
