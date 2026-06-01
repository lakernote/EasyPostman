package com.laker.postman.panel.collections.tree;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;

@UtilityClass
class SavedRequestUiSynchronizer {

    void syncRequest(HttpRequestItem updatedItem) {
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        JTabbedPane tabbedPane = editPanel.getTabbedPane();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                HttpRequestItem tabItem = subPanel.getCurrentRequest();
                if (tabItem != null && updatedItem.getId().equals(tabItem.getId())) {
                    editPanel.updateTabDirty(subPanel, false);
                    subPanel.setOriginalRequestItem(updatedItem);
                }
            }
        }

        UiSingletonFactory.getInstance(com.laker.postman.panel.functional.FunctionalPanel.class)
                .syncRequestItem(updatedItem);
        UiSingletonFactory.getInstance(com.laker.postman.panel.performance.PerformancePanel.class)
                .syncRequestItem(updatedItem);
    }
}
