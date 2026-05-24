package com.laker.postman.panel.collections;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.common.exception.CancelException;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.collections.OpenedRequestsStore;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class OpenedRequestTabsSaver {

    public static void saveOpenTabsOnExit() {
        RequestEditPanel editPanel = UiSingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();
        List<Integer> unsavedTabs = findUnsavedTabs(tabbedPane);

        boolean saveAll = false;
        if (!unsavedTabs.isEmpty()) {
            int result = showUnsavedChangesDialog();
            if (result == 2 || result == JOptionPane.CLOSED_OPTION) {
                throw new CancelException();
            }
            if (result == 0) {
                saveUnsavedTabs(editPanel, tabbedPane, unsavedTabs);
                saveAll = true;
            }
        }

        List<HttpRequestItem> openedRequestItems = collectOpenedRequestItems(tabbedPane, saveAll);
        OpenedRequestsStore.saveAll(limitOpenedRequests(openedRequestItems));
    }

    private static List<Integer> findUnsavedTabs(JTabbedPane tabbedPane) {
        List<Integer> unsavedTabs = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComp = tabbedPane.getTabComponentAt(i);
            if (tabComp instanceof ClosableTabComponent closable && closable.isDirty()) {
                unsavedTabs.add(i);
            }
        }
        return unsavedTabs;
    }

    private static int showUnsavedChangesDialog() {
        String[] options = {
                I18nUtil.getMessage(MessageKeys.EXIT_SAVE_ALL),
                I18nUtil.getMessage(MessageKeys.EXIT_DISCARD_ALL),
                I18nUtil.getMessage(MessageKeys.EXIT_CANCEL)
        };
        return JOptionPane.showOptionDialog(UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.EXIT_UNSAVED_CHANGES),
                I18nUtil.getMessage(MessageKeys.EXIT_UNSAVED_CHANGES_TITLE),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);
    }

    private static void saveUnsavedTabs(RequestEditPanel editPanel, JTabbedPane tabbedPane, List<Integer> unsavedTabs) {
        for (Integer i : unsavedTabs) {
            tabbedPane.setSelectedIndex(i);
            editPanel.saveCurrentRequest();
        }
    }

    private static List<HttpRequestItem> collectOpenedRequestItems(JTabbedPane tabbedPane, boolean saveAll) {
        List<HttpRequestItem> openedRequestItems = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComp = tabbedPane.getTabComponentAt(i);
            Component comp = tabbedPane.getComponentAt(i);
            if (tabComp instanceof ClosableTabComponent closable && comp instanceof RequestEditSubPanel subPanel) {
                if (subPanel.isSavedResponseTab()) {
                    continue;
                }

                HttpRequestItem item;
                if (closable.isNewRequest()) {
                    item = subPanel.getCurrentRequest();
                } else if (closable.isDirty() && !saveAll) {
                    item = subPanel.getOriginalRequestItem();
                } else {
                    item = subPanel.getCurrentRequest();
                }
                openedRequestItems.add(item);
            }
        }
        return openedRequestItems;
    }

    private static List<HttpRequestItem> limitOpenedRequests(List<HttpRequestItem> openedRequestItems) {
        int maxOpenedRequests = SettingManager.getMaxOpenedRequestsCount();
        if (openedRequestItems.size() <= maxOpenedRequests) {
            return openedRequestItems;
        }
        return new ArrayList<>(openedRequestItems.subList(
                openedRequestItems.size() - maxOpenedRequests,
                openedRequestItems.size()
        ));
    }
}
