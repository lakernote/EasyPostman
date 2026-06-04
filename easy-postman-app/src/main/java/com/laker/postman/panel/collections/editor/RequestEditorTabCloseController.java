package com.laker.postman.panel.collections.editor;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/**
 * 请求编辑器 Tab 关闭控制器。
 * <p>
 * 关闭前保存确认、批量关闭顺序和删除后的选中恢复都是行为编排，统一放在控制层，面板只暴露入口。
 */
@RequiredArgsConstructor
final class RequestEditorTabCloseController {
    private final JTabbedPane tabbedPane;
    private final IntPredicate plusTabPredicate;
    private final Runnable currentRequestSaveAction;
    private final IntConsumer tabRemover;
    private final Consumer<Component> componentRemover;

    void closeCurrentTab() {
        int currentIndex = tabbedPane.getSelectedIndex();
        if (currentIndex < 0 || plusTabPredicate.test(currentIndex)) {
            return;
        }

        Component component = tabbedPane.getComponentAt(currentIndex);
        if (!confirmBeforeRemoving(component, MessageKeys.TAB_UNSAVED_CHANGES_SAVE_CURRENT, false)) {
            return;
        }

        tabRemover.accept(currentIndex);
        restoreLastRequestTabSelectionIfNeeded();
    }

    void closeOtherTabs() {
        int currentIndex = tabbedPane.getSelectedIndex();
        if (currentIndex < 0 || plusTabPredicate.test(currentIndex)) {
            return;
        }

        Component currentComponent = tabbedPane.getComponentAt(currentIndex);
        List<Component> removableComponents = findOtherClosableComponents(currentIndex);
        for (Component component : removableComponents) {
            if (!confirmBeforeRemoving(component, MessageKeys.TAB_UNSAVED_CHANGES_SAVE_OTHERS, true)) {
                return;
            }
            componentRemover.accept(component);
        }

        int currentComponentIndex = tabbedPane.indexOfComponent(currentComponent);
        if (currentComponentIndex >= 0) {
            tabbedPane.setSelectedIndex(currentComponentIndex);
        }
    }

    void closeAllTabs() {
        List<Component> removableComponents = findAllClosableComponents();
        for (Component component : removableComponents) {
            if (!confirmBeforeRemoving(component, MessageKeys.TAB_UNSAVED_CHANGES_SAVE_ALL, true)) {
                return;
            }
            componentRemover.accept(component);
        }
    }

    private List<Component> findOtherClosableComponents(int currentIndex) {
        List<Component> removableComponents = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (i != currentIndex && !(component instanceof RequestEditorEmptyStatePanel)) {
                removableComponents.add(component);
            }
        }
        return removableComponents;
    }

    private List<Component> findAllClosableComponents() {
        List<Component> removableComponents = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (!(component instanceof RequestEditorEmptyStatePanel)) {
                removableComponents.add(component);
            }
        }
        return removableComponents;
    }

    private boolean confirmBeforeRemoving(Component component, String confirmMessageKey, boolean selectComponentBeforeSave) {
        if (!(component instanceof RequestEditSubPanel requestEditSubPanel) || !requestEditSubPanel.isModified()) {
            return true;
        }

        int componentIndex = tabbedPane.indexOfComponent(component);
        int result = JOptionPane.showConfirmDialog(UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(confirmMessageKey),
                I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_TITLE),
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.CANCEL_OPTION) {
            selectTabIfPresent(componentIndex);
            return false;
        }
        if (result == JOptionPane.YES_OPTION) {
            if (selectComponentBeforeSave) {
                selectTabIfPresent(componentIndex);
            }
            currentRequestSaveAction.run();
        }
        return true;
    }

    private void restoreLastRequestTabSelectionIfNeeded() {
        int count = tabbedPane.getTabCount();
        if (count <= 1) {
            return;
        }
        int selected = tabbedPane.getSelectedIndex();
        if (selected == -1 || selected == count - 1) {
            tabbedPane.setSelectedIndex(count - 2);
        }
    }

    private void selectTabIfPresent(int tabIndex) {
        if (tabIndex >= 0 && tabIndex < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(tabIndex);
        }
    }
}
