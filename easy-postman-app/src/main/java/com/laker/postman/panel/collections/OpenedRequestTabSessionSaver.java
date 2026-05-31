package com.laker.postman.panel.collections;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.exception.CancelException;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.service.collections.OpenedRequestTabsStore;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.util.List;

@UtilityClass
public class OpenedRequestTabSessionSaver {

    public static void saveOpenTabsOnExit() {
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();
        List<Integer> unsavedTabs = OpenedRequestTabSnapshotCollector.findUnsavedTabs(tabbedPane);

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

        List<HttpRequestItem> openedRequestItems =
                OpenedRequestTabSnapshotCollector.collectOpenedRequestItems(tabbedPane, saveAll);
        OpenedRequestTabsStore.saveAll(OpenedRequestTabSnapshotCollector.limitToMostRecent(
                openedRequestItems,
                SettingManager.getMaxOpenedRequestsCount()
        ));
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

    private static void saveUnsavedTabs(RequestEditorPanel editPanel, JTabbedPane tabbedPane, List<Integer> unsavedTabs) {
        for (Integer i : unsavedTabs) {
            tabbedPane.setSelectedIndex(i);
            editPanel.saveCurrentRequest();
        }
    }
}
