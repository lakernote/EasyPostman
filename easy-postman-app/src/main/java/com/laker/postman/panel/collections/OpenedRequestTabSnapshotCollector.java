package com.laker.postman.panel.collections;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import lombok.experimental.UtilityClass;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
class OpenedRequestTabSnapshotCollector {

    static List<Integer> findUnsavedTabs(JTabbedPane tabbedPane) {
        List<Integer> unsavedTabs = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component tabComp = tabbedPane.getTabComponentAt(i);
            if (tabComp instanceof ClosableTabComponent closable && closable.isDirty()) {
                unsavedTabs.add(i);
            }
        }
        return unsavedTabs;
    }

    static List<HttpRequestItem> collectOpenedRequestItems(JTabbedPane tabbedPane, boolean saveAll) {
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

    static List<HttpRequestItem> limitToMostRecent(List<HttpRequestItem> openedRequestItems, int maxOpenedRequests) {
        if (openedRequestItems == null || openedRequestItems.isEmpty() || maxOpenedRequests <= 0) {
            return List.of();
        }
        if (openedRequestItems.size() <= maxOpenedRequests) {
            return openedRequestItems;
        }
        return new ArrayList<>(openedRequestItems.subList(
                openedRequestItems.size() - maxOpenedRequests,
                openedRequestItems.size()
        ));
    }
}
